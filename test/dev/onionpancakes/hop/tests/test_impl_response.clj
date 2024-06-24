(ns dev.onionpancakes.hop.tests.test-impl-response
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.impl.response :as response])
  (:import [java.net.http
            HttpClient$Version
            HttpHeaders
            HttpRequest
            HttpResponse]
           [java.util Optional]
           [java.util.function BiPredicate]))

(def example-response-object
  (let [body     "Body"
        headers  (HttpHeaders/of {"content-encoding" ["gzip"]
                                  "content-type"     ["text/plain;charset=utf-8"]}
                                 (reify java.util.function.BiPredicate
                                   (test [_ _ _] true)))
        prevResp (Optional/ofNullable nil)
        req      (.. (HttpRequest/newBuilder)
                     (uri (java.net.URI. "http://www.example.com"))
                     (build))
        ssl      (Optional/ofNullable nil)
        status   (int 200)
        uri      (java.net.URI. "http://www.example.com")
        version  HttpClient$Version/HTTP_1_1]
    (reify HttpResponse
      (body [_] body)
      (headers [_] headers)
      (previousResponse [_] prevResp)
      (request [_] req)
      (sslSession [_] ssl)
      (statusCode [_] status)
      (uri [_] uri)
      (version [_] version))))

(def ^java.util.Map example-response-proxy
  (response/response-proxy example-response-object))

(def ^java.util.Map example-response-map
  {:request            (.. (HttpRequest/newBuilder)
                           (uri (java.net.URI. "http://www.example.com"))
                           (build))
   :uri                (java.net.URI. "http://www.example.com")
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
    :request            (.. (HttpRequest/newBuilder)
                            (uri (java.net.URI. "http://www.example.com"))
                            (build))
    :uri                (java.net.URI. "http://www.example.com")
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

(deftest test-response-proxy-http-response
  (are [value expected] (identical? value expected)
    (.body example-response-proxy)             (.body example-response-object)
    (.headers example-response-proxy)          (.headers example-response-object)
    (.previousResponse example-response-proxy) (.previousResponse example-response-object)
    (.request example-response-proxy)          (.request example-response-object)
    (.sslSession example-response-proxy)       (.sslSession example-response-object)
    ;; Primitives can't be identical, use value test instead.
    #_#_
    (.statusCode example-response-proxy)       (.statusCode example-response-object)
    (.uri example-response-proxy)              (.uri example-response-object)
    (.version example-response-proxy)          (.version example-response-object))
  (is (= (.statusCode example-response-proxy) (.statusCode example-response-object))))
