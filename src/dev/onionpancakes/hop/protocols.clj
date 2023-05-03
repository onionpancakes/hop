(ns dev.onionpancakes.hop.protocols)

(defprotocol URI
  (uri [this] "Return this as URI."))

(defprotocol RequestBody
  (body-publisher [this] "Return this as BodyPublisher."))

(defprotocol RequestHeaders
  (request-headers [this] "Return this as [name value] entries."))

(defprotocol Request
  (request [this] "Return this as HttpRequest."))

;; Extend

(extend-protocol URI
  String
  (uri [this] (java.net.URI. this))
  java.net.URL
  (uri [this] (.toURI this))
  java.net.URI
  (uri [this] this))
