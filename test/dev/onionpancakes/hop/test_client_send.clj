(ns dev.onionpancakes.hop.test-client-send
  (:require [dev.onionpancakes.hop.client :as client]
            [clojure.test :refer [deftest is use-fixtures]])
  (:import [org.eclipse.jetty.util Callback]
           [org.eclipse.jetty.server Handler$Abstract Response Server]))

;; Server

(def server-handler
  (proxy [Handler$Abstract] []
    (handle [request ^Response response ^Callback callback]
      (.setStatus response 200)
      (.. response
          (getHeaders)
          (put "content-type" "text/plain;charset=utf-8")
          (put "foo" "bar"))
      (-> (Response/asBufferedOutputStream request response)
          (spit "foo"))
      (.succeeded callback)
      true)))

(def server-port 42000)

(defonce server
  (doto (Server. server-port)
    (.setHandler server-handler)))

(defn with-server-started
  [f]
  (.start server)
  (f)
  (.stop server)
  (.join server))

(use-fixtures :once with-server-started)

;; Tests

(def request
  (str "http://localhost:" server-port))

(deftest test-send
  (let [resp (client/send request :string)]
    (is (= (:status resp) 200))
    (is (= (get (:headers resp) "foo") ["bar"]))
    (is (= (:body resp) "foo"))
    (is (= (:content-type resp) "text/plain;charset=utf-8"))
    (is (= (:media-type resp) "text/plain"))
    (is (= (:character-encoding resp) "utf-8"))))

(deftest test-send-default-body
  (let [resp (client/send request)]
    (is (bytes? (:body resp)))
    (is (= (slurp (:body resp)) "foo"))))

(deftest test-send-async
  (let [resp (-> (client/send-async request :string)
                 (.get))]
    (is (= (:status resp) 200))
    (is (= (get (:headers resp) "foo") ["bar"]))
    (is (= (:body resp) "foo"))
    (is (= (:content-type resp) "text/plain;charset=utf-8"))
    (is (= (:media-type resp) "text/plain"))
    (is (= (:character-encoding resp) "utf-8"))))

(deftest test-send-async-default-body
  (let [resp (.get (client/send-async request))]
    (is (bytes? (:body resp)))
    (is (= (slurp (:body resp)) "foo"))))
