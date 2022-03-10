(ns my.example.ring-handler
  (:require [net.lewisship.trace :refer [trace]]))

(defn handle-request
  [request]
  (trace
    :method (:request-method request)
    :uri (:uri request))
  ;; Off to do some request handler type things
  )

(comment
  (handle-request {:request-method :get :uri "/status"})

  )