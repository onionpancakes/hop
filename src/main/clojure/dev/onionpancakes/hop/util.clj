(ns dev.onionpancakes.hop.util
  (:import [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]))

(defn decompress-body-gzip
  "Returns the decompressed input stream from the response body.
  Accepts either input-stream or bytes as body."
  [{:keys [body content-encoding]}]
  (cond-> body
    (bytes? body) (ByteArrayInputStream.)
    (= content-encoding "gzip") (GZIPInputStream.)))

(def parse-charset-regex
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-charset
  "Parse the charset from the response's content-type."
  [{:keys [content-type]}]
  (some->> content-type
           (re-find parse-charset-regex)
           (second)))
