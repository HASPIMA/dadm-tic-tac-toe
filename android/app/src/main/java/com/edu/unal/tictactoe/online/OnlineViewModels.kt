package com.edu.unal.tictactoe.online

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edu.unal.tictactoe.TicTacToeGame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val WHILE_VISIBLE = SharingStarted.WhileSubscribed(5_000)

/**
 * Base for the online view models: runs callable functions one at a time and
 * reports their failures as one-off [messages] (shown in a Snackbar).
 */
abstract class OnlineViewModel(
    protected val repository: GameRepository = GameRepository(),
) : ViewModel() {
    var busy by mutableStateOf(false)
        private set

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    protected fun report(error: Throwable) {
        messageChannel.trySend(error.localizedMessage ?: error.toString())
    }

    protected fun launchCall(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(e)
            } finally {
                busy = false
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModel : OnlineViewModel() {
    val account: StateFlow<Account?> = repository.account
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.currentAccount())

    /** Nickname for email accounts, `Guest-xxxx` otherwise (or without profile). */
    val displayName: StateFlow<String?> = account.flatMapLatest { account ->
        when {
            account == null -> flowOf(null)
            account.isAnonymous -> flowOf(guestName(account.uid))
            else -> repository.nickname(account.uid)
                .map { it ?: guestName(account.uid) }
                .catch { emit(guestName(account.uid)) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Whether the current guest would lose active games by signing in. */
    var guestHasActiveGames by mutableStateOf(false)
        private set

    init {
        ensureSignedIn()
    }

    /** Signs in anonymously when nobody is signed in. */
    fun ensureSignedIn() {
        if (repository.currentAccount() == null) launchCall { repository.signInAsGuest() }
    }

    fun refreshGuestHasActiveGames() {
        val account = account.value ?: return
        guestHasActiveGames = false
        if (!account.isAnonymous) return
        viewModelScope.launch {
            guestHasActiveGames = runCatching { repository.hasActiveGames(account.uid) }
                .getOrDefault(false)
        }
    }

    fun createAccount(email: String, password: String, nickname: String, onDone: () -> Unit) =
        launchCall {
            repository.createAccount(email.trim(), password, nickname.trim())
            onDone()
        }

    fun signIn(email: String, password: String, onDone: () -> Unit) = launchCall {
        repository.signIn(email.trim(), password)
        onDone()
    }

    fun signOut(onDone: () -> Unit) = launchCall {
        repository.signOut()
        onDone()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModel : OnlineViewModel() {
    val uid: StateFlow<String?> = repository.account.map { it?.uid }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.currentAccount()?.uid)

    /** Other players' open games; null while loading. */
    val openGames: StateFlow<List<GameSession>?> = uid.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else repository.openGames()
            .map { games -> games.filter { it.x != uid } }
            .catch { report(it); emit(emptyList()) }
    }.stateIn(viewModelScope, WHILE_VISIBLE, null)

    /** Active games the user plays in, including their own open game. */
    val myGames: StateFlow<List<GameSession>?> = uid.flatMapLatest { uid ->
        if (uid == null) flowOf(null)
        else repository.myGames(uid).catch { report(it); emit(emptyList()) }
    }.stateIn(viewModelScope, WHILE_VISIBLE, null)

    fun createGame(onOpen: (String) -> Unit) = launchCall { onOpen(repository.createGame()) }

    fun joinGame(gameId: String, onOpen: (String) -> Unit) = launchCall {
        repository.joinGame(gameId)
        onOpen(gameId)
    }
}

class OnlineGameViewModel(val gameId: String) : OnlineViewModel() {
    val uid: String? = repository.currentAccount()?.uid

    /** Null while loading; [GameState.NotFound] when the document is missing. */
    val game: StateFlow<GameState?> = repository.game(gameId)
        .map { game -> if (game == null) GameState.NotFound else GameState.Loaded(game) }
        .catch { report(it); emit(GameState.NotFound) }
        .stateIn(viewModelScope, WHILE_VISIBLE, null)

    fun move(cell: Int) {
        val game = (game.value as? GameState.Loaded)?.game ?: return
        if (uid == null || !game.isTurnOf(uid) || game.board[cell] != TicTacToeGame.OPEN_SPOT) {
            return
        }
        launchCall { repository.makeMove(gameId, cell) }
    }

    fun leave() = launchCall { repository.leaveGame(gameId) }
}

sealed interface GameState {
    data object NotFound : GameState
    data class Loaded(val game: GameSession) : GameState
}
