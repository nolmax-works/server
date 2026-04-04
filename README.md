# nolmax's chat server
This repository contains the codebase for the nolmax chat app's server. Currently somewhat functional :trollface:.

For now, packet schemas are in the [packet library](https://github.com/nolmax-works/packet).

# Usage
- Download the JAR executable file as `server.jar`. Then run `java -jar server.jar`.
- On first run, the application will spawn a `config.yml` file in the file next to the JAR executable and exit. Make sure to edit the configuration file to include the necessary server configuration and credentials.
- Start the server again, and the app will be online at the listening address and the listening port of your choice. By default, this is `0.0.0.0:18181`.

# Docker image
- The configuration file can be inserted in the container with the path `/app/config.yml`.

## Requirements
- JDK 21 (or later)

## Build
Simply run `gradlew.bat build` if you're on Windows, `./gradlew build` if you are on *nix, or `gradle build` if you have Gradle installed already.