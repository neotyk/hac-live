
(ns hac-live.core
  (:require [http.async.client :as http]
            [cheshire.core :as json])
  (:use [clojure.pprint :only [pprint]]))

(comment

  ;; Create client
  (def c (http/create-client))

  
  ;; GET resource and inspect response
  (-> (http/GET c "http://localhost:8080/welcome")
      http/await
      pprint)

  
  ;; convenience functions
  (let [resp (http/GET c "http://localhost:8080/welcome")]
    (println "status")
    (-> resp
        http/status
        pprint)

    (println "headers")
    (-> resp
        http/headers
        pprint)

    (println "string")
    (-> resp
        http/string
        pprint))

  
  ;; reading json
  (let [resp (http/GET c "http://localhost:8080/json")]
    (-> resp
        http/string
        (json/parse-string true)
        pprint))

  
  ;; query parameters
  (let [resp (http/GET c "http://localhost:8080/json-q"
                       :query {:cmd "and control"})]
    (-> resp
        http/string
        (json/parse-string true)
        pprint))

  
  ;; Close client
  (http/close c)
  )
