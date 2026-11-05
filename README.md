# HomeHub 

![HomeHub logo](screenshots/logo.png)

HomeHub is an Android app (Kotlin) that lets South African households book trusted home services
(plumbing, electrical, cleaning, painting, pest control and more) from their phone, track the job,
and choose Cash or Card once the job is done.

**Team: Amadyani Systems ** Vuyolwethu Sicwebi,Musasa Ncukana and Cwenga Sokupe | **Module:Open Source Coding** module code:OPSC6321 | **Final PoE ** `final-poe`

## 1. Purpose of the app
Finding a reliable plumber or electrician is stressful. HomeHub gives users one place to request a
service, follow its status (Pending → In Progress → Completed) and pay afterwards.
It keeps working with a poor or missing connection, and it is available in English, isiZulu and isiXhosa.

## 2. Features
| Feature | How it works |
|---|---|
| Register / Login | ASP.NET Core REST API. Passwords are hashed with ASP.NET `PasswordHasher` and never stored in plain text |
| Single sign-on (SSO) | Google Sign-In (Credential Manager) → Firebase Auth → the API verifies the Google ID token |
| Settings | Notifications on/off, default payment method and language |
| REST API + database | ASP.NET Core API hosted on Azure App Service (https://homehub20261105134433-f6dzbebrh6frcjet.canadacentral-01.azurewebsites.net/) with SQLite through Entity Framework. Swagger is at `/swagger` || Offline mode with sync | Requests made with no connection are saved in a local SQLite database and sent automatically when the phone reconnects |
| Push notifications | Firebase Cloud Messaging (topic `homehub_updates`) plus a local "Requests synced" notification |
| Multi-language | English, isiZulu and isiXhosa, switched in Settings |
| NoSQL database | Cloud Firestore stores service reviews and profile data |
| Blob storage | Firebase Storage stores profile pictures (compressed before upload) |
| Booking and payments | Validated service booking, request tracking, Cash/Card payment |

## 3. Design considerations
- **Colours:** navy (#0B3273) for trust, orange (#F7941D) for action buttons, cream background (#FFF3E0).
- **Simple navigation:** one home screen with large buttons and a "Back to Home" link on every screen.
- **Error handling:** every input is validated. Network and Firebase failures show a friendly message and never crash the app.
- **Offline first for bookings:** a booking that cannot reach the server is saved on the phone instead of being lost.
- **Fast failure:** the API client uses short timeouts, so a missing connection is detected quickly.
- **Security:** hashed passwords, token-verified SSO, and Firestore/Storage rules that require sign-in.
- **Logging:** `Log.d / Log.i / Log.e` with the tag `HomeHub` shows what happens at each step.

![Screenshots](screenshots/screens.png)

## 4. Architecture
- Android app (Kotlin, Retrofit) → ASP.NET Core API → SQLite
- Android app → Firebase (Auth, Firestore, Storage, Cloud Messaging)
- Android app → local SQLite (offline request queue and cached services list)

### How offline mode with sync works
1. **While online**, opening *Services* downloads the services list and saves a copy in the local SQLite table `cached_services`.
2. **With no connection**, *Services* shows the saved copy straight away, so the user can still open the booking screen.
3. When the user taps *Submit Request* offline (or the server cannot be reached), the request is saved in the local table `pending_requests`.
4. *My Requests* shows these saved requests as ** Waiting to sync** cards. They come from the phone's own database, so the server call can never remove them.
5. A network callback in `HomeHubApp.kt` detects when a working connection returns and calls `SyncManager`. It sends the saved requests to the API in the order they were made, deletes each one after success, and shows a **Requests synced** notification.
6. Requests the server rejects for good (for example a date that is now in the past) are dropped so they do not block the queue.

### Firebase sign-in
Email/password users receive a silent anonymous Firebase session (`FirebaseHelper.ensureSignedIn`) before any Firestore or Storage call. This is needed because the security rules require `request.auth != null`.

## 5. GitHub and GitHub Actions
- The repository was initialised with this README. We worked on separate branches (`feature/cloud`, `feature/offline-lang`) and merged them with pull requests.
- We committed regularly, and the final commit is tagged `final-poe`.
- **GitHub Actions** (`.github/workflows/android.yml`) runs on every push and pull request. It sets up JDK 17, runs the unit tests (`ValidationUtilsTest`, `RequestStatusHelperTest`), builds the debug APK and uploads the test report and the APK.
- `google-services.json` is stored as an encrypted GitHub secret (`GOOGLE_SERVICES_JSON`) and is not in the repository.

![Passing workflow](screenshots/actions.png)

## 6. Running the project
1. Clone the repo and open it in Android Studio.
2. Add your own `google-services.json` to the `app/` folder.
3. Set `BASE_URL` in `network/ApiClient.kt` to your API address.
4. Run the app on a physical Android phone (Android 7.0 or newer).

### Firebase setup needed
- **Authentication:** enable Google and Anonymous sign-in.
- **Firestore:** create the database and publish rules that require sign-in for `reviews` and `users`.
- **Storage:** set it up (Blaze plan required) and publish rules that require sign-in for `profile_photos`.
- **Cloud Messaging:** send test pushes to the topic `homehub_updates`.

## 7. Testing

### Automated tests
Unit tests cover email validation, the password rules and the request status flow.
Run them with `./gradlew testDebugUnitTest`. GitHub Actions runs them on every push.

### Manual test: offline mode with sync (physical phone)
1. With internet **on**, open *Services* and wait 3 seconds (the list is cached).
2. Go back to Home, then turn **flight mode on and Wi-Fi/mobile data off**.
3. Open *Services*. A message says "You are offline. Showing saved services."
4. Choose a service, fill in all fields with a future date and tap *Submit Request*.
5. *My Requests* shows the ** Waiting to sync** card.
6. Turn Wi-Fi back on. After a few seconds the card disappears and a **Requests synced** notification appears.

### Troubleshooting
| Problem | Cause and fix                                                                    |
|---|----------------------------------------------------------------------------------|
| "Services are still loading" offline | The cache is empty. Open *Services* once while online first                      |
| No "Waiting to sync" card | The phone was still online. Turn Wi-Fi and mobile data off, not only flight mode |
| Review or photo fails with `PERMISSION_DENIED` | Firestore/Storage rules are not published, or Anonymous sign-in is off           |
| Photo upload fails with "Object does not exist" | Storage is not set up (needs the Blaze plan not a Standard one)                  |
| Build error "google-services.json is missing" | Put the file in `app/`, not in the project root                                  |

## 8. Publishing preparation
The signed release build, app icon, phone screenshots and Play Console upload are shown in `/publishing`.

## 9. AI tools used
- **Planning:** Claude helped us break the final PoE into two parts (cloud and API work, and app experience and quality work) and suggested a Git branching order to avoid merge conflicts.
- **Deployment help:** Claude explained how to set the Azure API address in `ApiClient.kt`, how to keep the SQLite file from being wiped on deployment, and how to enable Swagger for our demonstration.
  **Debugging:** We described our errors to Claude and it helped us find the causes.

## 10. References
Firebase (2026a) *Firebase Authentication*. Google. Available at: https://firebase.google.com/docs/auth (Accessed: 2 November 2026).

Firebase (2026b) *Cloud Firestore*. Google. Available at: https://firebase.google.com/docs/firestore (Accessed: 2 November 2026).

Firebase (2026c) *Cloud Storage for Firebase*. Google. Available at: https://firebase.google.com/docs/storage (Accessed: 4 November 2026).

Firebase (2026d) *Firebase Cloud Messaging*. Google. Available at: https://firebase.google.com/docs/cloud-messaging (Accessed: 3 November 2026).

Microsoft (2026a) *Azure App Service overview*. Microsoft Learn. Available at: https://learn.microsoft.com/en-us/azure/app-service/overview (Accessed: 3 November 2026).

Microsoft (2026b) *Deploy an ASP.NET Core web app to Azure App Service*. Microsoft Learn. Available at: https://learn.microsoft.com/en-us/azure/app-service/quickstart-dotnetcore (Accessed: 4 November 2026).

---

# Release notes

## Version 2.0 (Final PoE)
Updates since the Part 2 prototype:

###  New innovative features
- **Single sign-on:** sign in with Google.
- **Offline mode with sync:** book a service with no internet. The request is stored on the phone, shown as "Waiting to sync", and sent automatically when you are back online.
- **Push notifications:** real-time alerts from Firebase Cloud Messaging, plus a notification when offline requests sync.
- **Multi-language:** English, isiZulu and isiXhosa.
- **Service reviews:** stored in and read from Cloud Firestore (NoSQL).
- **Profile pictures:** uploaded to Firebase Storage (blob storage).

### Improvements
- The API is hosted online so the app runs on real phones.
- The services list is cached and loaded immediately, so booking works offline.
- Short network timeouts make offline detection fast.
- Waiting requests now have their own container and are shown before the server call, so they never disappear.
- Profile photos are resized and compressed before upload.
- Firestore and Storage calls wait for a Firebase session, and error pop-ups show the real cause.
- The notification toggle now turns push notifications on or off.
- The payment method is sent in a language-independent way, so it works in every language.
- Added automated tests and a GitHub Actions build pipeline.

### Fixes
- Failed network calls no longer lose the user's booking.
- The "Waiting to sync" card is no longer wiped when the request list loads.

## Version 1.0 (Part 2 prototype)
Register/login, service booking, request tracking, payments (cash/card), profile, settings and a REST API with SQLite.
