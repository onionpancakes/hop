(ns dev.onionpancakes.hop.test-client
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.client :as client]
            [dev.onionpancakes.serval.jetty :as srv.jetty])
  (:import [java.net.http HttpClient$Redirect HttpClient$Version]))

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

(deftest test-client-empty
  (let [cl (client/client nil)]
    (is (.. cl (authenticator) (isEmpty)))
    (is (.. cl (connectTimeout) (isEmpty)))
    (is (.. cl (cookieHandler) (isEmpty)))
    (is (.. cl (executor) (isEmpty)))
    (is (= (.. cl (followRedirects)) HttpClient$Redirect/NEVER))
    (is (.. cl (proxy) (isEmpty)))
    (is (= (.. cl (version)) HttpClient$Version/HTTP_2))))

(deftest test-client
  (let [cl (client/client {:authenticator    (proxy [java.net.Authenticator] []
                                               (getPasswordAuthentication []
                                                 (java.net.PasswordAuthentication. "foo" "bar")))
                           :connect-timeout  (java.time.Duration/ofMinutes 5)
                           :cookie-handler   (proxy [java.net.CookieHandler] []
                                               (get [_ _])
                                               (put [_ _]))
                           :executor         (java.util.concurrent.ForkJoinPool/commonPool)
                           :follow-redirects :always
                           :proxy            :no-proxy
                           ;; SSL params too complicated for now.
                           :version          :http})]
    (is (.. cl (authenticator) (isPresent)))
    (is (.. cl (connectTimeout) (isPresent)))
    (is (.. cl (cookieHandler) (isPresent)))
    (is (.. cl (executor) (isPresent)))
    (is (= (.. cl (followRedirects)) HttpClient$Redirect/ALWAYS))
    (is (.. cl (proxy) (isPresent)))
    (is (= (.. cl (version)) HttpClient$Version/HTTP_1_1))))

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
      (is (= (:media-type resp) "text/plain"))
      (is (= (:character-encoding resp) "utf-8")))))

(deftest test-send-default-body
  (with-response {:serval.response/status             200
                  :serval.response/body               "foo"
                  :serval.response/content-type       "text/plain"
                  :serval.response/character-encoding "utf-8"}
    (let [resp (client/send server-uri)]
      (is (bytes? (:body resp)))
      (is (= (slurp (:body resp)) "foo")))))

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
      (is (= (:media-type resp) "text/plain"))
      (is (= (:character-encoding resp) "utf-8")))))

(deftest test-send-async-default-body
  (with-response {:serval.response/status             200
                  :serval.response/body               "foo"
                  :serval.response/content-type       "text/plain"
                  :serval.response/character-encoding "utf-8"}
    (let [resp (.get (client/send-async server-uri))]
      (is (bytes? (:body resp)))
      (is (= (slurp (:body resp)) "foo")))))
