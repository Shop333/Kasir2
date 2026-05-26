@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShiftScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val shiftList by viewModel.shiftsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val logList by viewModel.activityLogsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val activeShift = viewModel.activeShift
    val activePettyCashList by viewModel.getPettyCashForCurrentShift().collectAsStateWithLifecycle(initialValue = emptyList())

    var activeSubTab by remember { mutableStateOf(0) } // 0: Shift Aktif/Kontrol, 1: Buku Kas Kecil, 2: Log Keamanan Audit
    
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCloseShiftDialog by remember { mutableStateOf(false) }
    var showPettyCashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shift Kasir & Kas Kecil", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("shift_back_btn")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    // Shift active symbol indicator
                    val statusText = if (activeShift != null) "Kas Terbuka" else "Kas Tutup"
                    val colorContainer = if (activeShift != null) Color(0xFFE8F5E9UL) else Color(0xFFFFEBEEUL)
                    val contentColorText = if (activeShift != null) Color(0xFF2E7D32UL) else Color(0xFFC62828UL)
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColorText,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(colorContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
        ) {
            // Tab Header Row
            TabRow(selectedTabIndex = activeSubTab) {
                Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }, text = { Text("Manajemen Shift") })
                Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }, text = { Text("Kas Kecil Operasional") })
                Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }, text = { Text("Log Audit Aktivitas") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeSubTab) {
                0 -> {
                    // TAB 0: Active Shift controllers
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (activeShift == null) {
                            // Offline lock, cashier can't checkout until setup
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha=0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Lock, "Locked Drawer", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Shift Kasir Terkunci", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Sebelum memproses belanja pelanggan, Anda wajib mendaftarkan jumlah modal laci kasir pembuka hari ini.",
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { showOpenShiftDialog = true },
                                        modifier = Modifier.testTag("open_cashier_shift_btn")
                                    ) {
                                        Icon(Icons.Default.Key, "Key Open")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buka Shift Laci (Modal Awal)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Active Shift controls showing detailed indicators
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.15f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Petugas Kasir: ${activeShift.userName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("ID: #${activeShift.id}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                    Text("Buka sejak: ${sdf.format(Date(activeShift.openTime))}", fontSize = 11.sp, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Breakdowns
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Saku Modal Awal", fontSize = 10.sp, color = Color.Gray)
                                            Text("Rp${String.format("%,.0f", activeShift.startingCash)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sales Tunai", fontSize = 10.sp, color = Color.Gray)
                                            Text("Rp${String.format("%,.0f", activeShift.cashSales)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sales Non-Tunai (QRIS/Card)", fontSize = 10.sp, color = Color.Gray)
                                            val nonTunai = activeShift.qrisSales + activeShift.cardSales + activeShift.bankSales
                                            Text("Rp${String.format("%,.0f", nonTunai)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Sales Hutang Member", fontSize = 10.sp, color = Color.Gray)
                                            Text("Rp${String.format("%,.0f", activeShift.debtSales)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Theoretical drawer calculations
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Saldo Laci Seharusnya (Tunai):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Rp${String.format("%,.0f", activeShift.expectedCash)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { showCloseShiftDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.NoMeetingRoom, "Close Drawer")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tutup Shift Laci (Gaji Fisik & Selesai)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Shift History
                        if (shiftList.isNotEmpty()) {
                            Text("Riwayat Pertanggungjawaban Shift (Hutang Kasir)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            shiftList.filter { it.isClosed }.forEach { s ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        val cdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Shift #${s.id} - ${s.userName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                text = if (s.cashDifference >= 0) "Cocok/Lebih" else "Defisit Kas",
                                                color = if (s.cashDifference == 0.0) Color(0xFF2E7D32UL) else if (s.cashDifference > 0) Color.Blue else Color.Red,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text("Waktu: ${cdf.format(Date(s.openTime))} s/d ${if (s.closeTime != null) cdf.format(Date(s.closeTime)) else ""}", fontSize = 10.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Saku Teori: Rp${String.format("%,.0f", s.expectedCash)} | Fisik: Rp${String.format("%,.0f", s.actualCash)}", fontSize = 11.sp)
                                            Text("Selisih: Rp${String.format("%,.0f", s.cashDifference)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: Petty Cash bookkeeping
                    if (activeShift == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Kas Kecil terkunci. Anda wajib membuka shift kasir terlebih dahulu.", modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pencatatan Biaya Kas Kecil", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Button(onClick = { showPettyCashDialog = true }) {
                                    Icon(Icons.Default.AddCard, "Add Cost")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tambah Kas Operasional")
                                }
                            }

                            if (activePettyCashList.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                    Text("Belum ada mutasi biaya operational hari ini.")
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    items(activePettyCashList) { pc ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (pc.type == "IN") Color(0xFFE8F5E9UL) else Color(0xFFFFEBEEUL)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(pc.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    val pt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(pc.timestamp))
                                                    Text("Waktu: $pt | Shift: #${pc.id}", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                val prefix = if (pc.type == "IN") "+Rp" else "-Rp"
                                                val txtColor = if (pc.type == "IN") Color(0xFF2E7D32UL) else Color(0xFFC62828UL)
                                                Text(
                                                    text = "$prefix${String.format("%,.0f", pc.amount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = txtColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    // TAB 2: Audits security timelines
                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        Text("Audit Log Perubahan & Keamanan", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        if (logList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                                Text("Belum ada rekam log audit keamanan.")
                            }
                        } else {
                            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                items(logList) { l ->
                                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Column(modifier = Modifier.padding(11.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "User: ${l.username}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                val logDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(l.timestamp))
                                                Text(logDate, fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = l.details,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog: open shift drawer
    if (showOpenShiftDialog) {
        var modalText by remember { mutableStateOf("200000") } // default 200.000 modal
        Dialog(onDismissRequest = { showOpenShiftDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pendaftaran Modal Awal Laci", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pastikan saldo modal sesuai laci kasir fisik saat ini.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = modalText,
                        onValueChange = { modalText = it },
                        label = { Text("Jumlah Modal Tunai (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("modal_awal_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showOpenShiftDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val value = modalText.toDoubleOrNull() ?: 100000.0
                                viewModel.openShift(value)
                                showOpenShiftDialog = false
                            },
                            modifier = Modifier.testTag("confirm_open_shift_btn")
                        ) {
                            Text("Buka Laci")
                        }
                    }
                }
            }
        }
    }

    // Modal dialog: close shift drawer & reconcile
    if (showCloseShiftDialog) {
        var physText by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        val expected = activeShift?.expectedCash ?: 0.0

        Dialog(onDismissRequest = { showCloseShiftDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tutup Shift & Verifikasi Kas Fisik", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("Uang Teori Laci: Rp ${String.format("%,.0f", expected)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = physText,
                        onValueChange = { physText = it },
                        label = { Text("Uang Fisik Dihitung (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cash_fisik_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Keterangan Selisih Lain-Lain") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCloseShiftDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val physVal = physText.toDoubleOrNull() ?: 0.0
                                viewModel.closeShift(physVal, comment)
                                showCloseShiftDialog = false
                                Toast.makeText(context, "Shift Kasir Berhasil Ditutup!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("confirm_verify_shift_btn")
                        ) {
                            Text("Tutup Laci & Serah Terima")
                        }
                    }
                }
            }
        }
    }

    // Modal dialog: record operational petty cash
    if (showPettyCashDialog) {
        var pettyAmt by remember { mutableStateOf("") }
        var pettyDesc by remember { mutableStateOf("") }
        var isOutflow by remember { mutableStateOf(true) } // default pengeluaran

        Dialog(onDismissRequest = { showPettyCashDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catat Kas Kecil Operasional", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row {
                        Button(
                            onClick = { isOutflow = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isOutflow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isOutflow) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Biaya Keluar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { isOutflow = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isOutflow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isOutflow) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Modal Masuk")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = pettyAmt,
                        onValueChange = { pettyAmt = it },
                        label = { Text("Jumlah Nominal Dana (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = pettyDesc,
                        onValueChange = { pettyDesc = it },
                        label = { Text("Keterangan Biaya (contoh: Air Aqua Galon)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPettyCashDialog = false }) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = pettyAmt.toDoubleOrNull() ?: 0.0
                                val type = if (isOutflow) "OUT" else "IN"
                                if (amount > 0.0 && pettyDesc.isNotBlank()) {
                                    viewModel.recordPettyCash(amount, type, pettyDesc)
                                    showPettyCashDialog = false
                                }
                            }
                        ) {
                            Text("Simpan Kas")
                        }
                    }
                }
            }
        }
    }
}
