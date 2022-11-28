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

(ns net.lewisship.trace-test
  (:require
    [clojure.test :refer [deftest is]]
    [net.lewisship.trace :as t
     :refer [trace trace> trace>> *compile-trace* *enable-trace*]]
    [net.lewisship.target :as target]
    [net.lewisship.trace.impl :as impl]))

;; Note: these tests may fail if executed from REPL (and trace compilation
;; is enabled).

(deftest trace-uncompiled-is-nil
  (binding [*compile-trace* false]
    (is (= nil
          (macroexpand-1 `(trace :foo 1 :bar 2))))))

;; Because line numbers are embedded, any changes above this line may make the lines below fail.

;; Disabled tests because work in Cursive but not from CLI.  Macros are tricky.

;; These are now further out of date due to changes related to set-ns-override!

#_
(deftest trace-with-compile-enabled
  (binding [*compile-trace* true]
    (is (= `(impl/emit-trace *enable-trace* 35 :foo 1 :bar 2)
           (macroexpand-1 '(trace :foo 1 :bar 2))))

    (is (= `(let [~'% ~'n] (impl/emit-trace *enable-trace* 38 :value ~'% :foo 1) ~'%)
           (macroexpand-1 '(trace> n :value % :foo 1))))

    (is (= `(let [~'% ~'n] (impl/emit-trace *enable-trace* 41 :value ~'% :bar 2) ~'%)
           (macroexpand-1 '(trace>> :value % :bar 2 n))))))

#_
(deftest emit-trace-expansion
  (binding [*compile-trace* true]
    (is (= `(when ~'flag?
              (tap> (array-map
                      :in (impl/extract-in)
                      :line 99
                      :thread (.getName (java.lang.Thread/currentThread))
                      :x 1
                      :y 2))
              nil)
           (macroexpand-1 '(impl/emit-trace flag? 99 :x 1 :y 2))))

    ;; When line number is not known:
    (is (= `(when ~'flag?
              (tap> (array-map
                      :in (impl/extract-in)
                      :thread (.getName (java.lang.Thread/currentThread))
                      :x 1
                      :y 2))
              nil)
           (macroexpand-1 '(impl/emit-trace flag? nil :x 1 :y 2))))))

;; The rest are just experiments used to manually test the macro expansions.

(defn calls-trace
  []
  (trace :msg "called"))

(defn calls-trace>
  []
  (-> {:value 1}
    (update :value inc)
    (trace> :data % :label :post-inc)
    (assoc :after true)))

(defn calls-trace>>
  []
  (->> (range 10)
    (map inc)
    (trace>> :values % :label :post-inc)
    (partition 2)))

(defn calls-extract-in
  []
  (impl/extract-in))

(deftest identifies-trace-location
  (is (= 'net.lewisship.trace-test/calls-extract-in
        (calls-extract-in))))

(comment

  (set! *print-meta* true)
  ;; Rest of this is very tricky to automated test due to dynamic nature of the macros.

  (calls-trace)
  ;; no output

  (t/setup-default)
  ;; Reload this NS to test the remainder:

  (macroexpand-1 '(trace :msg "hello"))
  (clojure.walk/macroexpand-all '(trace :msg "hello"))

  (calls-trace)                                             ; => nil
  ;; {:in net.lewisship.trace-test/calls-trace,
  ;;  :line 23,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :msg "called"}

  (t/set-ns-override!)
  (t/set-enable-trace! false)

  (calls-trace)                                             ; => nil


  (calls-trace>)                                            ; => {:value 2, :after true }
  ;; {:in net.lewisship.trace-test/calls-trace>,
  ;;  :line 25,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :data {:value 2},
  ;;  :label :post-inc}
  (macroexpand-1 '(trace> :value :foo :bar))

  (calls-trace>>)                                           ; => ((1 2) (3 4) (5 6) (7 8) (9 10))
  ;; {:in net.lewisship.trace-test/calls-trace>>,
  ;;  :line 32,
  ;;  :thread "nREPL-session-e439a250-d27a-474b-a694-69a97dbe5572",
  ;;  :values (1 2 3 4 5 6 7 8 9 10),
  ;;  :label :post-inc}
  (macroexpand-1 '(trace>> :foo :bar :value))

  (calls-extract-in)                                        ;; ==> net.lewisship.trace-test/calls-extract-in

  (target/do-work)
  )
