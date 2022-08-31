(ns dev.onionpancakes.hop.client
  (:refer-clojure :exclude [send])
  (:require [clojure.string :refer [upper-case lower-case]])
  (:import [java.nio.file Path]
           [java.net URI]
           [java.net.http
            HttpClient HttpClient$Builder
            HttpClient$Redirect HttpClient$Version 
            HttpRequest HttpRequest$Builder
            HttpRequest$BodyPublishers HttpRequest$BodyPublisher
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers
            HttpHeaders]
           [java.util.concurrent CompletableFuture]
           [java.util.function Function]
           [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]))

;; Request

(defprotocol Uri
  (to-uri [this]))

(extend-protocol Uri
  String
  (to-uri [this] (URI. this))
  URI
  (to-uri [this] this))

(defprotocol BodyPublisher
  (to-body-publisher [this]))

(extend-protocol IBodyPublisher
  (Class/forName "[B")
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  nil
  (to-body-publisher [_]
    (HttpRequest$BodyPublishers/noBody))
  Path
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile this))
  HttpRequest$BodyPublisher
  (to-body-publisher [this] this))

(defprotocol Request
  (to-request [this]))

(defn ^HttpRequest$Builder set-request-builder-headers
  [^HttpRequest$Builder builder headers]
  (doseq [[k values] headers
          :let       [header (name k)]
          value      values]
    (.setHeader builder header value))
  builder)

(defn map-request
  [{:keys [uri method body headers]}]
  (cond-> (HttpRequest/newBuilder)
    uri     (.uri (to-uri uri))
    method  (.method (upper-case (name method)) (to-body-publisher body))
    headers (set-request-builder-headers headers)
    true    (.build)))

(defn uri-request
  [u]
  (.. (HttpRequest/newBuilder)
      (uri (to-uri u))
      (build)))

(extend-protocol Request
  java.util.Map
  (to-request [this]
    (map-request this))
  String
  (to-request [this]
    (uri-request))
  URI
  (to-request [this]
    (uri-request this))
  HttpRequest
  (to-request [this] this))

;; Response

(def headers-xf
  (map (juxt key (comp vec val))))

;; Parsing mimetype and charset is intentionally not to spec.
;; AKA, good enough for now.
;;
;; The spec compliant parsing is left to the user.
;; Refer to this for correct spec here:
;; https://www.w3.org/Protocols/rfc1341/4_Content-Type.html

(def parse-mimetype-regex
  ;; Parses mimetype as #"^ <type> / <subtype> ;|$".
  ;; Conditions:
  ;;  - Must begin at the start of string.
  ;;  - May have spaces inbetween any of the tokens.
  ;;  - Must have one foward slash inbetween types.
  ;;  - Terminated by either semi-colon or end of string.
  #"^\s*+([^\s;/]++)\s*+/\s*+([^\s;/]++)\s*+(?:;|$)")

(defn parse-mimetype
  "Parse the mimetype from a content-type string. Truncates all whitespace."
  [s]
  (when-let [parse (and s (re-find parse-mimetype-regex s))]
    (str (second parse) "/" (nth parse 2))))

(def parse-charset-regex
  ;; Parses charset as #" charset = <encoding> ".
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-charset-encoding
  "Parse the charset encoding from a content-type string."
  [s]
  (when s
    (second (re-find parse-charset-regex s))))

(defn response-map
  "Creates a map from HttpResponse."
  [^HttpResponse response]
  (let [headers          (.headers response)
        content-encoding (.. headers (firstValue "content-encoding") (orElse nil))
        content-type     (.. headers (firstValue "content-type") (orElse nil))]
    {:status           (.statusCode response)
     :headers          (into {} headers-xf (.map headers))
     :body             (.body response)
     :content-encoding content-encoding
     :content-type     content-type
     :mimetype         (parse-mimetype content-type)
     :charset          (parse-charset-encoding content-type)}))

;; Client

(def redirect-keys
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def version-keys
  {:http    HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def proxy-keys
  {:no-proxy HttpClient$Builder/NO_PROXY})

(defn ^HttpClient client
  "Creates a HttpClient."
  ([] (client nil))
  ([{:keys [authenticator
            connect-timeout
            cookie-handler
            executor
            follow-redirects
            priority
            proxy-selector
            ssl-context
            ssl-parameters
            version]}]
   (cond-> (HttpClient/newBuilder)
     authenticator    (.authenticator authenticator)
     connect-timeout  (.connectTimeout connect-timeout)
     cookie-handler   (.cookieHandler cookie-handler)
     executor         (.executor executor)
     follow-redirects (.followRedirects (redirect-keys follow-redirects follow-redirects))
     priority         (.priority priority)
     proxy-selector   (.proxy (proxy-keys proxy-selector proxy-selector))
     ssl-context      (.sslContext ssl-context)
     ssl-parameters   (.sslParameters ssl-parameters)
     version          (.version (version-keys version version))
     true             (.build))))

;; Send

(def default-client
  "Delayed default instance of HttpClient."
  (delay (client {:connect-timeout  (java.time.Duration/ofMinutes 5)
                  :follow-redirects :normal})))

(def body-handler-keys
  {:byte-array   (HttpResponse$BodyHandlers/ofByteArray)
   :discarding   (HttpResponse$BodyHandlers/discarding)
   :input-stream (HttpResponse$BodyHandlers/ofInputStream)
   :string       (HttpResponse$BodyHandlers/ofString)})

(defn send-with
  "Send request with given client."
  [^HttpClient client request body-handler]
  (-> client
      (.send (to-request request) (body-handler-keys body-handler body-handler))
      (response-map)))

(defn send
  "Send request with default client."
  [request body-handler]
  (send-with @default-client request body-handler))

(def ^Function response-map-function
  (reify Function
    (apply [_ response]
      (response-map response))))

(defn ^CompletableFuture send-async-with
  "Send async request with given client, returning a CompletableFuture."
  [^HttpClient client request body-handler]
  (-> client
      (.sendAsync (to-request request) (body-handler-keys body-handler body-handler))
      (.thenApply response-map-function)))

(defn ^CompletableFuture send-async
  "Send async request with default client, returning a CompletableFuture."
  [request body-handler]
  (send-async-with @default-client request body-handler))
