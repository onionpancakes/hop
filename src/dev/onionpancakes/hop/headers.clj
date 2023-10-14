(ns dev.onionpancakes.hop.headers
  (:import [java.net.http HttpHeaders]))

(def headers-map-xf
  (map (juxt key (comp vec val))))

(defn headers-map
  [^HttpHeaders headers]
  (into {} headers-map-xf (.map headers)))
