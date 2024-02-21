# Hop

Minimal wrapper for Java's [HttpClient](https://docs.oracle.com/en/java/javase/21/docs/api/java.net.http/java/net/http/package-summary.html).

# Status

[![Run tests](https://github.com/onionpancakes/hop/actions/workflows/run_tests.yml/badge.svg)](https://github.com/onionpancakes/hop/actions/workflows/run_tests.yml)

Currently for my personal use. Future breaking changes possible.

# Deps

Add to deps.edn

```clojure
{:deps
  {dev.onionpancakes/hop
    {:git/url   "https://github.com/onionpancakes/hop"
     :git/sha   "<GIT SHA>"}}}
```

# Examples

### GET

```clojure
(require '[dev.onionpancakes.hop.client :as hop])

(hop/send "http://www.example.com")

(hop/send {:uri "http://www.example.com"} :input-stream)
```

### POST

```clojure
(hop/send {:method :POST
           :uri    "http://www.example.com"
           :body   "my post data"})
```


# Usage

Require the namespace.

```clojure
(require '[dev.onionpancakes.hop.client :as hop])
```

## Send Requests

Send some request.

* First arg is a request map. Strings are accepted as shorthand as `{:uri <str value>}`.
* Second arg is a `BodyHandler`. When omitted, defaults to `:byte-array`.

```clojure
(def resp
  (hop/send {:uri "http://www.example.com"}))
```

Read the response as a lookup map.

```clojure
(println (:status resp)) ; 200
(println (:headers resp)) ; { some headers map value }
(println (:body resp)) ; Default body data as byte-array
(println (:media-type resp)) ; "text/html"
(println (:character-encoding resp)) ; "UTF-8"
(println (:content-type resp)) ; "text/html; charset=UTF-8"
```

## Set Accept-Encoding Manually

Set accept-encoding manually.

```clojure
(def resp
  (hop/send {:uri     "http://www.example.com"
             :headers {:accept-encoding "gzip"}}))
```

### Decompress

```clojure
(require '[dev.onionpancakes.hop.io :as io])

(slurp (io/decompress (:body resp) "gzip"))
```

# License

Released under the MIT License.