(ns dev.onionpancakes.hop.keywords
  (:import [java.net.http
            HttpClient$Builder
            HttpClient$Redirect
            HttpClient$Version
            HttpResponse$BodyHandlers]))

(def proxy-selector
  {:no-proxy HttpClient$Builder/NO_PROXY})

(def redirect
  {:always HttpClient$Redirect/ALWAYS
   :never  HttpClient$Redirect/NEVER
   :normal HttpClient$Redirect/NORMAL})

(def version
  {:http    HttpClient$Version/HTTP_1_1
   :http1.1 HttpClient$Version/HTTP_1_1
   :http2   HttpClient$Version/HTTP_2})

(def body-handler
  {:byte-array   (HttpResponse$BodyHandlers/ofByteArray)
   :discarding   (HttpResponse$BodyHandlers/discarding)
   :input-stream (HttpResponse$BodyHandlers/ofInputStream)
   :lines        (HttpResponse$BodyHandlers/ofLines)
   :publisher    (HttpResponse$BodyHandlers/ofPublisher)
   :string       (HttpResponse$BodyHandlers/ofString)})
