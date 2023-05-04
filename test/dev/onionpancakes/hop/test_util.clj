(ns dev.onionpancakes.hop.test-util
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.util :as util])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]))

(deftest test-parse-media-type
  (are [s expected] (= (util/parse-media-type s) expected)
    nil             nil
    ""              nil
    "text"          nil
    "texthtml"      nil
    "text/html/xml" nil
    "text/html xml" nil

    "text/html"     "text/html"
    "text/html "    "text/html"
    "text/html;"    "text/html"
    "text / html ;" "text/html"))

(deftest test-parse-character-encoding
  (are [s expected] (= (util/parse-character-encoding s) expected)
    nil        nil
    ""         nil
    "charset=" nil

    "charset=utf-8"              "utf-8"
    "text/html; charset=utf-8"   "utf-8"
    "text/html; charset =  utf-8" "utf-8"
    "text/html; charset=UTF-8"   "UTF-8"))

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
