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
  (hashers/check password (get (get-user email) :pass)))


;;; PAGES

(defn home-page [request]
  (layout/render "home.html" request))

(defn test-page [request]
  (layout/render "test.html" request))

(defn register-page [request]
  (layout/render "register.html" request))

(defn about-page [request]
  (layout/render "about.html" request))

;;; USER FUNCTIONALITY

(defn add-user-to-session [response email]
  (assoc-in response [:session :identity] email))

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

(defn login-user [{params :params :as request}]
  (let [email (str/lower-case (get params :email))
        password (get params :password)]
    (if (validate-password email password)
      (add-user-to-session (home-page (add-user-to-session request email)) email)
      "That's an invalid login!")))

(defn logout-user [request]
  (clear-session-identity (home-page (clear-session-identity request))))

(defroutes home-routes
  (GET "/" request (home-page request))
  (GET "/test" request (test-page request))
  (GET "/register" request (register-page request))
  (POST "/register" request (register-user request))
  (POST "/login" request (login-user request))
  (GET "/logout" request (logout-user request))
  (GET "/about" request (about-page request)))

