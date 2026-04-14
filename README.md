# DanceMusicShuffler

DanceMusicShuffler is a Java application that can automatically shuffle multiple Spotify playlists into a unified queue. It uses the Spotify Web API to interact with the Spotify player (App, Webplayer, or even your Phone).

DanceMusicShuffler was initially designed for Windows, but should work on other desktop platforms like Linux and MacOS as well.

## Screenshots

| Config Window                                                                  | Dance Floor Display                                                             |
|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| ![DanceMusicShuffler Application Window Screenshot](/doc/imgs/screenshot1.png) | ![DanceMusicShuffler Dance Floor Display Screenshot](/doc/imgs/screenshot2.png) |

## How to build and run

To build a JAR file, run the gradle command:
```bash
./gradlew shadowJar
```

Or just run the main method in the `Main` class.

### Limitations:

As we use the Spotify WebAPI in development mode, we need to manually add your Spotify user account to the list of authorized users before you can access the API:
https://developer.spotify.com/dashboard/b7cea7e9e1af4b16b985cd76af7ea846/users

Alternatively, you can create your own Spotify API key and change the `clientId` in `API.java`.
