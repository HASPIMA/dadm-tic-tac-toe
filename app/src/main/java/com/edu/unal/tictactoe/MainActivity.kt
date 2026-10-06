package com.edu.unal.tictactoe

import android.app.Activity
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.PopupMenu
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.get
import com.edu.unal.tictactoe.ui.theme.TicTacToeTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val CharListSaver: Saver<MutableState<List<Char>>, CharArray> = Saver(
    save = { state -> state.value.toCharArray() },
    restore = { mutableStateOf(it.toList()) }
)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeApp() {
    val game = remember { TicTacToeGame() }
    var resetKey by rememberSaveable { mutableIntStateOf(0) }
    var showDifficultyDialog by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var currentDifficultyName by rememberSaveable { mutableStateOf(game.computerDifficultyLevel.name) }
    val currentDifficulty = DifficultyLevel.valueOf(currentDifficultyName)
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    val menuItems = remember(context) {
        val popup = PopupMenu(context, View(context))
        popup.menuInflater.inflate(R.menu.options_menu, popup.menu)
        val menu = popup.menu
        List(menu.size()) { index -> menu[index] }
    }

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
                        menuItems.forEach { menuItem ->
                            DropdownMenuItem(
                                text = { Text(menuItem.title.toString()) },
                                onClick = {
                                    menuExpanded = false
                                    when (menuItem.itemId) {
                                        R.id.new_game -> resetKey++
                                        R.id.ai_difficulty -> showDifficultyDialog = true
                                        R.id.quit_game -> showQuitDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        TicTacToeBoard(
            game = game,
            modifier = Modifier.padding(innerPadding),
            resetKey = resetKey,
            currentDifficulty = currentDifficulty
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
                                            currentDifficultyName = level.name
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
    resetKey: Int = 0,
    currentDifficulty: DifficultyLevel = DifficultyLevel.Expert
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    var humanSoundId by remember { mutableIntStateOf(0) }
    var computerSoundId by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        humanSoundId = soundPool.load(context, R.raw.human_sound, 1)
        computerSoundId = soundPool.load(context, R.raw.computer_sound, 1)

        onDispose {
            soundPool.release()
        }
    }

    fun playHumanSound() {
        val streamId = if (humanSoundId != 0) soundPool.play(humanSoundId, 1f, 1f, 1, 0, 1f) else 0
        if (streamId == 0) {
            MediaPlayer.create(context, R.raw.human_sound)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        }
    }

    fun playComputerSound() {
        val streamId = if (computerSoundId != 0) soundPool.play(computerSoundId, 1f, 1f, 1, 0, 1f) else 0
        if (streamId == 0) {
            MediaPlayer.create(context, R.raw.computer_sound)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        }
    }

    var board by rememberSaveable(saver = CharListSaver) {
        mutableStateOf(List(9) { TicTacToeGame.OPEN_SPOT })
    }
    var gameStatusResId by rememberSaveable { mutableIntStateOf(R.string.human_turn) }
    var gameOver by rememberSaveable { mutableStateOf(false) }
    var isComputerThinking by rememberSaveable { mutableStateOf(false) }

    // Tracks who starts next (alternates after each match)
    var humanStartsNext by rememberSaveable { mutableStateOf(false) }

    // Scoreboard
    var humanWins by rememberSaveable { mutableIntStateOf(0) }
    var computerWins by rememberSaveable { mutableIntStateOf(0) }
    var ties by rememberSaveable { mutableIntStateOf(0) }

    // Keep game internal state in sync with saved Compose state
    game.computerDifficultyLevel = currentDifficulty
    game.setBoard(board)

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
            humanStartsNext = true
            isComputerThinking = true
        } else {
            // Human starts this match
            gameStatusResId = R.string.human_turn
            humanStartsNext = false
            isComputerThinking = false
        }
    }

    LaunchedEffect(resetKey) {
        if (resetKey > 0) {
            startNewGame()
        }
    }

    LaunchedEffect(isComputerThinking, gameOver, resetKey) {
        if (isComputerThinking && !gameOver) {
            delay(1000L.milliseconds)
            val move = game.getComputerMove()
            if (move in 0..<TicTacToeGame.BOARD_SIZE) {
                game.setMove(TicTacToeGame.COMPUTER_PLAYER, move)
                board = board.toMutableList().also {
                    it[move] = TicTacToeGame.COMPUTER_PLAYER
                }
                playComputerSound()
            }

            val winner = game.checkForWinner()
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

            isComputerThinking = false
        }
    }

    fun onCellClick(location: Int) {
        if (board[location] != TicTacToeGame.OPEN_SPOT || gameOver || isComputerThinking) {
            return
        }

        // Human's Turn
        game.setMove(TicTacToeGame.HUMAN_PLAYER, location)
        board = board.toMutableList().also {
            it[location] = TicTacToeGame.HUMAN_PLAYER
        }
        playHumanSound()

        val winner = game.checkForWinner()

        if (winner != Winner.NOBODY) {
            gameStatusResId = when (winner) {
                Winner.TIE -> R.string.result_tie
                Winner.X -> R.string.result_human_wins
                Winner.O -> R.string.result_computer_wins
                else -> R.string.human_turn
            }
            gameOver = true
            recordResult(winner)
        } else {
            // Computer's turn
            gameStatusResId = R.string.computer_turn
            isComputerThinking = true
        }
    }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                factory = { ctx ->
                    BoardView(ctx).apply {
                        setGame(game)
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                view.performClick()
                                val cellWidth = boardCellWidth
                                val cellHeight = boardCellHeight
                                if (cellWidth > 0 && cellHeight > 0) {
                                    val col = (event.x / cellWidth).toInt().coerceIn(0, 2)
                                    val row = (event.y / cellHeight).toInt().coerceIn(0, 2)
                                    val location = row * 3 + col
                                    onCellClick(location)
                                }
                            }
                            true
                        }
                    }
                },
                update = { boardView ->
                    if (board.isNotEmpty()) {
                        boardView.setGame(game)
                        boardView.invalidate()
                    }
                }
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(gameStatusResId),
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(stringResource(R.string.number_computer_wins, computerWins))
                    Text(stringResource(R.string.number_ties, ties))
                    Text(stringResource(R.string.number_wins_human, humanWins))
                }

                Button(
                    onClick = { startNewGame() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(stringResource(R.string.new_game_button))
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .size(300.dp)
                    .padding(16.dp),
                factory = { ctx ->
                    BoardView(ctx).apply {
                        setGame(game)
                        setOnTouchListener { view, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                view.performClick()
                                val cellWidth = boardCellWidth
                                val cellHeight = boardCellHeight
                                if (cellWidth > 0 && cellHeight > 0) {
                                    val col = (event.x / cellWidth).toInt().coerceIn(0, 2)
                                    val row = (event.y / cellHeight).toInt().coerceIn(0, 2)
                                    val location = row * 3 + col
                                    onCellClick(location)
                                }
                            }
                            true
                        }
                    }
                },
                update = { boardView ->
                    if (board.isNotEmpty()) {
                        boardView.setGame(game)
                        boardView.invalidate()
                    }
                }
            )

            Text(
                text = stringResource(gameStatusResId),
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 20.dp)
            )

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
}
