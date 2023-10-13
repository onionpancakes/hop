(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keys :as k]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.net.http HttpRequest HttpResponse]))

(declare response-proxy-function)

(defn lookup-response
  [^HttpResponse this k not-found]
  (case k
    :request            (.request this)
    :uri                (.uri this)
    :version            (.version this)
    :status             (.statusCode this)
    :headers            (.map (.headers this))
    :body               (.body this)
    :content-encoding   (.. this
                            (headers)
                            (firstValue "content-encoding")
                            (orElse not-found))
    :content-type       (.. this
                            (headers)
                            (firstValue "content-type")
                            (orElse not-found))
    :media-type         (.. this
                            (headers)
                            (firstValue "content-type")
                            (map util/parse-media-type-function)
                            (orElse not-found))
    :character-encoding (.. this
                            (headers)
                            (firstValue "content-type")
                            (map util/parse-character-encoding-function)
                            (orElse not-found))
    :ssl-session        (.. this (sslSession) (orElse not-found))
    :previous-response  (.. this
                            (previousResponse)
                            (map response-proxy-function)
                            (orElse not-found))
    not-found))

(def response-lookup-keys
  [:request :uri :version :status :headers :body
   :content-encoding :content-type :media-type :character-encoding
   :ssl-session :previous-response])

(defn response-map
  [response]
  (let [map-entry-fn #(when-let [value (lookup-response response % nil)]
                        (clojure.lang.MapEntry/create % value))]
    (into {} (keep map-entry-fn) response-lookup-keys)))

(deftype ResponseProxy [response]
  clojure.lang.ILookup
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (lookup-response response k not-found))
  clojure.lang.IFn
  (invoke [this k]
    (.valAt this k nil))
  (invoke [this k not-found]
    (.valAt this k not-found))
  Object
  (toString [this]
    (str (:version this) " " (:status this) " " (:uri this) " " )))

(defn response-proxy
  [response]
  (ResponseProxy. response))

(def ^java.util.function.Function response-proxy-function
  "Function which returns response proxy from HttpResponse object."
  (reify java.util.function.Function
    (apply [_ response]
      (response-proxy response))))

;; BodyHandler

(defn body-handler
  "Return as BodyHandler."
  [bh]
  (k/http-response-body-handler bh bh))
