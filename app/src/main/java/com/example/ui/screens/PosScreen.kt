@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.PosViewModel
import com.example.ui.CartSummary
import kotlinx.coroutines.launch

@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Check screen configurations
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 720
    
    val products by viewModel.productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val customers by viewModel.customersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var activeTab by remember { mutableStateOf(0) } // 0: Katalog, 1: Keranjang (on Mobile)
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerSelectDialog by remember { mutableStateOf(false) }
    var showDiscountItemDialog by remember { mutableStateOf<Product?>(null) }
    var showReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    
    // Auto-lock checker hook
    LaunchedEffect(Unit) {
        if (viewModel.currentUser == null) {
            onNavigateBack()
        }
    }
    
    // Render
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PointOfSale, "POS", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Menu POS Utama", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(viewModel.storeName, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("pos_back_button")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    // Selected customer indicator
                    if (viewModel.selectedCustomer != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .clickable { showCustomerSelectDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Stars, "Member ID", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${viewModel.selectedCustomer?.name} (${viewModel.selectedCustomer?.memberLevel})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = { showCustomerSelectDialog = true },
                            modifier = Modifier.testTag("select_customer_header_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, "Pilih Pelanggan", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    
                    TextButton(
                        onClick = { viewModel.clearCart() },
                        modifier = Modifier.testTag("clear_cart_btn")
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isTablet) {
                // Large tablet layout: side-by-side
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        CatalogSection(
                            viewModel = viewModel,
                            products = products,
                            categories = categories
                        )
                    }
                    Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Box(modifier = Modifier.weight(0.8f)) {
                        CartSection(
                            viewModel = viewModel,
                            onCheckoutClick = { showCheckoutDialog = true },
                            onSelectCustomerClick = { showCustomerSelectDialog = true },
                            onEditItemDiscountClick = { showDiscountItemDialog = it }
                        )
                    }
                }
            } else {
                // Mobile compact layout: Tab switcher
                Column(modifier = Modifier.fillMaxSize()) {
                    val summary = viewModel.getCartSummary()
                    TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth()) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Katalog Produk (${products.size})") }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { 
                                Text(
                                    text = if (viewModel.cartItems.value.isEmpty()) "Keranjang" else "Keranjang (${viewModel.cartItems.value.size})",
                                    fontWeight = if (viewModel.cartItems.value.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (viewModel.cartItems.value.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (activeTab == 0) {
                            CatalogSection(
                                viewModel = viewModel,
                                products = products,
                                categories = categories
                            )
                        } else {
                            CartSection(
                                viewModel = viewModel,
                                onCheckoutClick = { showCheckoutDialog = true },
                                onSelectCustomerClick = { showCustomerSelectDialog = true },
                                onEditItemDiscountClick = { showDiscountItemDialog = it }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Select Customer
    if (showCustomerSelectDialog) {
        CustomerSelectDialog(
            customers = customers,
            selectedCustomer = viewModel.selectedCustomer,
            onSelect = { viewModel.selectedCustomer = it; showCustomerSelectDialog = false },
            onDismiss = { showCustomerSelectDialog = false }
        )
    }

    // Modal Dialog: Edit Single Item Discount
    if (showDiscountItemDialog != null) {
        DiscountMultiplierDialog(
            product = showDiscountItemDialog!!,
            currentDiscount = viewModel.cartItemDiscounts.value[showDiscountItemDialog!!.id] ?: 0.0,
            onSave = { dev ->
                viewModel.setItemDiscount(showDiscountItemDialog!!.id, dev)
                showDiscountItemDialog = null
            },
            onDismiss = { showDiscountItemDialog = null }
        )
    }

    // Modal Dialog: Active Payment Checkout
    if (showCheckoutDialog) {
        PaymentCheckoutDialog(
            viewModel = viewModel,
            onComplete = { tx ->
                showReceiptDialog = tx
                showCheckoutDialog = false
            },
            onDismiss = { showCheckoutDialog = false }
        )
    }

    // Modal Dialog: Completed Thermal Printer Invoice Preview
    if (showReceiptDialog != null) {
        ReceiptPreviewDialog(
            transaction = showReceiptDialog!!,
            viewModel = viewModel,
            onDismiss = { showReceiptDialog = null }
        )
    }
}

// Sub-Section: Catalog
@Composable
fun CatalogSection(
    viewModel: PosViewModel,
    products: List<Product>,
    categories: List<Category>
) {
    val context = LocalContext.current
    var scanMockActive by remember { mutableStateOf(false) }
    
    // Filter logic
    val filteredProducts = remember(products, viewModel.productSearchQuery, viewModel.posSelectedCategoryId) {
        products.filter { prod ->
            val matchName = prod.name.contains(viewModel.productSearchQuery, ignoreCase = true) ||
                    prod.barcode.contains(viewModel.productSearchQuery) ||
                    prod.sku.contains(viewModel.productSearchQuery, ignoreCase = true)
            val matchCategory = viewModel.posSelectedCategoryId == 0 || prod.categoryId == viewModel.posSelectedCategoryId
            matchName && matchCategory
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))) {
        // Search & Mock Barcode Area
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = viewModel.productSearchQuery,
                onValueChange = { viewModel.productSearchQuery = it },
                placeholder = { Text("Cari barang, barcode, SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                singleLine = true,
                trailingIcon = {
                    if (viewModel.productSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.productSearchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f).testTag("pos_search_input")
            )
            
            // Barcode camera scanner simulator
            IconButton(
                onClick = { scanMockActive = !scanMockActive },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (scanMockActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (scanMockActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.size(54.dp).testTag("pos_scan_barcode_btn")
            ) {
                Icon(Icons.Default.QrCodeScanner, "Scan Camera")
            }
        }

        // Camera scan panel simulation
        if (scanMockActive) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Camera, "Camera Sync", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulasi Kamera Barcode Scanner (ZXing)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Kamera aktif mengawasi barcode. Ketuk barang cepat di bawah untuk mensimulasikan sorotan laser kamera fisik:",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Quick simulation buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                val barcode = "89686016074" // Indomie 
                                val prod = products.find { it.barcode == barcode }
                                if (prod != null) {
                                    viewModel.addToCart(prod)
                                    Toast.makeText(context, "Barcode: Indomie Goreng dideteksi!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("scan_indomie_mock")
                        ) {
                            Text("Scan Indomie", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val barcode = "8992222050021" // Teh Pucuk
                                val prod = products.find { it.barcode == barcode }
                                if (prod != null) {
                                    viewModel.addToCart(prod)
                                    Toast.makeText(context, "Barcode: Teh Pucuk dideteksi!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("scan_teh_mock")
                        ) {
                            Text("Scan Teh Pucuk", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Kategori tags row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = viewModel.posSelectedCategoryId == 0,
                    onClick = { viewModel.posSelectedCategoryId = 0 },
                    label = { Text("Semua") }
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = viewModel.posSelectedCategoryId == cat.id,
                    onClick = { viewModel.posSelectedCategoryId = cat.id },
                    label = { Text(cat.name) }
                )
            }
        }

        // Items Grid
        if (filteredProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, "Empty", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Produk tidak ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts) { prod ->
                    ProductGridCard(
                        product = prod,
                        sellingPrice = viewModel.getProductSellingPrice(prod),
                        hasDiscountPrice = viewModel.getProductSellingPrice(prod) < prod.sellingPrice,
                        onClick = { viewModel.addToCart(prod) }
                    )
                }
            }
        }
    }
}

// Sub-Component: Product grid item card
@Composable
fun ProductGridCard(
    product: Product,
    sellingPrice: Double,
    hasDiscountPrice: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("pos_product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (product.stock <= product.minStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Category tag or status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val stockText = if (product.stock == 0) "Habis" else "${product.stock} ${product.unit}"
                val color = if (product.stock <= product.minStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(
                    text = stockText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.height(34.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "SKU: ${product.sku.ifEmpty { "None" }}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Prices
            Column {
                if (hasDiscountPrice) {
                    Text(
                        text = "Rp${String.format("%,.0f", product.sellingPrice)}",
                        fontSize = 11.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "Rp${String.format("%,.0f", sellingPrice)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (hasDiscountPrice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Sub-Section: Shopping Cart Desk UI
@Composable
fun CartSection(
    viewModel: PosViewModel,
    onCheckoutClick: () -> Unit,
    onSelectCustomerClick: () -> Unit,
    onEditItemDiscountClick: (Product) -> Unit
) {
    val items = viewModel.cartItems.value
    val summary = viewModel.getCartSummary()
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // active cashier details banner
        Row(
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, "Cashier", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Kasir Aktif: ${viewModel.currentUser?.name ?: "Guest"} (${viewModel.currentUser?.role ?: "-"})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageOf = Icons.Default.ShoppingCart,
                        contentDescription = "Empty Basket",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Keranjang Belanja Kosong",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Silakan sentuh barang dalam katalog produk untuk mulai checkout.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Cart List Scroll
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(10.dp)) {
                items(items) { (prod, qty) ->
                    val unitPrice = viewModel.getProductSellingPrice(prod)
                    val discount = viewModel.cartItemDiscounts.value[prod.id] ?: 0.0
                    val subTotal = (unitPrice - discount) * qty
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rp${String.format("%,.0f", unitPrice)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (discount > 0.0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(Potongan Rp${String.format("%,.0f", discount)})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            
                            IconButton(
                                onClick = { viewModel.removeFromCart(prod) },
                                modifier = Modifier.size(24.dp).testTag("remove_from_cart_${prod.id}")
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            // Discounts and custom modifiers
                            Row {
                                TextButton(
                                    onClick = { onEditItemDiscountClick(prod) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp).testTag("discount_item_btn_${prod.id}")
                                ) {
                                    Icon(Icons.Default.Discount, "Diskon Item", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Diskon per unit", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            // Quantity selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.updateCartQty(prod, qty - 1) },
                                    modifier = Modifier.size(28.dp).testTag("qty_minus_${prod.id}")
                                ) {
                                    Icon(Icons.Default.Remove, "Less", modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = "$qty",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(28.dp)
                                )
                                IconButton(
                                    onClick = { viewModel.updateCartQty(prod, qty + 1) },
                                    modifier = Modifier.size(28.dp).testTag("qty_plus_${prod.id}")
                                ) {
                                    Icon(Icons.Default.Add, "More", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary calculations panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Loyalty selector link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectCustomerClick() }
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, "Customer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = viewModel.selectedCustomer?.let { "Pelanggan: ${it.name}" } ?: "Pilih Pelanggan / Member",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(Icons.Default.ChevronRight, "Chevron", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                
                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                // Subtotal
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rp${String.format("%,.0f", summary.subTotal)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Discounts
                if (summary.discountAmount > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon Toko", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        Text("-Rp${String.format("%,.0f", summary.discountAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
                
                // PPN
                if (viewModel.isTaxEnabled) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("PPN (${viewModel.taxPercentage}%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rp${String.format("%,.0f", summary.taxAmount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Loyalty points reward estimation
                if (summary.pointsEarned > 0) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimasi Poin Diperoleh", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text("+${summary.pointsEarned} Poin", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Tagihan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Rp${String.format("%,.0f", summary.totalAmount)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (viewModel.activeShift == null) {
                            Toast.makeText(context, "Buka shift kasir terlebih dahulu di menu Shift!", Toast.LENGTH_LONG).show()
                        } else if (items.isNotEmpty()) {
                            onCheckoutClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pos_checkout_button"),
                    enabled = items.isNotEmpty()
                ) {
                    Icon(Icons.Default.Payment, "Payment")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Proses Pembayaran (F10)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// Dialog: Choose Customer
@Composable
fun CustomerSelectDialog(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelect: (Customer?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = customers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Daftar Pelanggan (Member)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cari nama atau telepon...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Group, "Non-Member", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Pelanggan Umum (Tanpa Member)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    items(filtered) { cust ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCustomer?.id == cust.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(cust) }.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Telp: ${cust.phone} | Poin: ${cust.points}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = cust.memberLevel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                }
            }
        }
    }
}

// Dialog: Discount Multiplier
@Composable
fun DiscountMultiplierDialog(
    product: Product,
    currentDiscount: Double,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var rawText by remember { mutableStateOf(if (currentDiscount > 0) currentDiscount.toInt().toString() else "") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Diskon Item per Unit", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(product.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Potongan Nilai Rupiah (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("discount_price_input")
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        onClick = {
                            val discVal = rawText.toDoubleOrNull() ?: 0.0
                            onSave(discVal)
                        },
                        modifier = Modifier.testTag("save_discount_btn")
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

// Dialog: Checkout Payment Details
@Composable
fun PaymentCheckoutDialog(
    viewModel: PosViewModel,
    onComplete: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val summary = viewModel.getCartSummary()
    var paymentMethod by remember { mutableStateOf("TUNAI") } // TUNAI, QRIS, BANK_TRANSFER, DEBIT_KREDIT
    var cashPaidText by remember { mutableStateOf("") }
    var voucherCodeInput by remember { mutableStateOf("") }
    var splitAmountText by remember { mutableStateOf("") }
    var customerNotes by remember { mutableStateOf("") }
    var isDebtPayment by remember { mutableStateOf(false) } // piutang
    
    val totalToPay = (summary.totalAmount - viewModel.activeVoucherDiscount).coerceAtLeast(0.0)
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 680.dp).padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Konfirmasi Pembayaran Kasir",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Left Column: Payment Modes Toggles
                    Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pilih Jalur Bayar:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val methods = listOf(
                            Pair("TUNAI", Icons.Default.Money),
                            Pair("QRIS", Icons.Default.QrCode),
                            Pair("BANK_TRANSFER", Icons.Default.AccountBalance),
                            Pair("DEBIT_KREDIT", Icons.Default.CreditCard)
                        )
                        methods.forEach { (method, icon) ->
                            val selected = paymentMethod == method && !isDebtPayment
                            Card(
                                onClick = {
                                    paymentMethod = method
                                    isDebtPayment = false
                                    if (method != "TUNAI") cashPaidText = totalToPay.toInt().toString()
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth().height(42.dp).border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            ) {
                                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, method, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = when(method){
                                            "TUNAI" -> "Tunai (Cash)"
                                            "QRIS" -> "QRIS Digital Mandiri"
                                            "BANK_TRANSFER" -> "Transfer Bank"
                                            else -> "Debit / Kredit Kartu"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Debt Option
                        val canDebt = viewModel.selectedCustomer != null
                        val debtSelected = isDebtPayment
                        Card(
                            onClick = {
                                if (canDebt) {
                                    isDebtPayment = true
                                    paymentMethod = "SPLIT"
                                }
                            },
                            enabled = canDebt,
                            colors = CardDefaults.cardColors(
                                containerColor = if (debtSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface.copy(alpha = if (canDebt) 1f else 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth().height(42.dp).border(1.dp, if (debtSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Book, "Debt", tint = if (debtSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Catat Piutang (Bon)", fontSize = 12.sp, fontWeight = if (debtSelected) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }

                    // Right Column: Value Entries
                    Column(modifier = Modifier.weight(1.3f)) {
                        // Billing summary card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Belanja:", fontSize = 11.sp)
                                    Text("Rp${String.format("%,.0f", summary.totalAmount)}", fontSize = 11.sp)
                                }
                                if (viewModel.activeVoucherDiscount > 0) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Voucher:", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                        Text("-Rp${String.format("%,.0f", viewModel.activeVoucherDiscount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("HARUS DIBAYAR:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text("Rp${String.format("%,.0f", totalToPay)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        // Vouchers entry
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = voucherCodeInput,
                                onValueChange = { voucherCodeInput = it },
                                placeholder = { Text("Kode kupon/voucher") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(48.dp),
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if(voucherCodeInput.uppercase() == "BONUS10") {
                                        viewModel.activeVoucherCode = "BONUS10"
                                        viewModel.activeVoucherDiscount = 10000.0
                                    } else {
                                        viewModel.activeVoucherCode = ""
                                        viewModel.activeVoucherDiscount = 0.0
                                    }
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Cek", fontSize = 11.sp)
                            }
                        }

                        if (isDebtPayment) {
                            Text(
                                text = "Menyimpan tagihan Rp ${String.format("%,.0f", totalToPay)} ke rekap piutang: ${viewModel.selectedCustomer?.name}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else if (paymentMethod == "TUNAI") {
                            // Quick Cash suggestion pills
                            Text("Pilihan Uang Cepat:", fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                            val sug = listOf(
                                totalToPay.toInt(),
                                5000 * ((totalToPay.toInt() + 4999) / 5000), // round up to nearest 5k
                                10000 * ((totalToPay.toInt() + 9999) / 10000), // round to 10k
                                50000,
                                100000
                            ).distinct().filter { it >= totalToPay.toInt() }.take(4)
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                sug.forEach { amt ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { cashPaidText = amt.toString() }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text("Rp${String.format("%,.0f", amt.toDouble())}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = cashPaidText,
                                onValueChange = { cashPaidText = it },
                                label = { Text("Uang Tunai Diterima (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("cash_payment_input")
                            )

                            // Show Change (Kembalian)
                            val cashNum = cashPaidText.toDoubleOrNull() ?: 0.0
                            val changeValue = (cashNum - totalToPay).coerceAtLeast(0.0)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Kembalian Otomatis:", fontSize = 10.sp)
                                    Text(
                                        text = "Rp${String.format("%,.0f", changeValue)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (changeValue > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else if (paymentMethod == "QRIS") {
                            // QRIS Simulator QR code canvas
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Pemindai QRIS Sentosa POS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Canvas(modifier = Modifier.size(110.dp)) {
                                    drawRect(Color.White)
                                    // Custom visual pattern simulating QR code
                                    drawRect(Color.Black, size = androidx.compose.ui.geometry.Size(30f, 30f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(size.width - 30f, 0f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                                    drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - 30f), size = androidx.compose.ui.geometry.Size(30f, 30f))
                                    
                                    // small grid dots
                                    for(x in 10 .. size.width.toInt() - 10 step 15) {
                                        for(y in 10 .. size.height.toInt() - 10 step 15) {
                                            if((x+y) % 2 == 0) {
                                                drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()), size = androidx.compose.ui.geometry.Size(8f, 8f))
                                            }
                                        }
                                    }
                                }
                                Text("Status: MENUNGGU PEMBAYARAN...", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            // Transfer / Debit generic inputs
                            Text("Isi Detail Mesin/Struk EDC jika diperlukan:", fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
                            OutlinedTextField(
                                value = customerNotes,
                                onValueChange = { customerNotes = it },
                                placeholder = { Text("No. kartu / nomor referensi transfer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val paidVal = if (isDebtPayment) 0.0 else if (paymentMethod == "TUNAI") cashPaidText.toDoubleOrNull() ?: 0.0 else totalToPay
                            viewModel.executeCheckout(
                                paymentMethod = paymentMethod,
                                paidAmount = paidVal,
                                isDebt = isDebtPayment,
                                onSuccess = { tx -> onComplete(tx) },
                                onFailure = { err -> 
                                    // Display failure
                                }
                            )
                        },
                        modifier = Modifier.height(44.dp).testTag("confirm_print_and_checkout_btn")
                    ) {
                        Icon(Icons.Default.Check, "Confirm")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Konfirmasi Selesai & Cetak", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dialog: Receipt Thermal Preview & Sending Mock
@Composable
fun ReceiptPreviewDialog(
    transaction: Transaction,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val items = remember { mutableStateOf<List<TransactionItem>>(emptyList()) }
    val context = LocalContext.current
    
    // Read transaction items asynchronously
    LaunchedEffect(transaction.transactionId) {
        val db = (context.applicationContext as com.example.MainApplication).database
        items.value = db.posDao().getItemsForTransaction(transaction.transactionId)
    }

    val receiptString = viewModel.buildInvoiceString(transaction, items.value)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp).heightIn(max = 620.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Struk Kasir (Thermal Receipt Preview)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable receipt box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                        .border(1.dp, Color.LightGray)
                        .padding(14.dp)
                ) {
                    LazyColumn {
                        item {
                            Text(
                                text = receiptString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Printer Config tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Konfigurasi Printer: ${viewModel.activePrinterSize}mm thermal",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "USB / Bluetooth",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: print, share WhatsApp, share Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            // Mock Whatsapp sending
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, receiptString)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Kirim Struk via WA"))
                        },
                        modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)).testTag("share_wa_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, "WhatsApp", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bagikan WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Mencetak ke printer thermal ${viewModel.activePrinterSize}mm...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.2f).testTag("printer_thermal_print_btn")
                    ) {
                        Icon(Icons.Default.Print, "Print")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Kasir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outlineVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup Dialog")
                }
            }
        }
    }
}

// Small utilities to prevent compiler errors

