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

(defn response-entries
  [response]
  (let [map-entry-fn #(when-let [value (lookup-response response % nil)]
                        (clojure.lang.MapEntry/create % value))]
    (eduction (keep map-entry-fn) response-lookup-keys)))

(deftype ResponseProxy [response]
  java.util.Map
  (clear [this]
    (throw (UnsupportedOperationException.)))
  (containsKey [this k]
    (some? (lookup-response response k nil)))
  (containsValue [this value]
    (-> (into #{} (map val) (response-entries response))
        (contains? value)))
  (entrySet [this]
    (set (response-entries response)))
  (get [this k]
    (lookup-response response k nil))
  (isEmpty [this] false)
  (keySet [this]
    (into #{} (map key) (response-entries response)))
  (put [this k value]
    (throw (UnsupportedOperationException.)))
  (putAll [this m]
    (throw (UnsupportedOperationException.)))
  (remove [this k]
    (throw (UnsupportedOperationException.)))
  (size [this]
    (count (vec (response-entries response))))
  (values [this]
    (mapv val (response-entries response))))

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
  (k/body-handler bh bh))
