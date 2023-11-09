(ns io.schemamap.pg-query-clj
  (:require
   [jsonista.core :as j]
   [tech.v3.datatype.ffi :as dt-ffi]
   [tech.v3.datatype.ffi.size-t :as ffi-size-t]
   [tech.v3.datatype.struct :as dt-struct])
  (:import [tech.v3.datatype.ffi Pointer]))

(defn int-type
  []
  (if (= (ffi-size-t/size-t-size) 8)
    :int64
    :int32))

(defonce int-type* (delay (int-type)))

(defonce ptr-dtype* (delay (ffi-size-t/ptr-t-type)))

(defonce pg-query-error-def*
  (delay (dt-struct/define-datatype! :pg-query-error
           [{:name :message
             :datatype @ptr-dtype*}
            {:name :funcname
             :datatype @ptr-dtype*}
            {:name :filename
             :datatype @ptr-dtype*}
            {:name :lineno
             :datatype :int32} ;; FIXME int
            {:name :cursorpos
             :datatype :int32} ;; FIXME int
            {:name :context
             :datatype @ptr-dtype*}])))

(defonce pg-query-parse-result-def*
  (delay (dt-struct/define-datatype! :pg-query-parse-result
           [{:name :parse_tree
             :datatype @ptr-dtype*}
            {:name :stderr_buffer
             :datatype @ptr-dtype*}
            {:name :error
             :datatype @ptr-dtype*}])))

(defn define-datatypes! []
  @pg-query-error-def*
  @pg-query-parse-result-def*)

(dt-ffi/define-library!
  lib
  '{
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    ;; Database setup/teardown
    ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

    :pg_query_parse {:rettype (by-value :pg-query-parse-result)
                     :argtypes [[input :pointer]]}}
  nil
  nil)

(defonce ^:private initialize* (atom false))

(defn initialized?
  []
  @initialize*)

(defn lib-path []
  "/darwin-aarch64/libpg_query/libpg_query.dylib")

(defn initialize! []
  (swap! initialize*
         (fn [is-init?]
           (when-not is-init?
             (define-datatypes!)
             (dt-ffi/library-singleton-set! lib (lib-path)))
           true)))

(defn ->pointer [uint]
  (Pointer. uint))

(defn ptr->string [uint]
  (-> (->pointer uint)
      (dt-ffi/c->string)))

(defn ptr->error [uint]
  (let [data-type (:datatype-name @pg-query-error-def*)
        ptr (->pointer uint)
        {:keys [message funcname filename lineno cursorpos context]} (dt-ffi/ptr->struct data-type ptr)]
    {:message (ptr->string message)
     :funcname (ptr->string funcname)
     :filename (ptr->string filename)
     :lineno lineno
     :cursorpos cursorpos
     :context (ptr->string context)}))

(defn ptr->result [ptr]
  (let [data-type (:datatype-name @pg-query-parse-result-def*)
        {:keys [parse_tree stderr_buffer error]} (dt-ffi/ptr->struct data-type ptr)]
    {:parse-tree (ptr->string parse_tree)
     :stderr-buffer (ptr->string stderr_buffer)
     :error (when-not (= error 0)
              (ptr->error error))}))

(defn parse [query]
  (let [char (dt-ffi/string->c query)
        result-ptr (pg_query_parse char)
        result (ptr->result result-ptr)
        json (j/read-value (:parse-tree result))]
    (assoc result :json json)))

(comment
  (initialize!)

  (dt-ffi/c->string (dt-ffi/string->c "SELECT 1;"))

  (pg_query_parse (dt-ffi/string->c "SELECT 1;"))

  (parse "SELECT foo, bar FROM foobar WHERE bar = 'BAR' LIMIT 1;")

  (parse "SELECT 1;")

  (parse "x-SELECT 1;")
  )
