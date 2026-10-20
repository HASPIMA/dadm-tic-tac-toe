package com.edu.unal.tictactoe.online

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Access to Firebase for online play. Firestore is only read here; every
 * write goes through the callable functions in `backend/functions`.
 */
class GameRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
) {
    private val games = db.collection("game_sessions")
    private val users = db.collection("users")

    // region Auth

    /** Current account; also emits when a guest is linked to an email. */
    val account: Flow<Account?> = callbackFlow {
        val listener = FirebaseAuth.IdTokenListener { trySend(currentAccount()) }
        auth.addIdTokenListener(listener)
        awaitClose { auth.removeIdTokenListener(listener) }
    }.distinctUntilChanged()

    fun currentAccount(): Account? = auth.currentUser?.let {
        Account(uid = it.uid, isAnonymous = it.isAnonymous, email = it.email)
    }

    suspend fun signInAsGuest() {
        auth.signInAnonymously().await()
    }

    /** Turns the current guest into an email account, keeping its uid. */
    suspend fun createAccount(email: String, password: String, nickname: String) {
        val user = checkNotNull(auth.currentUser)
        user.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await()
        // Refresh the token so the backend sees the email provider, not anonymous.
        user.getIdToken(true).await()
        call("setNickname", mapOf("nickname" to nickname))
    }

    /** Switches to an existing email account. The current guest is lost. */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /** Signs out and continues as a brand-new guest. */
    suspend fun signOut() {
        auth.signOut()
        signInAsGuest()
    }

    // endregion

    // region Reads

    fun nickname(uid: String): Flow<String?> =
        users.document(uid).snapshots().map { it.getString("nickname") }

    /** Games waiting for an opponent, newest first. Includes the caller's own. */
    fun openGames(): Flow<List<GameSession>> =
        games.whereEqualTo("active", true)
            .whereEqualTo("O", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(OPEN_GAMES_LIMIT)
            .snapshots()
            .map { it.toGameSessions() }

    /** Active games where [uid] plays X or O, most recently updated first. */
    fun myGames(uid: String): Flow<List<GameSession>> =
        myGamesQuery(uid).snapshots().map { it.toGameSessions() }

    suspend fun hasActiveGames(uid: String): Boolean =
        !myGamesQuery(uid).limit(1).get().await().isEmpty

    fun game(gameId: String): Flow<GameSession?> =
        games.document(gameId).snapshots().map { GameSession.fromSnapshot(it) }

    private fun myGamesQuery(uid: String): Query =
        games.where(
            Filter.and(
                Filter.equalTo("active", true),
                Filter.or(Filter.equalTo("X", uid), Filter.equalTo("O", uid)),
            )
        ).orderBy("updatedAt", Query.Direction.DESCENDING)

    // endregion

    // region Writes (callable functions)

    /** Creates a game, or returns the caller's existing open game. */
    suspend fun createGame(): String = call("createGame")["gameId"] as String

    suspend fun joinGame(gameId: String) {
        call("joinGame", mapOf("gameId" to gameId))
    }

    suspend fun makeMove(gameId: String, cell: Int) {
        call("makeMove", mapOf("gameId" to gameId, "cell" to cell))
    }

    /** Cancels an open game or forfeits an ongoing one. */
    suspend fun leaveGame(gameId: String) {
        call("leaveGame", mapOf("gameId" to gameId))
    }

    private suspend fun call(name: String, data: Map<String, Any>? = null): Map<*, *> =
        functions.getHttpsCallable(name).call(data).await().getData() as? Map<*, *>
            ?: emptyMap<Any, Any>()

    // endregion

    private companion object {
        const val OPEN_GAMES_LIMIT = 50L
    }
}

private fun QuerySnapshot.toGameSessions(): List<GameSession> =
    documents.mapNotNull { GameSession.fromSnapshot(it) }

private fun Query.snapshots(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

private fun DocumentReference.snapshots(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) close(error) else if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}
