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

(def parse-media-type-regex
  ;; Parses mimetype as #"^ <type> / <subtype> ;|$".
  ;; Conditions:
  ;;  - Must begin at the start of string.
  ;;  - May have spaces inbetween any of the tokens.
  ;;  - Must have one foward slash inbetween types.
  ;;  - Terminated by either semi-colon or end of string.
  #"^\s*+([^\s;/]++)\s*+/\s*+([^\s;/]++)\s*+(?:;|$)")

(defn parse-media-type
  "Parse the mimetype from a content-type string. Truncates all whitespace."
  [s]
  (when-let [parse (and s (re-find parse-media-type-regex s))]
    (str (second parse) "/" (nth parse 2))))

(def parse-character-encoding-regex
  ;; Parses charset as #" charset = <encoding> ".
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-character-encoding
  "Parse the character encoding from a content-type string."
  [s]
  (when s
    (second (re-find parse-character-encoding-regex s))))

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
