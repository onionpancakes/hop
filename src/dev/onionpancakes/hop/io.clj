(ns dev.onionpancakes.hop.io
  (:require [clojure.string :refer [lower-case]])
  (:import [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]))

;; Decompress

(defmulti ^java.io.InputStream decompress
  (fn [data encoding] [(class data) (some-> encoding (lower-case))]))

(defmethod decompress [(Class/forName "[B") "gzip"]
  [data _]
  (GZIPInputStream. (ByteArrayInputStream. data)))

(defmethod decompress [(Class/forName "[B") nil]
  [data _]
  (ByteArrayInputStream. data))

(defmethod decompress [java.io.InputStream "gzip"]
  [data _]
  (GZIPInputStream. data))

(defmethod decompress [java.io.InputStream nil]
  [data _]
  data)
