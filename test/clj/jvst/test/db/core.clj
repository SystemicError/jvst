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
               {:id         "1"
                :first_name "Sam"
                :last_name  "Smith"
                :email      "sam.smith@example.com"
                :pass       "pass"})))
    (is (= {:id         "1"
            :first_name "Sam"
            :last_name  "Smith"
            :email      "sam.smith@example.com"
            :pass       "pass"
            :admin      nil
            :last_login nil
            :is_active  nil
            :question_set_queue nil
            :vocab_queries nil
            :survey_results nil}
           (db/get-user-by-id t-conn {:id "1"})))
    (is (= {:id         "1"
            :first_name "Sam"
            :last_name  "Smith"
            :email      "sam.smith@example.com"
            :pass       "pass"
            :admin      nil
            :last_login nil
            :is_active  nil
            :question_set_queue nil
            :vocab_queries nil
            :survey_results nil}
           (db/get-user-by-email t-conn {:email "sam.smith@example.com"})))
    (is (= 1 (db/update-user!
               t-conn
               {:id         "1"
                :first_name "Samantha"
                :last_name  "Smithers"
                :email      "sam.smithers@example.com"})))
    (is (= 1 (db/update-pass!
               t-conn
               {:id   "1"
                :pass "swordfish"})))
    (is (= 1 (db/update-question-set-queue!
               t-conn
               {:id   "1"
                :question_set_queue "test results"})))
    (is (= 1 (db/update-vocab-queries!
               t-conn
               {:id   "1"
                :vocab_queries "vocab queries"})))
    (is (= 1 (db/update-survey-results!
               t-conn
               {:id   "1"
                :survey_results "survey results"})))
    (is (= {:id         "1"
            :first_name "Samantha"
            :last_name  "Smithers"
            :email      "sam.smithers@example.com"
            :pass       "swordfish"
            :admin      nil
            :last_login nil
            :is_active  nil
            :question_set_queue "test results"
            :vocab_queries "vocab queries"
            :survey_results "survey results"}
           (db/get-user-by-id t-conn {:id "1"})))
    (is (= 1 (db/delete-user!
               t-conn
               {:id "1"})))
    (is (= nil (db/get-user-by-id
               t-conn
               {:id "1"})))
    ))
