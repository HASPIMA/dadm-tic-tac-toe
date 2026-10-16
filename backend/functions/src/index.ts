/**
 * Callable functions backing online tic-tac-toe.
 *
 * Clients can only read Firestore (see firestore.rules); every write to
 * `/game_sessions` and `/users` goes through these functions.
 */

import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions";
import { HttpsError, onCall } from "firebase-functions/https";
import { applyMove, EMPTY_BOARD, Mark, parseNickname } from "./game.js";
import {
  displayName,
  GAMES,
  GameSession,
  requireGameId,
  requireUid,
  USERS,
} from "./utils.js";

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

export const createGame = onCall(async (request) => {
  const uid = requireUid(request);
  const name = await displayName(uid);

  return db.runTransaction(async (tx) => {
    const open = await tx.get(
      db
        .collection(GAMES)
        .where("X", "==", uid)
        .where("O", "==", null)
        .where("active", "==", true)
        .limit(1),
    );
    if (!open.empty) return { gameId: open.docs[0].id };

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

export const joinGame = onCall(async (request) => {
  const uid = requireUid(request);
  const gameId = requireGameId(request.data);
  const name = await displayName(uid);
  const ref = db.collection(GAMES).doc(gameId);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) throw new HttpsError("not-found", "Game not found.");
    const game = snapshot.data() as GameSession;
    if (game.X === uid) {
      throw new HttpsError(
        "failed-precondition",
        "You cannot join your own game.",
      );
    }
    if (!game.active || game.O !== null) {
      throw new HttpsError("failed-precondition", "Game is no longer open.");
    }
    tx.update(ref, {
      O: uid,
      OName: name,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
  return { gameId };
});

export const makeMove = onCall(async (request) => {
  const uid = requireUid(request);
  const gameId = requireGameId(request.data);
  const cell = (request.data as { cell?: unknown }).cell;
  if (typeof cell !== "number") {
    throw new HttpsError("invalid-argument", "cell must be a number.");
  }
  const ref = db.collection(GAMES).doc(gameId);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) throw new HttpsError("not-found", "Game not found.");
    const game = snapshot.data() as GameSession;
    if (game.X !== uid && game.O !== uid) {
      throw new HttpsError("permission-denied", "You are not in this game.");
    }
    if (!game.active) {
      throw new HttpsError("failed-precondition", "Game is over.");
    }
    if (game.O === null) {
      throw new HttpsError("failed-precondition", "Waiting for an opponent.");
    }

    const mark: Mark = game.X === uid ? "X" : "O";
    const result = applyMove(game.board, mark, cell);
    if ("error" in result) {
      throw new HttpsError(
        result.error === "invalid-cell"
          ? "invalid-argument"
          : "failed-precondition",
        result.error,
      );
    }
    tx.update(ref, {
      board: result.board,
      winner: result.winner,
      active: result.winner === null,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
  return { gameId };
});

export const leaveGame = onCall(async (request) => {
  const uid = requireUid(request);
  const gameId = requireGameId(request.data);
  const ref = db.collection(GAMES).doc(gameId);

  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) throw new HttpsError("not-found", "Game not found.");
    const game = snapshot.data() as GameSession;
    if (game.X !== uid && game.O !== uid) {
      throw new HttpsError("permission-denied", "You are not in this game.");
    }
    if (!game.active) {
      throw new HttpsError("failed-precondition", "Game is over.");
    }
    // Leaving an open game cancels it (no winner); leaving an ongoing game
    // forfeits it in favour of the opponent.
    const winner: Mark | null =
      game.O === null ? null : game.X === uid ? "O" : "X";
    tx.update(ref, {
      active: false,
      winner,
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
  return { gameId };
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
