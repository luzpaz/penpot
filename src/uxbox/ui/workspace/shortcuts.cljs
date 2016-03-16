;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) 2015-2016 Andrey Antukh <niwi@niwi.nz>
;; Copyright (c) 2015-2016 Juan de la Cruz <delacruzgarciajuan@gmail.com>

(ns uxbox.ui.workspace.shortcuts
  (:require-macros [uxbox.util.syntax :refer [define-once]])
  (:require [goog.events :as events]
            [beicon.core :as rx]
            [uxbox.rstore :as rs]
            [uxbox.data.workspace :as dw])
  (:import goog.events.EventType
           goog.events.KeyCodes
           goog.ui.KeyboardShortcutHandler
           goog.ui.KeyboardShortcutHandler))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keyboard Shortcuts Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn move-selected
  [dir speed]
  (case speed
    :std (rs/emit! (dw/move-selected dir 1))
    :fast (rs/emit! (dw/move-selected dir 20))))

(defonce ^:const +shortcuts+
  {:ctrl+g #(rs/emit! (dw/toggle-flag :grid))
   :ctrl+shift+f #(rs/emit! (dw/toggle-flag :drawtools))
   :ctrl+shift+i #(rs/emit! (dw/toggle-flag :icons))
   :ctrl+shift+l #(rs/emit! (dw/toggle-flag :layers))
   :ctrl+r #(rs/emit! (dw/toggle-flag :ruler))
   :ctrl+d #(rs/emit! (dw/duplicate-selected))
   :esc #(rs/emit! (dw/deselect-all))
   :backspace #(rs/emit! (dw/delete-selected))
   :delete #(rs/emit! (dw/delete-selected))
   :shift+up #(move-selected :up :fast)
   :shift+down #(move-selected :down :fast)
   :shift+right #(move-selected :right :fast)
   :shift+left #(move-selected :left :fast)
   :up #(move-selected :up :std)
   :down #(move-selected :down :std)
   :right #(move-selected :right :std)
   :left #(move-selected :left :std)})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keyboard Shortcuts Watcher
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:const ^:private +bus+
  (rx/bus))

(defonce ^:const +stream+ +bus+)

(defn- init-handler
  []
  (let [handler (KeyboardShortcutHandler. js/document)]
    ;; Register shortcuts.
    (doseq [item (keys +shortcuts+)]
      (let [identifier (name item)]
        (.registerShortcut handler identifier identifier)))

    ;; Initialize shortcut listener.
    (let [event KeyboardShortcutHandler.EventType.SHORTCUT_TRIGGERED
          callback #(rx/push! +bus+ (keyword (.-identifier %)))
          key (events/listen handler event callback)]
      (fn []
        (events/unlistenByKey key)
        (.clearKeyListener handler)))))

(define-once :subscriptions
  (rx/on-value +stream+ #(println "[debug]: shortcut:" %))
  (rx/on-value +stream+ (fn [event]
                          (when-let [handler (get +shortcuts+ event)]
                            (handler)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Keyboard Shortcuts Mixin
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -will-mount
  [own]
  (let [sub (init-handler)]
    (assoc own ::subscription sub)))

(defn -will-unmount
  [own]
  (let [sub (::subscription own)]
    (sub)
    (dissoc own ::subscription)))

(defn -transfer-state
  [old-own own]
  (assoc own ::subscription (::subscription old-own)))

(def mixin
  {:will-mount -will-mount
   :will-unmount -will-unmount
   :transfer-state -transfer-state})
