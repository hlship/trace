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
  (:require [io.aviso.exception :refer [demangle]]
            [clojure.string :as string]))

(defn ^:private extract-fn-name
  [class-name]
  (let [[ns-id & raw-function-ids] (string/split class-name #"\$")
        fn-name (->> raw-function-ids
                     (map #(string/replace % #"__\d+" ""))
                     (map demangle)
                     (string/join "/"))]
    (symbol (demangle ns-id) fn-name)))

(defn ^:private in-trace-ns?
  [^StackTraceElement frame]
  (string/starts-with? (.getClassName frame) "net.lewisship.trace.impl$"))

(defn extract-in
  []
  (let [stack-frame (->> (Thread/currentThread)
                         .getStackTrace
                         (drop 1)                           ; Thread/getStackTrace
                         (drop-while in-trace-ns?)
                         first)]
    (extract-fn-name (.getClassName ^StackTraceElement stack-frame))))

(defmacro emit-trace
  [enabled? trace-line & kvs]
  ;; Maps are expected to be small; array-map ensures that the keys are in insertion order.
  `(when ~enabled?
     (tap> (array-map
             :in (extract-in)
             ~@(when trace-line [:line trace-line])
             :thread (.getName (Thread/currentThread))
             ~@kvs))
     nil))
