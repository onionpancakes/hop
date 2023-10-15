(ns dev.onionpancakes.hop.test-io
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.io :as io])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.util.zip GZIPOutputStream]))

(defn gzip-bytes
  [^bytes data]
  (let [out (ByteArrayOutputStream.)
        _   (doto (GZIPOutputStream. out)
              (.write data)
              (.close))]
    (.toByteArray out)))

(deftest test-decompress
  (are [data enc expected] (with-open [i (io/decompress data enc)]
                             (= (slurp i) expected))
    (gzip-bytes (.getBytes "foo"))                         "gzip" "foo"
    (.getBytes "foo")                                      nil    "foo"
    (ByteArrayInputStream. (gzip-bytes (.getBytes "foo"))) "gzip" "foo"
    (ByteArrayInputStream. (.getBytes "foo"))              nil    "foo"))
