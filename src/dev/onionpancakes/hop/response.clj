(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keywords :as k]
            [dev.onionpancakes.hop.headers :as h]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.net.http HttpRequest HttpResponse]))

(declare response-proxy-function)

(def response-proxy-keys
  [:request :uri :version :status :headers :body
   :content-encoding :content-type :media-type :character-encoding
   :ssl-session :previous-response])

(deftype ResponseProxy [^HttpResponse response headers]
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
                              (map h/parse-media-type-function)
                              (orElse nil))
      :character-encoding (.. response
                              (headers)
                              (firstValue "content-type")
                              (map h/parse-character-encoding-function)
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

(defn response-proxy
  [^HttpResponse response]
  (let [headers (delay (h/to-map (.headers response)))]
    (ResponseProxy. response headers)))

(def ^java.util.function.Function response-proxy-function
  "Function which returns response proxy from HttpResponse object."
  (reify java.util.function.Function
    (apply [_ response]
      (response-proxy response))))

;; BodyHandler

;; Note: Handle non-keyword body handlers in the future?
;; e.g. Strings/Paths as BodyHandlers/ofFile ?

(defn body-handler
  "Return as BodyHandler."
  [bh]
  (k/body-handler bh bh))
