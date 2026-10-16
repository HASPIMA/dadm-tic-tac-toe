/**
 * Callable functions backing online tic-tac-toe.
 *
 * Clients can only read Firestore (see firestore.rules); every write to
 * `/game_sessions` and `/users` goes through these functions.
 */

import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/https";
import {
  EMPTY_BOARD,
  Winner,
  guestName,
  parseNickname,
} from "./game.js";

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

initializeApp();
const db = getFirestore();

const GAMES = "game_sessions";
const USERS = "users";

interface GameSession {
  X: string;
  O: string | null;
  XName: string;
  OName: string | null;
  board: string;
  active: boolean;
  winner: Winner | null;
}

/**
 * Returns the caller's uid or rejects unauthenticated calls.
 * @param {CallableRequest} request Incoming callable request.
 * @return {string} The caller's uid.
 */
function requireUid(request: CallableRequest): string {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }
  return request.auth.uid;
}

/**
 * Extracts and validates the `gameId` argument.
 * @param {unknown} data Callable payload.
 * @return {string} The game id.
 */
function requireGameId(data: unknown): string {
  const gameId = (data as { gameId?: unknown } | null)?.gameId;
  if (typeof gameId !== "string" || gameId.length === 0) {
    throw new HttpsError("invalid-argument", "gameId must be a string.");
  }
  return gameId;
}

/**
 * Looks up the name shown to other players.
 * @param {string} uid User id.
 * @return {Promise<string>} The nickname, or a guest name without profile.
 */
async function displayName(uid: string): Promise<string> {
  const profile = await db.collection(USERS).doc(uid).get();
  const nickname = profile.get("nickname");
  return typeof nickname === "string" ? nickname : guestName(uid);
}

export const createGame = onCall(async (request) => {
  const uid = requireUid(request);
  const name = await displayName(uid);

  return db.runTransaction(async (tx) => {
    const ref = db.collection(GAMES).doc();
    const newGameSession: GameSession = {
      X: uid,
      O: null,
      XName: name,
      OName: null,
      board: EMPTY_BOARD,
      active: true,
      winner: null,
    };

    tx.create(ref, {
      ...newGameSession,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return { gameId: ref.id };
  });
});
export const setNickname = onCall(async (request) => {
  const uid = requireUid(request);
  if (request.auth?.token.firebase.sign_in_provider === "anonymous") {
    throw new HttpsError("permission-denied", "Guests cannot set a nickname.");
  }
  const nickname = parseNickname(
    (request.data as { nickname?: unknown } | null)?.nickname,
  );
  if (nickname === null) {
    throw new HttpsError(
      "invalid-argument",
      "nickname must be between 3 and 20 characters.",
    );
  }
  await db.collection(USERS).doc(uid).set({ nickname });
  return { nickname };
});
