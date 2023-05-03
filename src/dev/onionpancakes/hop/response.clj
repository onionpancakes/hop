(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keys :as k])
  (:import [java.net.http HttpResponse]))

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

;; Response

(defn response-map
  [^HttpResponse response]
  (let [headers          (.headers response)
        content-encoding (.. headers (firstValue "content-encoding") (orElse nil))
        content-type     (.. headers (firstValue "content-type") (orElse nil))]
    {:status           (.statusCode response)
     :headers          (.map headers)
     :body             (.body response)
     :content-encoding content-encoding
     :content-type     content-type
     :mimetype         (parse-mimetype content-type)
     :charset          (parse-charset-encoding content-type)}))

(def ^java.util.function.Function response-map-function
  (reify java.util.function.Function
    (apply [_ response]
      (response-map response))))

;; BodyHandler

(defn body-handler
  [bh]
  (k/http-response-body-handler bh bh))
