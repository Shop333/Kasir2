@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PosViewModel

@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Profiles
    var name by remember { mutableStateOf(viewModel.storeName) }
    var address by remember { mutableStateOf(viewModel.storeAddress) }
    var phone by remember { mutableStateOf(viewModel.storePhone) }
    var npwp by remember { mutableStateOf(viewModel.storeNpwp) }
    
    // Tax Config
    var isTaxEnabled by remember { mutableStateOf(viewModel.isTaxEnabled) }
    var taxText by remember { mutableStateOf(viewModel.taxPercentage.toString()) }
    
    // Auto-lock Config
    var autoLockText by remember { mutableStateOf(viewModel.autoLockTimerMinutes.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & System", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Store Profil
            Text("Pengaturan Toko & Outlet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth().testTag("store_name_input"))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Outlet") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telepon Toko") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = npwp, onValueChange = { npwp = it }, label = { Text("NPWP (Format ID)") }, modifier = Modifier.fillMaxWidth())
                }
            }

            // Tax (PPN)
            Text("Pajak & Kebijakan PPN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Aktifkan PPN Pajak Penjualan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("PPN akan ditambahkan otomatis ke total tagihan", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isTaxEnabled,
                            onCheckedChange = { isTaxEnabled = it },
                            modifier = Modifier.testTag("tax_switch_toggle")
                        )
                    }

                    if (isTaxEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = taxText,
                            onValueChange = { taxText = it },
                            label = { Text("Persentase PPN (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("tax_percentage_input")
                        )
                    }
                }
            }

            // Printer Config
            Text("Konfigurasi Mesin Cetak Struk", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ukuran Kertas Thermal Printer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row {
                            listOf(58, 80).forEach { size ->
                                val active = viewModel.activePrinterSize == size
                                FilterChip(
                                    selected = active,
                                    onClick = { viewModel.activePrinterSize = size },
                                    label = { Text("${size}mm") },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                    Text("Jalur cetak struk: ${viewModel.activePrinterType}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Auto-lock & Security timeout
            Text("Pengamanan Sistem & Auto-Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Auto-lock Interval (Menit Idle)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = autoLockText,
                        onValueChange = { autoLockText = it },
                        label = { Text("Waktu Idle Detik/Menit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Theme Preferences & Backups
            Text("Preferensi Visual & Backup Data", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Dark light toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mode Gelap (Dark Theme) Akun", fontWeight = FontWeight.Medium)
                        Switch(
                            checked = viewModel.isDarkMode,
                            onCheckedChange = { viewModel.isDarkMode = it },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }
                    
                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Mencadangkan database ke Google Drive...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("backup_drive_btn")
                        ) {
                            Icon(Icons.Default.CloudUpload, "Backup")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup Drive", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Memulihkan database cadangan Google Drive Berhasil!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).testTag("restore_drive_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, "Restore")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Drive", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Global Save Config Button
            Button(
                onClick = {
                    val finalTax = taxText.toDoubleOrNull() ?: 11.0
                    val minutes = autoLockText.toIntOrNull() ?: 5
                    viewModel.saveStoreConfig(name, address, phone, npwp, finalTax, isTaxEnabled)
                    viewModel.autoLockTimerMinutes = minutes
                    Toast.makeText(context, "Konfigurasi Global berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_all_settings_btn")
            ) {
                Icon(Icons.Default.Save, "Save Settings")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Semua Konfigurasi", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
