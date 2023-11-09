(ns io.schemamap.pg-query-clj
  (:require [tech.v3.datatype.ffi :as dt-ffi]
            [tech.v3.datatype.ffi.size-t :as ffi-size-t]
            [tech.v3.datatype.struct :as dt-struct]))

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
             :datatype :int64} ;; FIXME int
            {:name :cursorpos
             :datatype :int64} ;; FIXME int
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

(defn parse [query]
  (let [char (dt-ffi/string->c query)]
    char))

(defn foo []
  ;; (pg_query_parse "SELECT 1;")
  (initialize!)
  )

(comment
  (foo)
  ;; (parse "SELECT 1;")
  )
