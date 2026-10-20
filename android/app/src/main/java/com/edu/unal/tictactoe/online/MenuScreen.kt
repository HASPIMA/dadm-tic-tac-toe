package com.edu.unal.tictactoe.online

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edu.unal.tictactoe.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    accountViewModel: AccountViewModel,
    onPlayLocal: () -> Unit,
    onPlayOnline: () -> Unit,
) {
    val account by accountViewModel.account.collectAsStateWithLifecycle()
    val displayName by accountViewModel.displayName.collectAsStateWithLifecycle()
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(accountViewModel) {
        accountViewModel.messages.collect { message ->
            // A Snackbar would be hidden behind the dialog
            if (showAccountDialog) accountError = message
            else snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            val buttonModifier = Modifier.widthIn(min = 220.dp)
            Button(onClick = onPlayLocal, modifier = buttonModifier) {
                Text(stringResource(R.string.menu_vs_computer))
            }
            Button(
                onClick = onPlayOnline,
                enabled = account != null,
                modifier = buttonModifier
            ) {
                Text(stringResource(R.string.menu_online))
            }
            OutlinedButton(
                onClick = { showAccountDialog = true },
                enabled = account != null,
                modifier = buttonModifier
            ) {
                Text(stringResource(R.string.menu_account))
            }

            if (account == null) {
                Text(stringResource(R.string.signing_in))
                if (!accountViewModel.busy) {
                    TextButton(onClick = accountViewModel::ensureSignedIn) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                displayName?.let { Text(stringResource(R.string.playing_as, it)) }
            }
        }
    }

    val currentAccount = account
    if (showAccountDialog && currentAccount != null) {
        AccountDialog(
            account = currentAccount,
            displayName = displayName.orEmpty(),
            accountViewModel = accountViewModel,
            error = accountError,
            onClearError = { accountError = null },
            onDismiss = {
                showAccountDialog = false
                accountError = null
            }
        )
    }
}

private enum class AccountMode { Overview, CreateAccount, SignIn }

/**
 * Guests can create an account (keeping their uid and games) or sign in to an
 * existing one; email users can sign out, going back to a new guest.
 */
@Composable
private fun AccountDialog(
    account: Account,
    displayName: String,
    accountViewModel: AccountViewModel,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(AccountMode.Overview) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var nickname by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<Int?>(null) }
    val busy = accountViewModel.busy

    LaunchedEffect(account.uid) {
        accountViewModel.refreshGuestHasActiveGames()
    }

    fun submit() {
        onClearError()
        validationError = when {
            email.isBlank() || password.isEmpty() -> R.string.email_password_required
            mode == AccountMode.CreateAccount && nickname.trim().length !in 3..20 ->
                R.string.nickname_invalid

            else -> null
        }
        if (validationError != null) return
        when (mode) {
            AccountMode.CreateAccount ->
                accountViewModel.createAccount(email, password, nickname, onDismiss)

            AccountMode.SignIn -> accountViewModel.signIn(email, password, onDismiss)
            AccountMode.Overview -> {}
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (mode) {
                        AccountMode.Overview -> R.string.account_title
                        AccountMode.CreateAccount -> R.string.create_account
                        AccountMode.SignIn -> R.string.sign_in
                    }
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (mode) {
                    AccountMode.Overview -> {
                        Text(
                            if (account.isAnonymous) {
                                stringResource(R.string.account_guest, displayName)
                            } else {
                                stringResource(
                                    R.string.account_signed_in,
                                    displayName,
                                    account.email.orEmpty()
                                )
                            }
                        )
                        if (account.isAnonymous) {
                            Button(
                                onClick = { mode = AccountMode.CreateAccount },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.create_account)) }
                            OutlinedButton(
                                onClick = { mode = AccountMode.SignIn },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.sign_in)) }
                        } else {
                            OutlinedButton(
                                onClick = { accountViewModel.signOut(onDismiss) },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(stringResource(R.string.sign_out)) }
                        }
                    }

                    AccountMode.CreateAccount, AccountMode.SignIn -> {
                        if (mode == AccountMode.SignIn && accountViewModel.guestHasActiveGames) {
                            Text(
                                stringResource(R.string.guest_games_warning),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(R.string.email)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.password)) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        if (mode == AccountMode.CreateAccount) {
                            OutlinedTextField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                label = { Text(stringResource(R.string.nickname)) },
                                singleLine = true
                            )
                        }
                        validationError?.let {
                            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (mode != AccountMode.Overview) {
                TextButton(onClick = ::submit, enabled = !busy) {
                    Text(
                        stringResource(
                            if (mode == AccountMode.CreateAccount) R.string.create_account
                            else R.string.sign_in
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (mode == AccountMode.Overview) {
                        onDismiss()
                    } else {
                        onClearError()
                        mode = AccountMode.Overview
                    }
                }
            ) {
                Text(
                    stringResource(
                        if (mode == AccountMode.Overview) R.string.close else R.string.cancel
                    )
                )
            }
        }
    )
}
