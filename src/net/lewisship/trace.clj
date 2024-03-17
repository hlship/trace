; Copyright (c) 2022-present Howard Lewis Ship.
;
; Licensed under the Apache License, Version 2.0 (the "License")
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.

(ns net.lewisship.trace
  "Light-weight, asynchronous logging built around `clojure.core/tap>`.

  Follows the same pattern as `clojure.core/assert`: When tracing is not compiled,
  the tracing macros should create no runtime overhead.

  When tracing is compiled, a check occurs to see if tracing is enabled; only then
  do the most expensive operations (e.g., identifying the function containing the
  trace call) occur, as well as the call to `clojure.core/tap>`."
  (:require [net.lewisship.trace.impl :as impl :refer [emit-trace enabled?]]
            [clojure.pprint :refer [pprint]]))

(def ^:dynamic *compile-trace*
  "If false (the default), calls to `trace` evaluate to nil (and `trace>` and `trace>>` simply return
 the threaded value)."
  false)

(def ^:dynamic *enable-trace*
  "If false (the default is true) then compiled calls to `trace` (and `trace>` and `trace>>`)
  are a no-op."
  true)

(defn set-compile-trace!
  "Sets the default value of the `*compile-trace*` var.

  Remember that after changing this, it may be necessary to re-load namespaces for the change to take effect."
  [value]
  (alter-var-root #'*compile-trace* (constantly value)))

(defn set-enable-trace!
  "Sets the default value of the `*enable-trace*` var.

  Changes take effect immediately.

  When this global flag is true, tracing is enabled for all namespaces.
  When this flag is false, tracing is only enabled for namespaces specifically enabled via
  [[set-ns-override!]]."
  [value]
  (alter-var-root #'*enable-trace* (constantly value)))

(defmacro trace
  "Calls to trace generate a map that is passed to `tap>`.

  The map includes keys:

  * :in - a symbol of the namespace and function
  * :line - the line number of the trace invocation (if available)
  * :thread - the string name of the current thread

  Additional keys and values may be supplied.

  `trace` expands to nil, if compilation is disabled.

  Any invocation of `trace` evaluates to nil."
  [& kvs]
  (assert (even? (count kvs))
          "pass key/value pairs")
  (when *compile-trace*
    (let [ns-symbol (ns-name *ns*)
          {:keys [line]} (meta &form)]
      `(when (enabled? *enable-trace* '~ns-symbol)
         (emit-trace ~line ~@kvs)))))

(defmacro trace>
  "A version of `trace` that works inside `->` thread expressions.  Within the
  `trace>` body, `%` is bound to the threaded value. When compilation is disabled,
  it simply evaluates to the threaded value."
  [value & kvs]
  (assert (even? (count kvs))
          "pass key/value pairs")
  (if-not *compile-trace*
    value
    (let [ns-symbol (ns-name *ns*)
          {:keys [line]} (meta &form)]
      `(let [~'% ~value]
         (when (enabled? *enable-trace* '~ns-symbol)
           (emit-trace ~line ~@kvs))
         ~'%))))

(defmacro trace>>
  "A version of `trace` that works inside `->>` thread expressions.  Within the
  `trace>>` body, `%` is bound to the threaded value.  When compilation is disabled,
  it simply evaluates to the threaded value."
  ;; This is tricky because the value comes at the end due to ->> so we have to
  ;; work harder (fortunately, at compile time) to separate the value expression
  ;; from the keys and values.
  [& kvs-then-value]
  (let [value (last kvs-then-value)
        kvs (butlast kvs-then-value)]
    (assert (even? (count kvs))
            "pass key/value pairs")
    (if-not *compile-trace*
      value
      (let [ns-symbol (ns-name *ns*)
            {:keys [line]} (meta &form)]
        `(let [~'% ~value]
           (when (enabled? *enable-trace* '~ns-symbol)
             (emit-trace ~line ~@kvs))
           ~'%)))))


(defn trace-result-reader
  "A reader for the #trace/result tagged literal.  When compilation is off,
  returns the form unchanged.  When compilation is enabled, it will trace
  the form (as :form) and its evaluation (as :result), and evaluate
  to the result."
  {:added "1.2.0"}
  [form]
  (if-not *compile-trace*
    form
    (let [result (gensym "result-")
          trace-call (with-meta
                       `(trace :form '~form
                               :result ~result)
                       ;; Copy meta (line and location) so that trace can capture the line number
                       (meta form))]
      `(let [~result ~form]
         ~trace-call
         ~result))))

(defn setup-default
  "Enables tracing output with a default tap of `pprint`."
  []
  (set-compile-trace! true)
  (set-enable-trace! true)
  (add-tap pprint))

(defn set-ns-override!
  "Enables or disables tracing for a single namespace (by default, the current namespace).
  The namespace must be a simple symbol.  Enabling tracing for a namespace overrides the
  global flag managed by [[set-enable-trace!]] (tracing occurs if either the global flag
  is true, or the namespace is specifically enabled).

  Manages a set of namespaces that are enabled in this way."
  {:added "1.1"}
  ([]
   (set-ns-override! true))
  ([enabled?]
   (set-ns-override! (ns-name *ns*) enabled?))
  ([ns-symbol enabled?]
   (impl/set-ns-enabled! ns-symbol enabled?)))

