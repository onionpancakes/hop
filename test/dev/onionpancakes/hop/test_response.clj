(ns dev.onionpancakes.hop.test-response
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.request :as request]
            [dev.onionpancakes.hop.response :as response])
  (:import [java.net.http HttpResponse HttpHeaders HttpRequest
            HttpClient$Version]
           [java.util Optional]
           [java.util.function BiPredicate]))

(def example-response-object
  (reify HttpResponse
    (body [this]
      "Body")
    (headers [this]
      (-> {"content-encoding" ["gzip"]
           "content-type"     ["text/plain;charset=utf-8"]}
          (HttpHeaders/of (reify BiPredicate
                            (test [_ _ _] true)))))
    (previousResponse [this]
      (Optional/ofNullable nil))
    (request [this]
      (request/request "http://www.example.com"))
    (sslSession [this]
      (Optional/ofNullable nil))
    (statusCode [this]
      (int 200))
    (uri [this]
      (request/uri "http://www.example.com"))
    (version [this]
      HttpClient$Version/HTTP_1_1)))

(def example-response-proxy
  (response/response-proxy example-response-object))

(def example-response-map
  {:request            (request/request "http://www.example.com")
   :uri                (request/uri "http://www.example.com")
   :version            HttpClient$Version/HTTP_1_1
   :status             200
   :headers            {"content-encoding" ["gzip"]
                        "content-type"     ["text/plain;charset=utf-8"]}
   :body               "Body"
   :content-encoding   "gzip"
   :content-type       "text/plain;charset=utf-8"
   :media-type         "text/plain"
   :character-encoding "utf-8"})

(deftest test-response-proxy-lookup
  (are [kw expected] (= (kw example-response-proxy) expected)
    :request            (request/request "http://www.example.com")
    :uri                (request/uri "http://www.example.com")
    :version            HttpClient$Version/HTTP_1_1
    :status             200
    :headers            {"content-encoding" ["gzip"]
                         "content-type"     ["text/plain;charset=utf-8"]}
    :body               "Body"
    :content-encoding   "gzip"
    :content-type       "text/plain;charset=utf-8"
    :media-type         "text/plain"
    :character-encoding "utf-8"
    :ssl-session        nil
    :previous-response  nil
    :not-a-key          nil))

(deftest test-response-proxy-clj
  (are [value expected] (= value expected)
    example-response-proxy                     example-response-map
    (into {} example-response-proxy)           example-response-map
    (zipmap (keys example-response-proxy)
            (vals example-response-proxy))     example-response-map
    (get example-response-proxy :status)       200
    (contains? example-response-proxy :status) true
    (find example-response-proxy :status)      [:status 200]
    (count example-response-proxy)             (count example-response-map)))
