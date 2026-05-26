@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
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

@Composable
fun ProductScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.productsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(0) } // 0: Semua
    
    var showEditDialog by remember { mutableStateOf<Product?>(null) } // if ID=0L is new
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showMutationDialog by remember { mutableStateOf<Product?>(null) }
    
    val filtered = remember(products, searchQuery, selectedCategoryId) {
        products.filter { prod ->
            val matchText = prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true) ||
                    prod.barcode.contains(searchQuery)
            val matchCategory = selectedCategoryId == 0 || prod.categoryId == selectedCategoryId
            matchText && matchCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Produk & Stok", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("prod_back_btn")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryDialog = true }, modifier = Modifier.testTag("manage_category_btn")) {
                        Icon(Icons.Default.Category, "Saring Kategori", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(
                        onClick = { showEditDialog = Product(name = "", sku = "", barcode = "") },
                        modifier = Modifier.testTag("add_product_btn")
                    ) {
                        Icon(Icons.Default.Add, "Tambah Baru", tint = MaterialTheme.colorScheme.onPrimary)
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
            // Search bars & Category Chips Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari barang dengan Nama, Barcode atau SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, "Cari") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .testTag("product_mgmt_search_input")
            )
            
            // Stock indicators alerts panel
            val lowStockCount = products.count { it.stock <= it.minStock }
            if (lowStockCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha=0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, "Stock Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Awas: Ada $lowStockCount produk dengan stok menipis (hampir habis)!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Products list
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Belum ada data produk.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filtered) { prod ->
                        val isAlert = prod.stock <= prod.minStock
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha=0.08f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "SKU: ${prod.sku.ifBlank { "None" }} | Kode: ${prod.barcode.ifBlank { "None" }}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Mutasi stock quick actions
                                    IconButton(
                                        onClick = { showMutationDialog = prod },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                        modifier = Modifier.size(36.dp).testTag("quick_mutasi_btn_${prod.id}")
                                    ) {
                                        Icon(Icons.Default.SwapVert, "Mutasi", modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { showEditDialog = prod },
                                        modifier = Modifier.size(36.dp).testTag("edit_product_btn_${prod.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, "Ubah", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Informational row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Prices details
                                    Column {
                                        Text("Harga Jual (Umum)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Rp${String.format("%,.0f", prod.sellingPrice)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Column {
                                        Text("Harga Grosir/Member", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Rp${String.format("%,.0f", prod.grosirPrice)} / Rp${String.format("%,.0f", prod.memberPrice)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Sisa Stok", fontSize = 10.sp, color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${prod.stock} ${prod.unit}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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

    // Modal Room: Create/Update Product Box
    if (showEditDialog != null) {
        ProductEditDialog(
            product = showEditDialog!!,
            categories = categories,
            onSave = { prod ->
                viewModel.saveProduct(prod)
                showEditDialog = null
                Toast.makeText(context, "Produk berhasil disimpan!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { prod ->
                viewModel.deleteProduct(prod)
                showEditDialog = null
                Toast.makeText(context, "Produk dihapus!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showEditDialog = null }
        )
    }

    // Modal Categories
    if (showCategoryDialog) {
        CategoryManagerDialog(
            categories = categories,
            onAdd = { viewModel.addCategory(it) },
            onDelete = { viewModel.deleteCategory(it) },
            onDismiss = { showCategoryDialog = false }
        )
    }

    // Modal Mutations
    if (showMutationDialog != null) {
        StockMutationDialog(
            product = showMutationDialog!!,
            onSave = { delta, reason ->
                viewModel.adjustStock(showMutationDialog!!.id, delta, reason)
                showMutationDialog = null
            },
            onDismiss = { showMutationDialog = null }
        )
    }
}

// Dialog Component: Edit Product Details
@Composable
fun ProductEditDialog(
    product: Product,
    categories: List<Category>,
    onSave: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var sku by remember { mutableStateOf(product.sku) }
    var barcode by remember { mutableStateOf(product.barcode) }
    var unit by remember { mutableStateOf(product.unit) }
    var purchasePriceText by remember { mutableStateOf(product.purchasePrice.toInt().toString()) }
    var sellingPriceText by remember { mutableStateOf(product.sellingPrice.toInt().toString()) }
    var memberPriceText by remember { mutableStateOf(product.memberPrice.toInt().toString()) }
    var grosirPriceText by remember { mutableStateOf(product.grosirPrice.toInt().toString()) }
    var stockText by remember { mutableStateOf(product.stock.toString()) }
    var minStockText by remember { mutableStateOf(product.minStock.toString()) }
    var categoryId by remember { mutableStateOf(product.categoryId) }
    var variantName by remember { mutableStateOf(product.variantName) }
    
    var expandDrop by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (product.id == 0L) "Tambah Produk Baru" else "Ubah Detail Produk",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Barang") }, modifier = Modifier.fillMaxWidth().testTag("add_prod_name_input"))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            modifier = Modifier.weight(1f).testTag("add_prod_barcode_input"),
                            trailingIcon = {
                                IconButton(onClick = { barcode = "${(100000000000..999999999999).random()}" }) {
                                    Icon(Icons.Default.Refresh, "Auto SKU")
                                }
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Custom dropdown for categories selection
                        Box(modifier = Modifier.weight(1f)) {
                            val activeCat = categories.find { it.id == categoryId }?.name ?: "Saring"
                            OutlinedButton(
                                onClick = { expandDrop = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("Kat: $activeCat")
                            }
                            DropdownMenu(expanded = expandDrop, onDismissRequest = { expandDrop = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = { categoryId = cat.id; expandDrop = false }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Satuan (Pcs/Kg)") }, modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = purchasePriceText, onValueChange = { purchasePriceText = it }, label = { Text("Harga Modal (Beli)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = sellingPriceText, onValueChange = { sellingPriceText = it }, label = { Text("Harga Retail (Jual)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f).testTag("add_prod_sell_price"))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = memberPriceText, onValueChange = { memberPriceText = it }, label = { Text("Harga Member (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = grosirPriceText, onValueChange = { grosirPriceText = it }, label = { Text("Harga Grosir (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = stockText, onValueChange = { stockText = it }, label = { Text("Saldo Stok") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = product.id == 0L, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = minStockText, onValueChange = { minStockText = it }, label = { Text("Alarm Minimum") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }

                    OutlinedTextField(value = variantName, onValueChange = { variantName = it }, label = { Text("Varian Info (contoh: Merah, L, Dingin)") }, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (product.id != 0L) {
                        IconButton(onClick = { onDelete(product) }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) { Text("Batal") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val finalProd = product.copy(
                                        name = name,
                                        sku = sku,
                                        barcode = barcode,
                                        unit = unit,
                                        purchasePrice = purchasePriceText.toDoubleOrNull() ?: 0.0,
                                        sellingPrice = sellingPriceText.toDoubleOrNull() ?: 0.0,
                                        memberPrice = memberPriceText.toDoubleOrNull() ?: 0.0,
                                        grosirPrice = grosirPriceText.toDoubleOrNull() ?: 0.0,
                                        stock = stockText.toIntOrNull() ?: 0,
                                        minStock = minStockText.toIntOrNull() ?: 5,
                                        categoryId = categoryId,
                                        variantName = variantName
                                    )
                                    onSave(finalProd)
                                }
                            },
                            modifier = Modifier.testTag("save_product_mgmt_btn")
                        ) {
                            Text("Simpan Produk")
                        }
                    }
                }
            }
        }
    }
}

// Dialog Component: Category Manager
@Composable
fun CategoryManagerDialog(
    categories: List<Category>,
    onAdd: (String) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Daftar Kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Kategori baru") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onAdd(textInput)
                                textInput = ""
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Default.Add, "Add Category")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDelete(cat) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Selesai") }
                }
            }
        }
    }
}

// Dialog Component: Stock manual mutation (Stok Masuk / Keluar / Opname)
@Composable
fun StockMutationDialog(
    product: Product,
    onSave: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rawDelta by remember { mutableStateOf("") }
    var isPemasukan by remember { mutableStateOf(true) } // true = Tambah (+), false = Kurangi (-)
    var reason by remember { mutableStateOf("Re-stock Supplier") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Mutasi Logistik Stok", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(product.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    // Type selector
                    Button(
                        onClick = { isPemasukan = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPemasukan) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPemasukan) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stok Masuk (+)")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { isPemasukan = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isPemasukan) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isPemasukan) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Stok Keluar (-)")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = rawDelta,
                    onValueChange = { rawDelta = it },
                    label = { Text("Jumlah Nilai Fisik Barang") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Keterangan Mutasi:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                // quick descriptions
                val reasons = listOf("Re-stock Supplier", "Barang Rusak", "Stok Opname", "Kompensasi")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    reasons.forEach { r ->
                        Box(
                            modifier = Modifier
                                .background(if (reason == r) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .clickable { reason = r }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(r, fontSize = 10.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Keterangan Tambahan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = rawDelta.toIntOrNull() ?: 0
                            if (qty > 0) {
                                val adjustment = if (isPemasukan) qty else -qty
                                onSave(adjustment, reason)
                            }
                        }
                    ) {
                        Text("Simpan Mutasi")
                    }
                }
            }
        }
    }
}



