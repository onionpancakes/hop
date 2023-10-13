(ns dev.onionpancakes.hop.request
  (:require [dev.onionpancakes.hop.protocols :as p]
            [dev.onionpancakes.hop.keys :as k]
            [clojure.string :refer [upper-case]])
  (:import [java.net.http HttpRequest HttpRequest$Builder HttpRequest$BodyPublishers]))

;; Protocols

(defprotocol URI
  (to-uri [this] "Return this as URI."))

(defprotocol Body
  (to-body-publisher [this] "Return this as BodyPublisher."))

(defprotocol Headers
  (set-headers [this builder] "Set request builder headers."))

(defprotocol Request
  (to-request [this] "Return this as HttpRequest."))

;; URI

(extend-protocol URI
  String
  (to-uri [this] (java.net.URI. this))
  java.net.URL
  (to-uri [this] (.toURI this))
  java.net.URI
  (to-uri [this] this))

;; Body

(extend-protocol Body
  (Class/forName "[B")
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  java.nio.file.Path
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile this))
  java.io.File
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile (.toPath this)))
  java.util.concurrent.Flow$Publisher
  (to-body-publisher [this]
    (HttpRequest$BodyPublishers/fromPublisher this))
  nil
  (to-body-publisher [_]
    (HttpRequest$BodyPublishers/noBody))
  java.net.http.HttpRequest$BodyPublisher
  (to-body-publisher [this] this))

;; Headers

(defn add-headers-from-key-values
  [builder key values]
  (let [header-name   (name key)
        add-header-rf #(.header ^HttpRequest$Builder % header-name %2)]
    (reduce add-header-rf builder values)))

(extend-protocol Headers
  java.util.Map
  (set-headers [this builder]
    (reduce-kv add-headers-from-key-values builder this))
  nil
  (set-headers [_ builder] builder))

(defn set-request-builder-headers
  ^HttpRequest$Builder
  [builder headers]
  (set-headers headers builder))

;; Request

(defn set-request-builder-from-map
  "Sets HttpRequest Builder."
  ^HttpRequest$Builder
  [^HttpRequest$Builder builder {:keys [uri method headers body
                                        timeout version expect-continue]}]
  (cond-> builder
    (some? uri)             (.uri (to-uri uri))
    (some? method)          (.method (upper-case (name method)) (to-body-publisher body))
    (some? headers)         (set-request-builder-headers headers)
    (some? timeout)         (.timeout timeout)
    (some? version)         (.version (k/http-client-version version version))
    (some? expect-continue) (.expectContinue expect-continue)))

(extend-protocol Request
  java.util.Map
  (to-request [this]
    (-> (HttpRequest/newBuilder)
        (set-request-builder-from-map this)
        (.build)))
  String
  (to-request [this]
    (.. (HttpRequest/newBuilder)
        (uri (to-uri this))
        (build)))
  java.net.URI
  (to-request [this]
    (.. (HttpRequest/newBuilder)
        (uri this)
        (build)))
  java.net.URL
  (to-request [this]
    (.. (HttpRequest/newBuilder)
        (uri (to-uri this))
        (build)))
  HttpRequest
  (to-request [this] this))

(defn request
  ^HttpRequest
  [req]
  (to-request req))
