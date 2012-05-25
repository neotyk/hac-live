(ns hac.server.handlers
  (:use [ring.util.response :only [redirect]]
        [status-codes.compojure])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [aleph.http :as aleph]
            [lamina.core :as lamina]
            [compojure.core :as compojure]
            [cheshire.core :as json]))

(defn websocket-handler [ch {ip :remote-addr}]
  (lamina/enqueue ch "Echo WebSocket - http.async.client demo")
  (lamina/siphon ch ch))

(defn stream-handler [request]
  (let [response-ch (lamina/channel)]
    (future
      (dotimes [x 10]
        (Thread/sleep 200)
        (lamina/enqueue response-ch (json/generate-string {:i x})))
      (lamina/close response-ch))
    {:status  :ok
     :headers {"Content-Type" "application/json"}
     :body    response-ch}))

(def demo-server
  (handler/api
   (compojure/routes
    (compojure/GET "/" [] (redirect "info.json"))
    (compojure/GET "/info.json" []
                   {:status  :ok
                    :headers {"Content-Type" "application/json"}
                    :body    (json/generate-string
                              {:server "demo"})})
    (compojure/GET "/q.json" [command]
                   {:status  :ok
                    :headers {"Content-Type" "application/json"}
                    :body    (json/generate-string
                              {:status  :ok
                               :command command
                               :output  "got it done!"})})
    (compojure/GET "/stream.json" [] stream-handler)
    (compojure/GET "/socket" [] (aleph/wrap-aleph-handler websocket-handler))
    (route/not-found "This is not what you are looking for."))))

