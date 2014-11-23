(ns play-clj-sandbox.core.desktop-launcher
  (:require [play-clj-sandbox.core :refer :all])
  (:import [com.badlogic.gdx.backends.lwjgl LwjglApplication]
           [org.lwjgl.input Keyboard])
  (:gen-class))

(defn -main
  []
  (LwjglApplication. play-clj-sandbox "play-clj-sandbox" 800 600)
  (Keyboard/enableRepeatEvents true))
