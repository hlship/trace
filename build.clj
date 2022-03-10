(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'io.github.hlship/trace)
(def version "v0.1")

(def jar-params {:project-name lib
                 :version version
                 :url "https://github.com/hlship/trace"})

(defn clean
  [_params]
  (b/delete {:path "target"}))

(defn jar
  [_params]
  ((requiring-resolve 'net.lewisship.build.jar/create-jar) jar-params))

(defn deploy
  [_params]
  (clean nil)
  (jar nil)
  ((requiring-resolve 'net.lewisship.build.jar/deploy-jar) jar-params))

(defn codox
  [_params]
  ((requiring-resolve 'net.lewisship.build.codox/generate)
   {:project-name lib
    :version version}))