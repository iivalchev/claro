(ns claro.data.tree.collection
  (:require [claro.data.protocols :as p]
            [claro.data.tree.utils :as u])
  (:import [claro.data.protocols ResolvableTree]))

(deftype ResolvableCollection [resolvables prototype elements]
  ResolvableTree
  (wrapped? [_]
    false)
  (processable? [_]
    false)
  (unwrap-tree [tree]
    tree)
  (partial-value [_ _]
    (if (map? prototype)
      (into prototype u/all-unwrap-xf elements)
      (into prototype elements)))
  (resolved? [_]
    false)
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->value]
    (loop [elements     (transient elements)
           resolvables' nil
           index        0]
      (if (< index (count elements))
        (let [v (get elements index)]
          (if (p/resolved? v)
            (recur elements resolvables' (inc index))
            (let [v' (u/apply-resolution v resolvable->value)
                  rs (p/resolvables* v')]
              (recur
                (assoc! elements index v')
                (into resolvables' rs)
                (inc index)))))
        (let [elements (persistent! elements)]
          (if (empty? resolvables')
            (into prototype elements)
            (ResolvableCollection. resolvables' prototype elements)))))))

(defmethod print-method ResolvableCollection
  [value ^java.io.Writer writer]
  (print-method (p/partial-value value nil) writer))
