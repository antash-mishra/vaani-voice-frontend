(ns com.vaani.app
  (:require ["@chatscope/chat-ui-kit-react" :refer [MainContainer ChatContainer MessageList Message MessageInput ConversationHeader Avatar]]
            ["@livekit/components-react" :refer [ConnectionState LiveKitRoom RoomName useTracks VoiceAssistantControlBar DisconnectButton CloseIcon  AudioVisualizer useVoiceAssistant BarVisualizer ControlBar RoomAudioRenderer AudioConference]]
            ["livekit-client" :refer [Track]]
            ["framer-motion" :refer [motion AnimatePresence]] 
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [com.vaani.conference :refer [video-conference]]
            ["react" :as react]
            ["react-dom" :as react-dom]))


;; Livekit Server and APIKEY
(def serverUrl "wss://vaani-voice-sc018bec.livekit.cloud")
(def access-token (r/atom nil))
(def room-name nil)
(defn fetch-and-set-access-token []
  (-> (js/fetch "http://127.0.0.1:8000/getToken" ;; Replace with your actual API URL
                (clj->js {:method "GET"}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response) ;; Parse the response as JSON
                 (throw (js/Error (str "HTTP Error: " (.-status response)))))))
      (.then (fn [json]
               (let [parsed-response (js->clj json :keywordize-keys true) 
                     token (get-in parsed-response [:token])] ;; Extract the token
                 (reset! access-token token) ;; Store the token in the atom
                 (js/console.log "Access token fetched and stored:" token)
                 token))) ;; Return the token
      (.catch (fn [error]
                (js/console.error "Error fetching access token:" error)))))


;; (defn get-access-token [] 
;;   (do
;;     (fetch-and-set-access-token) ;; Trigger the fetch-access-token function to get a new token
;;     @access-token)) ;; Return the current value of the token after fetching


;; Avatar Call
(def avatar
  [:> Avatar
   {:name "Vaani"
    :src "https://chatscope.io/storybook/react/assets/emily-xzL8sDL2.svg"}])

;; Creating Message HTML Structure 
(defn message-block [msg isBot]
  {:direction (if isBot "incoming" "outgoing")
   :message msg
   :sender (if isBot "vaani" "user")})

;; Creating Message structure for chat package
(defn message [message-block isBot]
  (if isBot avatar "")
  [:> Message {:model message-block}])

;; Conversation State Manager
(def conversation-history (r/atom (list  (message-block "Hello" true))))

(defn add-message [new-message is-bot]
  (let [message-entry (message-block new-message is-bot)]
    (swap! conversation-history #(conj (vec %) message-entry))))

(defn fetch-response [data]
  (-> (js/fetch "http://127.0.0.1:8000/chat"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/json"}
                          :body (js/JSON.stringify 
                                 #js {:user_message (if (string? data) data "")})}))
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (throw (js/Error (str "HTTP Error: " (.-status response)))))))
      (.then (fn [json]
               (let [parsed-response (js->clj json :keywordize-keys true)]
                 parsed-response))) ;; Return the entire parsed response
      (.catch (fn [error]
                (js/console.error "Fetch error:" error)
                (throw error)))))



;; (def response-text (r/atom nil))

(defn fetch-response-bot-message [user-message]
  (-> (fetch-response user-message)
    (.then (fn [response]
             (let [bot-response (get-in response [:bot_response])]
               (add-message bot-response true)
               bot-response)))))


(defn send-message [message]
  (add-message message false) 
  (fetch-response-bot-message message)
  
  ;; (add-message @response-text true)
)


(defn handle-connect-button []
  (js/console.log "Connect button clicked!"))


(defn control-bar [props]
  (let [agent-state (props :agentState)
        on-connect-clicked (props :onConnectButtonClicked)]
    [:div {:class "relative h-[100px]"}
     ;; First AnimatePresence block for "disconnected" state
    ;;  [:> AnimatePresence
    ;;   (when (= agent-state "disconnected")
    ;;     [:> (.-button motion)
    ;;      {:initial #js {:opacity 0 :top 0}
    ;;       :animate #js {:opacity 1}
    ;;       :exit #js {:opacity 0 :top "-10px"}
    ;;       :transition #js {:duration 1 :ease #js [0.09 1.04 0.245 1.055]}
    ;;       :className "uppercase absolute left-1/2 -translate-x-1/2 px-4 py-2 bg-white text-black rounded-md"
    ;;       :onClick (fn [] (on-connect-clicked))}
    ;;      "Start a conversation"])]

     ;; Second AnimatePresence block for other states (not disconnected or connecting)
     [:> AnimatePresence
      [:> (.-div motion)
       {:initial {:opacity 0 :top "10px"}
        :animate  {:opacity 1 :top 0}
        :exit {:opacity 0 :top "-10px"}
        :transition {:duration 0.4 :ease #js [0.09 1.04 0.245 1.055]}
        :className "flex h-8 absolute left-1/2 -translate-x-1/2 justify-center"}
       [:> VoiceAssistantControlBar {:controls {:leave false}}]
       [:> DisconnectButton
        [:> CloseIcon]]]]]))



(defn simple-voice-assistant []
  ;; Use `r/with-let` to handle React hooks properly within Reagent
  (let [{:keys [state audioTrack]} (js->clj (useVoiceAssistant) :keywordize-keys true)]
    ;; Render the BarVisualizer component with the required props
    (println state audioTrack)
    [:> BarVisualizer
     {:state state
      :barCount 7
      :trackRef audioTrack
      :style {:width "75vw" :height "300px"}}]))

(defn livekit-chat-page []
  (let [{:keys [state audioTrack]} (:f> useVoiceAssistant)
        handle-state-change (fn [state]
                              (js/console.log "State changed:" state))]

    [:div {:style {:position "relative" :height "500px"}}
     [:main {:data-lk-theme "default" :className "h-full grid content-center bg-[var(--lk-bg)]"}
      [:> LiveKitRoom {:serverUrl "wss://vaani-voice-sc018bec.livekit.cloud"
                       :token @access-token
                       :connect true
                       :video false
                       :audio true
                       :className "grid grid-rows-[2fr_1fr] items-center"}
     
       [:f> simple-voice-assistant {:on-state-change handle-state-change}]
     
       [:div {:style {:display "flex"
                      :flexDirection "column"
                      :min-height "800px"
                      :width "600px"
                      :margin "auto"
                      :fontFamily "sans-serif"}}
        [:h2 "LiveKit Audio + Chat"]
     
                                   ;; Participants area
        ;; [:div {:style {:border "1px solid #ccc"
        ;;                :padding "10px"
        ;;                :marginBottom "20px"}}
        ;;  [:h4 "Participants (Audio only)"]]
        
        [:> ControlBar]
     
                                   ;; Audio Visualizer
        [:div {:style {:marginBottom "20px"}}
         [:h4 "Audio Visualizer"]
         [:> BarVisualizer
          {:state state
           :barCount 5
           :class "agent-visualizer"
           :options {:minHeight 24}}]
         [:> RoomAudioRenderer]


         [:> VoiceAssistantControlBar {:controls {:leave false}}]
         ]]]]]))




;; (defn livekit-audio-conf []
;;   (fetch-and-set-access-token)
;;   ;; (let [token (js->clj useToken "wss://vaani-voice-sc018bec.livekit.cloud" room-name {:userInfo {:identity }})])
;;   [:div {:data-lk-theme "default"}
;;    [:> LiveKitRoom {:serverUrl "wss://vaani-voice-sc018bec.livekit.cloud"
;;                     :token @access-token
;;                     :connect true
;;                     :video false
;;                     :audio true}
;;     [:> RoomName]
;;     [:> ConnectionState]
;;     [:> AudioConference]]])

(def should-connect (r/atom false))

(defn livekit-voice-assistant [] 
  (fetch-and-set-access-token)
  (println "hello: " @should-connect)
  [:main {:data-lk-theme "default" :style {:height "100%"
                                           :display "grid"
                                           :gridTemplateRows "auto min-content"}}
   [:> LiveKitRoom {:serverUrl "wss://vaani-voice-sc018bec.livekit.cloud"
                    :token @access-token
                    :audio true
                    :connect (deref should-connect)
                    :onDisconnected (fn [] (reset! should-connect false))
                    :className "room"
                    :style {:display "grid" :grid-template-rows "auto min-content"}}
    [:> ConnectionState]
    [:div {:className "inner" :style {:display "flex" :justify-coontent "center" :align-items "center"}}
     (if @should-connect
  
       [:f> simple-voice-assistant]
       [:button {:className "lk-button" :onClick (fn [] (reset! should-connect true))} "Connect"])]
    [:> VoiceAssistantControlBar]
    [:> RoomAudioRenderer]]])

;; (defn audio-call-button []
;;   [:button {:style {:background-color "#28a745"
;;                     :border "none"
;;                     :border-radius "50%"
;;                     :color "white"
;;                     :padding "10px"
;;                     :cursor "pointer"
;;                     :margin-left "10px"}
;;             :on-click (fn [] (livekit-voice-assistant))}
;;    [:svg {:xmlns "http://www.w3.org/2000/svg"
;;           :width "16" :height "16"
;;           :fill "currentColor"
;;           :class "bi bi-telephone-fill"
;;           :viewBox "0 0 16 16"}
;;     [:path {:d "M3.654 1.328a.678.678 0 0 1 1.015-.063L6.29 2.593c.329.329.445.812.284 1.254l-.805 2.162a.678.678 0 0 1-.588.44c-.95.112-1.564.748-2.156 1.34-.592.593-1.228 1.207-1.34 2.156a.678.678 0 0 1-.44.588l-2.162.805a1.745 1.745 0 0 0-1.255-.284l-1.327 1.326a1.745 1.745 0 0 0 1.326-.528l3.414-3.414a.678.678 0 0 0-.528-1.326L1.18 6.647A.678.678 0 0 0 .768 6.1C.68 5.94.564 5.457.624 5.015L1.55 3.66c.18-.42.527-.768.947-.947L3.654 1.328zM3.354 1.763c-.2-.2-.488-.295-.768-.18L1.313 2.59c-.226.09-.398.262-.487.487L.182 4.352c-.036.09-.037.188-.002.278l.683 1.707c.102.257.204.486.334.716.13.23.267.458.41.685.143.227.29.455.438.68.148.225.298.448.453.664.155.217.316.433.482.645.166.212.336.421.508.626l-.183.732a.678.678 0 0 0 .717.717l.732-.183c.205.172.414.342.626.508.212.166.428.327.645.482.216.155.439.305.664.453.226.148.454.296.68.438.227.143.455.28.685.41.23.13.459.232.716.334l1.707.683c.09.036.188.035.278-.002l1.265-.44c.226-.09.398-.262.487-.487l.558-1.515c.115-.28.02-.567-.18-.768L3.354 1.763z"}]]])


(def show-voice-assistant? (r/atom false))
(defn audio-call-button []
  [:div
         ;; Button to trigger voice assistant
   [:button {:style {:background-color "#4CAF50"
                     :color "white"
                     :padding "10px 20px"
                     :border "none"
                     :border-radius "4px"
                     :cursor "pointer"}
             :onClick (fn []
                        (reset! show-voice-assistant? true))}
    "Start Audio Call"]
         ;; Conditional rendering of the voice assistant
   (when @show-voice-assistant?
     [:div {:style {:position "fixed"
                    :top "0"
                    :left "0"
                    :width "100vw"
                    :height "500px"
                    :background-color "rgba(0,0,0,0.8)"
                    :z-index "1000"
                    :display "flex"
                    :align-items "center"
                    :justify-content "center"}}
            ;; Wrapper for the voice assistant with relative positioning
      [:div {:style {:width "90%"
                     :height "90%"
                     :background-color "white"
                     :box-shadow "0px 4px 10px rgba(0, 0, 0, 0.25)"
                     :border-radius "8px"
                     :overflow "hidden"
                     :position "relative"}}
             ;; Close button with proper layering
       [:button {:style {:position "absolute"
                         :top "10px"
                         :right "10px"
                         :z-index "1001" ;; Ensure it's above all other content
                         :background "red"
                         :color "white"
                         :border "none"
                         :border-radius "50%"
                         :width "30px"
                         :height "30px"
                         :cursor "pointer"}
                 :onClick (fn [] (reset! show-voice-assistant? false))}
        "X"]
             ;; Render livekit-voice-assistant
       [livekit-voice-assistant]]])])


(defn new-component []
  [:div {:style {:position "relative" :height "500px"}}
   [:> MainContainer
    [:> ChatContainer
     [:> ConversationHeader {:style {:background-color "#343a40"}}
      [:> Avatar {:name "Vaani"  :src "https://chatscope.io/storybook/react/assets/emily-xzL8sDL2.svg"}]
      [:> ConversationHeader.Content {:userName "Vaani"}
       [:span {:style {:background-color "#343a40" :color "black"}}]]
      [:> ConversationHeader.Actions [audio-call-button]]]
     [:> MessageList
      (for [m @conversation-history]
        (message m false))]
     [:> MessageInput {:placeholder "Type  message here" :onSend send-message}]]]])


(defn ^:export run []
  (rdom/render [new-component] (js/document.getElementById "root")))