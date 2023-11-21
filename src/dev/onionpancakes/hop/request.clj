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

(defprotocol HeaderValue
  (add-header-to-request-builder [this builder header-name] "Adds header to request builder."))

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

(extend-protocol HeaderValue
  clojure.lang.Indexed
  (add-header-to-request-builder [this builder header-name]
    (loop [i 0 cnt (count this)]
      (when (< i cnt)
        (-> (nth this i)
            (add-header-to-request-builder builder header-name))
        (recur (inc i) cnt))))
  clojure.lang.Seqable
  (add-header-to-request-builder [this builder header-name]
    (doseq [value this]
      (add-header-to-request-builder value builder header-name)))
  String
  (add-header-to-request-builder [this builder header-name]
    (.header ^HttpRequest$Builder builder header-name this))
  Object
  (add-header-to-request-builder [this builder header-name]
    (.header ^HttpRequest$Builder builder header-name (str this)))
  nil
  (add-header-to-request-builder [_ builder _] builder))

(defn add-request-builder-header
  [builder k value]
  (add-header-to-request-builder value builder (name k))
  builder)

(defn add-request-builder-headers
  ^HttpRequest$Builder
  [builder headers]
  (reduce-kv add-request-builder-header builder headers))

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
  Object
  (request [this]
    (.. (HttpRequest/newBuilder (uri this))
        (build)))
  HttpRequest
  (request [this] this))
