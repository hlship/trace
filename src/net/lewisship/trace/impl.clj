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

(ns ^:no-doc net.lewisship.trace.impl
  (:require [clj-commons.format.exceptions :refer [format-stack-trace-element]]
            [clojure.string :as string]))

(def *enabled-namespaces (atom #{}))

(defn set-ns-enabled!
  [ns-symbol flag]
  {:pre [(simple-symbol? ns-symbol)]}
  (let [op (if flag conj disj)]
    (swap! *enabled-namespaces op ns-symbol))
  nil)

(defn enabled?
  [global-flag current-ns]
  (or global-flag
      (contains? @*enabled-namespaces (ns-name current-ns))))

(defn ^:private in-trace-ns?
  [^StackTraceElement frame]
  (string/starts-with? (.getClassName frame) "net.lewisship.trace.impl$"))

(defn extract-in
  []
  (let [element (->> (Thread/currentThread)
                         .getStackTrace
                         (drop 1)                           ; Thread/getStackTrace
                         (drop-while in-trace-ns?)
                         first)
        frame-name (format-stack-trace-element element)]
    (symbol frame-name)))

(defmacro emit-trace
  [trace-line & kvs]
  ;; Maps are expected to be small; array-map ensures that the keys are in insertion order.
  `(do
     (tap> (array-map
             :in (extract-in)
             ~@(when trace-line [:line trace-line])
             :thread (.getName (Thread/currentThread))
             ~@kvs))
     nil))
