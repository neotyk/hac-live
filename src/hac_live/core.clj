
(ns hac-live.core
  (:require [http.async.client :as http]
            [http.async.client.request :as http-r]
            [cheshire.core :as json])
  (:use [clojure.pprint :only [pprint]]))

;;

;; Create client
(def c (http/create-client))

;;

;; GET HTTP resource
(-> (http/GET c "http://localhost:8080/welcome")
    http/await
    pprint)

;;

;; Inspecting HTTP response
(def response (http/GET c "http://localhost:8080/json"))

;; HTTP response on the wire:
(-> response
    http/status
    pprint)

(-> response
    http/headers
    pprint)

(-> response
    http/cookies
    pprint)

(-> response
    http/string
    pprint)

;;

;; reading json
(-> response
    http/string
    (json/parse-string true)
    pprint)

;;

;; Provide query parameters to request
(let [resp (http/GET c "http://localhost:8080/json-q"
                     :query {:command "get things done"})]
  (-> resp
      http/string
      (json/parse-string true)
      pprint))

;;

;; Issue 20 request w/o waiting for each one
(let [reqs (for [n (range 20)] (http/GET c "http://localhost:8080/json-q"
                                         :query {:command n}))]
  ;; Issue 20 requests w/o waiting for response
  (doall reqs)
  (doseq [r reqs]
    (-> r
        http/string
        (json/parse-string true)
        :command
        println)))

;;

(http-r/execute-request c (http-r/prepare-request :get "http://localhost:8080/json")
                        :part (fn [resp part]
                                (println :p part)
                                [part :continue] ;; store part in (:body resp)
                                )
                        :completed (fn [resp]
                                     (println :d @(:body resp))))

;;

(http-r/execute-request c (http-r/prepare-request :get "http://localhost:8080/json")
                        :part (fn [resp part]
                                (println :p part)
                                [:test :continue] ;; store arbitrary data in (:body resp)
                                )
                        :completed (fn [resp]
                                     (println :d @(:body resp))))

;;

;; a bit more requests executed
(let [r (http-r/prepare-request :get "http://localhost:8080/json")]
  (dotimes [n 20]
    (http-r/execute-request c r
                            :part (fn [_ _]
                                    [n :continue]) ;; just store n in body
                            :completed (fn [resp]
                                         (println :d @(:body resp))))))

;;

;; Close client
(http/close c)
