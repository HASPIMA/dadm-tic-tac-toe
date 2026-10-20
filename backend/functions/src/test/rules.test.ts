/**
 * Security rules tests. Run against the Firestore emulator with
 * `npm run test:rules`.
 */

import { readFileSync } from "fs";
import { resolve } from "path";
import {
  RulesTestEnvironment,
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { deleteDoc, doc, getDoc, setDoc, updateDoc } from "firebase/firestore";

const GAME = "game_sessions/game1";
const USER = "users/alice";

describe("firestore.rules", () => {
  let env: RulesTestEnvironment;

  before(async () => {
    env = await initializeTestEnvironment({
      projectId: "demo-tictactoe",
      firestore: {
        rules: readFileSync(
          resolve(__dirname, "../../../firestore.rules"),
          "utf8",
        ),
      },
    });
  });

  beforeEach(async () => {
    await env.clearFirestore();
    await env.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, GAME), {
        X: "alice",
        O: null,
        XName: "Alice",
        OName: null,
        board: "---------",
        active: true,
        winner: null,
      });
      await setDoc(doc(db, USER), { nickname: "Alice" });
    });
  });

  after(async () => {
    await env.cleanup();
  });

  it("lets signed-in users read games and profiles", async () => {
    const db = env.authenticatedContext("bob").firestore();
    await assertSucceeds(getDoc(doc(db, GAME)));
    await assertSucceeds(getDoc(doc(db, USER)));
  });

  it("denies reads to signed-out users", async () => {
    const db = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, GAME)));
    await assertFails(getDoc(doc(db, USER)));
  });

  it("denies every client write to games, even to players", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(db, "game_sessions/game2"), { X: "alice" }));
    await assertFails(updateDoc(doc(db, GAME), { board: "X--------" }));
    await assertFails(deleteDoc(doc(db, GAME)));
  });

  it("denies every client write to profiles, even to the owner", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(setDoc(doc(db, USER), { nickname: "Mallory" }));
    await assertFails(deleteDoc(doc(db, USER)));
  });

  it("denies access to other collections", async () => {
    const db = env.authenticatedContext("alice").firestore();
    await assertFails(getDoc(doc(db, "other/doc")));
    await assertFails(setDoc(doc(db, "other/doc"), { a: 1 }));
  });
});
