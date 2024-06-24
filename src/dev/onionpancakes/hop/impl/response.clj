(ns dev.onionpancakes.hop.impl.response
  (:require [dev.onionpancakes.hop.impl.parse :as impl.parse])
  (:import [java.net.http HttpHeaders HttpResponse]))

(def response-proxy-keys
  [:request :uri :version :status :headers :body
   :content-encoding :content-type :media-type :character-encoding
   :ssl-session :previous-response])

(declare response-proxy-function)

(deftype ResponseProxy [^HttpResponse response headers]
  HttpResponse
  (body [_]
    (.body response))
  (headers [_]
    (.headers response))
  (previousResponse [_]
    (.previousResponse response))
  (request [_]
    (.request response))
  (sslSession [_]
    (.sslSession response))
  (statusCode [_]
    (.statusCode response))
  (uri [_]
    (.uri response))
  (version [_]
    (.version response))
  clojure.lang.ILookup
  (valAt [this k]
    (.get this k))
  (valAt [this k not-found]
    (if (.containsKey this k)
      (.get this k)
      not-found))
  java.util.Map
  (clear [this]
    (throw (UnsupportedOperationException.)))
  (containsKey [this k]
    (some? (.get this k)))
  (containsValue [this value]
    (->> response-proxy-keys
         (keep (comp #{value} #(.get this %)))
         (first)
         (some?)))
  (entrySet [this]
    (let [create-map-entry #(when-some [value (.get this %)]
                              (clojure.lang.MapEntry. % value))]
      (into #{} (keep create-map-entry) response-proxy-keys)))
  (get [this k]
    (case k
      :request            (.request response)
      :uri                (.uri response)
      :version            (.version response)
      :status             (.statusCode response)
      :headers            (deref headers)
      :body               (.body response)
      :content-encoding   (.. response
                              (headers)
                              (firstValue "content-encoding")
                              (orElse nil))
      :content-type       (.. response
                              (headers)
                              (firstValue "content-type")
                              (orElse nil))
      :media-type         (.. response
                              (headers)
                              (firstValue "content-type")
                              (map impl.parse/parse-media-type-function)
                              (orElse nil))
      :character-encoding (.. response
                              (headers)
                              (firstValue "content-type")
                              (map impl.parse/parse-character-encoding-function)
                              (orElse nil))
      :ssl-session        (.. response (sslSession) (orElse nil))
      :previous-response  (.. response
                              (previousResponse)
                              (map response-proxy-function)
                              (orElse nil))
      nil))
  (isEmpty [this] false)
  (keySet [this]
    (into #{} (filter #(.get this %)) response-proxy-keys))
  (put [this k value]
    (throw (UnsupportedOperationException.)))
  (putAll [this m]
    (throw (UnsupportedOperationException.)))
  (remove [this k]
    (throw (UnsupportedOperationException.)))
  (size [this]
    (count (keep #(.get this %) response-proxy-keys)))
  (values [this]
    (into [] (keep #(.get this %)) response-proxy-keys)))

(def headers-map-xf
  (map (juxt key (comp vec val))))

(defn headers-map
  [^HttpHeaders headers]
  (into {} headers-map-xf (.map headers)))

(defn response-proxy
  [^HttpResponse response]
  (ResponseProxy. response (delay (headers-map (.headers response)))))

(def ^java.util.function.Function response-proxy-function
  "Function which returns response proxy from HttpResponse object."
  (reify java.util.function.Function
    (apply [_ response]
      (response-proxy response))))
