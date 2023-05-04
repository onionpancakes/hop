(ns dev.onionpancakes.hop.response
  (:require [dev.onionpancakes.hop.keys :as k]
            [dev.onionpancakes.hop.util :as util]
            [dev.onionpancakes.hop.datafy :as datafy])
  (:import [java.net.http HttpResponse]))

;; Response

(defn response-map
  [^HttpResponse response]
  (datafy/HttpResponse->map response))

(def ^java.util.function.Function response-map-function
  (reify java.util.function.Function
    (apply [_ response]
      (response-map response))))

;; BodyHandler

(defn body-handler
  [bh]
  (k/http-response-body-handler bh bh))
