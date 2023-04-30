(ns dev.onionpancakes.hop.protocols)

(defprotocol URI
  (uri [this] "Coerce to URI."))

(defprotocol RequestBody
  (body-publisher [this] "Coerce to BodyPublisher."))

(defprotocol RequestHeaders
  (request-headers [this] "Return as header kv entries."))

(defprotocol Request
  (request [this] "Build HttpRequest."))

;; Extend

(extend-protocol URI
  String
  (uri [this] (java.net.URI. this))
  java.net.URL
  (uri [this] (.toURI this))
  java.net.URI
  (uri [this] this))
