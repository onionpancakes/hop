(ns dev.onionpancakes.hop.request
  (:require [dev.onionpancakes.hop.keywords :as k]
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

(defprotocol HeaderValues
  (add-header-values [this header-name builder] "Add request builder header values."))

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

;; HeaderValues

(extend-protocol HeaderValues
  java.util.Collection
  (add-header-values [this header-name builder]
    (let [add-header-rf #(add-header-values %2 header-name %)]
      (reduce add-header-rf builder this)))
  String
  (add-header-values [this header-name builder]
    (.header ^HttpRequest$Builder builder header-name this))
  nil
  (add-header-values [_ _ builder] builder))

(defn add-request-builder-header-values
  [builder k values]
  (add-header-values values (name k) builder))

;; Headers

(defn add-request-builder-headers
  ^HttpRequest$Builder
  [builder headers]
  (reduce-kv add-request-builder-header-values builder headers))

;; Request

(defn set-request-builder-from-map
  "Sets HttpRequest Builder."
  ^HttpRequest$Builder
  [^HttpRequest$Builder builder m]
  (cond-> builder
    (contains? m :uri)             (.uri (uri (:uri m)))
    (contains? m :method)          (.method (upper-case (name (:method m))) 
                                            (body-publisher (:body m)))
    (contains? m :headers)         (add-request-builder-headers (:headers m))
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
