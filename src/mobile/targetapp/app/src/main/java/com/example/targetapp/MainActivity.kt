package com.example.targetapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.targetapp.ui.theme.TargetappTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TargetAppFlow(
                        modifier = Modifier.padding(innerPadding),
                        onExit = { finishAffinity() }
                    )
                }
            }
        }
    }
}

enum class AppPage { LOGIN, DECISION, PIN }

@Composable
fun TargetAppFlow(modifier: Modifier = Modifier, onExit: () -> Unit) {
    var currentPage by remember { mutableStateOf(AppPage.LOGIN) }

    when (currentPage) {
        AppPage.LOGIN -> LoginScreen(
            modifier = modifier,
            onLoginSuccess = { currentPage = AppPage.DECISION }
        )
        AppPage.DECISION -> DecisionScreen(
            modifier = modifier,
            onTestText = { currentPage = AppPage.PIN },
            onNotTestText = onExit
        )
        AppPage.PIN -> PinScreen(modifier = modifier)
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login Page",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics {
                heading()
                contentDescription = "Login Page"
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                showError = false
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            isError = showError,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Password"
                }
        )

        if (showError) {
            Text(
                text = "Wrong password",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (password == "Test@2026") {
                    onLoginSuccess()
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .semantics {
                    contentDescription = "Login"
                }
        ) {
            Text(text = "Login", fontSize = 16.sp)
        }
    }
}

@Composable
fun DecisionScreen(
    modifier: Modifier = Modifier,
    onTestText: () -> Unit,
    onNotTestText: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Decision Page",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics {
                heading()
                contentDescription = "Decision Page"
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "test text 1",
            fontSize = 20.sp,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics {
                contentDescription = "test text 1"
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onTestText,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .semantics {
                    contentDescription = "Test text"
                }
        ) {
            Text(text = "Test text", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNotTestText,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .semantics {
                    contentDescription = "Not test text"
                }
        ) {
            Text(text = "Not test text", fontSize = 16.sp)
        }
    }
}

@Composable
fun PinScreen(modifier: Modifier = Modifier) {
    var pin by remember { mutableStateOf("") }
    val isComplete = pin == "8526"
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PIN Page",
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics {
                heading()
                contentDescription = "PIN Page"
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                    pin = newValue
                }
            },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = "PIN"
                }
        )

        if (isComplete) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Automation complete",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = "Automation complete"
                }
            )
        }
    }
}
