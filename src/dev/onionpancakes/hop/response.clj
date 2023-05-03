(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keys :as k]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.net.http HttpResponse]))

;; Response

(defn response-map
  [^HttpResponse response]
  (let [headers          (.headers response)
        content-encoding (.. headers (firstValue "content-encoding") (orElse nil))
        content-type     (.. headers (firstValue "content-type") (orElse nil))]
    {:uri              (.uri response)
     :version          (k/from-http-client-version (.version response))
     :status           (.statusCode response)
     :headers          (.map headers)
     :body             (.body response)
     :content-encoding content-encoding
     :content-type     content-type
     :mimetype         (util/parse-mimetype content-type)
     :charset          (util/parse-charset-encoding content-type)}))

(def ^java.util.function.Function response-map-function
  (reify java.util.function.Function
    (apply [_ response]
      (response-map response))))

;; BodyHandler

(defn body-handler
  [bh]
  (k/http-response-body-handler bh bh))
