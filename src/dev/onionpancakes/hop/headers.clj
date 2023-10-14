(ns dev.onionpancakes.hop.headers
  (:import [java.net.http HttpHeaders]))

;; Parse

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

(def parse-media-type-function
  (reify java.util.function.Function
    (apply [_ s]
      (parse-media-type s))))

;; Character Encoding

(def parse-character-encoding-regex
  ;; Parses charset as #" charset = <encoding> ".
  #"(?i)charset\s*+=\s*+(\S++)")

(defn parse-character-encoding
  "Parse the character encoding from a content-type string."
  [s]
  (when s
    (second (re-find parse-character-encoding-regex s))))

(def parse-character-encoding-function
  (reify java.util.function.Function
    (apply [_ s]
      (parse-character-encoding s))))

;; Headers

(def headers-map-xf
  (map (juxt key (comp vec val))))

(defn headers-map
  [^HttpHeaders headers]
  (into {} headers-map-xf (.map headers)))
