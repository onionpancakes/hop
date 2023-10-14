(ns dev.onionpancakes.hop.client
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.hop.keywords :as k]
            [dev.onionpancakes.hop.request :as request]
            [dev.onionpancakes.hop.response :as response])
  (:import [java.net.http HttpClient HttpClient$Builder]
           [java.util.concurrent CompletableFuture]))

;; Client

(defn set-client-builder-from-map
  ^HttpClient$Builder
  [^HttpClient$Builder builder m]
  (cond-> builder
    (contains? m :authenticator)    (.authenticator (:authenticator m))
    (contains? m :connect-timeout)  (.connectTimeout (:connect-timeout m))
    (contains? m :cookie-handler)   (.cookieHandler (:cookie-handler m))
    (contains? m :executor)         (.executor (:executor m))
    (contains? m :follow-redirects) (.followRedirects (k/redirect (:follow-redirects m)
                                                                  (:follow-redirects m)))
    (contains? m :priority)         (.priority (:priority m))
    (contains? m :proxy)            (.proxy (k/proxy-selector (:proxy m) (:proxy m)))
    (contains? m :ssl-context)      (.sslContext (:ssl-context m))
    (contains? m :ssl-parameters)   (.sslParameters (:ssl-parameters m))
    (contains? m :version)          (.version (k/version (:version m) (:version m)))))

(defn ^HttpClient client
  "Creates a HttpClient."
  ([] (client nil))
  ([m]
   (-> (HttpClient/newBuilder)
       (set-client-builder-from-map m)
       (.build))))

;; Send

(def default-client
  "Delayed default instance of HttpClient."
  (delay (client {:connect-timeout  (java.time.Duration/ofMinutes 5)
                  :follow-redirects :normal})))

(defn send-with
  "Send request with given client."
  ([client request]
   (send-with client request :byte-array))
  ([^HttpClient client request body-handler]
   (-> client
       (.send (request/request request) (response/body-handler body-handler))
       (response/response-proxy))))

(defn send
  "Send request with default client."
  ([request]
   (send-with @default-client request))
  ([request body-handler]
   (send-with @default-client request body-handler)))

(defn ^CompletableFuture send-async-with
  "Send async request with given client, returning a CompletableFuture."
  ([client request]
   (send-async-with client request :byte-array))
  ([^HttpClient client request body-handler]
   (.. client
       (sendAsync (request/request request) (response/body-handler body-handler))
       (thenApply response/response-proxy-function))))

(defn ^CompletableFuture send-async
  "Send async request with default client, returning a CompletableFuture."
  ([request]
   (send-async-with @default-client request))
  ([request body-handler]
   (send-async-with @default-client request body-handler)))
