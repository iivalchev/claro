(ns claro.data.tree.map-entry
  (:require [claro.data.protocols :as p]
            [claro.data.tree.utils :as u])
  (:import [claro.data.protocols ResolvableTree]))

(deftype ResolvableMapEntry [resolvables k v]
  ResolvableTree
  (wrapped? [_]
    false)
  (processable? [_]
    true)
  (unwrap-tree [tree]
    tree)
  (partial-value [tree _]
    [(.-k tree) (.-v tree)])
  (resolved? [_]
    false)
  (resolvables* [tree]
    (.-resolvables tree))
  (apply-resolved-values [tree resolvable->value]
    (let [k' (u/apply-resolution (.-k tree) resolvable->value)
          v' (u/apply-resolution (.-v tree) resolvable->value)
          remaining (u/merge-resolvables k' v')]
      (if (empty? remaining)
        [k' v']
        (ResolvableMapEntry. remaining k' v')))))
