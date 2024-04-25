(ns build
  (:require [net.lewisship.build :as b]
            [clojure.tools.build.api :as build]))

(def lib 'io.github.hlship/trace)
(def version "1.3")

(def jar-params {:project-name lib
                 :version version
                 :scm
                 {:url     "https://github.com/hlship/trace"
                  :license :asl}})

(defn clean
  [_params]
  (build/delete {:path "target"}))

(defn jar
  [_params]
  (b/create-jar jar-params))

(defn deploy
  [_params]
  (clean nil)
  (b/deploy-jar (assoc (jar nil) :sign-artifacts? false)))

(defn codox
  [_params]
  (b/generate-codox {:project-name lib
                     :version      version}))
