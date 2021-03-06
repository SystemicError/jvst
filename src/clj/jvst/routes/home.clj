(ns jvst.routes.home
  (:require [jvst.layout :as layout]
            [jvst.db.core :as db]
            [compojure.core :refer :all]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.core :as core]))



;;; DATABASE

(defn get-user [email]
  (db/get-user {:email email}))

;;; VOCAB TEST

(defn generate-test []
  "Creates a question set queue for a new test."
  (let [qs (db/get-vocab-questions)
        bands (sort (core/set (map #(:set %) qs)))
        ids-by-band (for [band bands] (map #(:id %) (filter #(= (:set %) band) qs)))]
    (into []
          (for [ids ids-by-band] (take 10 (shuffle ids))))))

(defn shuffle-answer-choices [question]
  "Shuffles the answer choices of a question."
  (let [choices [(question :option_1)
                 (question :option_2)
                 (question :option_3)
                 (question :option_4)]
        shuffled (shuffle choices)]
    (into question (apply hash-map (interleave (list :option_1 :option_2 :option_3 :option_4)
                                                shuffled)))))

(defn queue-to-questions [queue]
  "returns a collection of the vocab questions in the first question set in queue"
  (for [q (first queue)]
       (db/get-vocab-question {:id q})))

(defn record-responses [questions responses]
  "Returns a hashmap of grades/responses for a question set, with timestamp."
  ; each entry of form :id {:timestamp timestamp :response response}
  (let [timestamp (.toString (java.util.Date.))
        ids (for [q questions] (:id q))
        replies (vals (sort (dissoc responses :__anti-forgery-token)))]
    (apply hash-map
           (interleave
             ids
             (for [i (range (count ids))]
               (let [question (nth questions i)
                     correct (question :option_1)
                     response (nth replies i)]
                 {:timestamp timestamp
                  :response response
                  :grade (= response correct)}))))))


(defn band-scores [user]
  "Returns the scores in each frequency band based on user results."
  (let [results (edn/read-string (user :vocab_results))
        corrects (filter #(:grade (results %)) (keys results))
        bands (for [i (range 1 11)] (filter #(= i (:set (db/get-vocab-question {:id %}))) corrects))]
    (for [b bands] (count  b))))

(defn estimate-vocabulary [user]
  "Returns the users estimated vocabulary size.  Assumes ten responses per vocab frequency band."
  ; number correct out of 100 multiplied by 100 = vocab size
  (let [results (edn/read-string (user :vocab_results))]
    (* 100 (count (filter #(:grade %) (vals results))))))

;;; PASSWORD
(defn password-hash
  [password]
  (hashers/derive password {:alg :bcrypt+sha512}))

(defn validate-password [email password]
  (let [user (get-user email)
        pass (:pass user)]
  (hashers/check password (get (get-user email) :pass))))

(defn admin? [request]
  (let [session (request :session)
        email (if session (session :identity))
        super-users (str/split (slurp "superusers.txt") #"\n")]
    (some #{email} super-users)))


;;; CONSENT

(defn consent-status [user]
  "Returns a user consent status (agreed/declined/none)."
  (let [consent-results (edn/read-string (:consent_results user))]
    (if consent-results
      (if (and (= (:age-18+ consent-results)
                  (:georgia consent-results)
                  "yes")
               (= (:ga-participate consent-results) "raffle"))
        "raffle"  ; do not give jvst; just enter into raffle
        (if (or (and (= (:age-18+ consent-results)
                        (:georgia consent-results)
                        (:ga-participate consent-results)
                        (:ga-read-understand consent-results)
                        (:ga-store-share consent-results)
                        "yes")
                     (or (= (:ga-eu-citizen consent-results) "no")
                         (= (:ga-eu-citizen consent-results)
                            (:ga-eu-consent consent-results)
                            "yes")))
                (and (= (:georgia consent-results) "no")
                     (= (:age-18+ consent-results)
                        (:read-understand consent-results)
                        (:store-share consent-results)
                        (:participate consent-results)
                        "yes")
                     (or (= (:eu-citizen consent-results) "no")
                         (= (:eu-citizen consent-results)
                            (:eu-consent consent-results)
                            "yes"))))
          "record"  ; keep user data for study
          "discard"))  ; discard all user data
      "none"))) ; have not yet submitted consent page

;;; PAGES

(defn home-page [request]
  (let [session (request :session)
        email (if session (session :identity))
        user (if email (get-user email))
        finished (if user (edn/read-string (user :survey_results)))]
    (layout/render "home.html" (into request {:finished finished}))))


(defn test-page [request]
  (let [session (request :session)
        email (if session (session :identity))
        user (if email (get-user email))
        consent (if user (consent-status user))
        template-consent (case consent
                           "record" "agreed"
                           "discard" "agreed"
                           "raffle" "declined"
                           "none" "none")]
  (layout/render "test.html" (into request {:consent template-consent}))))

(def language-list ["English" "Mandarin Chinese" "Korean" "Spanish" "Arabic" "Armenian" "Bengali" "Burmese" "Cantonese" "Cherokee" "Dutch" "Farsi" "Filipino" "French" "German" "Greek" "Hakka" "Hawaiian" "Hebrew" "Hindi" "Hmon" "Ilocano" "Irish" "Japanese" "Javanese" "Khmer" "Kurdish" "Laotian" "Malay" "Marathi" "Navajo" "Nepali" "Persian" "Polish" "Portuguese" "Punjabi" "Romanian" "Russian" "Samoan" "Serbo-Croatian" "Sinhala" "Somali" "Sudanese" "Swahili" "Swedish" "Tagalog" "Taiwanese" "Tajik" "Tamil" "Telugu" "Thai" "Turkish" "Ukrainian" "Urdu" "Uzbek" "Vietnamese" "Welsh" "Wu Chinese" "Yiddish" "Yue Chinese"])

(defn process-test-responses [request]
  ; queue      responses          display           to-template  updated-queue     results
  ;
  ; nil        nil                consent forms     nil          nil               []
  ; nil        consent            example page      nil          :consented        []
  ; nil        nonconsent         done              nil          :nonconsented     []
  ; :consented nil                example page      nil          (generate-test)   []
  ; [vector]   answer form        next question set questions    advance one set   push responses
  ; last page  answer form        post-survey       finished     :finished         push responses
  ; :finished  *                  post-survey       finished     :finished         unmodified

  ; We don't check if results are incomplete, since the default answer is set as "I don't know"
  ; and the csrf field has our back on forged submissions.
  ; However, we do check if responses are absent, in case someone leaves the test page and comes back at a later time, or if they've only just begun.
  (let [session (request :session)
        email (if session (session :identity))
        user (if email (get-user email))
        queue (if user (edn/read-string (user :question_set_queue)))
        results (if user (edn/read-string (user :vocab_results)) {})
        responses (request :params)
        generated-test (generate-test)
        shuffler (fn [x] (for [y (queue-to-questions x)] (shuffle-answer-choices y)))
        consent-response (if (:confirm responses)
                           (db/update-consent-results! {:email email
                                                        :consent_results (str (dissoc responses :__anti-forgery-token :confirm))}))
        updates (if consent-response
                  ; don't update anything if we just got a consent response
                  {:queue nil
                   :to-template nil}
                  (if queue
                    (if (= :finished queue)
                      ; finished
                      {:queue :finished
                       :to-template {:finished true}}
                      (if (= 1 (count queue))
                        ; last page
                        (if responses
                          {:queue :finished
                           :to-template {:finished true}
                           :results (record-responses (queue-to-questions queue) responses)}
                          {:queue queue
                           :to-template {:questions (into [] (shuffler queue))}})
                        ; [vector]
                        (if (:submit responses)
                          {:queue (rest queue)
                           :to-template {:questions (into [] (shuffler (rest queue)))}
                           :results (record-responses (queue-to-questions queue) responses)}
                          {:queue queue
                           :to-template {:questions (into [] (shuffler queue))}})))
                    ; nil
                    {:queue generated-test
                     :to-template {:questions (into [] (shuffler generated-test))}}))]
    (do
      (db/update-question-set-queue! {:email email
                                      :question_set_queue (str (updates :queue))})
      (if (updates :results)
        (db/update-vocab-results! {:email email
                                   :vocab_results (str (merge results (updates :results)))}))
      (test-page (into request (assoc (updates :to-template) :languages language-list))))))

(defn process-survey-results [request]
  "Save the results of the survey, compute the user's vocab size, and pass the result to template."
  (let [session (request :session)
        email (if session (session :identity))
        user (if email (get-user email))
        results (dissoc (request :params) :__anti-forgery-token)
        bands (if user (into [] (band-scores user)))
        to-template {:vocabulary (estimate-vocabulary (db/get-user {:email email}))
                     :band-scores bands}]
    (do
      (db/update-survey-results! {:email email
                                  :survey_results (str results)})
      (layout/render "results.html" (into request to-template)))))

(defn register-page [request]
  (layout/render "register.html" request))

(defn about-page [request]
  (layout/render "about.html" request))

(defn logout-page [request]
  (layout/render "logout.html" request))

(defn admin-page [request]
  (if (admin? request)
    (let [users (for [user (db/get-users)]
                  (let [results (edn/read-string (user :vocab_results))]
                    (assoc
                      (dissoc user :pass)
                      :consent-status
                      (consent-status user)
                      :vocab_results
                      (sort-by :id
                               (into []
                                 (for [id (keys results)]
                                   (assoc (results id) :id id)))))))]
      (layout/render
        "admin.html"
        (into request
              {:admin admin?
               :users users
               :vqs (db/get-vocab-questions)})))
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
  "Converts lines of tsv file into database entries."
  [lines]
  (let [cells (for [line lines] (str/split line #"\t"))
        labels (list :id :set :headword :furigana :example :option_1 :option_2 :option_3 :option_4)
        questions (for [cell cells] (apply assoc {} (interleave labels cell)))]
    (add-vocab-questions questions))
  )

(defn load-test-bank [request]
  (if (admin? request)
    (let [tempfile (((request :params) :file) :tempfile)
          reader (clojure.java.io/reader tempfile)]
      (do
        (parse-test-bank-tsv (line-seq reader))
        (admin-page request)))))


;;; USER FUNCTIONALITY

(defn add-user-to-session [response email]
  (assoc-in response [:session :identity] email))

(defn clear-session-identity [request]
  (assoc-in request [:session :identity] nil))

(defn register-user [{params :params}]
  ;;; TODO - add validation
  (let [email (str/lower-case (get params :email))
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
  (clear-session-identity (logout-page (clear-session-identity request))))

(defroutes home-routes
  (GET "/" request (home-page request))
  (GET "/test" request (test-page request))
  (POST "/test" request (process-test-responses request))
  (POST "/results" request (process-survey-results request))
  (GET "/register" request (register-page request))
  (POST "/register" request (register-user request))
  (POST "/login" request (login-user request))
  (GET "/logout" request (logout-user request))
  (GET "/about" request (about-page request))
  (GET "/admin" request (admin-page request))
  (POST "/load_test_bank" request (load-test-bank request)))

