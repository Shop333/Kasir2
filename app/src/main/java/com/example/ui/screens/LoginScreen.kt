package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    viewModel: PosViewModel,
    onLoginSuccess: () -> Unit
) {
    var isPinMode by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin") }
    var pinValue by remember { mutableStateOf("") }
    
    val authError = viewModel.authError
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Visual branding
                Icon(
                    imageOf = Icons.Default.PointOfSale,
                    contentDescription = "POS Logo",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = viewModel.storeName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Aplikasi Kasir POS Pintar",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Toggle between Password and PIN mode
                TabRow(
                    selectedTabIndex = if (isPinMode) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = isPinMode,
                        onClick = { isPinMode = true; viewModel.authError = null },
                        text = { Text("Login PIN", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = !isPinMode,
                        onClick = { isPinMode = false; viewModel.authError = null },
                        text = { Text("Kata Sandi", fontWeight = FontWeight.SemiBold) }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (authError != null) {
                    Text(
                        text = authError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                AnimatedContent(
                    targetState = isPinMode,
                    transitionSpec = {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
                    },
                    label = "LoginTransition"
                ) { isPin ->
                    if (isPin) {
                        // PIN keypad view
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Masukkan 4 Digit PIN Keamanan",
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Visual PIN digits indicator
                            Row(
                                modifier = Modifier.padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                repeat(4) { idx ->
                                    val checked = idx < pinValue.length
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(
                                                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                shape = MaterialTheme.shapes.small
                                            )
                                    )
                                }
                            }
                            
                            // Keypad layout
                            Column(
                                modifier = Modifier.width(260.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val keys = listOf(
                                    listOf("1", "2", "3"),
                                    listOf("4", "5", "6"),
                                    listOf("7", "8", "9"),
                                    listOf("C", "0", "OK")
                                )
                                keys.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        row.forEach { char ->
                                            Button(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1.2f)
                                                    .testTag("pin_key_$char"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (char == "OK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (char == "OK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                onClick = {
                                                    when (char) {
                                                        "C" -> if (pinValue.isNotEmpty()) pinValue = pinValue.dropLast(1)
                                                        "OK" -> {
                                                            if (pinValue.length >= 4) {
                                                                viewModel.loginWithPin(pinValue, onLoginSuccess)
                                                            }
                                                        }
                                                        else -> {
                                                            if (pinValue.length < 4) {
                                                                pinValue += char
                                                            }
                                                            // Auto submit
                                                            if (pinValue.length == 4) {
                                                                viewModel.loginWithPin(pinValue, onLoginSuccess)
                                                            }
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(char, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            // Quick biometric suggestion
                            TextButton(
                                onClick = {
                                    // Simulated Bio-metric login
                                    pinValue = "1234" // admin pin mockup
                                    viewModel.loginWithPin(pinValue, onLoginSuccess)
                                },
                                modifier = Modifier.testTag("bio_login_button")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Biometrik")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk dengan Biometrik (Mock)")
                            }
                        }
                    } else {
                        // Standard Password form
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Nama Pengguna (Username)") },
                                leadingIcon = { Icon(Icons.Default.Person, "User Icon") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("username_input")
                            )
                            
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Kata Sandi (Password)") },
                                leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("password_input")
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    if (username.isNotEmpty() && password.isNotEmpty()) {
                                        viewModel.loginWithPassword(username, password, onLoginSuccess)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("login_submit_button")
                            ) {
                                Icon(Icons.Default.Login, "Login")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Masuk Sistem", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Tip Login Pengujian:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("• Admin: admin / admin (PIN: 1234)", fontSize = 12.sp)
                                    Text("• Kasir: kasir / kasir (PIN: 5678)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Icon helper function to load default material icons safely
@Composable
fun Icon(imageOf: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.onSurface) {
    androidx.compose.material3.Icon(imageVector = imageOf, contentDescription = contentDescription, modifier = modifier, tint = tint)
}

// Helper color alias mapping
typealias Color = androidx.compose.ui.graphics.Color
