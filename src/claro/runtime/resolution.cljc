(ns claro.runtime.resolution
  (:require [claro.runtime.impl :as impl]))

(defn- assert-deferrable
  "Make sure the given value is a deferrable, throw `IllegalStateException`
   otherwise."
  [{:keys [impl]} batch value]
  (when-not (impl/deferrable? impl value)
    (throw
      (IllegalStateException.
        (str "'resolve-fn' has to return a deferrable for class "
             (-> batch first class (.getName))
             ", returned:"
             (-> value class (.getName))))))
  value)

(defn- generate-deferred
  "Create a function that takes batch of resolvables and generates a deferred
   containing the in-order results."
  [{:keys [resolve-fn impl] :as opts} batch]
  (some->> batch
           resolve-fn
           (assert-deferrable opts batch)
           (impl/->deferred impl)))

(defn- merge-resolvables
  "Merge all resolved values with the original batch."
  [batch resolved-values]
  (let [batch-count (count batch)
        resolved-values (take batch-count resolved-values)]
    (when (< batch-count (count resolved-values))
      (throw
        (IllegalStateException.
          (str "'resolve-fn' did not resolve all values - "
               (count batch) " values were given, only "
               (count resolved-values) " were produced."))))
    (zipmap batch resolved-values)))

(defn- resolve-batch!
  "Returns a deferred representing the resolution of the given batch.
   `resolve-fn` has to return a deferred with the resolution results
   in-order."
  [{:keys [impl] :as opts} batch]
  (impl/chain1
    impl
    (generate-deferred opts batch)
    #(merge-resolvables batch %)))

(defn resolve-batches!
  "Resolve the given batches, returning a deferred with a map of
   original value -> resolved value pairs."
  [{:keys [impl] :as opts} batches]
  (let [ds (mapv #(resolve-batch! opts %) batches)
        zipped (impl/zip impl ds)]
    (impl/chain1 impl zipped #(into {} %))))