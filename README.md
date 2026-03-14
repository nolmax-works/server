# nolmax's chat server
This repository contains the codebase for the nolmax chat app's server. Very preliminary, currently in the works.

For now, packet schemas are in `./src/main/proto` and shared between projects. Might unify into a shared library soon once we deem all of those things "final".

## Requirements
- JDK 21 (or later)

## Build
Simply run `gradlew.bat build` if you're on Windows, `./gradlew build` if you are on *nix, or `gradle build` if you have the JDK installed already.