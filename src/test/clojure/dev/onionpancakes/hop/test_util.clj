(ns dev.onionpancakes.hop.test-util
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.util :as u])
  (:import [java.io InputStream ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPInputStream GZIPOutputStream]))

(deftest test-body-gzip-input-stream-not-compressed
  ;; Input stream body
  (let [req {:body (ByteArrayInputStream. (.getBytes "foo"))}]
    (with-open [input-stream (u/body-gzip-input-stream req)]
      (is (instance? InputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream))))))
  ;; Bytes body
  (let [req {:body (.getBytes "foo")}]
    (with-open [input-stream (u/body-gzip-input-stream req)]
      (is (instance? InputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream)))))))

(deftest test-body-gzip-input-stream-compressed
  (let [out (ByteArrayOutputStream.)
        _   (doto (GZIPOutputStream. out)
              (.write (.getBytes "foo"))
              (.close))
        req {:body             (.toByteArray out)
             :content-encoding "gzip"}]
    (with-open [input-stream (u/body-gzip-input-stream req)]
      (is (instance? GZIPInputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream)))))))
