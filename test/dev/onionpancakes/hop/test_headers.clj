(ns dev.onionpancakes.hop.test-headers
  (:require [clojure.test :refer [deftest is are]]
            [dev.onionpancakes.hop.headers :as h]))

(deftest test-parse-media-type
  (are [s expected] (= (h/parse-media-type s) expected)
    ""              nil
    "text"          nil
    "texthtml"      nil
    "text/html/xml" nil
    "text/html xml" nil
    "text/html"     "text/html"
    "text/html "    "text/html"
    "text/html;"    "text/html"
    "text / html ;" "text/html"
    "text/html+foo" "text/html+foo"))

(deftest test-parse-character-encoding
  (are [s expected] (= (h/parse-character-encoding s) expected)
    ""                             nil
    "charset="                     nil
    "charset=utf-8"                "utf-8"
    "text/html; charset=utf-8"     "utf-8"
    "text/html; charset =  utf-8"  "utf-8"
    "text/html; charset=UTF-8"     "UTF-8"
    "text/html+foo; charset=UTF-8" "UTF-8"))
