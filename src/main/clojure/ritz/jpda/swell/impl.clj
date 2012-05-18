(ns ritz.jpda.swell.impl
  (:require
   [clojure.string :as string]
   [ritz.jpda.jdi :as jdi]
   [ritz.jpda.jdi-clj :as jdi-clj]))

(def ^{:dynamic true} *available-restarts* nil)
(def ^{:dynamic true} *selected-restart* nil)

(def swell-restart-message "ritz-handle-swell-restarts")

(defn handle-restarts
  [e restarts]
  (binding [*available-restarts* restarts
            *selected-restart* nil]
    (try
      (throw (Exception. swell-restart-message e))
      (catch Exception _))
    (when *selected-restart*
      [*selected-restart* nil])))

(defn available-restarts
  [context thread]
  (when-let [s (jdi-clj/eval-to-string
                context thread jdi/invoke-single-threaded
                `(when-let [v# (ns-resolve
                                'ritz.jpda.swell.impl '~'*available-restarts*)]
                   (string/join "---" (map pr-str (var-get v#)))))]
    (->>
     (string/split s #"---")
     (remove string/blank?)
     (map read-string))))

(defn select-restart
  [context thread restart]
  (jdi-clj/eval-to-string
   context thread jdi/invoke-single-threaded
   `(try
      (when-let [v# (ns-resolve
                     'ritz.jpda.swell.impl '~'*selected-restart*)]
        (.set v# (read-string ~(pr-str restart)))
        nil)
      (catch Exception e#
        (println e#)
        (.printStackTrace e#)))))
