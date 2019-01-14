(ns jvst.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [jvst.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[jvst started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[jvst has shut down successfully]=-"))
   :middleware wrap-dev})
