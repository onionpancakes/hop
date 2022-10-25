# Hop

[![Run tests](https://github.com/onionpancakes/hop/actions/workflows/run_tests.yml/badge.svg)](https://github.com/onionpancakes/hop/actions/workflows/run_tests.yml)

Minimal wrapper around the JDK [HttpClient](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/package-summary.html).

# Status

Currently for my personal use. Future breaking changes possible.

# Deps

Add to deps.edn

```clojure
{:deps
  {dev.onionpancakes/hop
    {:git/url   "https://github.com/onionpancakes/hop"
     :git/sha   "<GIT SHA>"}}}
```

# Usage

Require the namespace.

```clojure
(require '[dev.onionpancakes.hop.client :as hop])
```

GET

```clojure
(hop/send {:uri "http://www.example.com"}
          :byte-array)
```

POST

```clojure
(hop/send {:method :POST
           :uri    "http://www.example.com"
           :body   "my post data"}
          :byte-array)
```

# License

Released under the MIT License.