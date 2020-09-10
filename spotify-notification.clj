#!/usr/bin/env bb

(ns spotify-notification
  (:require
   [babashka.curl :as curl]
   [cheshire.core :as json]))

(def icons {"start" "/usr/share/icons/gnome/32x32/actions/player_play.png"
            "change" "/usr/share/icons/gnome/32x32/actions/player_fwd.png"
            "stop" "/usr/share/icons/gnome/32x32/actions/player_stop.png"})

(def notify-send-cmd ["notify-send" "--urgency=low" "--expire-time=7000"])

(defn get-access-token
  [client-id secret-id]
  (-> (curl/post "https://accounts.spotify.com/api/token" {:basic-auth [client-id secret-id]
                                                           :body "grant_type=client_credentials"
                                                           :throw false})
      :body
      (json/parse-string true)
      :access_token))

(defn get-track-info
  [access-token track-id]
  (-> (curl/get (str "https://api.spotify.com/v1/tracks/" track-id)
                {:headers {"Accept" "application/json"
                           "Content-Type" "application/json"
                           "Authorization" (str "Bearer " access-token)}
                 :throw false})
      :body
      (json/parse-string true)))

(defn notify-send
  [track]
  (when-not (contains? track :error)
    (let [event (System/getenv "PLAYER_EVENT")]
      (-> (ProcessBuilder. (conj notify-send-cmd (str "--icon=" (get icons event)) "spotifyd" (:name track)))
          (.start)
          (.waitFor)))))

(defn display-notification
  [client-id secret-id]
  (some-> (get-access-token client-id secret-id)
          (get-track-info (System/getenv "TRACK_ID"))
          (notify-send)))

(let [[client-id secret-id] *command-line-args*]
  (when (and client-id secret-id)
    (display-notification client-id secret-id)))
