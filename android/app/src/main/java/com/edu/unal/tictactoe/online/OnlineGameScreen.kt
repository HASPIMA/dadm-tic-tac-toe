package com.edu.unal.tictactoe.online

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.tictactoe.GameBoard
import com.edu.unal.tictactoe.R
import com.edu.unal.tictactoe.TicTacToeGame
import com.edu.unal.tictactoe.Winner
import com.edu.unal.tictactoe.rememberGameSounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineGameScreen(
    gameId: String,
    onBack: () -> Unit,
    viewModel: OnlineGameViewModel = viewModel(key = "game-$gameId") {
        OnlineGameViewModel(gameId)
    },
) {
    val state by viewModel.game.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_online)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val current = state) {
                null -> CircularProgressIndicator()
                GameState.NotFound -> Text(stringResource(R.string.game_not_found))
                is GameState.Loaded -> OnlineGameContent(
                    game = current.game,
                    uid = viewModel.uid.orEmpty(),
                    busy = viewModel.busy,
                    onCellClick = viewModel::move,
                    onLeave = viewModel::leave,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun OnlineGameContent(
    game: GameSession,
    uid: String,
    busy: Boolean,
    onCellClick: (Int) -> Unit,
    onLeave: () -> Unit,
    onBack: () -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val myMark = game.markOf(uid)
    val boardGame = remember { TicTacToeGame() }
    boardGame.setBoard(game.board)

    // Play a sound for every new mark: "human" for mine, "computer" for theirs
    val sounds = rememberGameSounds()
    var previousBoard by remember { mutableStateOf<List<Char>?>(null) }
    LaunchedEffect(game.board) {
        val previous = previousBoard
        if (previous != null) {
            val placed = game.board.indices.firstOrNull {
                previous[it] == TicTacToeGame.OPEN_SPOT && game.board[it] != TicTacToeGame.OPEN_SPOT
            }
            if (placed != null) {
                if (game.board[placed] == myMark) sounds.playHuman() else sounds.playComputer()
            }
        }
        previousBoard = game.board
    }

    val board = @Composable {
        GameBoard(
            size = if (isLandscape) 220.dp else 300.dp,
            game = boardGame,
            board = game.board,
            // Installed once by GameBoard's factory, so it must not capture
            // state; the view model checks turn and busy state itself.
            onCellClick = onCellClick
        )
    }
    val info = @Composable { modifier: Modifier ->
        GameInfo(
            game = game,
            uid = uid,
            boardWinner = boardGame.checkForWinner(),
            busy = busy,
            onLeave = onLeave,
            onBack = onBack,
            modifier = modifier
        )
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            board()
            info(
                Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            board()
            info(Modifier)
        }
    }
}

@Composable
private fun GameInfo(
    game: GameSession,
    uid: String,
    boardWinner: Winner,
    busy: Boolean,
    onLeave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val myMark = game.markOf(uid)?.toString()
    val opponent = game.opponentNameOf(uid).orEmpty()
    var showForfeitDialog by rememberSaveable { mutableStateOf(false) }

    // The winner is decided by the server. The board is only checked to tell a
    // win on the board apart from a forfeit.
    val wonOnBoard = boardWinner == Winner.X || boardWinner == Winner.O
    val status = when {
        game.isWaitingForOpponent -> stringResource(R.string.waiting_for_opponent)
        game.active && game.isTurnOf(uid) -> stringResource(R.string.your_turn)
        game.active -> stringResource(R.string.their_turn, opponent)
        game.winner == null -> stringResource(R.string.result_cancelled)
        game.winner == "TIE" -> stringResource(R.string.result_tie)
        game.winner == myMark && wonOnBoard -> stringResource(R.string.result_you_won)
        game.winner == myMark -> stringResource(R.string.result_opponent_left, opponent)
        wonOnBoard -> stringResource(R.string.result_you_lost)
        else -> stringResource(R.string.result_you_left)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (myMark != null) {
            Text(
                text = stringResource(R.string.you_are, myMark) +
                    (game.opponentNameOf(uid)?.let { " · " + stringResource(R.string.vs_player, it) }
                        ?: ""),
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Text(
            text = status,
            fontSize = 20.sp,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (game.active && myMark != null) {
            OutlinedButton(
                onClick = {
                    if (game.isWaitingForOpponent) onLeave() else showForfeitDialog = true
                },
                enabled = !busy,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    stringResource(
                        if (game.isWaitingForOpponent) R.string.cancel_game
                        else R.string.forfeit_game
                    )
                )
            }
        } else if (!game.active) {
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.back_to_lobby))
            }
        }
    }

    if (showForfeitDialog) {
        AlertDialog(
            onDismissRequest = { showForfeitDialog = false },
            title = { Text(stringResource(R.string.forfeit_confirm_title)) },
            text = { Text(stringResource(R.string.forfeit_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForfeitDialog = false
                        onLeave()
                    }
                ) { Text(stringResource(R.string.forfeit_game)) }
            },
            dismissButton = {
                TextButton(onClick = { showForfeitDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
