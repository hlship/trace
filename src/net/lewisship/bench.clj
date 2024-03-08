(ns net.lewisship.bench
  "Useful wrappers around criterium."
  {:added "1.1.0"}
  (:require [criterium.core :as c]
            [clj-commons.ansi :refer [pcompose]]))


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
  (let [lines       (mapv (fn [{:keys [expr-string]} {:keys [mean sample-variance]}]
                            {:title              expr-string
                             :mean               mean
                             :formatted-mean     (format-estimate mean)
                             :formatted-variance (str "± " (format-estimate-sqrt sample-variance))})
                          blocks results)
        lines'      (sort-by #(-> % :mean first) lines)
        fastest     (first lines')
        slowest     (last lines')
        title-width (max 10 (col-width :title lines'))
        mean-width  (max 4 (col-width :formatted-mean lines'))
        var-width   (max 3 (col-width :formatted-variance lines'))]
    (pcompose
      [{:font  :bold
        :width title-width} "Expression"]
      " | "
      [{:font  :bold
        :width mean-width} "Mean"]
      " | "
      [{:font  :bold
        :width var-width} "Var"])
    (doseq [l (if sort? lines' lines)]
      (pcompose
        [{:font (cond
                  sort? nil

                  (= l fastest)
                  :bright-green.bold
                  (= l slowest)
                  :yellow)}
         [{:width title-width}
          (:title l)]
         " | "
         [{:width mean-width}
          (:formatted-mean l)]
         " | "
         [{:width var-width}
          (:formatted-variance l)]
         (cond
           sort? nil

           (= l fastest)
           " (fastest)"

           (= l slowest)
           " (slowest)")]))))

(defn- benchmark-block
  [options block]
  (c/progress "Benchmarking" (:expr-string block) "...")
  (c/benchmark* (:f block) options))

(defn bench*
  [opts & blocks]
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
  : :round-robin? If true (the default), used round-robin testing of the expressions rather
    than running a benchmark for each expression.
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
        [opts all-exprs]
        (if (map? expr)
          [expr more-exprs]
          [nil exprs])]
    (assert (every? list? all-exprs)
            "Each benchmarked expression must be a list (a function call)")
    (assert (seq all-exprs)
            "No expressions to benchmark")
    `(apply bench* ~opts
            ~(mapv wrap-expr-as-block all-exprs))))

