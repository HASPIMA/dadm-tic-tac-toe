import { getFirestore } from "firebase-admin/firestore";
import { CallableRequest, HttpsError } from "firebase-functions/https";
import { guestName, Winner } from "./game";

const db = getFirestore();

export interface GameSession {
  X: string;
  O: string | null;
  XName: string;
  OName: string | null;
  board: string;
  active: boolean;
  winner: Winner | null;
}

export const GAMES = "game_sessions";
export const USERS = "users";

/**
 * Returns the caller's uid or rejects unauthenticated calls.
 * @param {CallableRequest} request Incoming callable request.
 * @return {string} The caller's uid.
 */
export function requireUid(request: CallableRequest): string {
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
export function requireGameId(data: unknown): string {
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
export async function displayName(uid: string): Promise<string> {
  const profile = await db.collection(USERS).doc(uid).get();
  const nickname = profile.get("nickname");
  return typeof nickname === "string" ? nickname : guestName(uid);
}
