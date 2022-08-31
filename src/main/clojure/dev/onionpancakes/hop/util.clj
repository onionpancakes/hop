(ns dev.onionpancakes.hop.util
  (:require [clojure.string :refer [lower-case]])
  (:import [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]))

;; Gzip

(defn ^java.io.InputStream decompress-body-gzip
  "Returns the decompressed input stream from the response body.
  Accepts either input-stream or bytes as body."
  [{:keys [body content-encoding]}]
  (let [is-gzip (if content-encoding
                  (= (lower-case content-encoding) "gzip"))]
    (cond-> body
      (bytes? body) (ByteArrayInputStream.)
      is-gzip       (GZIPInputStream.))))
