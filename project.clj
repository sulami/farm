(defproject farm "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.3.1"]
                 [re-frame "0.10.5"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-asset-minifier "0.2.7"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel
  {:http-server-root "public"
   :server-port 3449
   :nrepl-port 7002
   :nrepl-middleware [cider.piggieback/wrap-cljs-repl]
   :css-dirs ["resource/public/css"]
   :ring-handler farm.handler/app}

  :profiles
  {:dev
   {:dependencies [[cider/piggieback "0.3.8"]
                   [figwheel-sidecar "0.5.16"]
                   [binaryage/devtools "0.9.10"]]
    :plugins      [[lein-figwheel "0.5.16"]]}
   :prod { }
   }

  :minify-assets
  {:assets
   {"resources/public/css/site.min.css" "resources/public/css/site.css"}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljc"]
     :figwheel     {:on-jsload "farm.core/mount-root"}
     :compiler     {:main                 farm.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljc"]
     :compiler     {:main            farm.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
