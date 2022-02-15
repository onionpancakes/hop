(ns dev.onionpancakes.hop.core
  (:refer-clojure :exclude [send])
  (:require [clojure.string :refer [upper-case]])
  (:import [java.nio.file Path]
           [java.net URI]
           [java.net.http
            HttpClient HttpClient$Redirect HttpClient$Version HttpClient$Builder
            HttpRequest HttpRequest$BodyPublisher HttpRequest$BodyPublishers
            HttpResponse HttpResponse$BodyHandler HttpResponse$BodyHandlers]))

;; Client

(def follow-redirects-alias
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def http-version-alias
  {:http1.1 HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def proxy-selector-alias
  {:no-proxy HttpClient$Builder/NO_PROXY})

(defn client
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
     follow-redirects (.followRedirects (follow-redirects-alias follow-redirects follow-redirects))
     priority         (.priority priority)
     proxy-selector   (.proxy (proxy-selector-alias proxy-selector proxy-selector))
     ssl-context      (.sslContext ssl-context)
     ssl-parameters   (.sslParameters ssl-parameters)
     version          (.version (http-version-alias version version))
     true             (.build))))

;; Request

(defprotocol IRequestBody
  (body-publisher [this]))

(extend-protocol IRequestBody
  (Class/forName "[B")
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  nil
  (body-publisher [_]
    (HttpRequest$BodyPublishers/noBody))
  Path
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile this))
  HttpRequest$BodyPublisher
  (body-publisher [this] this))

(defn set-request-headers
  [builder headers]
  (doseq [[k values] headers
          :let       [header (name k)]
          value      values]
    (.setHeader builder header values))
  builder)

(defn http-request
  [{:keys [method body headers]}]
  (cond-> (HttpRequest/newBuilder)
    true    (.method (upper-case (name method)) (body-publisher body))
    headers (set-request-headers headers)
    true    (.build)))

;; Response

(defprotocol IResponseBodyHandler
  (body-handler* [this]))

(extend-protocol IResponseBodyHandler
  clojure.lang.Keyword
  (body-handler* [this]
    (case this
      :byte-array   (HttpResponse$BodyHandlers/ofByteArray)
      :discarding   (HttpResponse$BodyHandlers/discarding)
      :input-stream (HttpResponse$BodyHandlers/ofInputStream)
      :string       (HttpResponse$BodyHandlers/ofString)))
  HttpResponse$BodyHandler
  (body-handler* [this] this))

(defn response-map
  [resp]
  {})

;; Send

(def default-client
  (delay (client {:follow-redirects :normal})))

(defn send
  ([req] (send @default-client req))
  ([client {:keys [body-handler] :or {body-handler :byte-array} :as req}]
   (-> (or client @default-client)
       (.send (http-request req) (body-handler* body-handler))
       (response-map))))
