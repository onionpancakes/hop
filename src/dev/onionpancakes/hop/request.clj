(ns dev.onionpancakes.hop.request
  (:require [dev.onionpancakes.hop.protocols :as p]
            [dev.onionpancakes.hop.keys :as k]
            [clojure.string :refer [upper-case]])
  (:import [java.net.http HttpRequest HttpRequest$Builder HttpRequest$BodyPublishers]))

;; Body

(extend-protocol p/RequestBody
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
  nil
  (body-publisher [_]
    (HttpRequest$BodyPublishers/noBody))
  java.net.http.HttpRequest$BodyPublisher
  (body-publisher [this] this))

;; Headers

(defn request-headers-from-map-entry
  [^java.util.Map$Entry entry]
  (let [header-name (some-> (key entry) (name))]
    (eduction (comp (map (partial vector header-name))
                    (filter (partial every? some?)))
              (val entry))))

(def request-headers-from-map-xf
  (mapcat request-headers-from-map-entry))

(defn request-headers-from-map
  [m]
  (eduction request-headers-from-map-xf m))

(extend-protocol p/RequestHeaders
  java.util.Map
  (request-headers [this]
    (request-headers-from-map this)))

;; Build request

(defn add-request-builder-headers-rf
  [^HttpRequest$Builder builder [k v]]
  (.header builder k v))

(defn ^HttpRequest$Builder add-request-builder-headers
  [builder headers]
  (reduce add-request-builder-headers-rf builder headers))

(defn ^HttpRequest$Builder set-request-builder
  [^HttpRequest$Builder builder {:keys [uri method headers body
                                        timeout version expect-continue]}]
  (cond-> builder
    uri             (.uri (p/uri uri))
    method          (.method (upper-case (name method)) (p/body-publisher body))
    headers         (add-request-builder-headers (p/request-headers headers))
    timeout         (.timeout timeout)
    version         (.version (k/http-client-version version version))
    expect-continue (.expectContinue expect-continue)))

(extend-protocol p/Request
  java.util.Map
  (request [this]
    (-> (HttpRequest/newBuilder)
        (set-request-builder this)
        (.build)))
  String
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri (p/uri this))
        (build)))
  java.net.URI
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri this)
        (build)))
  java.net.URL
  (request [this]
    (.. (HttpRequest/newBuilder)
        (uri (p/uri this))
        (build)))
  HttpRequest
  (request [this] this))

(defn request
  [req]
  (p/request req))
