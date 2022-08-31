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

(defprotocol IUri
  (uri [this]))

(extend-protocol IUri
  String
  (uri [this] (URI. this))
  URI
  (uri [this] this))

(defprotocol IBodyPublisher
  (body-publisher [this]))

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

(defprotocol IHttpRequest
  (http-request [this]))

(defn set-http-request-builder-headers ^HttpRequest$Builder
  [^HttpRequest$Builder builder headers]
  (doseq [[k values] headers
          :let       [header (name k)]
          value      values]
    (.setHeader builder header value))
  builder)

(extend-protocol IHttpRequest
  java.util.Map
  (http-request [{:keys [uri method body headers]}]
    (cond-> (HttpRequest/newBuilder)
      uri     (.uri uri)
      method  (.method (upper-case (name method)) (body-publisher body))
      headers (set-http-request-builder-headers headers)
      true    (.build))))

;; Response

(def map-of-vecs-xf
  (map (juxt key (comp vec val))))

(defn map-of-vecs
  [m]
  (into {} map-of-vecs-xf m))

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
     :headers          (map-of-vecs (.map headers))
     :body             (.body response)
     :content-encoding content-encoding
     :content-type     content-type
     :mimetype         (parse-mimetype content-type)
     :charset          (parse-charset-encoding content-type)}))

;; Client

(def follow-redirect-keys
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def http-version-keys
  {:http    HttpClient$Version/HTTP_1_1
   :http1.1 HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def proxy-selector-keys
  {:no-proxy HttpClient$Builder/NO_PROXY})

(defn ^HttpClient http-client
  "Creates a HttpClient."
  ([] (http-client nil))
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
     follow-redirects (.followRedirects (follow-redirect-keys follow-redirects follow-redirects))
     priority         (.priority priority)
     proxy-selector   (.proxy (proxy-selector-keys proxy-selector proxy-selector))
     ssl-context      (.sslContext ssl-context)
     ssl-parameters   (.sslParameters ssl-parameters)
     version          (.version (http-version-keys version version))
     true             (.build))))

;; Send

(def default-http-client
  "Delayed default instance of HttpClient."
  (delay (http-client {:connect-timeout  (java.time.Duration/ofMinutes 5)
                       :follow-redirects :normal})))

(def body-handler-keys
  {:byte-array   (HttpResponse$BodyHandlers/ofByteArray)
   :discarding   (HttpResponse$BodyHandlers/discarding)
   :input-stream (HttpResponse$BodyHandlers/ofInputStream)
   :string       (HttpResponse$BodyHandlers/ofString)})

(defn send-with
  [client request body-handler]
  (-> client
      (.send (http-request request) (body-handler-keys body-handler body-handler))
      (response-map)))

(defn send
  [request body-handler]
  (send-with @default-http-client request body-handler))

(def response-map-function
  (reify Function
    (apply [_ response]
      (response-map response))))

(defn ^CompletableFuture send-async-with
  [client request body-handler]
  (-> client
      (.sendAsync (http-request request) (body-handler-keys body-handler body-handler))
      (.thenApply response-map-function)))

(defn ^CompletableFuture send-async
  [request body-handler]
  (send-async-with request body-handler))
