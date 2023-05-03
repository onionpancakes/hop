(ns dev.onionpancakes.hop.test-util
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]))

(deftest test-parse-mimetype
  (is (= nil (util/parse-mimetype nil)))
  (is (= nil (util/parse-mimetype "")))
  (is (= nil (util/parse-mimetype "text")))
  (is (= nil (util/parse-mimetype "texthtml")))
  (is (= nil (util/parse-mimetype "text/html/xml")))
  (is (= nil (util/parse-mimetype "text/html xml")))
  (is (= "text/html" (util/parse-mimetype "text/html")))
  (is (= "text/html" (util/parse-mimetype " text/html ")))
  (is (= "text/html" (util/parse-mimetype "text/html;")))
  (is (= "text/html" (util/parse-mimetype " text / html ;"))))

(deftest test-parse-charset-encoding
  (is (= nil (util/parse-charset-encoding nil)))
  (is (= nil (util/parse-charset-encoding "")))
  (is (= nil (util/parse-charset-encoding "charset=")))
  (is (= "utf-8" (util/parse-charset-encoding "charset=utf-8")))
  (is (= "utf-8" (util/parse-charset-encoding "text/html; charset=utf-8")))
  (is (= "utf-8" (util/parse-charset-encoding "text/html; charset  = utf-8")))
  (is (= "UTF-8" (util/parse-charset-encoding "text/html; charset=UTF-8"))))

(deftest test-decompress-body-gzip-not-compressed
  ;; Input stream body
  (let [value "foo"
        req   {:body (ByteArrayInputStream. (.getBytes value))}]
    (with-open [input-stream (util/decompress-body-gzip req)]
      (is (= value (String. (.readAllBytes input-stream))))))
  ;; Bytes body
  (let [value "foo"
        req   {:body (.getBytes value)}]
    (with-open [input-stream (util/decompress-body-gzip req)]
      (is (= value (String. (.readAllBytes input-stream)))))))

(deftest test-decompress-body-gzip-compressed
  (let [value "foo"
        out   (ByteArrayOutputStream.)
        _     (doto (GZIPOutputStream. out)
                (.write (.getBytes value))
                (.close))
        req   {:body             (.toByteArray out)
               :content-encoding "gzip"}]
    (with-open [input-stream (util/decompress-body-gzip req)]
      (is (= value (String. (.readAllBytes input-stream)))))))
