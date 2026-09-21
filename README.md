# HomeHub

HomeHub is an Android app for requesting and tracking home services. A user can register, log in, choose a service, book a job, track the request, update profile information, change app settings, and choose Cash or Card after a job is completed.

## Main Features

- Register and log in with a secure password.
- Passwords are hashed by the ASP.NET Core API before they are stored.
- View services such as Plumbing, Electrical, Cleaning, Painting and Handyman.
- Book a service with a problem description, address and preferred date.
- View requests with Pending, In Progress and Completed statuses.
- Choose Cash or Card after a request is completed.
- View and update profile information.
- Change HomeHub settings such as notifications and the default payment method.
- Log out safely.

## Technology Used

- **Android:** Kotlin and XML
- **API calls:** Retrofit
- **Backend:** ASP.NET Core Web API (.NET 8)
- **Database:** SQLite with Entity Framework Core
- **Testing:** JUnit
- **Automation:** GitHub Actions

## How the App Connects

```text
Android App
    ↓ Retrofit
ASP.NET Core Web API
    ↓ Entity Framework Core
SQLite Database
```

## Run the Project

### 1. Run the API

1. Open the `HomeHub` API solution in Visual Studio.
2. Select the **http** profile.
3. Run the project.
4. Swagger should open and the API should listen on port **5092**.
5. Keep Visual Studio running while testing the Android app.

The API creates `homehub.db` automatically the first time it runs and adds the available HomeHub services.

### 2. Check the Android API Address

Open:

`app/src/main/java/com/example/homehubapp/network/ApiClient.kt`

The current development address is:

```kotlin
private const val BASE_URL = "http://10.0.0.4:5092/"
```

This was set for BlueStacks on the development PC/VM. If your computer's IPv4 address changes, run `ipconfig` in Command Prompt and replace `10.0.0.4` with the new IPv4 address.

### 3. Run Android

1. Open `HomeHubApp` in Android Studio.
2. Sync Gradle.
3. Start BlueStacks or an Android emulator.
4. Run the app.
5. Register a new account, then log in with the same email and password.

## Password Rules

A password must:

- Have at least 6 characters.
- Have an uppercase letter.
- Have a lowercase letter.
- Have a number.
- Have a symbol.

Example: `HomeHub@123`

## API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/Auth/register` | Register a user |
| POST | `/api/Auth/login` | Log in |
| GET | `/api/Services` | Get HomeHub services |
| POST | `/api/Requests` | Create a service request |
| GET | `/api/Requests/user/{userId}` | Get a user's requests |
| PUT | `/api/Requests/{id}/status` | Update request status for prototype demonstration |
| POST | `/api/Payments` | Save Cash/Card payment choice |
| GET | `/api/Users/{id}` | Get profile information |
| PUT | `/api/Users/{id}` | Update profile information |

## Settings

The Settings screen lets the user:

- Turn service notifications on or off.
- Choose a default payment method (Cash or Card).

The selected payment method is automatically selected on the Payment screen.

## Testing

Unit tests are in:

`app/src/test/java/com/example/homehubapp/ValidationUtilsTest.kt`

They test email and password validation. GitHub Actions runs the tests and builds the Android app after code is pushed.

To run tests in Android Studio, right-click `ValidationUtilsTest` and select **Run**.

## GitHub

For submission, push the Android project source code to GitHub. Do not submit the Android project as a zip if your assessment says GitHub is required.

Suggested commands:

```bash
git init
git add .
git commit -m "Initial HomeHub Part 2 prototype"
git branch -M main
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```

Make several meaningful commits as you continue testing and improving the app.

## Demonstration Video

Add your unlisted video link here before submission:

`VIDEO LINK: ADD_YOUR_VIDEO_LINK_HERE`

In the video, show:

1. Registration.
2. Login.
3. Settings.
4. Services loaded from the API.
5. Booking a service.
6. My Requests and request status.
7. Cash/Card payment.
8. Profile update and logout.
9. Swagger/API and the SQLite data being stored.

Include a voice-over explaining what you are demonstrating.

## AI Use

AI tools were used to help explain errors, debug code, and improve documentation wording. The project code and functionality should still be reviewed, tested and explained by the student before submission.
