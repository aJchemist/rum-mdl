(ns rum.mdl.examples.snackbar
  (:require
   [rum.core :as rum]
   [rum.mdl  :as mdl]
   [rum.mdl.demo :as demo]))

(defn rand-color []
  (str "#"
       (-> (rand)
         (* 0xffffff)
         #?(:clj (Math/floor) :cljs (js/Math.floor))
         #?(:clj (Integer/toHexString) :cljs (.toString 16)))))

(let [color (volatile! "")]
  (defn on-click-fn [this]
    #?(:cljs
       (fn [_]
         (vreset! color (rand-color))
         (rum/request-render this)
         (let [snackbar (aget this "refs" "snackbar")]
           (mdl/show-snackbar snackbar
            #js {:message "Button color changed."
                 :timeout 1200
                 :actionHandler (fn [_]
                                  (vreset! color "")
                                  (.forceUpdate this))
                 :actionText "Undo"})))))

  (rum/defcc example-snackbar [this]
    #?(:cljs
       [:div
        (rum/with-ref (mdl/snackbar) "snackbar")
        (mdl/button
         {:mdl [:raised]
          :style {:background-color @color}
          :on-click (on-click-fn this)}
         "Show Snackbar")])))

(let [counter (volatile! 0)]
  (rum/defcc toast [this]
    #?(:cljs
       [:div
        (rum/with-ref (mdl/snackbar) "snackbar")
        (mdl/button
         {:mdl [:raised]
          :on-click
          (fn [_]
            (vswap! counter inc)
            (let [snackbar (aget this "refs" "snackbar")]
              (mdl/show-snackbar snackbar
               #js {:message (str "Example Message #" @counter)})))}
         "Show Toast")])))

(def example-snackbar-src
  "(let [color (volatile! \"\")]
  (defn on-click-fn [this]
    #?(:cljs
       (fn [_]
         (vreset! color (rand-color))
         (rum/request-render this)
         (let [snackbar (aget this \"refs\" \"snackbar\")]
           (mdl/show-snackbar snackbar
            #js {:message \"Button color changed.\"
                 :timeout 1200
                 :actionHandler (fn [_]
                                  (vreset! color \"\")
                                  (.forceUpdate this))
                 :actionText \"Undo\"})))))

  (rum/defcc example-snackbar [this]
    #?(:cljs
       [:div
        (rum/with-ref (mdl/snackbar) \"snackbar\")
        (mdl/button
         {:mdl [:raised]
          :style {:background-color @color}
          :on-click (on-click-fn this)}
         \"Show Snackbar\")])))")

(def toast-src
  "(let [counter (volatile! 0)]
  (rum/defcc toast [this]
    #?(:cljs
       [:div
        (rum/with-ref (mdl/snackbar) \"snackbar\")
        (mdl/button
         {:mdl [:raised]
          :on-click
          (fn [_]
            (vswap! counter inc)
            (let [snackbar (aget this \"refs\" \"snackbar\")]
              (mdl/show-snackbar snackbar
               #js {:message (str \"Example Message #\" @counter)})))}
         \"Show Toast\")])))")

(rum/defc examples
  []
  (demo/section
   (demo/intro "Snackbar")
   (demo/snippet-group
    {:demos
     [(example-snackbar)
      (toast)]
     :captions
     ["Snackbar" "Toast"]
     :sources
     [example-snackbar-src
      toast-src]})))
