# io.github.hlship/trace

[![Clojars Project](https://img.shields.io/clojars/v/io.github.hlship/trace.svg)](https://clojars.org/io.github.hlship/trace)
[![cljdoc badge](https://cljdoc.org/badge/io.github.hlship/trace)](https://cljdoc.org/d/io.github.hlship/trace)

Another small library, this one improves the experience when using output to debug code using the REPL.

In my experience, using `prn` to output debugging information works well enough in small cases, 
but doesn't scale when there is a lot of data to be printed, or a lot of threads are involved.  
It just becomes a jumble of output.

`trace` is a macro that (when enabled), will use Clojure's `tap>` to (by default) output a pretty-printed map of data to the console.

For example, consider this Ring handler function:

```clojure
(ns my.example.ring-handler
  (:require [net.lewisship.trace :refer [trace]]))

(defn handle-request
  [request] 
  (trace
    :method (:request-method request)
    :uri (:uri request))
  ;; Off to do some request handler type things
  )
```

When invoked at runtime, the following console output will be produced:

```clojure
{:in my.example.ring-handler/handle-request,
 :line 6,
 :thread "nREPL-session-c3dde1ce-ca19-4e78-95ad-d0e4beda61eb",
 :method :get,
 :uri "/status"}
```

`trace` has automatically identified the executing function name, the line number, and the thread; the remaining keys
in the map are provided as key/value pairs in the `trace` call.

Patterned after logging, `trace` calls may be compiled or not - when `net.lewisship.trace/*compile-trace*` is false
(the default), the `trace` macro expands to nil.  This means it is safe to leave `trace` calls in production code if
it can be assured that they will not be compiled.

Further, when compiled, if `net.lewisship.trace/*enable-trace*` is false then the map is not created or provided to `tap>`.

Outputting the map via `pprint` is merely the default operation; `tap>` provides the flexibility to replace or augment what
happens when `trace` is called.  For example, a tap could `dissoc` the :thread key before pretty-printing, if the thread
name is not interesting.

In addition, there are `trace>` and `trace>>` macros used in threaded expressions (using `->` and `->>`).

## Per-Namespace Override

`net.lewisship.trace/set-ns-override!` can be used to enable specific namespaces to be traced
even when the global trace flag (via `set-enable-trace!`) is set to false.

## Tagged Literal

Often, even using `trace` is a bit cumbersome; the #trace/result tagged literal precedes
a form, and will `trace` the form and the result of evaluating the form, and evaluate to the result.

Example:

```
(defn handle-request
  [request]
  (if (string/ends-with? #trace/result (:uri request) "/")
    {:status 401
     :body   "Invalid request"})
  (handle-resource-request (:uri request)))

> (handle-request {:request-method :get :uri "/status"})
=> {:status 200 ...}
{:in my.example.ring-handler/handle-request,
:line 11,
:thread "nREPL-session-62724fb3-7086-49bb-9d8f-4b238de8d01e",
:form (:uri request),
:result "/status"}
```

To enable this feature, create a `data_readers.clj` resource with the following value (or merge this entry into your existing `data_readers.clj`):

```
{trace/result net.lewisship.trace/trace-result-reader}
```

You must have the above file, or you'll see a RuntimeException "No reader function for tag trace/result" when
you load your namespace with the #trace/result tag.

## Benchmarking

Even though io.github.hlship/trace is used for REPL-oriented testing, it also includes a wrapper around
[Criterium](https://github.com/hugoduncan/criterium) to benchmark small snippets of code.

The `net.lewisship.bench` namespace provides a simple `bench` macro.

```
(let [list-data   (doall (map inc (range 1000)))
      vector-data (vec list-data)
      pred        #(< 900 %)
      v1          (fn [pred coll] (first (filter pred coll)))
      v2          (fn [pred coll] (reduce (fn [_ v] (when (pred v)
                                                      (reduced v)))
                                          nil coll))]
  (bench
    (v1 pred list-data)
    (v1 pred vector-data)
    (v2 pred list-data)
    (v2 pred vector-data)))
┏━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━┳━━━━━━━━━━━━━┳━━━━━━━━━┓
┃            Expression ┃     Mean ┃         Var ┃   Ratio ┃
┣━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━╋━━━━━━━━━━━━━╋━━━━━━━━━┫
┃   (v1 pred list-data) ┃ 11.54 µs ┃ ± 122.15 ns ┃ 180.1 % ┃
┃ (v1 pred vector-data) ┃ 11.62 µs ┃ ± 177.11 ns ┃ 181.3 % ┃ (slowest)
┃   (v2 pred list-data) ┃  6.85 µs ┃  ± 68.80 ns ┃ 107.0 % ┃
┃ (v2 pred vector-data) ┃  6.41 µs ┃ ± 191.67 ns ┃ 100.0 % ┃ (fastest)
┗━━━━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━┻━━━━━━━━━━━━━┻━━━━━━━━━┛

```

The actual output uses some [ANSI fonts](https://github.com/clj-commons/pretty) to highlight the
fastest and slowest expressions. The first argument to bench can be a map that provides options 
for how to execute the benchmarks, and how to format the result.

The `bench-for` macro builds on this, using an implicit `for` to build the expressions;
it does some re-writing of the expression that's reported in the table
to capture the symbols provided by the `for` bindings:

```
(let [inputs {:list   (doall (map inc (range 1000)))
                :vector (vec (doall (map inc (range 1000))))}
      pred   (fn [value] #(< % value))
      v1     (fn [pred coll] (first (filter pred coll)))
      v2     (fn [pred coll] (reduce (fn [_ v] (when (pred v)
                                                 (reduced v)))
                                     nil coll))]
  (bench-for [input [:list :vector]
              count [5 50 500]]
             (v1 (pred count) (input inputs))
             (v2 (pred count) (input inputs))))
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━┳━━━━━━━━━━━┳━━━━━━━━━━━┓
┃                       Expression ┃      Mean ┃       Var ┃     Ratio ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╋━━━━━━━━━━━╋━━━━━━━━━━━╋━━━━━━━━━━━┫
┃     (v1 (pred 5) (:list inputs)) ┃ 459.03 ns ┃ ± 4.47 ns ┃ 1,353.9 % ┃
┃     (v2 (pred 5) (:list inputs)) ┃  33.90 ns ┃ ± 1.27 ns ┃   100.0 % ┃ (fastest)
┃    (v1 (pred 50) (:list inputs)) ┃  82.29 ns ┃ ± 1.67 ns ┃   242.7 % ┃
┃    (v2 (pred 50) (:list inputs)) ┃ 500.76 ns ┃ ± 2.72 ns ┃ 1,477.0 % ┃
┃   (v1 (pred 500) (:list inputs)) ┃ 473.59 ns ┃ ± 6.02 ns ┃ 1,396.9 % ┃
┃   (v2 (pred 500) (:list inputs)) ┃  81.51 ns ┃ ± 1.40 ns ┃   240.4 % ┃
┃   (v1 (pred 5) (:vector inputs)) ┃ 498.76 ns ┃ ± 7.44 ns ┃ 1,471.1 % ┃
┃   (v2 (pred 5) (:vector inputs)) ┃  34.63 ns ┃ ± 0.77 ns ┃   102.1 % ┃
┃  (v1 (pred 50) (:vector inputs)) ┃  34.43 ns ┃ ± 0.45 ns ┃   101.5 % ┃
┃  (v2 (pred 50) (:vector inputs)) ┃  81.52 ns ┃ ± 1.58 ns ┃   240.4 % ┃
┃ (v1 (pred 500) (:vector inputs)) ┃ 514.36 ns ┃ ± 4.27 ns ┃ 1,517.1 % ┃ (slowest)
┃ (v2 (pred 500) (:vector inputs)) ┃ 512.08 ns ┃ ± 6.29 ns ┃ 1,510.4 % ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━┻━━━━━━━━━━━┻━━━━━━━━━━━┛
```

Notice how the `input` and `count` symbols have been replaced with a specific value
for that execution?  Be careful about using collections directly as inputs, as the (possibly infinite!)
contents of those collections will be part of the expression.
