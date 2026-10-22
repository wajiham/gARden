
## Getting started 

The app is **already connected to our shared Firebase project**. You do **not** need to create a new Firebase project, register another Android app in Firebase, or generate a new `google-services.json` to run it. The existing Android client configuration is committed at `app/google-services.json`.

1. Clone the team's **private GitLab repository** and open the **ARGarden** directory in Android Studio.
2. Set the Gradle JDK to Android Studio's bundled JDK 21 or JDK 17. Avoid Java 25 with Gradle 8.13.
3. Install Android SDK Platform 36, accept the SDK licences, and let Gradle sync and install required components. Android Studio will create your own untracked `local.properties` file for the SDK location if needed.
4. Start an Android emulator. Run the `app` configuration.
5. **Pre-created testing accounts are available in [`ACCOUNTS.md`](ACCOUNTS.md).** Use the account details in that file to sign in and test the app. 
6. Follow the [testing checklist](docs/TESTING.md) for login, friend requests, navigation, and device checks.
7. **No Firebase Console access is required, just to build and run the app**.
8. Running the app requires internet access and a functioning Firebase backend.
9. If you see a Firebase permission, configuration or authentication error, contact the me as i am managing Firebase; **do not create a replacement Firebase project**.
