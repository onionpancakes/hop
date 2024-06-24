(ns dev.onionpancakes.hop.tests.test-client-request
  (:require [dev.onionpancakes.hop.client :as client]
            [clojure.test :refer [deftest are]])
  (:import [java.net.http HttpClient$Version]))

(deftest test-uri
  (are [x expected] (= (client/as-uri x) expected)
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-simple-get-request
  (are [req uri] (let [obj (client/as-request req)]
                   (and (= (.method obj) "GET")
                        (= (.uri obj) uri)))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-uri
  (are [uri expected] (let [req {:uri uri}
                            obj (client/as-request req)]
                        (= (.uri obj) expected))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-method
  (are [method expected] (let [req {:uri    "http://example.com"
                                    :method method}
                               obj (client/as-request req)]
                           (= (.method obj) expected))
    :GET  "GET"
    :POST "POST"
    :HEAD "HEAD"
    :get  "GET"
    :post "POST"
    :head "HEAD"))

(defn example-header-value-fn
  []
  "bar")

(def example-header-value
  "bar")

(deftest test-request-headers
  (are [headers expected] (let [req {:uri     "http://example.com"
                                     :headers headers}
                                obj (client/as-request req)]
                            (= (.map (.headers obj)) expected))
    nil             {}
    {}              {}
    {"foo" nil}     {}
    {"foo" []}      {}
    {:foo [nil]}    {}
    {"foo" "bar"}   {"foo" ["bar"]}
    {"foo" ["bar"]} {"foo" ["bar"]}
    {:foo ["bar"]}  {"foo" ["bar"]}
    {:foo ["bar"]}  {"foo" ["bar"]}
    {:foo '("bar")} {"foo" ["bar"]}
    {"foo" 0}       {"foo" ["0"]}
    {"foo" [0]}     {"foo" ["0"]}
    
    {"foo" example-header-value-fn}   {"foo" ["bar"]}
    {"foo" #'example-header-value-fn} {"foo" ["bar"]}
    {"foo" example-header-value} {"foo" ["bar"]}
    {"foo" #'example-header-value} {"foo" ["bar"]}

    ;; Mixed
    {:Foo ["Bar" "Baz"] :Qux "Mux"} {"Foo" ["Bar" "Baz"]
                                     "Qux" ["Mux"]}))

;; Can't test here, no obvious way to compare body publishers.
;; Test in test_client.clj with real server?
#_(deftest test-request-map-method-body
    (are [method body expected] (let [req {:uri    "http://example.com"
                                           :method method
                                           :body   body}
                                      obj (client/as-request req)]
                                  (= (.. obj (bodyPublisher) (orElse nil)) expected))
      :GET nil (HttpRequest$BodyPublishers/noBody)))

(deftest test-request-timeout
  (are [tout expected] (let [req {:uri     "http://example.com"
                                  :timeout tout}
                             obj (client/as-request req)]
                         (= (.. obj (timeout) (orElse nil)) expected))
    (java.time.Duration/ofMinutes 5) (java.time.Duration/ofMinutes 5)))

(deftest test-request-version
  (are [ver expected] (let [req {:uri     "http://example.com"
                                 :version ver}
                            obj (client/as-request req)]
                        (= (.. obj (version) (orElse nil)) expected))
    :http                       HttpClient$Version/HTTP_1_1
    :http1.1                    HttpClient$Version/HTTP_1_1
    :http2                      HttpClient$Version/HTTP_2
    HttpClient$Version/HTTP_1_1 HttpClient$Version/HTTP_1_1
    HttpClient$Version/HTTP_2   HttpClient$Version/HTTP_2))

(deftest test-request-expect-continue
  (are [ec expected] (let [req {:uri             "http://example.com"
                                :expect-continue ec}
                            obj (client/as-request req)]
                        (= (.. obj (expectContinue)) expected))
    false false
    true  true))
