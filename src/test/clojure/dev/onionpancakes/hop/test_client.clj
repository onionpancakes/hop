(ns dev.onionpancakes.hop.test-client
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.client :as client]
            [dev.onionpancakes.serval.jetty :as srv.jetty]))

;; Test server

(def default-config
  {:connectors [{:port 42000}]
   :handler    (constantly nil)})

(defonce server
  (srv.jetty/server default-config))

(defmacro with-response
  [response & body]
  `(try
     (srv.jetty/stop server)
     (srv.jetty/configure-server! server {:handler (constantly ~response)})
     (srv.jetty/start server)
     ~@body
     (finally
       (srv.jetty/stop server)
       (srv.jetty/join server)
       (srv.jetty/configure-server! server default-config))))

(def server-uri
  "http://localhost:42000")

;; Tests

(deftest test-send
  (with-response {:serval.response/status             200
                  :serval.response/headers            {"foo" ["bar"]}
                  :serval.response/body               "foo"
                  :serval.response/content-type       "text/plain"
                  :serval.response/character-encoding "utf-8"}
    (let [resp (client/send server-uri :string)]
      (is (= (:status resp) 200))
      (is (= (get (:headers resp) "foo") ["bar"]))
      (is (= (:body resp) "foo"))
      (is (= (:content-type resp) "text/plain;charset=utf-8"))
      (is (= (:mimetype resp) "text/plain"))
      (is (= (:charset resp) "utf-8")))))

(deftest test-send-async
  (with-response {:serval.response/status             200
                  :serval.response/headers            {"foo" ["bar"]}
                  :serval.response/body               "foo"
                  :serval.response/content-type       "text/plain"
                  :serval.response/character-encoding "utf-8"}
    (let [resp (-> (client/send-async server-uri :string)
                   (.get))]
      (is (= (:status resp) 200))
      (is (= (get (:headers resp) "foo") ["bar"]))
      (is (= (:body resp) "foo"))
      (is (= (:content-type resp) "text/plain;charset=utf-8"))
      (is (= (:mimetype resp) "text/plain"))
      (is (= (:charset resp) "utf-8")))))

(deftest test-parse-mimetype
  (is (= nil (client/parse-mimetype nil)))
  (is (= nil (client/parse-mimetype "")))
  (is (= nil (client/parse-mimetype "text")))
  (is (= nil (client/parse-mimetype "texthtml")))
  (is (= nil (client/parse-mimetype "text/html/xml")))
  (is (= nil (client/parse-mimetype "text/html xml")))
  (is (= "text/html" (client/parse-mimetype "text/html")))
  (is (= "text/html" (client/parse-mimetype " text/html ")))
  (is (= "text/html" (client/parse-mimetype "text/html;")))
  (is (= "text/html" (client/parse-mimetype " text / html ;"))))

(deftest test-parse-charset-encoding
  (is (= nil (client/parse-charset-encoding nil)))
  (is (= nil (client/parse-charset-encoding "")))
  (is (= nil (client/parse-charset-encoding "charset=")))
  (is (= "utf-8" (client/parse-charset-encoding "charset=utf-8")))
  (is (= "utf-8" (client/parse-charset-encoding "text/html; charset=utf-8")))
  (is (= "utf-8" (client/parse-charset-encoding "text/html; charset  = utf-8")))
  (is (= "UTF-8" (client/parse-charset-encoding "text/html; charset=UTF-8"))))
