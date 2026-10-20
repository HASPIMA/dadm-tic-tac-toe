package com.edu.unal.tictactoe.online

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edu.unal.tictactoe.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    onBack: () -> Unit,
    onOpenGame: (String) -> Unit,
    viewModel: LobbyViewModel = viewModel(),
) {
    val uid by viewModel.uid.collectAsStateWithLifecycle()
    val openGames by viewModel.openGames.collectAsStateWithLifecycle()
    val myGames by viewModel.myGames.collectAsStateWithLifecycle()
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
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Button(
                    onClick = { viewModel.createGame(onOpenGame) },
                    enabled = !viewModel.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(stringResource(R.string.create_game))
                }
            }

            section(
                titleRes = R.string.my_games,
                emptyRes = R.string.no_my_games,
                games = myGames
            ) { game ->
                val me = uid.orEmpty()
                ListItem(
                    headlineContent = {
                        Text(
                            game.opponentNameOf(me)?.let { stringResource(R.string.vs_player, it) }
                                ?: stringResource(R.string.waiting_for_opponent)
                        )
                    },
                    supportingContent = {
                        if (!game.isWaitingForOpponent) {
                            Text(
                                if (game.isTurnOf(me)) stringResource(R.string.your_turn)
                                else stringResource(
                                    R.string.their_turn,
                                    game.opponentNameOf(me).orEmpty()
                                )
                            )
                        }
                    },
                    trailingContent = {
                        game.markOf(me)?.let { Text(stringResource(R.string.you_are, it)) }
                    },
                    modifier = Modifier.clickable { onOpenGame(game.id) }
                )
            }

            section(
                titleRes = R.string.open_games,
                emptyRes = R.string.no_open_games,
                games = openGames
            ) { game ->
                ListItem(
                    headlineContent = { Text(game.xName) },
                    trailingContent = {
                        Button(
                            onClick = { viewModel.joinGame(game.id, onOpenGame) },
                            enabled = !viewModel.busy
                        ) {
                            Text(stringResource(R.string.join))
                        }
                    }
                )
            }
        }
    }
}

/** A titled list of games; shows a spinner while [games] is null (loading). */
private fun LazyListScope.section(
    titleRes: Int,
    emptyRes: Int,
    games: List<GameSession>?,
    row: @Composable (GameSession) -> Unit,
) {
    item {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
        )
        HorizontalDivider()
    }
    when {
        games == null -> item {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }

        games.isEmpty() -> item {
            Text(
                text = stringResource(emptyRes),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        else -> items(games, key = { "$titleRes-${it.id}" }) { row(it) }
    }
}
