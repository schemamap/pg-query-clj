(ns io.schemamap.pg-query-clj
  (:require
   [jsonista.core :as j]
   [io.schemamap.pg-query-clj.ffi :as ffi]))

(defn initialize!
  ([] (initialize! {}))
  ([{:keys [lib-path]
     :as   init-config}]
   (ffi/initialize! init-config)))

(def default-json-object-mapper
  (j/object-mapper {:decode-key-fn true}))

(defn parse
  ([^String query]
   (parse query default-json-object-mapper))
  ([^String query json-object-mapper]
   (let [result (ffi/pg-query-parse query)
         json   (some-> result
                        :parse-tree
                        (j/read-value json-object-mapper))]
     (assoc result :parse-tree-edn json))))

(comment
  ; https://github.com/java-native-access/jna/blob/a4aca64973061cf7d6c9c21e031683340c674e92/src/com/sun/jna/NativeLibrary.java#L74-L81
  (initialize! {} #_{:lib-path "/darwin-aarch64/libpg_query.dylib"})

  (do
    (System/setProperty "jna.debug_load" "true")
    (System/setProperty "jna.debug_load.jna" "true"))

  (parse "SELECT foo, bar FROM foobar WHERE bar = 'BAR' LIMIT 1;")

  (parse "SELECT 1;")

  (parse "x-SELECT 1;")
  )
