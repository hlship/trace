(ns net.lewisship.bench-demo
  (:require [net.lewisship.bench :as bench :refer [bench* bench bench-for]]))

(comment

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

  (bench {:sort? true} (reduce + 0 (range 0 10000))
               (+ 1 3)
               (apply * (range 1 20))
               (mapv inc (range 0 1000)))

  ;; Fast experiment with the output side.
  ;; This is where I confirmed that IntelliJ's console (not just Cursive's) doesn't handle
  ;; ANSI codes properly.

  (#'bench/report true
    [{:expr-string "first"}
     {:expr-string "second"}
     {:expr-string "third"}]
    '[{:mean            [4.266282183424485E-5 (4.245749474127051E-5 4.310133277240219E-5)],
       :sample-variance [1.4094970219502932E-13 (0.0 0.0)]}
      {:mean            [5.266282183424485E-5 (4.245749474127051E-5 4.310133277240219E-5)],
       :sample-variance [1.4094970219502932E-13 (0.0 0.0)]}
      #_{:mean            [7.17364466888305E-9 (7.128314456370565E-9 7.253923221472023E-9)],
         :sample-variance [7.067038554971197E-21 (0.0 0.0)]}
      {:mean            [1.2077515200737978E-5 (1.1952211907913209E-5 1.2175522861268199E-5)],
       :sample-variance [2.3028652061321932E-14 (0.0 0.0)]}])


  (macroexpand-1
    '(bench-for [x (range 2)]
                (+ x x)
                (* x x)))

  (bench-for {:progress? true}
             [x (range 3)
              y (range 0 x)]
             (+ y x)
             (* x y))

  (with-redefs [bench* prn]
    (let [coll (range 1000)]
      (bench-for nil [{n :count} [{:count 5} {:count 50}]]
                 (reduce + (take n coll))))

    )

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

  (bench-for false)
  )


