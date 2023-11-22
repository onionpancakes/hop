(ns dev.onionpancakes.hop.tests.test-response
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.headers :as headers]
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
      (headers/from-map {"content-encoding" ["gzip"]
                         "content-type"     ["text/plain;charset=utf-8"]}))
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

(def ^java.util.Map example-response-proxy
  (response/response-proxy example-response-object))

(def ^java.util.Map example-response-map
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

(deftest test-response-proxy-lookup-not-found
  (is (= (get example-response-proxy ::foo :not-found) :not-found)))

(deftest test-response-proxy-clj
  (are [value expected] (= value expected)
    example-response-proxy                     example-response-map
    (into {} example-response-proxy)           example-response-map
    (zipmap (keys example-response-proxy)
            (vals example-response-proxy))     example-response-map
    (empty? example-response-proxy)            false
    (get example-response-proxy :status)       200
    (get-in example-response-proxy
            [:headers "content-encoding" 0])   "gzip"
    (contains? example-response-proxy :status) true
    (find example-response-proxy :status)      [:status 200]
    (count example-response-proxy)             (count example-response-map)
    (merge {:foo :bar} example-response-proxy) (merge {:foo :bar}
                                                      example-response-map)))

(deftest test-response-proxy-java
  (are [value expected] (= value expected)
    (.containsKey example-response-proxy :status) true
    (.containsValue example-response-proxy 200)   true
    (.containsValue example-response-proxy nil)   false
    (.entrySet example-response-proxy)            (.entrySet example-response-map)
    (.get example-response-proxy :status)         200
    (.isEmpty example-response-proxy)             false
    (.keySet example-response-proxy)              (.keySet example-response-map)
    (.size example-response-proxy)                (.size example-response-map)
    (set (.values example-response-proxy))        (set (.values example-response-map))))
