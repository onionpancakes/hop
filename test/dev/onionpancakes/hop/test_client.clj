(ns dev.onionpancakes.hop.test-client
  (:require [dev.onionpancakes.hop.client :as client]
            [clojure.test :refer [deftest is]])
  (:import [java.net.http HttpClient$Redirect HttpClient$Version]))

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


