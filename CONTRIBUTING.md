## Building 
To build a development version of the app from source, follow the instructions outlined below. 

1. Download and install [Android Studio](http://developer.android.com/sdk/index.html)
2. Clone the project or a fork of it to your local development machine
3. Import the project into Android Studio
4. Sync Project with Gradle files to download all the dependencies
5. Open the SDK manager to install the required Android SDK Tools and Android SDK Build-tools
6. The project contains a preconfigured keystore to sign debug builds. If you already have a custom keystore you can specify it in project/app/build.gradle android.signingConfigs.debug
8. Required: 
    1. Create a new file in the main directory called `gradle.properties`
    2. Input Fabric API Key. If you don't have one, use `fabricApiKey=000`

10. Build the project
