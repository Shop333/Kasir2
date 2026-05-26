@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example

import androidx.compose.ui.graphics.Color

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.PosViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class ScreenRoute {
    LOGIN,
    DASHBOARD,
    POS_TERMINAL,
    PRODUCTS,
    PARTNERS,
    SHIFTS,
    REPORTS,
    SETTINGS
}

data class DashboardMenuItem(
    val route: ScreenRoute,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Retrieve ViewModel with Factory and Simple Constructor DI
        val app = application as MainApplication
        
        setContent {
            val viewModel: PosViewModel by viewModels {
                PosViewModel.Factory(app, app.repository)
            }
            
            var currentScreenRoute by remember { mutableStateOf(ScreenRoute.LOGIN) }
            val currentUser = viewModel.currentUser
            
            // Sync theme preference
            MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        
                        // Screen Router with smooth fade transition
                        AnimatedContent(
                            targetState = currentScreenRoute,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "ScreenTransition"
                        ) { targetRoute ->
                            when (targetRoute) {
                                ScreenRoute.LOGIN -> {
                                    LoginScreen(
                                        viewModel = viewModel,
                                        onLoginSuccess = {
                                            currentScreenRoute = ScreenRoute.DASHBOARD
                                        }
                                    )
                                }
                                ScreenRoute.DASHBOARD -> {
                                    DashboardHub(
                                        viewModel = viewModel,
                                        onNavigateTo = { currentScreenRoute = it },
                                        onLogout = {
                                            viewModel.logout()
                                            currentScreenRoute = ScreenRoute.LOGIN
                                        }
                                    )
                                }
                                ScreenRoute.POS_TERMINAL -> {
                                    PosScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                                ScreenRoute.PRODUCTS -> {
                                    ProductScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                                ScreenRoute.PARTNERS -> {
                                    CustomerSupplierScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                                ScreenRoute.SHIFTS -> {
                                    ShiftScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                                ScreenRoute.REPORTS -> {
                                    ReportScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                                ScreenRoute.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { currentScreenRoute = ScreenRoute.DASHBOARD }
                                    )
                                }
                            }
                        }
                        
                        // SYSTEM AUTO-LOCK OVERLAY DIALOG (Indonesian)
                        if (viewModel.isLocked && currentUser != null) {
                            SystemPinLockDialog(
                                cashierName = currentUser.name,
                                onUnlock = { pinInput ->
                                    viewModel.unlockApp(pinInput)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Menu Utama Dashboard Hub (Indonesian)
@Composable
fun DashboardHub(
    viewModel: PosViewModel,
    onNavigateTo: (ScreenRoute) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val user = viewModel.currentUser ?: return
    val transactions by viewModel.transactionsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val lowStockProducts by viewModel.lowStockProductsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    // Menu Item Configuration
    val menuItems = remember {
        listOf(
            DashboardMenuItem(ScreenRoute.POS_TERMINAL, "Main POS Kasir", "Buka layar transaksi penjualan pos", Icons.Default.PointOfSale, "hub_pos_btn"),
            DashboardMenuItem(ScreenRoute.PRODUCTS, "Kelola Produk", "Atur barang, barcode & stock opname", Icons.Default.Inventory, "hub_products_btn"),
            DashboardMenuItem(ScreenRoute.PARTNERS, "Member & Supplier", "Kelola data pelanggan dan pasok pabrik", Icons.Default.Group, "hub_partners_btn"),
            DashboardMenuItem(ScreenRoute.SHIFTS, "Shift & Kas", "Buka/Tutup laci keuangan kasir", Icons.Default.MeetingRoom, "hub_shifts_btn"),
            DashboardMenuItem(ScreenRoute.REPORTS, "Laporan Keuangan", "Buku omzet harian, laba rugi & grafik", Icons.Default.Assessment, "hub_reports_btn"),
            DashboardMenuItem(ScreenRoute.SETTINGS, "Pengaturan POS", "Atur pajak PPN, profil toko & printer", Icons.Default.Settings, "hub_settings_btn")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.storeName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimary)
                        Text(viewModel.storeAddress, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.8f))
                    }
                },
                actions = {
                    // Lock App Manual Button
                    IconButton(onClick = { viewModel.lockApp() }, modifier = Modifier.testTag("manual_lock_app_btn")) {
                        Icon(Icons.Default.Lock, "Lock System", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("logout_applet_btn")) {
                        Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.onPrimary)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cashier User Greeting Card banner
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "User",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selamat Bekerja, ${user.name}!", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Hak Akses Jabatan: ${user.role} | Laci Kasir: ${if (viewModel.activeShift != null) "BUKA" else "TERKUNCI / TUTUP"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                    }
                }
            }

            // Quick live warning / indicators row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Low inventory stats indicator
                val alertCount = lowStockProducts.size
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateTo(ScreenRoute.PRODUCTS) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (alertCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha=0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Limit Stok Menipis", fontSize = 10.sp, color = Color.Gray)
                        Text("$alertCount Item Awas", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (alertCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }

                // Daily transaction counts
                val todaySalesVal = transactions.sumOf { it.totalAmount }
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .clickable { onNavigateTo(ScreenRoute.REPORTS) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Sirkulasi Omzet Kas", fontSize = 10.sp, color = Color.Gray)
                        Text("Rp${String.format("%,.0f", todaySalesVal)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Text("Pilihan Menu Utama Aplikasi:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)

            // Grid Dashboard links
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        onClick = {
                            // Verify permission level for ADMIN ONLY tasks
                            if (item.route == ScreenRoute.REPORTS && user.role != "Admin" && user.role != "Supervisor") {
                                Toast.makeText(context, "Akses Laporan khusus untuk level Admin / Supervisor!", Toast.LENGTH_SHORT).show()
                            } else if (item.route == ScreenRoute.SETTINGS && user.role != "Admin") {
                                Toast.makeText(context, "Akses Pengaturan khusus untuk level Admin!", Toast.LENGTH_SHORT).show()
                            } else {
                                onNavigateTo(item.route)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag(item.testTag),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(item.icon, item.title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, lineHeight = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// Dialog PIN Secure Auto-lock screen saver
@Composable
fun SystemPinLockDialog(
    cashierName: String,
    onUnlock: (String) -> Boolean
) {
    var rawInputPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { /* forces unlock code entry */ }) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .padding(14.dp),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.ScreenLockPortrait, "Screen Locked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                
                Text("Sistem POS Dikunci", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Layar siaga idle aktif. Masukkan PIN Anda ($cashierName) untuk memulihkan register:",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )

                if (hasError) {
                    Text("Kode PIN salah. Coba lagi!", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // visual dots
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(4) { idx ->
                        val active = idx < rawInputPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                // keypad
                Column(
                    modifier = Modifier.width(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "OK")
                    )

                    rows.forEach { r ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            r.forEach { c ->
                                Button(
                                    onClick = {
                                        when (c) {
                                            "C" -> if (rawInputPin.isNotEmpty()) rawInputPin = rawInputPin.dropLast(1)
                                            "OK" -> {
                                                val success = onUnlock(rawInputPin)
                                                hasError = !success
                                                if (success) rawInputPin = ""
                                            }
                                            else -> {
                                                if (rawInputPin.length < 4) {
                                                    rawInputPin += c
                                                }
                                                if (rawInputPin.length == 4) {
                                                    val success = onUnlock(rawInputPin)
                                                    hasError = !success
                                                    if (success) rawInputPin = ""
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (c == "OK") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (c == "OK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.2f)
                                        .testTag("lock_key_$c")
                                ) {
                                    Text(c, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
