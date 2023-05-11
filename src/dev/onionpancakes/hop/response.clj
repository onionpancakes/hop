(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keys :as k]
            [dev.onionpancakes.hop.datafy :as datafy])
  (:import [java.net.http HttpResponse]))

;; Response

(defn response-map
  "Return response map from HttpResponse object."
  [^HttpResponse response]
  (datafy/HttpResponse->map response))

(def ^java.util.function.Function response-map-function
  "Function which returns response map from HttpResponse object."
  (reify java.util.function.Function
    (apply [_ response]
      (response-map response))))

;; BodyHandler

(defn body-handler
  "Return as BodyHandler."
  [bh]
  (k/http-response-body-handler bh bh))
