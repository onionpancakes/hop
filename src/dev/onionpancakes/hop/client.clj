(ns dev.onionpancakes.hop.client
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.hop.impl.response :as impl.response]
            [clojure.string :refer [upper-case]])
  (:import [java.net.http
            HttpClient
            HttpClient$Builder
            HttpClient$Redirect
            HttpClient$Version
            HttpRequest
            HttpRequest$BodyPublisher
            HttpRequest$BodyPublishers
            HttpRequest$Builder
            HttpResponse$BodyHandler
            HttpResponse$BodyHandlers]
           [java.util.concurrent CompletableFuture]))

(defprotocol RequestURI
  (as-uri [this] "Return as URI."))

(defprotocol RequestBodyPublisher
  (as-body-publisher [this] "Return as BodyPublisher."))

(defprotocol RequestHeaderValue
  (add-header-to-request-builder [this builder header-name] "Adds header to request builder."))

(defprotocol Request
  (^HttpRequest as-request [this] "Return as HttpRequest."))

(defprotocol ResponseBodyHandler
  (as-body-handler [this]))

;; Enums

(defn as-proxy-selector
  [this]
  (case this
    :no-proxy HttpClient$Builder/NO_PROXY
    (if (instance? java.net.ProxySelector this)
      this
      (throw (IllegalArgumentException. "Not a java.net.ProxySelector")))))

(defn as-redirect
  [this]
  (case this
    :always HttpClient$Redirect/ALWAYS
    :never  HttpClient$Redirect/NEVER
    :normal HttpClient$Redirect/NORMAL
    (if (instance? HttpClient$Redirect this)
      this
      (throw (IllegalArgumentException. "Not a java.net.http.HttpClient$Redirect.")))))

(defn as-version
  [this]
  (case this
    :http    HttpClient$Version/HTTP_1_1
    :http1.1 HttpClient$Version/HTTP_1_1
    :http2   HttpClient$Version/HTTP_2
    (if (instance? HttpClient$Version this)
      this
      (throw (IllegalArgumentException. "Not a java.net.http.HttpClient$Version.")))))

;; Client

(defn client
  "Creates a HttpClient."
  {:tag HttpClient}
  ([] (client nil))
  ([m]
   (cond-> (HttpClient/newBuilder)
     (contains? m :authenticator)    (.authenticator (:authenticator m))
     (contains? m :connect-timeout)  (.connectTimeout (:connect-timeout m))
     (contains? m :cookie-handler)   (.cookieHandler (:cookie-handler m))
     (contains? m :executor)         (.executor (:executor m))
     (contains? m :follow-redirects) (.followRedirects (as-redirect (:follow-redirects m)))
     (contains? m :priority)         (.priority (:priority m))
     (contains? m :proxy)            (.proxy (as-proxy-selector (:proxy m)))
     (contains? m :ssl-context)      (.sslContext (:ssl-context m))
     (contains? m :ssl-parameters)   (.sslParameters (:ssl-parameters m))
     (contains? m :version)          (.version (as-version (:version m)))
     true                            (.build))))

;; Send

(def default-client
  "Default delayed java.net.http.HttpClient used for send and send-async."
  (delay (client {:connect-timeout  (java.time.Duration/ofMinutes 5)
                  :follow-redirects :normal})))

(defn send-with
  "Send request with given client."
  ([client request]
   (send-with client request :byte-array))
  ([^HttpClient client request body-handler]
   (-> client
       (.send (as-request request) (as-body-handler body-handler))
       (impl.response/response-proxy))))

(defn send
  "Send request with default client."
  ([request]
   (send-with @default-client request))
  ([request body-handler]
   (send-with @default-client request body-handler)))

(defn send-async-with
  "Send async request with given client, returning a CompletableFuture."
  {:tag CompletableFuture}
  ([client request]
   (send-async-with client request :byte-array))
  ([^HttpClient client request body-handler]
   (.. client
       (sendAsync (as-request request) (as-body-handler body-handler))
       (thenApply impl.response/response-proxy-function))))

(defn send-async
  "Send async request with default client, returning a CompletableFuture."
  {:tag CompletableFuture}
  ([request]
   (send-async-with @default-client request))
  ([request body-handler]
   (send-async-with @default-client request body-handler)))

;; RequestURI

(extend-protocol RequestURI
  String
  (as-uri [this] (java.net.URI. this))
  java.net.URL
  (as-uri [this] (.toURI this))
  java.net.URI
  (as-uri [this] this))

;; RequestBodyPublisher

(extend-protocol RequestBodyPublisher
  (Class/forName "[B")
  (as-body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (as-body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  java.nio.file.Path
  (as-body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile this))
  java.io.File
  (as-body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile (.toPath this)))
  java.util.concurrent.Flow$Publisher
  (as-body-publisher [this]
    (HttpRequest$BodyPublishers/fromPublisher this))
  HttpRequest$BodyPublisher
  (as-body-publisher [this] this)
  nil
  (as-body-publisher [_]
    (HttpRequest$BodyPublishers/noBody)))

;; RequestHeaderValue

(extend-protocol RequestHeaderValue
  clojure.lang.Indexed
  (add-header-to-request-builder [this builder header-name]
    (loop [i 0 cnt (count this)]
      (when (< i cnt)
        (-> (nth this i)
            (add-header-to-request-builder builder header-name))
        (recur (inc i) cnt))))
  clojure.lang.Seqable
  (add-header-to-request-builder [this builder header-name]
    (doseq [value this]
      (add-header-to-request-builder value builder header-name)))
  clojure.lang.Fn
  (add-header-to-request-builder [this builder header-name]
    (add-header-to-request-builder (this) builder header-name))
  clojure.lang.IDeref
  (add-header-to-request-builder [this builder header-name]
    (add-header-to-request-builder (deref this) builder header-name))
  String
  (add-header-to-request-builder [this builder header-name]
    (.header ^HttpRequest$Builder builder header-name this))
  Object
  (add-header-to-request-builder [this builder header-name]
    (.header ^HttpRequest$Builder builder header-name (str this)))
  nil
  (add-header-to-request-builder [_ builder _] builder))

(defn add-request-builder-header-kv
  [builder k value]
  (add-header-to-request-builder value builder (name k))
  builder)

(defn add-request-builder-headers
  {:tag HttpRequest$Builder}
  [builder headers]
  (reduce-kv add-request-builder-header-kv builder headers))

;; Request

(extend-protocol Request
  clojure.lang.IPersistentMap
  (as-request [this]
    (cond-> (HttpRequest/newBuilder)
      (contains? this :uri)             (.uri (as-uri (:uri this)))
      (contains? this :method)          (.method (upper-case (name (:method this))) 
                                                 (as-body-publisher (:body this)))
      (contains? this :headers)         (add-request-builder-headers (:headers this))
      (contains? this :timeout)         (.timeout (:timeout this))
      (contains? this :version)         (.version (as-version (:version this)))
      (contains? this :expect-continue) (.expectContinue (:expect-continue this))
      true                              (.build)))
  HttpRequest
  (as-request [this] this)
  Object
  (as-request [this]
    (.. (HttpRequest/newBuilder (as-uri this)) 
        (build))))

;; ResponseBodyHandler

(extend-protocol ResponseBodyHandler
  clojure.lang.Keyword
  (as-body-handler [this]
    (case this
      :byte-array   (HttpResponse$BodyHandlers/ofByteArray)
      :discarding   (HttpResponse$BodyHandlers/discarding)
      :input-stream (HttpResponse$BodyHandlers/ofInputStream)
      :lines        (HttpResponse$BodyHandlers/ofLines)
      :publisher    (HttpResponse$BodyHandlers/ofPublisher)
      :string       (HttpResponse$BodyHandlers/ofString)))
  HttpResponse$BodyHandler
  (as-body-handler [this] this))
