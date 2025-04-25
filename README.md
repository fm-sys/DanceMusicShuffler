To build a JAR file, run the gradle command:
```bash
./gradlew shadowJar
```

Or just run the main method in the `Main` class.

As we use the Spotify WebAPI in development mode, you need to edit the list of authorized users if you want to run this software on behalf of a foreign spotify user account:
https://developer.spotify.com/dashboard/b7cea7e9e1af4b16b985cd76af7ea846/users