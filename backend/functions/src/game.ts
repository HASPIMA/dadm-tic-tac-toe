/**
 * Pure tic-tac-toe rules shared by the callable functions.
 *
 * The board is a 9 character string in row-major order where each cell is
 * `X`, `O` or `-` (empty).
 */

export type Mark = "X" | "O";
export type Winner = Mark | "TIE";

export const BOARD_SIZE = 9;
export const EMPTY_CELL = "-";
export const EMPTY_BOARD = EMPTY_CELL.repeat(BOARD_SIZE);

const LINES: ReadonlyArray<readonly [number, number, number]> = [
  // Rows
  [0, 1, 2],
  [3, 4, 5],
  [6, 7, 8],

  // Columns
  [0, 3, 6],
  [1, 4, 7],
  [2, 5, 8],

  // Diagonals
  [0, 4, 8],
  [2, 4, 6],
];

/**
 * Checks whether a string is a well-formed board.
 * @param {string} board Candidate board.
 * @return {boolean} Whether it only contains valid cells and has 9 of them.
 */
export function isValidBoard(board: string): boolean {
  return /^[XO-]{9}$/.test(board);
}

/**
 * Returns whose turn it is. X always moves first.
 * @param {string} board Current board.
 * @return {Mark} The mark that should be placed next.
 */
export function nextTurn(board: string): Mark {
  let x = 0;
  let o = 0;
  for (const cell of board) {
    if (cell === "X") x++;
    else if (cell === "O") o++;
  }
  return x > o ? "O" : "X";
}

/**
 * Computes the outcome of a board.
 * @param {string} board Current board.
 * @return {Winner | null} The winner, `TIE` when full, or null if ongoing.
 */
export function checkWinner(board: string): Winner | null {
  for (const [a, b, c] of LINES) {
    const mark = board[a];
    if (mark !== EMPTY_CELL && mark === board[b] && mark === board[c]) {
      return mark as Mark;
    }
  }
  return board.includes(EMPTY_CELL) ? null : "TIE";
}

/** Reasons a move can be rejected. */
export type MoveError =
  | "invalid-cell"
  | "not-your-turn"
  | "cell-taken"
  | "game-over";

/** Outcome of a move: the new board and its winner, or a rejection. */
export type MoveResult =
  | { board: string; winner: Winner | null }
  | { error: MoveError };

/**
 * Validates and applies a move.
 * @param {string} board Current board.
 * @param {Mark} mark Mark of the player moving.
 * @param {number} cell Index in [0, 8] of the cell to mark.
 * @return {MoveResult} The new board and its outcome, or why the move is
 *   not allowed.
 */
export function applyMove(board: string, mark: Mark, cell: number): MoveResult {
  if (!Number.isInteger(cell) || cell < 0 || cell >= BOARD_SIZE) {
    return { error: "invalid-cell" };
  }
  if (checkWinner(board) !== null) return { error: "game-over" };
  if (nextTurn(board) !== mark) return { error: "not-your-turn" };
  if (board[cell] !== EMPTY_CELL) return { error: "cell-taken" };

  const next = board.slice(0, cell) + mark + board.slice(cell + 1);
  return { board: next, winner: checkWinner(next) };
}

/**
 * Display name used for users without a profile.
 * @param {string} uid User id.
 * @return {string} `Guest-` followed by the first 4 characters of the uid.
 */
export function guestName(uid: string): string {
  return `Guest-${uid.slice(0, 4)}`;
}

/**
 * Normalizes and validates a nickname.
 * @param {unknown} nickname Candidate nickname.
 * @return {string | null} The trimmed nickname, or null when invalid.
 */
export function parseNickname(nickname: unknown): string | null {
  if (typeof nickname !== "string") return null;
  const trimmed = nickname.trim();
  return trimmed.length >= 3 && trimmed.length <= 20 ? trimmed : null;
}
