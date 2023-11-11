(ns io.schemamap.pg-query-clj.ffi
  (:require
   [tech.v3.datatype.ffi :as dt-ffi]
   [tech.v3.datatype.ffi.size-t :as ffi-size-t]
   [tech.v3.datatype.struct :as dt-struct])
  (:import [tech.v3.datatype.ffi Pointer]
           [com.sun.jna Platform]))

(defonce ptr-dtype* (delay (ffi-size-t/ptr-t-type)))

(defonce pg-query-error-def*
  (delay (dt-struct/define-datatype! :pg-query-error
           [{:name     :message
             :datatype @ptr-dtype*}
            {:name     :funcname
             :datatype @ptr-dtype*}
            {:name     :filename
             :datatype @ptr-dtype*}
            {:name     :lineno
             :datatype :int32}
            {:name     :cursorpos
             :datatype :int32}
            {:name     :context
             :datatype @ptr-dtype*}])))

(defonce pg-query-parse-result-def*
  (delay (dt-struct/define-datatype! :pg-query-parse-result
           [{:name     :parse_tree
             :datatype @ptr-dtype*}
            {:name     :stderr_buffer
             :datatype @ptr-dtype*}
            {:name     :error
             :datatype @ptr-dtype*}])))

(defn define-datatypes! []
  @pg-query-error-def*
  @pg-query-parse-result-def*)

(dt-ffi/define-library!
  libpg_query
  '{:pg_query_parse {:rettype  (by-value :pg-query-parse-result)
                     :argtypes [[input :pointer]]}}
  nil
  nil)

(defonce ^:private initialize* (atom false))

(defn initialized?
  []
  @initialize*)

(defn initialize!
  [{:keys [lib-path]
    :or   {lib-path
           (str "/" com.sun.jna.Platform/RESOURCE_PREFIX "/" (System/mapLibraryName "pg_query"))}}]
  (swap! initialize*
         (fn [is-init?]
           (when-not is-init?
             (define-datatypes!)
             (dt-ffi/library-singleton-set! libpg_query lib-path))
           true)))

(defn ->pointer [uint]
  (Pointer. uint))

(defn ptr->string [uint]
  (-> (->pointer uint)
      (dt-ffi/c->string)))

(defn ptr->error [uint]
  (let [data-type (:datatype-name @pg-query-error-def*)
        ptr       (->pointer uint)
        {:keys [message funcname filename lineno cursorpos context]} (dt-ffi/ptr->struct data-type ptr)]
    {:message   (ptr->string message)
     :funcname  (ptr->string funcname)
     :filename  (ptr->string filename)
     :lineno    lineno
     :cursorpos cursorpos
     :context   (ptr->string context)}))

(defn ptr->result [ptr]
  (let [data-type                                (:datatype-name @pg-query-parse-result-def*)
        {:keys [parse_tree stderr_buffer error]} (dt-ffi/ptr->struct data-type ptr)]
    {:parse-tree    (ptr->string parse_tree)
     :stderr-buffer (ptr->string stderr_buffer)
     :error         (when-not (= error 0)
                      (ptr->error error))}))

(defn pg-query-parse [^String query-string]
  (let [char       (dt-ffi/string->c query-string)
        result-ptr (pg_query_parse char)
        result     (ptr->result result-ptr)]
    result)
  )

(comment
  com.sun.jna.Platform/OS



  )
