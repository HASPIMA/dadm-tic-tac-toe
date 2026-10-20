import { strict as assert } from "assert";
import {
  EMPTY_BOARD,
  applyMove,
  checkWinner,
  guestName,
  isValidBoard,
  nextTurn,
  parseNickname,
} from "../game.js";

describe("game", () => {
  describe("isValidBoard", () => {
    it("accepts boards of 9 valid cells", () => {
      assert.ok(isValidBoard(EMPTY_BOARD));
      assert.ok(isValidBoard("XO-XO-XO-"));
    });

    it("rejects malformed boards", () => {
      assert.ok(!isValidBoard("--------"));
      assert.ok(!isValidBoard("----------"));
      assert.ok(!isValidBoard("XO XO XO "));
    });
  });

  describe("nextTurn", () => {
    it("starts with X", () => assert.equal(nextTurn(EMPTY_BOARD), "X"));
    it("alternates", () => {
      assert.equal(nextTurn("X--------"), "O");
      assert.equal(nextTurn("XO-------"), "X");
    });
  });

  describe("checkWinner", () => {
    it("is null while ongoing", () => {
      assert.equal(checkWinner(EMPTY_BOARD), null);
      assert.equal(checkWinner("XO-------"), null);
    });

    it("detects rows, columns and diagonals", () => {
      assert.equal(checkWinner("XXXOO----"), "X");
      assert.equal(checkWinner("OX-OX-O-X"), "O");
      assert.equal(checkWinner("XO-OX---X"), "X");
      assert.equal(checkWinner("XXO-O-OX-"), "O");
    });

    it("detects ties", () => {
      assert.equal(checkWinner("XOXXOOOXX"), "TIE");
    });

    it("prefers a win on a full board", () => {
      assert.equal(checkWinner("XXXOOXOOX"), "X");
    });
  });

  describe("applyMove", () => {
    it("places the mark and reports the outcome", () => {
      assert.deepEqual(applyMove(EMPTY_BOARD, "X", 4), {
        board: "----X----",
        winner: null,
      });
      assert.deepEqual(applyMove("XX-OO----", "X", 2), {
        board: "XXXOO----",
        winner: "X",
      });
    });

    it("rejects out of range cells", () => {
      assert.deepEqual(applyMove(EMPTY_BOARD, "X", -1), {
        error: "invalid-cell",
      });
      assert.deepEqual(applyMove(EMPTY_BOARD, "X", 9), {
        error: "invalid-cell",
      });
      assert.deepEqual(applyMove(EMPTY_BOARD, "X", 1.5), {
        error: "invalid-cell",
      });
    });

    it("rejects moves out of turn", () => {
      assert.deepEqual(applyMove(EMPTY_BOARD, "O", 0), {
        error: "not-your-turn",
      });
      assert.deepEqual(applyMove("X--------", "X", 1), {
        error: "not-your-turn",
      });
    });

    it("rejects taken cells", () => {
      assert.deepEqual(applyMove("X--------", "O", 0), { error: "cell-taken" });
    });

    it("rejects moves on finished boards", () => {
      assert.deepEqual(applyMove("XXXOO----", "O", 5), { error: "game-over" });
    });
  });

  describe("names", () => {
    it("builds guest names from the uid", () => {
      assert.equal(guestName("1a2b3c4d"), "Guest-1a2b");
    });

    it("validates nicknames", () => {
      assert.equal(parseNickname("  Ana  "), "Ana");
      assert.equal(parseNickname("ab"), null);
      assert.equal(parseNickname("x".repeat(21)), null);
      assert.equal(parseNickname(42), null);
    });
  });
});
