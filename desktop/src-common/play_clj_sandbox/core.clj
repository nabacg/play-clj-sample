(ns play-clj-sandbox.core
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer :all]
            [play-clj.math :refer :all]
            [clojure.pprint :refer [pprint]]))

(def speed 15)

(defn- update-position [entity direction]
  (let [{x :x y :y} entity]
    (case direction
      :up    (assoc entity :y (+ y speed))
      :down  (assoc entity :y (- y speed))
      :right (assoc entity :x (+ x speed))
      :left  (assoc entity :x (- x speed))
      entity)))

(defn- update-texture [entity direction]
  (when (not= (:direction entity) direction)
    (texture! entity :flip true false))
  entity)

(defn move [entity direction]
  (-> entity
      (update-position direction)
      (update-texture direction)
      (assoc  :direction direction)))

(defn- get-first-player [entities]
  (first (filter #(-> % :id nil? not) entities)))


(comment
  "to restart screen from repl while game is running:"
  (on-gl (set-screen! play-clj-sandbox main-screen))
  "to deref current state of RUNNING game just do this"
  (-> main-screen :entities deref)

  "alternative is to use some helper function from"
  (use 'play-clj.repl)
  "then"
  (e main-screen) "will bring current state in entities coll"  )

(comment
      (let [p1 (get-first-player entities)
            p1' (move p1 (key->direction (:key screen)))]
        (pprint p1')
        (swap-player entities p1')))

(defn- key->direction [key]
  (cond
   (= key (key-code :dpad-up)) :up
   (= key (key-code :dpad-down)) :down
   (= key (key-code :dpad-right)) :right
   (= key (key-code :dpad-left)) :left))

(defn- touch->direction []
  (cond
      (> (game :y) (* (game :height) (/ 2 3))) :up
      (< (game :y) (/ (game :height) 3)) :down
      (> (game :x) (* (game :width) (/ 2 3))) :right
      (< (game :x) (/ (game :width) 3)) :left))

(defn create-koalio []
  (let [sheet (texture "koalio.png")
        tiles (texture! sheet :split 18 26)
        player-images (for [col [0 1 2 3 4]]
                        (texture (aget tiles 0 col)))]
    (println tiles player-images)
    (first  player-images)))

(defn- update-player [{id :id :as e} direction]
  (if (nil? id)
    e
    (-> e
        (move direction))))

(comment
  (defn- update-player-sprite [{id :id :as e } direction images]
    (if (nil? id)
      e
      (-> (move e direction)
          ))))

(defn- update-hitbox [{:keys [x y width height id is-turtle?] :as entity}]
  (if (or (not (nil? id)) is-turtle?)
    (assoc entity :hit-box (rectangle x y width height))
    entity))

(defn- spawn-turtle []
  (let [x (+ 50 (rand-int 600))
        y (+ 50 (rand-int 500))]
    (-> (texture "Turtle_1.png")
        (assoc :x x :y y :height 100 :width 100 :is-turtle? true))))

(defn- grab-touched-turtles [entities]
  (if-let [turtles (filter :is-turtle? entities)]
    (let [player (some #(when (-> % :id nil? not) %) entities)
          {score :score :as score-label}
          (some #(when (:is-score-board? %) %) entities)
          touched (filter
                   #(rectangle! (:hit-box player) :overlaps (:hit-box %)) turtles)
          new-score (+ score (count touched))]
      (label! score-label :set-text (str "Score: " new-score))
      (->> entities
           (remove (set touched))
           (map #(if (:is-score-board? %)
                   (assoc % :score new-score)
                   %))))
    entities))

(defscreen main-screen
  :on-show
  (fn [screen entities]
    (update! screen :renderer (stage))
    (add-timer! screen :spawn-turtle 1 3) ;first turle on 1 sec then every 3 sec
    [(-> (texture "Map.png")
         (assoc :width 800 :height 600))
     (-> (create-koalio)
         (assoc :x 50 :y 50 :width 100 :height 100)
         (assoc :id :p1 :direction :right))
     (assoc (label "Score: 0" (color :red))
       :is-score-board? true
       :score 0)])
  :on-key-down
  (fn [screen entities]
    (comment  (pprint entities))
    (pprint (filter #(-> % :id nil? not) entities))
    (->> entities
         (map #(-> %
                   (update-player (key->direction (:key screen)))
                   (update-hitbox)))
         (grab-touched-turtles)))
  :on-touch-down
  (fn [screen entities]
    (->> entities
     (map #(-> %
               (update-player (touch->direction))
               (update-hitbox)))
     (grab-touched-turtles)))
  :on-render
  (fn [screen entities]
    (clear!)
    (render! screen entities))
  :on-timer
  (fn [screen entities]
    (case (:id screen)
      :spawn-turtle (conj entities (spawn-turtle)))))

(defgame play-clj-sandbox
  :on-create
  (fn [this]
    (set-screen! this main-screen)))
