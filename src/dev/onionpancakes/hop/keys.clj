(ns dev.onionpancakes.hop.keys
  (:import [java.net.http
            HttpClient$Redirect
            HttpClient$Version]))

(def http-client-redirect
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def http-client-version
  {:http  HttpClient$Version/HTTP_1_1
   :http2 HttpClient$Version/HTTP_2})
