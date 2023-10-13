(ns dev.onionpancakes.hop.test-request
  (:require [dev.onionpancakes.hop.request :as request]
            [clojure.test :refer [deftest are]])
  (:import [java.net.http HttpClient$Version]))

(deftest test-request-short-uri
  (are [short expected] (let [req short
                              obj (request/request req)]
                          (and (= (.uri obj) expected)
                               (= (.method obj) "GET")))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-map-uri
  (are [uri expected] (let [req {:uri uri}
                            obj (request/request req)]
                        (= (.uri obj) expected))
    "http://example.com"                 (java.net.URI. "http://example.com")
    (java.net.URL. "http://example.com") (java.net.URI. "http://example.com")
    (java.net.URI. "http://example.com") (java.net.URI. "http://example.com")))

(deftest test-request-map-method
  (are [method expected] (let [req {:uri     "http://example.com"
                                    :method method}
                               obj (request/request req)]
                           (= (.method obj) expected))
    :GET  "GET"
    :POST "POST"
    :HEAD "HEAD"
    :get  "GET"
    :post "POST"
    :head "HEAD"
    nil   "GET"))

(deftest test-request-map-headers
  (are [headers expected] (let [req {:uri     "http://example.com"
                                     :headers headers}
                                obj (request/request req)]
                            (= (.map (.headers obj)) expected))
    {"Foo" ["Bar"]} {"Foo" ["Bar"]}
    {:Foo ["Bar"]}  {"Foo" ["Bar"]}
    ;; Handle nil in map headers?
    #_#_#_#_#_#_
    {:Foo nil}      {}
    {:Foo [nil]}    {}
    {nil nil}       {}))

;; Can't test here, no obvious way to compare body publishers.
;; Test in test_client.clj with real server?
#_(deftest test-request-map-method-body
    (are [method body expected] (let [req {:uri    "http://example.com"
                                           :method method
                                           :body   body}
                                      obj (request/request req)]
                                  (= (.. obj (bodyPublisher) (orElse nil)) expected))
      :GET nil (HttpRequest$BodyPublishers/noBody)))

(deftest test-request-map-timeout
  (are [tout expected] (let [req {:uri     "http://example.com"
                                  :timeout tout}
                             obj (request/request req)]
                         (= (.. obj (timeout) (orElse nil)) expected))
    nil                              nil
    (java.time.Duration/ofMinutes 5) (java.time.Duration/ofMinutes 5)))

(deftest test-request-map-version
  (are [ver expected] (let [req {:uri     "http://example.com"
                                 :version ver}
                            obj (request/request req)]
                        (= (.. obj (version) (orElse nil)) expected))
    nil                         nil
    :http                       HttpClient$Version/HTTP_1_1
    :http2                      HttpClient$Version/HTTP_2
    HttpClient$Version/HTTP_1_1 HttpClient$Version/HTTP_1_1
    HttpClient$Version/HTTP_2   HttpClient$Version/HTTP_2))

(deftest test-request-map-expect-continue
  (are [ec expected] (let [req {:uri             "http://example.com"
                                :expect-continue ec}
                            obj (request/request req)]
                        (= (.. obj (expectContinue)) expected))
    nil   false
    false false
    true  true))
