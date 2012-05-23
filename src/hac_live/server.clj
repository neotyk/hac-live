(ns hac-live.server
  (:use [hac-live.server.handlers :only [demo-server]]
        [aleph.http :only [start-http-server wrap-ring-handler]]))

(defn -main [& m]
  (let [port (or (when-let [p (System/getenv "PORT")]
                   (Integer/parseInt p))
                 8108)]
    (start-http-server (wrap-ring-handler #'demo-server)
                       {:port                     port
                        :websocket                true
                        :streaming-ring-requests? true})))
