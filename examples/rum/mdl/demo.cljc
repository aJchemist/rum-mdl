(ns rum.mdl.demo
  #?(:cljs (:require-macros rum.mdl.demo))
  (:require
   #?(:clj [sablono.compiler :as s])
   [clojure.string :as string]
   [rum.core :as rum]
   [rum.mdl  :as mdl]))

(declare syntaxify)

(defn intro
  ([title]
   [:.intro [:h3 title]])
  ([title text]
   [:.intro [:h3 title] [:p text]]))

(defn snippet-demos [demos]
  (conj
   (reduce conj [:.demos [:.padding]]
           (for [demo demos]
             [:.demo demo]))
   [:.padding]))

(defn snippet-captions [captions]
  (conj
   (reduce conj [:.captions [:.padding]]
           (for [caption captions]
             [:.caption caption]))
   [:.padding]))

(rum/defc snippet
  [{:keys [sources components captions]}]
  [:.snippet
   [:.header
    (snippet-demos components)
    (snippet-captions captions)]
   (reduce conj [:source] sources)])

#?(:clj
   (defmacro oneliner [& xs]
     (let [xs   (partition 2 xs)
           code (map first xs)
           captions (map second xs)
           sources (->> code
                     (map pr-str)
                     (map syntaxify))]
       `[:.snippet
         [:.header
          ~(snippet-demos code)
          ~(snippet-captions captions)]
         [:.source ~@sources]])))

(rum/defc section [& contents]
  (mdl/grid
   {:class ["demo" "example"]
    :mdl   [:color-text--grey-600]}
   (apply mdl/cell {:mdl [:12]} contents)))

;;; syntax highlight
;; https://github.com/reagent-project/reagent/blob/master/demo/reagentdemo/syntax.cljs

(def comment-style {:style {:color "gray" :font-style "italic"}})
(def string-style  {:style {:color "green"}})
(def keyword-style {:style {:color "blue"}})
(def builtin-style {:style {:color "#687868" :font-weight "bold"}})
(def def-style     {:style {:color "#5050c0" :font-weight "bold"}})

(def paren-style-1 {:style {:color "#272"}})
(def paren-style-2 {:style {:color "#940"}})
(def paren-style-3 {:style {:color "#44a"}})

;;;;; Colorization

(def builtins
  #{"def" "defn" "defonce" "ns" "atom" "let" "if" "when"
    "cond" "merge" "assoc" "swap!" "reset!" "for"
    "range" "nil?" "int" "or" "->" "->>" "%" "fn" "if-not"
    "empty?" "case" "str" "pos?" "zero?" "map" "remove"
    "empty" "into" "assoc-in" "dissoc" "get-in" "when-not"
    "filter" "vals" "count" "complement" "identity" "dotimes"
    "update-in" "sorted-map" "inc" "dec" "false" "true" "not"
    "=" "partial" "first" "second" "rest" "list" "conj"
    "drop" "when-let" "if-let" "add-watch" "mod" "quot"
    "bit-test" "vector" "do" "try" "catch" "finally"})

(def styles
  {:comment  comment-style
   :str-litt string-style
   :keyw     keyword-style
   :builtin  builtin-style
   :def      def-style})

(def paren-styles [paren-style-1 paren-style-2 paren-style-3])

(def tokenize-pattern
  (let [ws        " \\t\\n"
        open      "\\[({"
        close     ")\\]}"
        sep       (str ws open close)
        comment-p ";.*"
        str-p     "\"[^\"]*\""
        open-p    (str "[" open "]")
        close-p   (str "[" close "]")
        iden-p    (str "[^" sep "]+")
        meta-p    (str "\\^" iden-p)
        any-p     (str "[" ws "]+|\\^[^" sep "]+|.")]
    (re-pattern (str "("
                     (string/join ")|(" [comment-p str-p open-p
                                         close-p meta-p iden-p any-p])
                     ")"))))

(def keyw-re #"^:")
(def qualif-re #"^[a-z]+/")
(def def-re #"^def|^ns\b")

(defn tokenize [src]
  (for [[s comment strlitt open close met iden any]
        (re-seq tokenize-pattern src)]
    (cond
      (some? comment) [:comment s]
      (some? strlitt) [:str-litt s]
      (some? open)    [:open s]
      (some? close)   [:close s]
      (some? met)     [:other s]
      (some? iden)    (cond (re-find keyw-re s) [:keyw s]
                            (builtins s) [:builtin s]
                            (re-find qualif-re s) [:builtin s]
                            :else [:iden s])
      (some? any)     [:other s])))

(defn syntaxify [src]
  (let [ncol (count paren-styles)
        paren-style #(nth paren-styles (mod % ncol))]
    (loop [tokens (tokenize (str src " "))
           prev nil
           level 0
           res []]
      (let [[kind val] (first tokens)
            level' (case kind
                     :open  (inc level)
                     :close (dec level)
                     level)
            style (case kind
                    :iden  (when (and prev (re-find def-re prev))
                             (:def styles))
                    :open  (paren-style level)
                    :close (paren-style level')
                    (styles kind))
            remain (rest tokens)]
        (if-not (empty? remain)
          (recur remain
                 (case kind :other prev val)
                 level'
                 (if (nil? style)
                   (let [old (peek res)]
                     (if (string? old)
                       (conj (pop res) (str old val))
                       (conj res val)))
                   (conj res [:span style val])))
          (into [:pre] res))))))

(comment
  (snippet
   (mdl/button {:mdl [:fab :colored]} (mdl/icon "add"))
   "Colored FAB"
   (mdl/button {:mdl [:fab :colored :ripple]} (mdl/icon "add"))
   "With ripple")
  
  (syntaxify
   (str
    (pr-str
     '(defn simple-component []
        [:div
         [:p "I am a component!"]
         [:p.someclass
          "I have " [:strong "bold"]
          [:span {:style {:color "red"}} " and red "] "text."]])
     )
    "(defn simple-component []
      [:div
       [:p \"I am a component!\"]
       [:p.someclass
        \"I have \" [:strong \"bold\"]
        [:span {:style {:color \"red\"}} \" and red \"] \"text.\"]])"))

  )
