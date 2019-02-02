(ns jvst.parse_test
  (:require [clojure.string :as str]))
  ;[clojure.edn :as edn]))

(defn parse-test
  "Converts tsv file into edn format."
  [path]
  (let [text (slurp (path))
        lines (str/split-lines text)
        cells (for [line lines] (str/split line #"\t"))
        labels (list :id :set :headword :furigana :example :correct :option-1 :option-2 :option-3 :option-4)
        questions (for [cell cells] (apply assoc {} (interleave labels cell)))
        ]
    (println questions))
  )
