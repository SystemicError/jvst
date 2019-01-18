(ns jvst.routes.home
  (:require [jvst.layout :as layout]
            [jvst.db.core :as db]
            [compojure.core :refer :all]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))


;;; adapted code

;;; "DATABASE"

(defn get-user [email]
  (db/get-user {:email email}))

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

(defn register-user [{params :params}]
  ;;; TODO - add validation
  (let [email (get params :email)
        password (get params :password)
        first-name (get params :first-name)
        last-name (get params :last-name)
        hashed (password-hash password)
        new-params {:email email :pass hashed :first_name first-name :last_name last-name}]
    (do (db/create-user! new-params))
        (layout/render "registered.html")))

;;;


(defn home-page [request]
  (layout/render
    "home.html"
    {:message request}))

(defn test-page [request]
  (layout/render
    "test.html"))

(defn register-page []
  (layout/render
    "register.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" request (home-page request))
  (GET "/test" request (test-page request))
  (GET "/register" [] (register-page))
  (POST "/register" request (register-user request))
  (GET "/about" [] (about-page)))
