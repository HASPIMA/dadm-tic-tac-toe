package com.edu.unal.tictactoe.online

import com.edu.unal.tictactoe.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

/**
 * Points Firebase at the local emulators (ports from `backend/firebase.json`)
 * when a debug build sets `firebase.emulatorHost`. Otherwise does nothing.
 */
object FirebaseEmulators {
    private var connected = false

    /** Must run before any other Firebase call; safe to call more than once. */
    fun connectIfConfigured() {
        val host = BuildConfig.FIREBASE_EMULATOR_HOST ?: return
        if (connected) return
        connected = true
        FirebaseAuth.getInstance().useEmulator(host, 9099)
        FirebaseFirestore.getInstance().useEmulator(host, 8080)
        FirebaseFunctions.getInstance().useEmulator(host, 5001)
    }
}
