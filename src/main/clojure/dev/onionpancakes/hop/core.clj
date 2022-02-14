(ns dev.onionpancakes.hop.core
  (:import [java.net.http
            HttpClient HttpClient$Redirect HttpClient$Version HttpClient$Builder
            ]))

(def follow-redirects-keys
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def http-version-keys
  {:http1.1 HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def proxy-selector-keys
  {:no-proxy HttpClient$Builder/NO_PROXY})

(defn client
  ([] (client nil))
  ([{auth :authenticator
     tout :connect-timeout
     chdr :cookie-handler
     exec :executor
     rdir :follow-redirects
     prty :priority
     prox :proxy-selector
     sslc :ssl-context
     sslp :ssl-parameters
     hver :version}]
   (cond-> (HttpClient/newBuilder)
     auth (.authenticator auth)
     tout (.connectTimeout tout)
     chdr (.cookieHandler chdr)
     exec (.executor exec)
     rdir (.followRedirects (follow-redirects-keys rdir rdir))
     prty (.priority prty)
     prox (.proxy (proxy-selector-keys prox prox))
     sslc (.sslContext sslc)
     sslp (.sslParameters sslp)
     hver (.version (http-version-keys hver hver))
     true (.build))))
