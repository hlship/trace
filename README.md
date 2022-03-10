# io.github.hlship/trace

[![Clojars Project](https://img.shields.io/clojars/v/io.github.hlship/trace.svg)](https://clojars.org/io.github.hlship/trace)

[API Documentation](https://hlship.github.io/docs/trace/)

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

Further, when compiled, if `new.lewisship.trace/*enable-trace*` is false then the map is not created or provided to `tap>`.

Outputting the map via `pprint` is merely the default operation; `tap>` provides the flexibility to replace or augment what
happens when `trace` is called.  For example, a tap could `dissoc` the :thread key before pretty-printing, if the thread
name is not interesting.

In addition, there are `trace>` and `trace>>` macros used in threaded expressions (using `->` and `->>`).

