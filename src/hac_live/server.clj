
(ns hac-live.server
  (:require [noir.server :as server]
            [noir.content.getting-started]
            [noir.response :as response])
  (:use [noir.core :only [defpartial defpage]]
        [hiccup.core :only [html]]
        [hiccup.page-helpers :only [include-css html5]]))

(defpartial layout [& content]
            (html5
              [:head
               [:title "http.async.client live"]
               (include-css "/css/reset.css")]
              [:body
               [:div#wrapper
                content]]))

(defpage "/welcome" []
  (layout [:p "Welcome to http.async.client live"]))

(defpage "/json" []
  (response/json {:status 0 :msg "simple, right?"}))

(defpage "/json-q" {cmd :command}
  (response/json {:status 0
                  :command cmd
                  :msg "now with query argument you passed"}))

(server/load-views-ns 'hac-live.server)

(defn -main [& m]
  (let [port (Integer. (get (System/getenv) "PORT" "8080"))]
    (server/start port {:mode :dev :ns 'hac-live.server})))

;;
