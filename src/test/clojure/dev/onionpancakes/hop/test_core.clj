(ns dev.onionpancakes.hop.test-core
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.core :as c])
  (:import [java.io InputStream ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPInputStream GZIPOutputStream]))

(deftest test-parse-mimetype
  (is (= nil (c/parse-mimetype nil)))
  (is (= nil (c/parse-mimetype "")))
  (is (= nil (c/parse-mimetype "text")))
  (is (= nil (c/parse-mimetype "texthtml")))
  (is (= nil (c/parse-mimetype "text/html/xml")))
  (is (= nil (c/parse-mimetype "text/html xml")))
  (is (= "text/html" (c/parse-mimetype "text/html")))
  (is (= "text/html" (c/parse-mimetype " text/html ")))
  (is (= "text/html" (c/parse-mimetype "text/html;")))
  (is (= "text/html" (c/parse-mimetype " text / html ;"))))

(deftest test-parse-charset-encoding
  (is (= nil (c/parse-charset-encoding nil)))
  (is (= nil (c/parse-charset-encoding "")))
  (is (= nil (c/parse-charset-encoding "charset=")))
  (is (= "utf-8" (c/parse-charset-encoding "charset=utf-8")))
  (is (= "utf-8" (c/parse-charset-encoding "text/html; charset=utf-8")))
  (is (= "utf-8" (c/parse-charset-encoding "text/html; charset  = utf-8")))
  (is (= "UTF-8" (c/parse-charset-encoding "text/html; charset=UTF-8"))))

(deftest test-decompress-body-gzip-not-compressed
  ;; Input stream body
  (let [req {:body (ByteArrayInputStream. (.getBytes "foo"))}]
    (with-open [input-stream (c/decompress-body-gzip req)]
      (is (instance? InputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream))))))
  ;; Bytes body
  (let [req {:body (.getBytes "foo")}]
    (with-open [input-stream (c/decompress-body-gzip req)]
      (is (instance? InputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream)))))))

(deftest test-decompress-body-gzip-compressed
  (let [out (ByteArrayOutputStream.)
        _   (doto (GZIPOutputStream. out)
              (.write (.getBytes "foo"))
              (.close))
        req {:body             (.toByteArray out)
             ;; Must be case insensitive.
             :content-encoding "GzIp"}]
    (with-open [input-stream (c/decompress-body-gzip req)]
      (is (instance? GZIPInputStream input-stream))
      (is (= "foo" (String. (.readAllBytes input-stream)))))))
