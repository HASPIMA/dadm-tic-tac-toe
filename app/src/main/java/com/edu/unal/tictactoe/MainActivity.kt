package com.edu.unal.tictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private lateinit var mGame: TicTacToeGame

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        mGame = TicTacToeGame()

        setContent {
            TicTacToeTheme {
                var showMenu by remember { mutableStateOf(false) }
                var resetKey by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                Box {
                                    TextButton(onClick = { showMenu = true }) {
                                        Text("Menu")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.new_game)) },
                                            onClick = {
                                                showMenu = false
                                                resetKey++
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }) { innerPadding ->
                    TicTacToeBoard(
                        game = mGame,
                        modifier = Modifier.padding(innerPadding),
                        resetKey = resetKey
                    )
                }
            }
        }
    }
}

@Composable
fun TicTacToeBoard(game: TicTacToeGame, modifier: Modifier, resetKey: Int) {

    var board by remember { mutableStateOf(List(9) { TicTacToeGame.OPEN_SPOT }) }
    var gameStatusResId by remember { mutableIntStateOf(R.string.human_turn) }
    var gameOver by remember { mutableStateOf(false) }

    // Who's to start?
    var humanFirst by remember { mutableStateOf(true) }

    // Scoreboard
    var humanWins by remember { mutableIntStateOf(0) }
    var computerWins by remember { mutableIntStateOf(0) }
    var ties by remember { mutableIntStateOf(0) }

    // Swap player to be computer since human went first already
    LaunchedEffect(Unit) {
        humanFirst = false
    }

    // Update scoreboard
    fun recordResult(winner: Winner) {
        when (winner) {
            Winner.TIE -> ties++
            Winner.X -> humanWins++
            Winner.O -> computerWins++
            else -> {}
        }
    }

    // Starts a new game match
    fun startNewGame() {
        game.clearBoard()
        board = List(9) { TicTacToeGame.OPEN_SPOT }
        gameOver = false

        if (humanFirst) {
            gameStatusResId = R.string.human_turn
        } else {
            // Computer goes first now
            gameStatusResId = R.string.computer_turn
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            board = board.toMutableList().also { it[move] = TicTacToeGame.COMPUTER_PLAYER }
            gameStatusResId = R.string.human_turn
        }

        // Swap who goes next
        humanFirst = !humanFirst
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

        // [[ Human's Turn ]]
        game.setMove(TicTacToeGame.HUMAN_PLAYER, location)
        board = board.toMutableList().also {
            it[location] = TicTacToeGame.HUMAN_PLAYER
        }

        var winner = game.checkForWinner()

        // --- Computer's turn if game not finished ---
        if (winner == Winner.NOBODY) {
            val move = game.getComputerMove()
            game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
            board = board.toMutableList().also {
                it[move] = TicTacToeGame.COMPUTER_PLAYER
            }
            winner = game.checkForWinner()
        }

        gameStatusResId = when (winner) {
            Winner.NOBODY -> R.string.human_turn
            Winner.TIE -> R.string.result_tie
            Winner.X -> R.string.result_human_wins
            Winner.O -> R.string.result_computer_wins
            else -> gameStatusResId
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
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Text(
            text = value.toString(),
            fontSize = 48.sp
        )
    }
}