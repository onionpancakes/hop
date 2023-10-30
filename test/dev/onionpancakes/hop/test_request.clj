(ns dev.onionpancakes.hop.test-request
  (:require [dev.onionpancakes.hop.request :as request]
            [clojure.test :refer [deftest are]])
  (:import [java.net.http HttpClient$Version]))

(deftest test-uri
  (are [x expected] (= (request/uri x) expected)
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-simple-get-request
  (are [req uri] (let [obj (request/request req)]
                   (and (= (.method obj) "GET")
                        (= (.uri obj) uri)))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-uri
  (are [uri expected] (let [req {:uri uri}
                            obj (request/request req)]
                        (= (.uri obj) expected))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-method
  (are [method expected] (let [req {:uri    "http://example.com"
                                    :method method}
                               obj (request/request req)]
                           (= (.method obj) expected))
    :GET  "GET"
    :POST "POST"
    :HEAD "HEAD"
    :get  "GET"
    :post "POST"
    :head "HEAD"))

(deftest test-request-headers
  (are [headers expected] (let [req {:uri     "http://example.com"
                                     :headers headers}
                                obj (request/request req)]
                            (= (.map (.headers obj)) expected))
    nil             {}
    {}              {}
    {"Foo" nil}     {}
    {"Foo" []}      {}
    {:Foo [nil]}    {}
    {"Foo" "Bar"}   {"Foo" ["Bar"]}
    {"Foo" ["Bar"]} {"Foo" ["Bar"]}
    {:Foo ["Bar"]}  {"Foo" ["Bar"]}
    {"Foo" 0}       {"Foo" ["0"]}
    {"Foo" [0]}     {"Foo" ["0"]}

    ;; Mixed
    {:Foo ["Bar" "Baz"] :Qux "Mux"} {"Foo" ["Bar" "Baz"]
                                     "Qux" ["Mux"]}))

;; Can't test here, no obvious way to compare body publishers.
;; Test in test_client.clj with real server?
#_(deftest test-request-map-method-body
    (are [method body expected] (let [req {:uri    "http://example.com"
                                           :method method
                                           :body   body}
                                      obj (request/request req)]
                                  (= (.. obj (bodyPublisher) (orElse nil)) expected))
      :GET nil (HttpRequest$BodyPublishers/noBody)))

(deftest test-request-timeout
  (are [tout expected] (let [req {:uri     "http://example.com"
                                  :timeout tout}
                             obj (request/request req)]
                         (= (.. obj (timeout) (orElse nil)) expected))
    (java.time.Duration/ofMinutes 5) (java.time.Duration/ofMinutes 5)))

(deftest test-request-version
  (are [ver expected] (let [req {:uri     "http://example.com"
                                 :version ver}
                            obj (request/request req)]
                        (= (.. obj (version) (orElse nil)) expected))
    :http                       HttpClient$Version/HTTP_1_1
    :http1.1                    HttpClient$Version/HTTP_1_1
    :http2                      HttpClient$Version/HTTP_2
    HttpClient$Version/HTTP_1_1 HttpClient$Version/HTTP_1_1
    HttpClient$Version/HTTP_2   HttpClient$Version/HTTP_2))

(deftest test-request-expect-continue
  (are [ec expected] (let [req {:uri             "http://example.com"
                                :expect-continue ec}
                            obj (request/request req)]
                        (= (.. obj (expectContinue)) expected))
    false false
    true  true))
