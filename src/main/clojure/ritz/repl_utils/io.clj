(ns ritz.repl-utils.io
  "io for reading clojure files")

(defn guess-namespace [^java.io.File file]
  (->>
   (reverse (.split (.getParent file) "/"))
   (reductions #(str %1 "." %2))
   (map symbol)
   (filter find-ns)
   first))

(defn- line-at-position [^java.io.File file position]
  (try
    (with-open [f (java.io.LineNumberReader. (java.io.FileReader. file))]
      (.skip f position)
      (.getLineNumber f))
    (catch Exception e 1)))

(defn read-position-line [file position]
  (if (number? position)
    (if (.isFile file)
      (line-at-position file  position)
      0)
    (when (list? position)
      (or
       (second (first (filter #(= :line (first %)) position)))
       (when-let [p (second (first (filter #(= :position (first %)) position)))]
         (line-at-position file p))))))

(defn read-ns
  "Given a reader on a Clojure source file, read until an ns form is found."
  [rdr]
  (let [form (try (read rdr false ::done)
                  (catch Exception e ::done))]
    (if (try
          (and (list? form) (= 'ns (first form)))
          (catch Exception _))
      (try
        (str form) ;; force the read to read the whole form, throwing on error
        (let [sym (second form)]
          (when (instance? clojure.lang.Named sym)
            sym))
        (catch Exception _))
      (when-not (= ::done form)
        (recur rdr)))))
