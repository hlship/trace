(ns net.lewisship.bench
  "Useful wrappers around criterium."
  {:added "1.1.0"}
  (:require [criterium.core :as c]
            [clj-commons.format.table :as table]))

(defn- wrap-expr-as-block
  ;; blocks are what I call the inputs to criterium
  ([expr]
   (wrap-expr-as-block expr (str expr)))
  ([expr title]
   {:f           `(fn [] ~expr)
    :expr-string title}))

(defn- format-estimate
  [estimate]
  (let [mean (first estimate)
        [scale unit] (c/scale-time mean)]
    (format "%.2f %s" (* scale mean) unit)))

(defn- format-estimate-sqrt
  [estimate]
  (let [mean (Math/sqrt (first estimate))
        [scale unit] (c/scale-time mean)]
    (format "%.2f %s" (* scale mean) unit)))

(defn- col-width
  [k coll]
  (reduce max (map #(-> % k count) coll)))

(defn- report
  [sort? blocks results]
  (let [fastest-mean (->> results
                          (map #(-> % :mean first))
                          (reduce min))
        lines        (mapv (fn [i {:keys [expr-string]} {:keys [mean sample-variance]}]
                             (let [simple-mean (first mean)]
                               {:expression         expr-string
                                :mean               simple-mean
                                :ratio              (format "%,.1f %%"
                                                            (* 100.0 (/ simple-mean fastest-mean)))
                                :row                i
                                :formatted-mean     (format-estimate mean)
                                :formatted-variance (str "± " (format-estimate-sqrt sample-variance))}))
                           (iterate inc 0) blocks results)
        lines'       (sort-by :mean lines)
        decorate?    (not sort?)
        fastest-row  (-> lines' first :row)
        slowest-row  (-> lines' last :row)]

    (table/print-table
      (cond-> {:columns
               [:expression
                {:key   :formatted-mean
                 :title "Mean"}
                {:key   :formatted-variance
                 :title "Var"}
                {:key :ratio
                 :pad :left}]}
        decorate? (assoc :default-decorator (fn [row _]
                                              (cond
                                                (= row fastest-row)
                                                :bright-green.bold

                                                (= row slowest-row)
                                                :yellow))
                         :row-annotator
                         (fn [row _]
                           (cond
                             (= row fastest-row)
                             [:bright-green.bold " (fastest)"]


                             (= row slowest-row)
                             [:yellow " (slowest)"]))))
      (if sort?
        lines'
        lines))))

(defn- benchmark-block
  [options block]
  (c/progress "Benchmarking" (:expr-string block) "...")
  (c/benchmark* (:f block) options))

(defn bench*
  "The core of the [[bench ]]macro; the expressions to `bench` are converted into blocks, each a map
  with keys :f (a no-args function) and :expr-str (the string representation of the form being
  benchmarked)."
  [opts blocks]
  (let [{:keys [quick? progress? round-robin? report? sort?]
         :or   {quick?       true
                round-robin? true
                report?      true
                sort?        false
                progress?    false}} opts
        benchmark-options (merge (if quick?
                                   c/*default-quick-bench-opts*
                                   c/*default-benchmark-opts*)
                                 opts)
        results           (binding [c/*report-progress* progress?]
                            (if round-robin?
                              (c/benchmark-round-robin* blocks benchmark-options)
                              (mapv #(benchmark-block benchmark-options %) blocks)))]
    (if report?
      (report sort? blocks results)
      results)))

(defmacro bench
  "Benchmarks a sequence of expressions. Criterium is used to perform the benchmarking,
  then the results are reported in a tabular format, which the fastest and slowest
  expressions highlighted (marked in green and yellow, respectively).

  The first argument may be a map of options, rather than an expression to benchmark.

  Options:
  : :quick?  If true (the default), used quick benchmarking options
  : :round-robin? If true (the default), uses round-robin testing of the expressions rather
    than running an independent benchmark for each expression.
  : report? If true (the default), print a report and return nil.  Otherwise,
    returns a seq of benchmarking stats as returned by Criterium.
  : progress? If true (the default is false), enable Criterium progress reporting during benchmark
    collection.
  : sort? If true (the default is false), then when results are printed, they are
    sorted fastest to slowest (with no highlighting).

  In addition, the options are passed to Criterium, allowing overrides of the options
  it uses when benchmarking, such as :samples, etc."
  [& exprs]
  (let [[expr & more-exprs] exprs
        [opts all-exprs] (if (map? expr)
                           [expr more-exprs]
                           [nil exprs])]
    (assert (every? list? all-exprs)
            "Each benchmarked expression must be a list (a function call)")
    (assert (seq all-exprs)
            "No expressions to benchmark")
    `(bench* ~opts ~(mapv wrap-expr-as-block all-exprs))))
