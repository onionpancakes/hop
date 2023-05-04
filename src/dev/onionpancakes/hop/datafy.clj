(ns dev.onionpancakes.hop.datafy
  (:require [dev.onionpancakes.hop.util :as util]
            [clojure.core.protocols :as core.p])
  (:import [java.net.http HttpHeaders HttpRequest HttpResponse]))

(def headers-xf
  (map (juxt key (comp vec val))))

(defn HttpHeaders->map
  [^HttpHeaders headers]
  (into {} headers-xf (.map headers)))

(defn HttpRequest->map
  [^HttpRequest request]
  (into {:uri             (.uri request)
         :method          (.method request)
         :headers         (.headers request)
         :expect-continue (.expectContinue request)}
        (remove (comp nil? val))
        {:body    (.. request (bodyPublisher) (orElse nil))
         :timeout (.. request (timeout) (orElse nil))
         :version (.. request (version) (orElse nil))}))

(defn HttpResponse->map
  [^HttpResponse response]
  (let [headers          (.headers response)
        content-encoding (.. headers (firstValue "content-encoding") (orElse nil))
        content-type     (.. headers (firstValue "content-type") (orElse nil))]
    (into {:request (.request response)
           :uri     (.uri response)
           :version (.version response)
           :status  (.statusCode response)
           :headers headers
           :body    (.body response)}
          (remove (comp nil? val))
          {:content-encoding   content-encoding
           :content-type       content-type
           :media-type         (util/parse-media-type content-type)
           :character-encoding (util/parse-character-encoding content-type)
           :ssl-session        (.. response (sslSession) (orElse nil))
           :previous-response  (.. response (previousResponse) (orElse nil))})))

(defn extend-datafiable!
  []
  (extend-protocol core.p/Datafiable
    HttpHeaders
    (datafy [this]
      (HttpHeaders->map this))
    HttpRequest
    (datafy [this]
      (HttpRequest->map this))
    HttpResponse
    (datafy [this]
      (HttpResponse->map this))))
