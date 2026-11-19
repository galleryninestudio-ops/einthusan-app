package com.bench.einthusan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bench.einthusan.net.LoginResult
import com.bench.einthusan.ui.theme.Display
import com.bench.einthusan.ui.theme.Gold
import com.bench.einthusan.ui.theme.Ink
import com.bench.einthusan.ui.theme.TextHi
import com.bench.einthusan.ui.theme.TextLow
import com.bench.einthusan.ui.theme.TextMid
import kotlinx.coroutines.launch

/**
 * Account. Signed-in state comes from the stored session, not from a flag that
 * a recomposition can drop: if there is an email, there is a session.
 */
@Composable
fun LoginScreen(app: AppState, nav: Nav) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf(app.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val signedIn = app.email != null

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (signedIn) "Account" else "Sign in",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = Display,
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                ),
                color = TextHi,
            )
        }

        Column(Modifier.padding(horizontal = 24.dp)) {

            if (signedIn) {
                SignedInCard(
                    email = app.email.orEmpty(),
                    premium = app.premium,
                )

                Spacer(Modifier.height(20.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x0FFFFFFF))
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                        .pressScale {
                            app.signOut()
                            email = ""
                            password = ""
                            error = null
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Sign out",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextHi,
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    if (app.premium) "Signed in. Choose Standard or Premium HD in player settings. Resolution varies by title."
                    else "Signed in on a free account. Premium HD needs Premium on einthusan.tv.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLow,
                )
            } else {
                Text(
                    "Your Einthusan account. Premium unlocks HD on supported titles.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLow,
                )

                Spacer(Modifier.height(26.dp))

                Field(
                    label = "Email",
                    value = email,
                    onValue = { email = it },
                    keyboard = KeyboardType.Email,
                )
                Spacer(Modifier.height(12.dp))
                Field(
                    label = "Password",
                    value = password,
                    onValue = { password = it },
                    keyboard = KeyboardType.Password,
                    secret = true,
                    ime = ImeAction.Done,
                    onDone = {
                        if (!busy && email.isNotBlank() && password.isNotBlank()) {
                            scope.launch {
                                submit(app, email, password, { busy = it }, { error = it })
                            }
                        }
                    },
                )

                AnimatedVisibility(visible = error != null, enter = fadeIn()) {
                    Text(
                        error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .pressScale {
                            if (!busy && email.isNotBlank() && password.isNotBlank()) {
                                scope.launch {
                                    submit(app, email, password, { busy = it }, { error = it })
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0B0B0B),
                        )
                    } else {
                        Text(
                            "Sign in",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = Color(0xFF0B0B0B),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Not now",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMid,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .pressScale { nav.pop() }
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SignedInCard(email: String, premium: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x0FFFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (premium) Color.White else Color(0x1FFFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            if (premium) {
                Icon(Icons.Filled.Check, null, tint = Color(0xFF0B0B0B), modifier = Modifier.size(18.dp))
            } else {
                Text(
                    email.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextHi,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                email,
                style = MaterialTheme.typography.titleMedium,
                color = TextHi,
                maxLines = 1,
            )
            Text(
                if (premium) "Premium · HD" else "Free account · 480p",
                style = MaterialTheme.typography.bodySmall,
                color = if (premium) TextHi else TextLow,
            )
        }
    }
}

private suspend fun submit(
    app: AppState,
    email: String,
    password: String,
    setBusy: (Boolean) -> Unit,
    setError: (String?) -> Unit,
) {
    setBusy(true)
    setError(null)
    when (val result = app.auth.login(email.trim(), password)) {
        is LoginResult.Ok -> {
            com.bench.einthusan.data.Repo.clearWatchCache()
            app.applyEmail(email.trim())
            app.applyPremium(result.premium)
        }
        is LoginResult.Fail -> setError(result.message)
        LoginResult.NoNetwork -> setError("No connection to einthusan.tv")
    }
    setBusy(false)
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
    ime: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    val glow by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "glow")

    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (focused) TextHi else TextLow,
            modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x0FFFFFFF))
                .border(
                    width = 1.dp,
                    color = if (focused) Color(0x66FFFFFF) else Color(0x1FFFFFFF),
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 16.dp, vertical = 15.dp)
        ) {
            if (value.isEmpty()) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLow,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextHi),
                cursorBrush = SolidColor(Color.White),
                visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = ime),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
    }
}
