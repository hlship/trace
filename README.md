# io.github.hlship/trace

Another small library, this one helps with REPL and output based debugging.

`trace` is a macro that (when enabled), will use Clojure's `tap>` to (by default) output a pretty-printed map of data to the console.


For example, using this function:

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

Outputting the map via `pprint` is merely the default operation; `tap>` provided to flexibily to replace or augment what
happens when `trace` is called.

In addition, there are `trace>` and `trace>>` macros used in threaded expressions (using `->` and `->>`).

