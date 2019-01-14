(ns jvst.routes.home
  (:require [jvst.layout :as layout]
            [jvst.db.core :as db]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn test-page []
  (layout/render
    "test.html"))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/test" [] (test-page))
  (GET "/about" [] (about-page)))

