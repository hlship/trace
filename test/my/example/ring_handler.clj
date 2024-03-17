(ns my.example.ring-handler
  (:require [clojure.string :as string]
            [net.lewisship.trace :as trace :refer [trace]]))

(trace/setup-default)

(defn handle-resource-request [uri])

(defn handle-request
  [request]
  (if (string/ends-with? #trace/result (:uri request) "/")
    {:status 401
     :body   "Invalid request"})
  (handle-resource-request (:uri request)))

(comment
  (handle-request {:request-method :get :uri "/status"})

  *data-readers*
  )