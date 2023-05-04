(ns dev.onionpancakes.hop.test-response
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.request :as request]
            [dev.onionpancakes.hop.response :as response])
  (:import [java.util Optional]
           [java.net.http HttpResponse HttpHeaders HttpRequest
            HttpClient$Version]))

(defn make-http-headers
  [m]
  (HttpHeaders/of m (reify java.util.function.BiPredicate
                      (test [this t u] true))))

(defn make-response
  [m]
  (reify HttpResponse
    (body [this]
      (:body m))
    (headers [this]
      (:headers m))
    (previousResponse [this]
      (Optional/ofNullable (:previous-request m)))
    (request [this]
      (:request m))
    (sslSession [this]
      (Optional/ofNullable (:ssl-session m)))
    (statusCode [this]
      (:status m))
    (uri [this]
      (:uri m))
    (version [this]
      (:version m))))

(deftest test-response-map
  (let [headers  {"content-type"     ["text/html;charset=utf-8"]
                  "content-encoding" ["gzip"]}
        resp-obj (make-response {:request (request/request "http://example.com")
                                 :uri     (java.net.URI. "http://example.com")
                                 :status  200
                                 :headers (make-http-headers headers)
                                 :body    "foo"
                                 :version HttpClient$Version/HTTP_1_1})
        resp-map (response/response-map resp-obj)]
    (is (instance? HttpRequest (:request resp-map)))
    (is (= (:uri resp-map) (java.net.URI. "http://example.com")))
    (is (= (:status resp-map) 200))
    (is (= (:headers resp-map) {"content-type"     ["text/html;charset=utf-8"]
                                "content-encoding" ["gzip"]}))
    (is (= (:body resp-map) "foo"))
    (is (= (:version resp-map) HttpClient$Version/HTTP_1_1))
    (is (= (:content-encoding resp-map) "gzip"))
    (is (= (:content-type resp-map) "text/html;charset=utf-8"))
    (is (= (:media-type resp-map) "text/html"))
    (is (= (:character-encoding resp-map) "utf-8"))
    ;; Test ssl-session?
    ))
