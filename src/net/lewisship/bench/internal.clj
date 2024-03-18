(ns ^:no-doc net.lewisship.bench.internal
  "Internal use; subject to change at any time."
  (:require [clojure.set :as set]))

(defmacro capture-symbols
  [exclude-keys]
  (reduce (fn [m k]
            (if (contains? exclude-keys k)
              m
              (assoc m (list 'quote k) k)))
          {}
          (keys &env)))
