(ns io.schemamap.pg-query-clj-test
  (:require [clojure.test :refer :all]
            [io.schemamap.pg-query-clj :as sut]))

(deftest parse
  (testing "initalization works, automatically looking up architecture specific DLL on classpath"
    (sut/initialize!))
  (testing "simple test"
    (is (= {:parse-tree
            "{\"version\":150001,\"stmts\":[{\"stmt\":{\"SelectStmt\":{\"targetList\":[{\"ResTarget\":{\"val\":{\"A_Const\":{\"ival\":{\"ival\":1},\"location\":7}},\"location\":7}}],\"limitOption\":\"LIMIT_OPTION_DEFAULT\",\"op\":\"SETOP_NONE\"}}}]}",
            :stderr-buffer "",
            :error         nil,
            :parse-tree-edn
            {:stmts
             [{:stmt
               {:SelectStmt
                {:targetList
                 [{:ResTarget
                   {:val      {:A_Const {:ival {:ival 1}, :location 7}},
                    :location 7}}],
                 :op          "SETOP_NONE",
                 :limitOption "LIMIT_OPTION_DEFAULT"}}}],
             :version 150001}}
           (sut/parse "select 1")))))
