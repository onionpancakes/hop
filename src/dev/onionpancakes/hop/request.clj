(ns dev.onionpancakes.hop.request
  (:require [dev.onionpancakes.hop.keys :as k]
            [clojure.string :refer [upper-case]])
  (:import [java.net.http
            HttpRequest
            HttpRequest$Builder
            HttpRequest$BodyPublishers]))

;; Protocols

(defprotocol URI
  (uri [this] "Return as URI."))

(defprotocol Body
  (body-publisher [this] "Return as BodyPublisher."))

(defprotocol Headers
  (set-headers [this builder] "Set request builder headers."))

(defprotocol Request
  (^HttpRequest request [this] "Return as HttpRequest."))

;; URI

(extend-protocol URI
  String
  (uri [this] (java.net.URI. this))
  java.net.URL
  (uri [this] (.toURI this))
  java.net.URI
  (uri [this] this))

;; Body

(extend-protocol Body
  (Class/forName "[B")
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofByteArray this))
  String
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofString this))
  java.nio.file.Path
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile this))
  java.io.File
  (body-publisher [this]
    (HttpRequest$BodyPublishers/ofFile (.toPath this)))
  java.util.concurrent.Flow$Publisher
  (body-publisher [this]
    (HttpRequest$BodyPublishers/fromPublisher this))
  java.net.http.HttpRequest$BodyPublisher
  (body-publisher [this] this)
  nil
  (body-publisher [_]
    (HttpRequest$BodyPublishers/noBody)))

;; Headers

(defn add-headers-from-key-values
  [builder k values]
  (let [header-name   (name k)
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
  [^HttpRequest$Builder builder m]
  (cond-> builder
    (contains? m :uri)             (.uri (uri (:uri m)))
    (contains? m :method)          (.method (upper-case (name (:method m))) 
                                            (body-publisher (:body m)))
    (contains? m :headers)         (set-request-builder-headers (:headers m))
    (contains? m :timeout)         (.timeout (:timeout m))
    (contains? m :version)         (.version (k/version (:version m) (:version m)))
    (contains? m :expect-continue) (.expectContinue (:expect-continue m))))

(extend-protocol Request
  java.util.Map
  (request [this]
    (-> (HttpRequest/newBuilder)
        (set-request-builder-from-map this)
        (.build)))
  String
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri (uri this))
        (build)))
  java.net.URI
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri this)
        (build)))
  java.net.URL
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri (uri this))
        (build)))
  HttpRequest
  (request [this] this))
