(ns dev.onionpancakes.hop.test-util
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]))

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
