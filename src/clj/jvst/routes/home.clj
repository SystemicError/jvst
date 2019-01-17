(ns jvst.routes.home
  (:require [jvst.layout :as layout]
            [jvst.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))


;;; adapted code

;;; "DATABASE"

(defn get-user [email]
  (db/get-user {:email email}))

(defn add-user! [email password first-name last-name]
  (if (get-user (str/lower-case email))
    (throw (Exception. "User with that email already exists!"))
    (db/create-user! {:email (str/lower-case email)
                      :password password
                      :first-name first-name
                      :last-name last-name})))

;;; PASSWORD
(defn password-hash
  [password]
  (hashers/derive password {:alg :bcrypt+sha512}))

(defn validate-password [email password]
  (hashers/check password (get (get-user email) :password)))


;;; USER FUNCTIONALITY

(defn add-user-to-session [request user]
  (assoc-in request [:session :identity] user)) ; http://www.luminusweb.net/docs/routes.html#restricting_access

(defn clear-session-identity [request]
  (assoc-in request [:session :identity] nil))


;;;


(defn home-page [request]
  (layout/render
    "home.html"
    {:message request}))

(defn test-page []
  (layout/render
    "test.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" request (home-page request))
  (GET "/test" [] (test-page))
  (GET "/about" [] (about-page)))
