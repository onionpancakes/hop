(ns dev.onionpancakes.hop.util
  (:require [clojure.java.io :as io])
  (:import [java.util.zip GZIPInputStream]))

(defn body-gzip-input-stream
  [{:keys [body content-encoding]}]
  (cond-> (io/input-stream body)
    (= content-encoding "gzip") (GZIPInputStream.)))

(def parse-charset-regex
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-charset
  [{:keys [content-type]}]
  (some->> content-type
           (re-find parse-charset-regex)
           (second)))
