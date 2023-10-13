(ns dev.onionpancakes.hop.client
  (:refer-clojure :exclude [send])
  (:require [dev.onionpancakes.hop.keys :as k]
            [dev.onionpancakes.hop.request :as request]
            [dev.onionpancakes.hop.response :as response])
  (:import [java.net.http HttpClient]
           [java.util.concurrent CompletableFuture]))

;; Client

(defn ^HttpClient client
  "Creates a HttpClient."
  ([] (client nil))
  ([{:keys [authenticator
            connect-timeout
            cookie-handler
            executor
            follow-redirects
            priority
            proxy
            ssl-context
            ssl-parameters
            version]}]
   (cond-> (HttpClient/newBuilder)
     authenticator    (.authenticator authenticator)
     connect-timeout  (.connectTimeout connect-timeout)
     cookie-handler   (.cookieHandler cookie-handler)
     executor         (.executor executor)
     follow-redirects (.followRedirects (k/redirect follow-redirects follow-redirects))
     priority         (.priority priority)
     proxy            (.proxy (k/proxy-selector proxy proxy))
     ssl-context      (.sslContext ssl-context)
     ssl-parameters   (.sslParameters ssl-parameters)
     version          (.version (k/version version version))
     true             (.build))))

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
