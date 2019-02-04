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

(defn admin? [request]
  (let [session (request :session)
        email (if session (session :identity))]
    (= email "admin")))

;;; PAGES

(defn home-page [request]
  (layout/render "home.html" request))

(defn test-page [request]
  (let [session (request :session)
        email (if session (session :identity))
        user (if email (get-user email))
        queue (if user (user :question_set_queue))
        finished (= queue :finished)
        pretest (= queue nil)
        dummy (println user)]
    (layout/render "test.html" (into request
                                     (into queue
                                           {:finished finished
                                            :pretest pretest})))))

(defn register-page [request]
  (layout/render "register.html" request))

(defn about-page [request]
  (layout/render "about.html" request))

(defn admin-page [request]
  (if (admin? request)
    (layout/render
      "admin.html"
      (into request
            {:admin admin?
             :users (for [user (db/get-users)] (dissoc user :pass))
             :vqs (db/get-vocab-questions)}))
    (layout/render "admin.html"))
  )

;;; QUESTION BANK FUNCTIONALITY

(defn add-vocab-questions [qs]
  (if (empty? qs)
    0
    (let [q (first qs)]
      (do
        (try (db/create-vocab-question! q)
             (catch Exception e
               ; if this question is already in the database
               (do
                 (db/delete-vocab-question! {:id (:id q)}))
                 (db/create-vocab-question! q)))
        (recur (rest qs)))))
  )

(defn parse-test-bank-tsv
  "Converts tsv file into edn format."
  [path]
  (let [text (slurp path)
        lines (str/split-lines text)
        cells (for [line lines] (str/split line #"\t"))
        labels (list :id :set :headword :furigana :example :correct :option_1 :option_2 :option_3 :option_4)
        questions (for [cell cells] (apply assoc {} (interleave labels cell)))]
    (add-vocab-questions questions))
  )

(defn load-test-bank [request]
  (if (admin? request)
    (do
      (parse-test-bank-tsv "vocab_questions.tsv")
      (admin-page request))))


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
    (layout/render
      "registered.html"
      (try
        {:registered (db/create-user! new-params)}
        (catch Exception e {:reason e})))))


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
  (GET "/about" request (about-page request))
  (GET "/admin" request (admin-page request))
  (POST "/load_test_bank" request (load-test-bank request)))

