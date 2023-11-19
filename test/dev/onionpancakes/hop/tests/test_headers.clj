(ns dev.onionpancakes.hop.tests.test-headers
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.headers :as h]))

(deftest test-headers-to-map
  (are [value expected] (= (h/to-map value) expected)
    (h/from-map {})              {}
    (h/from-map {"foo" ["bar"]}) {"foo" ["bar"]}))
