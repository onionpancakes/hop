(ns dev.onionpancakes.hop.core
  (:refer-clojure :exclude [send])
  (:require [clojure.string :refer [upper-case]])
  (:import [java.nio.file Path]
           [java.net URI]
           [java.net.http
            HttpClient HttpClient$Redirect HttpClient$Version HttpClient$Builder
            HttpRequest HttpRequest$BodyPublisher HttpRequest$BodyPublishers HttpRequest$Builder
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers]))

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

(defn client ^HttpClient
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
  (to-uri [this]))

(extend-protocol IRequestUri
  String
  (to-uri [this] (URI. this)))

(defprotocol IRequestBodyPublisher
  (to-body-publisher [this]))

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

(defn set-request-headers ^HttpRequest$Builder
  [^HttpRequest$Builder builder headers]
  (doseq [[k values] headers
          :let       [header (name k)]
          value      values]
    (.setHeader builder header value))
  builder)

(defn http-request ^HttpRequest
  [{:keys [uri method body headers]}]
  (cond-> (HttpRequest/newBuilder)
    uri     (.uri (to-uri uri))
    method  (.method (upper-case (name method)) (to-body-publisher body))
    headers (set-request-headers headers)
    true    (.build)))

;; Response

(defprotocol IResponseBodyHandler
  (to-body-handler ^HttpResponse$BodyHandler [this]))

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

(defn response-map
  [^HttpResponse resp]
  {:status  (.statusCode resp)
   :headers (into {} response-map-header-xf (.map (.headers resp)))
   :body    (.body resp)})

;; Send

(def default-client
  (delay (client {:follow-redirects :normal})))

(defn send*
  [^HttpClient client req {:keys [body-handler]
                           :or   {body-handler :byte-array}}]
  (-> (.send client (http-request req) (to-body-handler body-handler))
      (response-map)))

(defn send
  ([req] (send req nil))
  ([req {:keys [client] :as opts}]
   (send* (or client @default-client) req opts)))
