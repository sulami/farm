(ns ^:figwheel-no-load farm.dev
  (:require
    [farm.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
