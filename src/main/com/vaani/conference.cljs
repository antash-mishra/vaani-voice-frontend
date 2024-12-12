(ns com.vaani.conference
  (:require
   [reagent.core :as r]
   ["@livekit/components-react" :refer [LiveKitRoom ParticipantTileGrid Chat useParticipants AudioVisualizer AudioConference BarVisualizer useVoiceAssistant ControlBar RoomAudioRenderer]]))


(defn simple-voice-assistant [{:keys [on-state-change]}]
  ;; Define atoms once (when the component is created)
  (let [assistant-state (r/atom nil)
        audio-track-ref (r/atom nil)]
    (fn [{:keys [on-state-change]}]
      ;; Call the hook at the top-level of the render function
      (let [{:keys [state audioTrack]} (useVoiceAssistant)]
        ;; Check if the state has changed from what we had before
        (when (not= @assistant-state state)
          (reset! assistant-state state)
          (on-state-change state))  ;; Trigger callback only on state change

        (reset! audio-track-ref audioTrack)

        [:div {:class "h-[300px] max-w-[90vw] mx-auto"}
         [:> BarVisualizer
          {:state @assistant-state
           :barCount 5
           :trackRef @audio-track-ref
           :class "agent-visualizer"
           :options {:minHeight 24}}]]))))

(defn livekit-chat-page []
  (let [handle-state-change (fn [state]
                              (js/console.log "State changed:" state))]

    [:main {:data-lk-theme "default" :className "h-full grid content-center bg-[var(--lk-bg)]"}
     [:> LiveKitRoom {:serverUrl "wss://vaani-voice-sc018bec.livekit.cloud"
                      :token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoibXkgbmFtZSIsInZpZGVvIjp7InJvb21Kb2luIjp0cnVlLCJyb29tIjoibXktcm9vbSIsImNhblB1Ymxpc2giOnRydWUsImNhblN1YnNjcmliZSI6dHJ1ZSwiY2FuUHVibGlzaERhdGEiOnRydWV9LCJzdWIiOiJpZGVudGl0eSIsImlzcyI6IkFQSWNmc2djN21Wb0tFOSIsIm5iZiI6MTczMzkwMzgzMSwiZXhwIjoxNzMzOTI1NDMxfQ.LjspqpkaRQntL6p513nbV5q5vyHxWCwwUDKfBV00QOM"
                      :connect true
                      :video false
                      :audio true
                      :className "grid grid-rows-[2fr_1fr] items-center"}

      [:f> simple-voice-assistant {:on-state-change handle-state-change}]

      [:div {:style {:display "flex"
                     :flexDirection "column"
                     :width "600px"
                     :margin "auto"
                     :fontFamily "sans-serif"}}
       [:h2 "LiveKit Audio + Chat"]

                              ;; Participants area
       [:div {:style {:border "1px solid #ccc"
                      :padding "10px"
                      :marginBottom "20px"}}
        [:h4 "Participants (Audio only)"]]
       [:> ControlBar]
       [:> RoomAudioRenderer]

                              ;; Audio Visualizer
       [:div {:style {:marginBottom "20px"}}
        [:h4 "Audio Visualizer"]]

                              ;; Chat area
       [:div {:style {:border "1px solid #ccc"
                      :padding "10px"
                      :flex 1
                      :display "flex"
                      :flexDirection "column"}}]]]]))


(defn CloseIcon []
  [:svg {:width "16" :height "16" :viewBox "0 0 16 16" :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M3.33398 3.33334L12.6673 12.6667M12.6673 3.33334L3.33398 12.6667"
           :stroke "currentColor"
           :stroke-width "2"
           :stroke-linecap "square"}]])


(def ^:export video-conference CloseIcon)

