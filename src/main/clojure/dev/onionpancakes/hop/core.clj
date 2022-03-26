(ns dev.onionpancakes.hop.core
  (:refer-clojure :exclude [send])
  (:require [clojure.string :refer [upper-case]])
  (:import [java.nio.file Path]
           [java.net URI]
           [java.net.http
            HttpClient HttpClient$Redirect HttpClient$Version HttpClient$Builder
            HttpRequest HttpRequest$BodyPublisher HttpRequest$BodyPublishers HttpRequest$Builder
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers
            HttpHeaders]
           [java.util.function Function]))

;; Client

(def to-follow-redirects
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def to-http-version
  {:http1.1 HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def to-proxy-selector
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
     follow-redirects (.followRedirects (to-follow-redirects follow-redirects follow-redirects))
     priority         (.priority priority)
     proxy-selector   (.proxy (to-proxy-selector proxy-selector proxy-selector))
     ssl-context      (.sslContext ssl-context)
     ssl-parameters   (.sslParameters ssl-parameters)
     version          (.version (to-http-version version version))
     true             (.build))))

;; Request

(defprotocol IRequestUri
  (to-uri [this] "Coerce to URI."))

(extend-protocol IRequestUri
  String
  (to-uri [this] (URI. this))
  URI
  (to-uri [this] this))

(defprotocol IRequestBodyPublisher
  (to-body-publisher [this] "Coerce to BodyPublisher."))

(extend-protocol IRequestBodyPublisher
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

(defn set-request-builder-headers ^HttpRequest$Builder
  [^HttpRequest$Builder builder headers]
  (doseq [[k values] headers
          :let       [header (name k)]
          value      values]
    (.setHeader builder header value))
  builder)

(defn ^HttpRequest to-request-map
  "Creates a HttpRequest from map."
  [{:keys [uri method body headers]}]
  (cond-> (HttpRequest/newBuilder)
    uri     (.uri (to-uri uri))
    method  (.method (upper-case (name method)) (to-body-publisher body))
    headers (set-request-builder-headers headers)
    true    (.build)))

(defprotocol IRequest
  (to-request [this] "Coerce to HttpRequest."))

(extend-protocol IRequest
  java.util.Map
  (to-request [this]
    (to-request-map this))
  HttpRequest
  (to-request [this] this))

;; Response

(defprotocol IResponseBodyHandler
  (to-body-handler [this] "Coerce to BodyHandler."))

(extend-protocol IResponseBodyHandler
  clojure.lang.Keyword
  (to-body-handler [this]
    (case this
      :byte-array   (HttpResponse$BodyHandlers/ofByteArray)
      :discarding   (HttpResponse$BodyHandlers/discarding)
      :input-stream (HttpResponse$BodyHandlers/ofInputStream)
      :string       (HttpResponse$BodyHandlers/ofString)))
  HttpResponse$BodyHandler
  (to-body-handler [this] this))

(def response-map-header-xf
  (map (juxt key (comp vec val))))

(defn response-map-headers
  "Creates a map from HttpHeaders."
  [^HttpHeaders headers]
  (into {} response-map-header-xf (.map headers)))

(defn response-map
  "Creates a map from HttpResponse."
  [^HttpResponse resp]
  (let [headers (response-map-headers (.headers resp))]
    {:status           (.statusCode resp)
     :headers          headers
     :body             (.body resp)
     :content-encoding (first (get headers "content-encoding"))
     :content-type     (first (get headers "content-type"))
     :content-length   (some-> (first (get headers "content-length"))
                               (Integer/parseInt))}))

;; Send

(def default-client
  "Delayed default HttpClient."
  (delay (client {:follow-redirects :normal})))

(defn send-with
  "Sends request with the given client."
  ([client req] (send-with client req nil))
  ([^HttpClient client req {:keys [body-handler response-fn]
                            :or   {body-handler :byte-array
                                   response-fn  response-map}}]
   (cond-> (.send client (to-request req) (to-body-handler body-handler))
     response-fn (response-fn))))

(defn send
  "Sends request with default client."
  ([req] (send req nil))
  ([req opts]
   (send-with @default-client req opts)))

(defn send-async-with
  "Sends request with given client, returning the response in a CompletableFuture."
  ([client req] (send-async-with client req nil))
  ([^HttpClient client req {:keys [body-handler response-fn]
                            :or   {body-handler :byte-array
                                   response-fn  response-map}}]
   (cond-> (.sendAsync client (to-request req) (to-body-handler body-handler))
     response-fn (.thenApply (reify Function
                               (apply [_ resp]
                                 (response-fn resp)))))))

(defn send-async
  "Sends request with default client, returning the response in a CompletableFuture."
  ([req] (send-async req nil))
  ([req opts]
   (send-async-with @default-client req opts)))
