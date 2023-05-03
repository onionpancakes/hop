(ns dev.onionpancakes.hop.util
  (:require [clojure.string :refer [lower-case]])
  (:import [java.io ByteArrayInputStream]
           [java.util.zip GZIPInputStream]))

;; Parsing mimetype and charset is intentionally not to spec.
;; AKA, good enough for now.
;;
;; The spec compliant parsing is left to the user.
;; Refer to this for correct spec here:
;; https://www.w3.org/Protocols/rfc1341/4_Content-Type.html

(def parse-mimetype-regex
  ;; Parses mimetype as #"^ <type> / <subtype> ;|$".
  ;; Conditions:
  ;;  - Must begin at the start of string.
  ;;  - May have spaces inbetween any of the tokens.
  ;;  - Must have one foward slash inbetween types.
  ;;  - Terminated by either semi-colon or end of string.
  #"^\s*+([^\s;/]++)\s*+/\s*+([^\s;/]++)\s*+(?:;|$)")

(defn parse-mimetype
  "Parse the mimetype from a content-type string. Truncates all whitespace."
  [s]
  (when-let [parse (and s (re-find parse-mimetype-regex s))]
    (str (second parse) "/" (nth parse 2))))

(def parse-charset-regex
  ;; Parses charset as #" charset = <encoding> ".
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-charset-encoding
  "Parse the charset encoding from a content-type string."
  [s]
  (when s
    (second (re-find parse-charset-regex s))))

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
