(ns dev.onionpancakes.hop.test-util
  (:require [clojure.test :refer [deftest is]]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]))

(deftest test-parse-media-type
  (is (= nil (util/parse-media-type nil)))
  (is (= nil (util/parse-media-type "")))
  (is (= nil (util/parse-media-type "text")))
  (is (= nil (util/parse-media-type "texthtml")))
  (is (= nil (util/parse-media-type "text/html/xml")))
  (is (= nil (util/parse-media-type "text/html xml")))
  (is (= "text/html" (util/parse-media-type "text/html")))
  (is (= "text/html" (util/parse-media-type " text/html ")))
  (is (= "text/html" (util/parse-media-type "text/html;")))
  (is (= "text/html" (util/parse-media-type " text / html ;"))))

(deftest test-parse-character-encoding
  (is (= nil (util/parse-character-encoding nil)))
  (is (= nil (util/parse-character-encoding "")))
  (is (= nil (util/parse-character-encoding "charset=")))
  (is (= "utf-8" (util/parse-character-encoding "charset=utf-8")))
  (is (= "utf-8" (util/parse-character-encoding "text/html; charset=utf-8")))
  (is (= "utf-8" (util/parse-character-encoding "text/html; charset  = utf-8")))
  (is (= "UTF-8" (util/parse-character-encoding "text/html; charset=UTF-8"))))

(deftest test-decompress-bytes-gzip
  (let [value "foo"
        out   (ByteArrayOutputStream.)
        _     (doto (GZIPOutputStream. out)
                (.write (.getBytes value))
                (.close))
        data  (.toByteArray out)
        enc   "gzip"]
    (with-open [input-stream (util/decompress data enc)]
      (is (= value (slurp input-stream))))))

(deftest test-decompress-bytes-uncompressed
  (let [value "foo"
        data  (.getBytes value)
        enc   nil]
    (with-open [input-stream (util/decompress data enc)]
      (is (= value (slurp input-stream))))))

(deftest test-decompress-input-stream-gzip
  (let [value "foo"
        out   (ByteArrayOutputStream.)
        _     (doto (GZIPOutputStream. out)
                (.write (.getBytes value))
                (.close))
        data  (ByteArrayInputStream. (.toByteArray out))
        enc   "gzip"]
    (with-open [input-stream (util/decompress data enc)]
      (is (= value (slurp input-stream))))))

(deftest test-decompress-input-stream-uncompressed
  (let [value "foo"
        data  (ByteArrayInputStream. (.getBytes value))
        enc   nil]
    (with-open [input-stream (util/decompress data enc)]
      (is (= value (slurp input-stream))))))
