# TicTacToe

## Table of contents

- [TicTacToe](#tictactoe)
  - [Table of contents](#table-of-contents)
  - [Backend](#backend)
    - [Prerequisites](#prerequisites)
    - [Testing locally](#testing-locally)
      - [Unit tests](#unit-tests)
      - [Security rules tests](#security-rules-tests)
      - [Running the emulators](#running-the-emulators)
      - [Running the Android app against the local backend](#running-the-android-app-against-the-local-backend)
    - [Deploying](#deploying)

## Backend

The Firebase backend for online play lives in [`backend/`](backend/):

| File | Purpose |
| --- | --- |
| [`firestore.rules`](backend/firestore.rules) | Signed-in users can read `/game_sessions` and `/users`. No client can write. |
| [`firestore.indexes.json`](backend/firestore.indexes.json) | Composite indexes for the lobby and "My games" queries. |
| [`functions/src/index.ts`](backend/functions/src/index.ts) | Callable functions that do every write: `createGame`, `joinGame`, `makeMove`, `leaveGame` and `setNickname`. |
| [`functions/src/game.ts`](backend/functions/src/game.ts) | Pure game rules: turns, move validation and winner detection. |
| [`firebase.json`](backend/firebase.json) | Deploy targets and emulator ports. |

### Prerequisites

- **Node.js 22**, the version in [`functions/.nvmrc`](backend/functions/.nvmrc). With nvm, run `nvm use` from `backend/functions`.
- **Firebase CLI**: `npm install -g firebase-tools`, then `firebase login`.
  - Avoid the standalone binary from `firebase.tools`. Its wrapper shell breaks `npm run` in the predeploy hooks with `/bin/sh: --: invalid option`. If you have it installed, run `npx firebase-tools <command>` instead, or remove the binary and install the npm package.
- **Java 11 or newer**, required by the Firestore emulator.

Install the functions dependencies once:

```sh
cd backend/functions
npm ci
```

Run every command below from `backend/functions` unless noted otherwise.

### Testing locally

#### Unit tests

These test the game rules in `game.ts` and need no emulator:

```sh
npm test
```

#### Security rules tests

These start the Firestore emulator, load `firestore.rules`, and check the following:

- Signed-out users can't read anything.
- Nobody can write directly, not even the players of a game.
- Every other collection is closed.

```sh
npm run test:rules
```

The emulator shuts down when the tests finish.

#### Running the emulators

This command compiles the functions and starts the Auth, Firestore and Functions emulators:

```sh
npm run serve
```

| Emulator | Address |
| --- | --- |
| Emulator UI | <http://127.0.0.1:4000> |
| Auth | `127.0.0.1:9099` |
| Firestore | `127.0.0.1:8080` |
| Functions | `127.0.0.1:5001` |

The Emulator UI lets you browse the `game_sessions` and `users` collections and inspect the function logs.

The emulators don't reload compiled code by themselves. After editing the functions, run `npm run build:watch` in another terminal so the Functions emulator picks up each change.

Emulator data is discarded when they stop.

> The emulators don't enforce composite indexes. A query that works locally can still fail in production if its index is missing from `firestore.indexes.json`.

#### Running the Android app against the local backend

Debug builds can talk to the emulators instead of the production project. This is opt-in: unless you set `firebase.emulatorHost`, every build uses production. Release builds always use production.

1. **Start the backend** from `backend/functions`, and leave it running:

   ```sh
   npm run serve
   ```

   If you're also editing the functions, run `npm run build:watch` in a second terminal.

2. **Point debug builds at the emulators.** Add this line to `android/local.properties`, which is git-ignored:

   ```properties
   firebase.emulatorHost=10.0.2.2
   ```

   `10.0.2.2` is how an Android emulator reaches your computer. Instead of editing the file, you can pass the setting for a single build:

   ```sh
   cd android
   ./gradlew installDebug -Pfirebase.emulatorHost=10.0.2.2
   ```

   In Android Studio, sync Gradle after editing `local.properties`, then run the app as usual.

   With the setting present, the debug build does two things:
   - [`FirebaseEmulators`](android/app/src/main/java/com/edu/unal/tictactoe/online/FirebaseEmulators.kt) connects Auth, Firestore and Functions to that host, on the ports from `firebase.json`.
   - The app is allowed to use plain HTTP, because the emulators don't use HTTPS.

3. **Clear the app's data** whenever you switch between production and the emulators, and after restarting the emulators:

   ```sh
   adb shell pm clear com.edu.unal.tictactoe
   ```

   The app remembers its signed-in user and caches Firestore data on the device. A user from production, or from an earlier emulator run, doesn't exist in the current emulator.

4. **Play against yourself.** An online game needs two users, so run the app on two devices at once, for example two Android emulators or an emulator and a phone. Each install signs in as a different guest. Open the Emulator UI at <http://127.0.0.1:4000> to watch what happens:
   - **Authentication** tab: the anonymous and email users.
   - **Firestore** tab: the `game_sessions` and `users` documents.
   - **Logs** tab: every function call and error.

**On a physical device over USB**, forward the emulator ports to the phone and use `127.0.0.1` as the host:

```sh
adb reverse tcp:9099 tcp:9099   # Auth
adb reverse tcp:8080 tcp:8080   # Firestore
adb reverse tcp:5001 tcp:5001   # Functions
```

```properties
firebase.emulatorHost=127.0.0.1
```

The forwarding is lost when the phone disconnects, so run these commands again after reconnecting. This setup also works on Android emulators.

**To go back to production**, remove `firebase.emulatorHost` from `local.properties`, rebuild, and clear the app's data again.

### Deploying

The project is set in [`.firebaserc`](backend/.firebaserc) (`dadm-tictactoe-9debc`). Cloud Functions require the project to be on the **Blaze** (pay as you go) plan.

From `backend/`, deploy the rules, indexes and functions together:

```sh
firebase deploy --only firestore:rules,firestore:indexes,functions
```

- Before uploading, the deploy runs `npm run lint` and `npm run build`, so it fails on lint or type errors.
- Firestore builds new indexes in the background, which can take a few minutes. Until they're ready, the app's lobby and "My games" lists show an error saying the query requires an index. Check progress in the Firebase console under *Firestore → Indexes*.
- Functions deploy to the default region, `us-central1`, which is also what the Android SDK calls by default.

To deploy one part at a time:

```sh
firebase deploy --only firestore:rules
firebase deploy --only firestore:indexes
firebase deploy --only functions                 # or: npm run deploy, from backend/functions
firebase deploy --only functions:makeMove        # a single function
```

To read the production function logs:

```sh
npm run logs
```
