(ns d2l.nlp
  (:require [clojure.reflect :refer [reflect]]
            [clojure.pprint :as pp]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as string]
            [clojure.data.csv :refer [read-csv]]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.xml]
            [cheshire.core :as json]
            [pad.prn.core :refer [linst]]
            [pad.coll.core :refer [contained?]]
            [pad.io.core :refer [read-nth-line count-lines]]
            [pad.core :refer [str-float? str>>float resolve-var]]
            [pad.math.core :refer [vec-standard-deviation-2
                                   scalar-subtract elwise-divide
                                   vec-mean scalar-divide
                                   mk-one-hot-vec std vec-normalize vec-norm]]
            [pad.mxnet.bert :as bert]
            [pad.mxnet.core :refer [read-glove! glove-path normalize normalize-row]]
            [org.apache.clojure-mxnet.io :as mx-io]
            [org.apache.clojure-mxnet.context :as context]
            [org.apache.clojure-mxnet.module :as m]
            [org.apache.clojure-mxnet.symbol :as sym]
            [org.apache.clojure-mxnet.kvstore :as kvstore]
            [org.apache.clojure-mxnet.kvstore-server :as kvstore-server]
            [org.apache.clojure-mxnet.eval-metric :as eval-metric]
            [org.apache.clojure-mxnet.optimizer :as optimizer]
            [org.apache.clojure-mxnet.lr-scheduler :as lr-scheduler]
            [org.apache.clojure-mxnet.initializer :as initializer]
            [org.apache.clojure-mxnet.resource-scope :as resource-scope]
            [org.apache.clojure-mxnet.ndarray :as nd]
            [org.apache.clojure-mxnet.ndarray-api :as ndapi]
            [org.apache.clojure-mxnet.dtype :as dtype]
            [org.apache.clojure-mxnet.callback :as callback]
            [org.apache.clojure-mxnet.layout :as layout]
            [org.apache.clojure-mxnet.random :as random]
            [org.apache.clojure-mxnet.shape :as shape]
            [org.apache.clojure-mxnet.infer :as infer]
            [org.apache.clojure-mxnet.visualization :as viz])
  (:gen-class))

(def data-dir "./tmp/data/nlp/")
(def glove-dir "./tmp/data/glove/")


(defn load-data!
  []
  (:exit (sh "bash" "-c" "bash bin/data.sh glove" :dir "/opt/app")))

#_(load-data!)

(defn vec>>ndarray
  [v]
  (nd/array v [(count v)]))

(comment

  (nd/concatenate [(nd/array [1 2] [2]) (nd/array [1 2] [2])])
  (def v (get glove-embeddings "matrix"))

  (do
    (def glove (-> (glove-path glove-dir 50) (read-glove!)))
    (def glove-embeddings (:token-to-embedding glove))
    (def glove-idxs (:idx-to-token glove))
    (def glove-normalized (->> glove-embeddings
                               (seq)
                               (reduce (fn [a [k v]]
                                         (assoc a k (vec-normalize v))) {})))
    (def glove-mx (nd/array
                   (mapcat second (seq glove-normalized))
                   [(count glove-normalized) (-> glove-normalized (first) (second) (count))])))

  (def w-baby (vec>>ndarray (get glove-normalized "baby")))
  (nd/norm w-baby)
  (def w-dot (nd/dot glove-mx w-baby))
  (def topk (-> (ndapi/topk {:data w-dot :axis 0 :k 5 :ret-typ "indices"})))
  (->> topk (nd/->vec) (mapv #(get glove-idxs (int %))))

  ;
  )

