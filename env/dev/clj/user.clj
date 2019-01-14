(ns user
  (:require [jvst.config :refer [env]]
            [clojure.spec.alpha :as s]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [jvst.core :refer [start-app]]
            [jvst.db.core]
            [conman.core :as conman]
            [luminus-migrations.core :as migrations]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start []
  (mount/start-without #'jvst.core/repl-server))

(defn stop []
  (mount/stop-except #'jvst.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn restart-db []
  (mount/stop #'jvst.db.core/*db*)
  (mount/start #'jvst.db.core/*db*)
  (binding [*ns* 'jvst.db.core]
    (conman/bind-connection jvst.db.core/*db* "sql/queries.sql")))

(defn reset-db []
  (migrations/migrate ["reset"] (select-keys env [:database-url])))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))

(defn create-migration [name]
  (migrations/create name (select-keys env [:database-url])))


