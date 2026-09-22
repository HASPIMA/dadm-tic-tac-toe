package com.edu.unal.tictactoe

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TicTacToeTheme {
                TicTacToeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeApp() {
    val game = remember { TicTacToeGame() }
    var resetKey by remember { mutableIntStateOf(0) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var currentDifficulty by remember { mutableStateOf(game.computerDifficultyLevel) }
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { menuExpanded = true }) {
                        Text(stringResource(R.string.menu))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.new_game)) },
                            onClick = {
                                menuExpanded = false
                                resetKey++
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.ai_difficulty)) },
                            onClick = {
                                menuExpanded = false
                                showDifficultyDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.quit_game)) },
                            onClick = {
                                menuExpanded = false
                                showQuitDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        TicTacToeBoard(
            game = game,
            modifier = Modifier.padding(innerPadding),
            resetKey = resetKey
        )

        if (showDifficultyDialog) {
            AlertDialog(
                onDismissRequest = { showDifficultyDialog = false },
                title = { Text(stringResource(R.string.difficulty_title)) },
                text = {
                    Column {
                        DifficultyLevel.entries.forEach { level ->
                            val label = when (level) {
                                DifficultyLevel.Easy -> stringResource(R.string.difficulty_easy)
                                DifficultyLevel.Harder -> stringResource(R.string.difficulty_harder)
                                DifficultyLevel.Expert -> stringResource(R.string.difficulty_expert)
                            }
                            val toastMessage = stringResource(R.string.difficulty_changed, label)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = (level == currentDifficulty),
                                        onClick = {
                                            game.computerDifficultyLevel = level
                                            currentDifficulty = level
                                            showDifficultyDialog = false
                                            Toast.makeText(
                                                context,
                                                toastMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (level == currentDifficulty),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDifficultyDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (showQuitDialog) {
            AlertDialog(
                onDismissRequest = { showQuitDialog = false },
                title = { Text(stringResource(R.string.quit_confirm_title)) },
                text = { Text(stringResource(R.string.quit_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showQuitDialog = false
                            (context as? Activity)?.finish()
                        }
                    ) {
                        Text(stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuitDialog = false }) {
                        Text(stringResource(R.string.no))
                    }
                }
            )
        }
    }
}

@Composable
fun TicTacToeBoard(
    game: TicTacToeGame,
    modifier: Modifier = Modifier,
    resetKey: Int = 0
) {
    var board by remember { mutableStateOf(List(9) { TicTacToeGame.OPEN_SPOT }) }
    var gameStatusResId by remember { mutableIntStateOf(R.string.human_turn) }
    var gameOver by remember { mutableStateOf(false) }

    // Tracks who starts next (alternates after each match)
    var humanStartsNext by remember { mutableStateOf(false) }

    // Scoreboard
    var humanWins by remember { mutableIntStateOf(0) }
    var computerWins by remember { mutableIntStateOf(0) }
    var ties by remember { mutableIntStateOf(0) }

    fun recordResult(winner: Winner) {
        when (winner) {
            Winner.TIE -> ties++
            Winner.X -> humanWins++
            Winner.O -> computerWins++
            else -> {}
        }
    }

    fun startNewGame() {
        game.clearBoard()
        board = List(9) { TicTacToeGame.OPEN_SPOT }
        gameOver = false

        if (!humanStartsNext) {
            // Computer starts this match
            gameStatusResId = R.string.computer_turn
            val move = game.getComputerMove()
            if (move in 0..<TicTacToeGame.BOARD_SIZE) {
                game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                board = board.toMutableList().also { it[move] = TicTacToeGame.COMPUTER_PLAYER }
            }
            gameStatusResId = R.string.human_turn
            humanStartsNext = true
        } else {
            // Human starts this match
            gameStatusResId = R.string.human_turn
            humanStartsNext = false
        }
    }

    LaunchedEffect(resetKey) {
        if (resetKey > 0) {
            startNewGame()
        }
    }

    fun onCellClick(location: Int) {
        if (board[location] != TicTacToeGame.OPEN_SPOT || gameOver) {
            return
        }

        // Human's Turn
        game.setMove(TicTacToeGame.HUMAN_PLAYER, location)
        board = board.toMutableList().also {
            it[location] = TicTacToeGame.HUMAN_PLAYER
        }

        var winner = game.checkForWinner()

        // Computer's turn if game not finished
        if (winner == Winner.NOBODY) {
            val move = game.getComputerMove()
            if (move in 0..<TicTacToeGame.BOARD_SIZE) {
                game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                board = board.toMutableList().also {
                    it[move] = TicTacToeGame.COMPUTER_PLAYER
                }
                winner = game.checkForWinner()
            }
        }

        gameStatusResId = when (winner) {
            Winner.NOBODY -> R.string.human_turn
            Winner.TIE -> R.string.result_tie
            Winner.X -> R.string.result_human_wins
            Winner.O -> R.string.result_computer_wins
        }

        if (winner != Winner.NOBODY) {
            gameOver = true
            recordResult(winner)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Row {
            GameButton(value = board[0], onClick = { onCellClick(0) })
            GameButton(value = board[1], onClick = { onCellClick(1) })
            GameButton(value = board[2], onClick = { onCellClick(2) })
        }

        Row {
            GameButton(value = board[3], onClick = { onCellClick(3) })
            GameButton(value = board[4], onClick = { onCellClick(4) })
            GameButton(value = board[5], onClick = { onCellClick(5) })
        }

        Row {
            GameButton(value = board[6], onClick = { onCellClick(6) })
            GameButton(value = board[7], onClick = { onCellClick(7) })
            GameButton(value = board[8], onClick = { onCellClick(8) })
        }

        Text(
            text = stringResource(gameStatusResId),
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 20.dp)
        )

        // Display wins
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(stringResource(R.string.number_computer_wins, computerWins))
            Text(stringResource(R.string.number_ties, ties))
            Text(stringResource(R.string.number_wins_human, humanWins))
        }

        Button(
            onClick = { startNewGame() },
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text(stringResource(R.string.new_game_button))
        }
    }
}

@Composable
fun GameButton(
    value: Char,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(100.dp)
            .padding(2.dp),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(
            text = value.toString(),
            fontSize = 48.sp
        )
    }
}
