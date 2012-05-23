;; ## live.clj -- http.async.client live demo

; Copyright 2012 Hubert Iwaniuk
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;   http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.




(ns ^{:doc     "http.async.client live"
      :author  "Hubert Iwaniuk"
      :project "https://github.com/neotyk/http.async.client/"
      :source  "https://github.com/neotyk/hac-live"}
  hac.live
  
  (:require [http.async.client :as http]
            [http.async.client.request :as http-r]
            [clojure.java.io :as io]
            [cheshire.core :as json])
  (:use [clojure.pprint :only [pprint]]))

;; create clients
(def client (http/create-client))


                                        ; request 1
;; simple GET
(def resp1 (http/await
            (http/GET client
                      "http://localhost:8108/")))

;; status line
(-> resp1
    http/status
    pprint)

;; headers
(-> resp1
    http/headers
    pprint)

;; is redirect
(println (http/redirect? resp1))

;; redirect location
(println (http/location resp1))


                                        ; request 2
;; follow redirects
(def redirect-client (http/create-client
                      :follow-redirects true))

;; other client configuration options:
;; - user agent branding,
;; - http compression,
;; - keep alive,
;; - max connections per host and global,
;; - timeouts.
;; - proxy.
;; - authentication (basic, digest),
;; - ssl certificates.

(def resp2 (http/await
            (http/GET redirect-client
                      "http://localhost:8108/")))

;; save to file
(let [body (http/body resp2)]
  (with-open [f (io/output-stream "info.json")]
    (.writeTo body f)))

;; body content type
(println (http/content-type resp2))

;; parse json
(-> resp2 http/string (json/parse-string true) pprint)

;; close
(http/close redirect-client)


                                        ; request 3
;; query parameters
(let [resp (http/await
            (http/GET client "http://localhost:8108/q.json"
                      :query {:command "get things done"}))]
  (-> resp
      http/string
      (json/parse-string true)
      pprint))

;; also supported:
;; - headers,
;; - body (string, form, InputStream, multipart),
;; - cookies,
;; - proxy,
;; - authentication,
;; - timeouts.


                                        ; request 4
;; stream: lazy seq
(let [resp (http/stream-seq
            client :get
            "http://localhost:8108/stream.json")]
  (doseq [p (http/string resp)]
    (print (json/parse-string p true)))
  (println "\ndone"))

;; this is backed by LinkedBlockingQueue
;; similar to c.c.seque, except no agents


                                        ; request 5
;; stream: callback
(let [resp (http/request-stream
            client :get
            "http://localhost:8108/stream.json"
            ;; body part callback
            (fn [_ part] ;; response map, body part
              (print (-> part
                         str
                         (json/parse-string true)))
              ;; deliver part to body promise, only first time
              ;; continue processing
              [part :continue]))]
  (http/await resp)
  (println "\ndone"))


                                        ; request 6
;; callbacks
(http-r/execute-request
 client (http-r/prepare-request
         :get "http://localhost:8108/stream.json")
 :part (fn [resp part]
         (println :p part)
         (let [body (:body resp)]
           (if (realized? body)
             (do (.writeTo part @body)
                 [nil :continue])
             [part :continue])) ;; store part in (:body resp)
         )
 :completed (fn [resp]
              (println :d (http/string resp))))

;; supported callbacks:
;; - status line received
;; - headers received
;; - body part received
;; - body completed
;; - error


                                        ; request 7
;; callbacks: result
(http-r/execute-request
 client (http-r/prepare-request :get "http://localhost:8108/stream.json")
 :part (fn [resp part]
         (println :p part)
         [:test :continue] ;; store arbitrary data in (:body resp)
         )
 :completed (fn [resp]
              (println :d (http/body resp))))


                                        ; request 8
;; callbacks: aborting
(http-r/execute-request
 client (http-r/prepare-request
         :get "http://localhost:8108/stream.json")
 :part (fn [resp part]
         (println :p part)
         [part :abort])
 :completed (fn [resp]
              (println :d (http/string resp))))


                                        ; request 9
;; callbacks: counting body parts
(http-r/execute-request
 client (http-r/prepare-request
         :get "http://localhost:8108/stream.json")
 :part (fn [resp part]
         (let [counter (if (realized? (:body resp))
                         (:body resp)
                         (atom 0))]
           (swap! counter inc)
           (println :p (http/string part))
           [counter :continue]))
 :completed (fn [resp]
              (println :d @@(:body resp) ;atom within a promise
                       )))


                                        ; request 10
;; callbacks: counting body parts
(let [counter (atom 0)]
  (http-r/execute-request
   client (http-r/prepare-request
           :get "http://localhost:8108/stream.json")
   :part (fn [resp part]
           (swap! counter inc)
           (println :p @counter)
           ;; delegate to default part callback
           (http-r/body-collect resp part))
   :completed (fn [resp]
                (println :d @counter))))


                                        ; request 11
;; a bit more requests executed
(let [r (http-r/prepare-request :get "http://localhost:8108/stream.json")]
  (dotimes [n 20]
    (http-r/execute-request
     client r
     :part (fn [_ _]
             [n :continue]) ;; just store n in body
     :completed (fn [resp]
                  (println (str :d " " (http/body resp)))))))


                                        ; request 12
;; websocket support
(http/websocket
 client "ws://localhost:8108/socket"

 :text (fn [soc msg]
         (println (str "< " msg))
         (when (re-matches #"Echo WebSocket - .*" msg)
               (future (doseq [msg (map str (range 7))]
                         (http/send soc :text msg)
                         (println (str "> " msg))))
               (future (Thread/sleep 100)
                       (http/close soc))))

 :open (fn [soc]
         (println "ws opened"))

 :close (fn [soc code reason]
          (println "ws closed:" code)))


                                        ; request 13
;; 'real' ws server
(let [one (atom true)]
  (http/websocket
   client "ws://lab01.kungfoo.pl:8108/socket"
   :text (fn [soc msg]
           (println "<" msg)
           (when @one
             (swap! one not)
             (doseq [msg ["/nick h.a.c." "/msg Hello!"]]
               (http/send soc :text msg))
             (future (Thread/sleep 10000)
                     (http/send soc :text "/msg Bye!")
                     (http/close soc))))))



;; close client
(http/close c)