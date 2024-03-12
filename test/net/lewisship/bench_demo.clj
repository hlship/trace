(ns net.lewisship.bench-demo
  (:require [clojure.string :as string]
            [net.lewisship.bench :as bench]))


(comment

  (bench/bench {:sort? true} (reduce + 0 (range 0 10000))
               (+ 1 3)
               (apply * (range 1 20))
               (mapv inc (range 0 1000)))

  ;; Fast experiment with the output side.
  ;; This is where I confirmed that IntelliJ's console (not just Cursive's) doesn't handle
  ;; ANSI code properly.

  (#'bench/report true
    [{:expr-string "first"}
     {:expr-string "second"}
     {:expr-string "third"}]
    '[{:mean            [4.266282183424485E-5 (4.245749474127051E-5 4.310133277240219E-5)],
       :sample-variance [1.4094970219502932E-13 (0.0 0.0)]}
      {:mean            [5.266282183424485E-5 (4.245749474127051E-5 4.310133277240219E-5)],
       :sample-variance [1.4094970219502932E-13 (0.0 0.0)]}
     #_  {:mean            [7.17364466888305E-9 (7.128314456370565E-9 7.253923221472023E-9)],
       :sample-variance [7.067038554971197E-21 (0.0 0.0)]}
      {:mean            [1.2077515200737978E-5 (1.1952211907913209E-5 1.2175522861268199E-5)],
       :sample-variance [2.3028652061321932E-14 (0.0 0.0)]}])

  )
