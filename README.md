# Farm

Tests: [![CircleCI](https://circleci.com/gh/sulami/farm.svg?style=svg)](https://circleci.com/gh/sulami/farm)

A harsh farming simulation set in medieval England. Think Stardew Valley meets Darkest Dungeon.

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
