(ns jvst.test.db.core
  (:require [jvst.db.core :refer [*db*] :as db]
            [luminus-migrations.core :as migrations]
            [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [jvst.config :refer [env]]
            [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'jvst.config/env
      #'jvst.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-users
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-user!
               t-conn
               {:email      "sam.smith@example.com"
                :first_name "Sam"
                :last_name  "Smith"
                :pass       "pass"})))
    (is (= {:email      "sam.smith@example.com"
            :first_name "Sam"
            :last_name  "Smith"
            :pass       "pass"
            :admin      nil
            :last_login nil
            :is_active  nil
            :question_set_queue nil
            :vocab_queries nil
            :survey_results nil}
           (db/get-user t-conn {:email "sam.smith@example.com"})))
    (is (= 1 (db/update-user!
               t-conn
               {:first_name "Samantha"
                :last_name  "Smithers"
                :email      "sam.smith@example.com"})))
    (is (= 1 (db/update-pass!
               t-conn
               {:email   "sam.smith@example.com"
                :pass "swordfish"})))
    (is (= 1 (db/update-question-set-queue!
               t-conn
               {:email   "sam.smith@example.com"
                :question_set_queue "test results"})))
    (is (= 1 (db/update-vocab-queries!
               t-conn
               {:email   "sam.smith@example.com"
                :vocab_queries "vocab queries"})))
    (is (= 1 (db/update-survey-results!
               t-conn
               {:email "sam.smith@example.com"
                :survey_results "survey results"})))
    (is (= {:first_name "Samantha"
            :last_name  "Smithers"
            :email      "sam.smith@example.com"
            :pass       "swordfish"
            :admin      nil
            :last_login nil
            :is_active  nil
            :question_set_queue "test results"
            :vocab_queries "vocab queries"
            :survey_results "survey results"}
           (db/get-user t-conn {:email "sam.smith@example.com"})))
    (is (= 1 (db/delete-user!
               t-conn
               {:email "sam.smith@example.com"})))
    (is (= nil (db/get-user
                 t-conn
                 {:email "sam.smith@example.com"})))
    ))


;; VOCAB QUESTIONS
(deftest test-vocab-questions
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    (is (= 1 (db/create-vocab-question!
               t-conn
               {:id 1
                :set 1
                :headword "Headword!"
                :furigana "Furigana!"
                :example "Example!"
                :correct "Right answer!"
                :option_1 "Wrong answer 1!"
                :option_2 "Right answer!"
                :option_3 "Wrong answer 2!"
                :option_4 "Wrong answer 3!"})))
    (is (= 1 (db/create-vocab-question!
               t-conn
               {:id 2
                :set 1
                :headword "Headword 2!"
                :furigana "Furigana 2!"
                :example "Example 2!"
                :correct "Right answer!"
                :option_1 "Wrong answer 1!"
                :option_2 "Right answer!"
                :option_3 "Wrong answer 2!"
                :option_4 "Wrong answer 3!"})))
    (is (= {:id 1
            :set 1
            :headword "Headword!"
            :furigana "Furigana!"
            :example "Example!"
            :correct "Right answer!"
            :option_1 "Wrong answer 1!"
            :option_2 "Right answer!"
            :option_3 "Wrong answer 2!"
            :option_4 "Wrong answer 3!"}
           (db/get-vocab-question t-conn {:id 1})))
    (is (= 1 (db/delete-vocab-question! t-conn {:id 1})))
    (is (= nil (db/get-vocab-question
                 t-conn
                 {:id 1})))
    ))
