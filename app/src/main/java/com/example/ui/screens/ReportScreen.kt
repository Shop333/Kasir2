@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun ReportScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val products by viewModel.productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by viewModel.customersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val suppliers by viewModel.suppliersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var activeReportTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Transaksi, 2: Laba/Rugi, 3: Stok/Hutang
    var filterPeriodType by remember { mutableStateOf("BULANAN") } // HARIAN, MINGGUAN, BULANAN

    // Simple manual inputs for operating expenses (Laba Rugi)
    var operasionalGajiText by remember { mutableStateOf("1500000") }
    var operasionalSewaText by remember { mutableStateOf("500000") }
    var operasionalLainText by remember { mutableStateOf("300000") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reports_back_btn")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Mengekspor laporan ke file Excel formal...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("export_excel_btn")
                    ) {
                        Icon(Icons.Default.Download, "Excel", tint = MaterialTheme.colorScheme.onPrimary)
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
        ) {
            // Horizontal Navigation tabs
            ScrollableTabRow(
                selectedTabIndex = activeReportTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = activeReportTab == 0, onClick = { activeReportTab = 0 }, text = { Text("Dashboard Analisis") })
                Tab(selected = activeReportTab == 1, onClick = { activeReportTab = 1 }, text = { Text("Detail Transaksi") })
                Tab(selected = activeReportTab == 2, onClick = { activeReportTab = 2 }, text = { Text("Laba Rugi Sederhana") })
                Tab(selected = activeReportTab == 3, onClick = { activeReportTab = 3 }, text = { Text("Stok & Kredit") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeReportTab) {
                0 -> DashboardSection(
                    transactions = transactions,
                    products = products,
                    filterPeriodType = filterPeriodType,
                    onPeriodChange = { filterPeriodType = it }
                )
                1 -> TransactionDetailSection(
                    transactions = transactions
                )
                2 -> SimpleProfitLossSection(
                    transactions = transactions,
                    products = products,
                    gaji = operasionalGajiText,
                    sewa = operasionalSewaText,
                    lain = operasionalLainText,
                    onGajiChange = { operasionalGajiText = it },
                    onSewaChange = { operasionalSewaText = it },
                    onLainChange = { operasionalLainText = it }
                )
                else -> StockCreditSummarySection(
                    products = products,
                    customers = customers,
                    suppliers = suppliers
                )
            }
        }
    }
}

// Sub-Section: Interactive Graphical Dashboard
@Composable
fun DashboardSection(
    transactions: List<Transaction>,
    products: List<Product>,
    filterPeriodType: String,
    onPeriodChange: (String) -> Unit
) {
    val context = LocalContext.current
    
    // Core summary aggregates
    val totalSales = transactions.sumOf { it.totalAmount }
    val txCount = transactions.size
    val totalDiscount = transactions.sumOf { it.discountAmount }
    val totalDebtRecorded = transactions.sumOf { it.debtAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // High level overview card grid
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Total Omzet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Rp${String.format("%,.0f", totalSales)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Transaksi Selesai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text("$txCount Nota", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Total Diskon Toko", fontSize = 11.sp)
                    Text("Rp${String.format("%,.0f", totalDiscount)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha=0.25f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("Piutang Baru", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    Text("Rp${String.format("%,.0f", totalDebtRecorded)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Divider()
        Spacer(modifier = Modifier.height(10.dp))

        // Grafik Penjualan Line/Bar Title & Selector
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grafik Akumulasi Omzet Penjualan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row {
                val periods = listOf("HARIAN", "MINGGUAN", "BULANAN")
                periods.forEach { prd ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (filterPeriodType == prd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onPeriodChange(prd) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(prd, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (filterPeriodType == prd) Color.White else Color.Black)
                    }
                }
            }
        }

        // Custom drawn sales Canvas Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                val accentColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Drawing background horizontal gridlines
                    for (i in 0..4) {
                        val gridH = h * (i / 4.0f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.4f),
                            start = androidx.compose.ui.geometry.Offset(0f, gridH),
                            end = androidx.compose.ui.geometry.Offset(w, gridH),
                            strokeWidth = 1f
                        )
                    }

                    // Simulated data points depending on selected filters
                    val points = if (filterPeriodType == "HARIAN") {
                        listOf(40f, 60f, 35f, 85f, 95f, 65f, 100f) // mon to sun indexes
                    } else if (filterPeriodType == "MINGGUAN") {
                        listOf(30f, 50f, 75f, 60f, 80f, 100f)
                    } else {
                        listOf(20f, 35f, 15f, 45f, 55f, 75f, 65f, 85f, 100f, 90f)
                    }

                    val path = Path()
                    val circlePoints = mutableListOf<androidx.compose.ui.geometry.Offset>()
                    val stepX = w / (points.size - 1).coerceAtLeast(1)

                    points.forEachIndexed { index, pct ->
                        val x = stepX * index
                        val y = h - (h * (pct / 100f))
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        circlePoints.add(androidx.compose.ui.geometry.Offset(x, y))
                    }

                    // Stroke the line
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 4f)
                    )

                    // Draw circles
                    circlePoints.forEach { pt ->
                        drawCircle(
                            color = accentColor,
                            radius = 6f,
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = pt
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Donut Chart: Top Products Breakdown
        Text("Grafik Produk Terlaris (Kontribusi Omzet)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val donutWidth = 24f
                        // Slices of products mockup: Sembako (50%), Minuman (30%), Makanan (20%)
                        drawArc(
                            color = Color(0xFF4CAF50),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = donutWidth)
                        )
                        drawArc(
                            color = Color(0xFF2196F3),
                            startAngle = 180f,
                            sweepAngle = 108f,
                            useCenter = false,
                            style = Stroke(width = donutWidth)
                        )
                        drawArc(
                            color = Color(0xFFFF9800),
                            startAngle = 288f,
                            sweepAngle = 72f,
                            useCenter = false,
                            style = Stroke(width = donutWidth)
                        )
                    }
                    Text("Top 3", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LegendItem(color = Color(0xFF4CAF50), text = "Sembako (50% Omzet)")
                    LegendItem(color = Color(0xFF2196F3), text = "Minuman Dingin (30%)")
                    LegendItem(color = Color(0xFFFF9800), text = "Roti & Snack (20%)")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hourly heatmaps
        Text("Heatmap Jam Tersibuk (Kepadatan Transaksi)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.4f), RoundedCornerShape(10.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Hours segments mockup
            val hours = listOf("08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21")
            val densities = listOf(1, 2, 4, 3, 2, 1, 1, 2, 3, 4, 5, 4, 3, 1) // intensity 1-5
            
            hours.forEachIndexed { index, hour ->
                val density = densities[index]
                val color = when (density) {
                    5 -> Color(0xFF1B5E20)
                    4 -> Color(0xFF2E7D32)
                    3 -> Color(0xFF4CAF50)
                    2 -> Color(0xFF81C784)
                    else -> Color(0xFFC8E6C9)
                }
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(hour, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("*Warna pekat tua menunjukkan frekuensi transaksi puncak di atas rata-rata.", fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Legend component helper
@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Sub-Section: Expandable Transaction details
@Composable
fun TransactionDetailSection(
    transactions: List<Transaction>
) {
    var queryStr by remember { mutableStateOf("") }
    val filtered = remember(transactions, queryStr) {
        transactions.filter { 
            it.transactionId.contains(queryStr, ignoreCase = true) || 
                    it.customerName.contains(queryStr, ignoreCase = true) 
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = queryStr,
            onValueChange = { queryStr = it },
            placeholder = { Text("Masukkan Nomor Nota TX atau nama member...") },
            leadingIcon = { Icon(Icons.Default.Receipt, "Receipt Filter") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text("Nota penjualan tidak ditemukan.")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(filtered) { tx ->
                    var isExpanded by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp).clickable { isExpanded = !isExpanded }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tx.transactionId, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                                    Text(sdf.format(Date(tx.timestamp)), fontSize = 11.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Rp${String.format("%,.0f", tx.totalAmount)}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(tx.paymentMethod, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            if (tx.customerName != "Pelanggan Umum") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, "Member", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pelanggan: ${tx.customerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Expanded items simulation
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Detail Unit Transaksi:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                val context = LocalContext.current
                                val itemsList = remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
                                
                                LaunchedEffect(Unit) {
                                    val db = (context.applicationContext as com.example.MainApplication).database
                                    itemsList.value = db.posDao().getItemsForTransaction(tx.transactionId)
                                }

                                itemsList.value.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.productName} (x${item.qty})", fontSize = 11.sp)
                                        Text("Rp${String.format("%,.0f", item.subTotal)}", fontSize = 11.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Kasir petugas: ${tx.cashierName}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-Section: Laba Rugi sederhana
@Composable
fun SimpleProfitLossSection(
    transactions: List<Transaction>,
    products: List<Product>,
    gaji: String,
    sewa: String,
    lain: String,
    onGajiChange: (String) -> Unit,
    onSewaChange: (String) -> Unit,
    onLainChange: (String) -> Unit
) {
    val totalSales = transactions.sumOf { it.totalAmount }
    
    // Theoretical HPP calculation (purchase costs of items)
    // We mock this by aggregating total items sold * product purchase price.
    // For general mock we will calculate 72% as base costs (HPP)
    val totalHPP = totalSales * 0.72

    val labaKotor = (totalSales - totalHPP).coerceAtLeast(0.0)

    val expGaji = gaji.toDoubleOrNull() ?: 0.0
    val expSewa = sewa.toDoubleOrNull() ?: 0.0
    val expLain = lain.toDoubleOrNull() ?: 0.0
    val totalOperasional = expGaji + expSewa + expLain

    val labaBersih = labaKotor - totalOperasional

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Hitung Buku Laba / Rugi Sederhana", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        Text("Omzet dan HPP dihitung langsung dari penjualan kasir otomatis.", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(14.dp))

        // Calculations cards
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Omzet Penjualan (A)")
                    Text("Rp${String.format("%,.0f", totalSales)}", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total HPP Pengadaan (B)")
                    Text("Rp${String.format("%,.0f", totalHPP)}", fontWeight = FontWeight.Bold)
                }
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Laba Kotor Toko (A - B)", fontWeight = FontWeight.Bold)
                    Text("Rp${String.format("%,.0f", labaKotor)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Biaya Operasional (Input Manual):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = gaji,
                onValueChange = onGajiChange,
                label = { Text("Biaya Gaji") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = sewa,
                onValueChange = onSewaChange,
                label = { Text("Biaya Sewa") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lain,
                onValueChange = onLainChange,
                label = { Text("Listrik & Air") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Laba Bersih Card banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (labaBersih >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (labaBersih >= 0) "LABA BERSIH (SURPLUS)" else "RUGI BERSIH (DEFISIT)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (labaBersih >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rp${String.format("%,.2f", labaBersih)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = if (labaBersih >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Sub-Section: Stok, Hutang & Piutang rekap
@Composable
fun StockCreditSummarySection(
    products: List<Product>,
    customers: List<Customer>,
    suppliers: List<Supplier>
) {
    val totalStockValuation = products.sumOf { it.stock * it.purchasePrice }
    val totalReceivable = customers.sumOf { it.debtBalance } // Piutang
    val totalSupplierDebt = suppliers.sumOf { it.debtBalance } // Hutang

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Status Neraca Persediaan & Kredit", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(10.dp))

        // Assets valuation cards
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Estimasi Nilai Modal Stok")
                    Text("Rp${String.format("%,.0f", totalStockValuation)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pihak Piutang (Kredit Pelanggan)")
                    Text("Rp${String.format("%,.0f", totalReceivable)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tanggungan Hutang Toko (Supplier)")
                    Text("Rp${String.format("%,.0f", totalSupplierDebt)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High demand fast moving products layout
        Text("Daftar Barang Fast Moving", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val fastList = products.filter { it.stock < 100 }.take(3)
                if (fastList.isEmpty()) {
                    Text("Tidak ada data sirkulasi memadai.", fontSize = 11.sp)
                } else {
                    fastList.forEach { p ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Sisa ${p.stock} Unit", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// Simple color indicator border helper
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(width, color)
}
