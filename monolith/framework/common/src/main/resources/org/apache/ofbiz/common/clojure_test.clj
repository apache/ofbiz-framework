;; Licensed to the Apache Software Foundation (ASF) under one
;; or more contributor license agreements.  See the NOTICE file
;; distributed with this work for additional information
;; regarding copyright ownership.  The ASF licenses this file
;; to you under the Apache License, Version 2.0 (the
;; "License"); you may not use this file except in compliance
;; with the License.  You may obtain a copy of the License at
;;
;; http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing,
;; software distributed under the License is distributed on an
;; "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
;; KIND, either express or implied.  See the License for the
;; specific language governing permissions and limitations
;; under the License.
(ns org.apache.ofbiz.common.clojure-test
  (:import [org.apache.ofbiz.base.util Debug]
           [org.apache.ofbiz.service ServiceUtil]
           [org.apache.ofbiz.service ModelService]))

(def ^String module (str (ns-name 'org.apache.ofbiz.common.clojure-test)))

(defn log-context
  "Logs context if not empty."
  [^java.util.Map ctx]
  (when-not (.isEmpty ctx)
    (doseq [keyval ctx]
      (Debug/logInfo (str "---- SVC-CONTEXT: "  (key keyval)  " => " (val keyval)) module))))


(defn test-clojure-svc
  "Clojure test service."
  [dctx ctx]
  (let [response (ServiceUtil/returnSuccess)
        has-message? (contains? ctx "messages")]
    (log-context ctx)
    (if has-message?
      (.put response "resp" "no message found")
      (do
        (Debug/logInfo (str "-----SERVICE TEST----- : " (.get ctx "message")) module)
        (.put response "resp" "service done")))
    (Debug/logInfo (str "----- SVC: " (.getName dctx) " -----") module)
    response))

(defn echo-service
  "Echo back all the parameters"
  [dctx ctx]
  (doto (new java.util.LinkedHashMap)
        (.putAll ctx)
        (.put ModelService/RESPONSE_MESSAGE ModelService/RESPOND_SUCCESS)))
