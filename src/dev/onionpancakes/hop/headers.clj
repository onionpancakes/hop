(ns dev.onionpancakes.hop.headers
  (:import [java.net.http HttpHeaders]))

;; Headers

(def to-map-xf
  (map (juxt key (comp vec val))))

(defn to-map
  [^HttpHeaders headers]
  (into {} to-map-xf (.map headers)))

(def from-map-bi-predicate
  (reify java.util.function.BiPredicate
    (test [_ _ _] true)))

(defn from-map
  [m]
  (HttpHeaders/of m from-map-bi-predicate))
