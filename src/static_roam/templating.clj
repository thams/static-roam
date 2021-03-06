(ns static-roam.templating
  (:require [static-roam.utils :as utils]
            [static-roam.parser :as parser]
            [static-roam.database :as database]
            [clojure.pprint :as pprint]))

(defn page-hiccup
  [body-hiccup site-title nav-bar-page-dict css-path js-path]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title site-title]
    [:link {:rel "stylesheet" :href css-path}]
    [:script {:src js-path}]]
   [:body
    [:header.site-header
     [:div.wrapper
      [:a.site-title {:rel "author" :href ".."} site-title]
      (into
       [:div.nav-links]
       (map (fn [pair] [:a.nav-link {:href (first pair)} (second pair)]) nav-bar-page-dict))]]
    body-hiccup]])

(defn children-of-block-template
  [block-id block-map]
  (let [properties (database/get-properties-for-block-id block-id block-map)]
    [:ul
     (if (or (nil? (:hiccup properties)) (= (:content properties) block-id))
       ""
       [:li {:onclick (str "location.href='" (utils/page-title->html-file-title block-id :case-sensitive) "'")} (:hiccup properties)])
     (let [children (:children properties)]
       (if (not= 0 (count children))
         ;; recurse on each of the children
         (map #(children-of-block-template % block-map) children)
         ;; otherwise, evaluate to empty div
         [:div]))]))

(defn linked-references-template
  [references block-map]
  (concat []
          (map
           (fn
             [r]
             (if (nil? (get block-map r))
               ""
               [:li
                {:onclick
                 (str "location.href='"
                      (str ""
                           (utils/page-title->html-file-title
                            r
                            :case-sensitive)) "'" )}
                (:hiccup (get block-map r))]))
           references)))

(defn- is-parent
  [block-id block-kv]
  (if (some #(= block-id %) (:children (second block-kv)))
    (first block-kv)
    nil))

(defn- find-parent
  [block-id block-map]
  (filter #(not (nil? %)) (map #(is-parent block-id %) block-map)))

(defn- get-parent
  [block-id block-map]
  (let [parent-id (first (find-parent block-id block-map))]
    (if (nil? parent-id)
      ""
      [:a {:href (str ""
                      (utils/page-title->html-file-title
                       parent-id
                       :case-sensitive))}
       (:content (get block-map parent-id))])))

(defn block-page-template
  [block-id block-content block-map]
  [:div
   [:div
    [:h3 (get-parent block-id block-map)]]
   [:h2 (vec (map #(parser/ele->hiccup % block-map) (parser/parse-to-ast block-content)))]
   [:div (children-of-block-template block-id block-map)]
   [:div {:style "background-color:lightblue;"}
    [:h3 "Linked References"]
    (linked-references-template (database/get-linked-references block-id block-map) block-map)]])

(defn page-index-hiccup
  [link-list css-path js-path]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href css-path}]
    [:script {:src js-path}]]
   [:body link-list]])

(defn home-page-hiccup
  [link-list title nav-bar-page-dict css-path js-path]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:title title]
    [:link {:rel "stylesheet" :href css-path}]
    [:script {:src js-path}]]
   [:body
    [:header.site-header
     [:div.wrapper
      [:a.site-title {:rel "author" :href "."} title]
      (into
       [:div.nav-links]
       (map (fn [pair] [:a.nav-link {:href (str "./pages" (subs (first pair) 1))} (second pair)]) nav-bar-page-dict))]]
    [:main.page-content {:aria-label "Content"}
     [:div.wrapper
       [:h2.post-list-heading "Entry Points"]
       link-list]]]])

(defn list-of-page-links
  "Generate a Hiccup unordered list of links to pages"
  ([page-titles]
   (let [page-links (map utils/page-link-from-title page-titles)]
     (conj [:ul.post-list ] (map (fn [a] [:li [:h3 a]]) page-links))))
  ([page-titles dir]
   (let [page-links (map #(utils/page-link-from-title %) page-titles)]
     (conj [:ul.post-list ] (map (fn [a] [:li [:h3 a]]) page-links))))
  ([page-titles dir link-class]
   (let [page-links (map #(utils/page-link-from-title dir % link-class) page-titles)]
     (conj [:ul.post-list ] (map (fn [a] [:li [:h3 a]]) page-links)))))
