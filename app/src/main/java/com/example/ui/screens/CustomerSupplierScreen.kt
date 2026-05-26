@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import androidx.compose.ui.graphics.Color

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.PosViewModel

@Composable
fun CustomerSupplierScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customersFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val suppliers by viewModel.suppliersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var isCustomerTab by remember { mutableStateOf(true) } // true: Pelanggan, false: Supplier
    var searchQuery by remember { mutableStateOf("") }
    
    var showCustomerEditDialog by remember { mutableStateOf<Customer?>(null) }
    var showSupplierEditDialog by remember { mutableStateOf<Supplier?>(null) }
    var showDigitalCardDialog by remember { mutableStateOf<Customer?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relasi Bisnis", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("connections_back_btn")) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isCustomerTab) {
                                showCustomerEditDialog = Customer(name = "", phone = "", cardCode = "MEM-00${customers.size + 1}")
                            } else {
                                showSupplierEditDialog = Supplier(name = "", phone = "")
                            }
                        },
                        modifier = Modifier.testTag("add_partner_btn")
                    ) {
                        Icon(Icons.Default.Add, "Tambah", tint = MaterialTheme.colorScheme.onPrimary)
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
            // Tab row toggler
            TabRow(selectedTabIndex = if (isCustomerTab) 0 else 1) {
                Tab(selected = isCustomerTab, onClick = { isCustomerTab = true; searchQuery = "" }, text = { Text("Pelanggan (CRM)") })
                Tab(selected = !isCustomerTab, onClick = { isCustomerTab = false; searchQuery = "" }, text = { Text("Suppliers (Pabrik)") })
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (isCustomerTab) "Cari pelanggan / telepon..." else "Cari supplier / CP...") },
                leadingIcon = { Icon(Icons.Default.Search, "Cari") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            )

            if (isCustomerTab) {
                // Customer List CRM View
                val filtered = customers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Daftar pelanggan kosong.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(filtered) { cust ->
                            Card(
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
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("Telp: ${cust.phone} | Poin: ${cust.points}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Row {
                                            // Show membership card button
                                            IconButton(
                                                onClick = { showDigitalCardDialog = cust },
                                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                                modifier = Modifier.size(36.dp).testTag("crm_show_card_btn_${cust.id}")
                                            ) {
                                                Icon(Icons.Default.QrCode, "Debit Card", modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { showCustomerEditDialog = cust },
                                                modifier = Modifier.size(36.dp).testTag("crm_edit_customer_btn_${cust.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, "Ubah", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Level: ${cust.memberLevel}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                        )

                                        if (cust.debtBalance > 0.0) {
                                            Text(
                                                text = "Hutang: Rp${String.format("%,.0f", cust.debtBalance)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha=0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        } else {
                                            Text("Lunas / Bersih", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Supplier List View
                val filtered = suppliers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery) }
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("Daftar supplier kosong.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(filtered) { sup ->
                            Card(
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
                                        Column {
                                            Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text("CP: ${sup.contactPerson} | Telp: ${sup.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        IconButton(
                                            onClick = { showSupplierEditDialog = sup },
                                            modifier = Modifier.size(36.dp).testTag("supplier_edit_btn_${sup.id}")
                                        ) {
                                            Icon(Icons.Default.Edit, "Ubah", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Alamat: ${sup.address.ifBlank { "None" }}", fontSize = 11.sp, color = Color.Gray)
                                        if (sup.debtBalance > 0.0) {
                                            Text(
                                                text = "Tagihan Toko: Rp${String.format("%,.0f", sup.debtBalance)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha=0.4f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        } else {
                                            Text("Selesai / Lunas", fontSize = 11.sp, color = Color(0xFF2E7D32))
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

    // CRM Customer dialog Edit Box
    if (showCustomerEditDialog != null) {
        CustomerEditDialog(
            customer = showCustomerEditDialog!!,
            onSave = { cust ->
                viewModel.saveCustomer(cust)
                showCustomerEditDialog = null
                Toast.makeText(context, "Data pelanggan tersimpan!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { cust ->
                viewModel.deleteCustomer(cust)
                showCustomerEditDialog = null
                Toast.makeText(context, "Pelanggan dihapus!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCustomerEditDialog = null }
        )
    }

    // Supplier dialog Edit Box
    if (showSupplierEditDialog != null) {
        SupplierEditDialog(
            supplier = showSupplierEditDialog!!,
            onSave = { sup ->
                viewModel.saveSupplier(sup)
                showSupplierEditDialog = null
                Toast.makeText(context, "Data supplier tersimpan!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { sup ->
                viewModel.deleteSupplier(sup)
                showSupplierEditDialog = null
                Toast.makeText(context, "Supplier dihapus!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSupplierEditDialog = null }
        )
    }

    // Digital Member representation box Dialog
    if (showDigitalCardDialog != null) {
        DigitalMemberCardDialog(
            customer = showDigitalCardDialog!!,
            onDismiss = { showDigitalCardDialog = null }
        )
    }
}

// Dialog CRM Customer Edit
@Composable
fun CustomerEditDialog(
    customer: Customer,
    onSave: (Customer) -> Unit,
    onDelete: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var email by remember { mutableStateOf(customer.email) }
    var cardCode by remember { mutableStateOf(customer.cardCode) }
    var debtText by remember { mutableStateOf(customer.debtBalance.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (customer.id == 0L) "Tambah Member Baru" else "Edit Profil Member",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Pelanggan") }, modifier = Modifier.fillMaxWidth().testTag("crm_name_input"))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon / WA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cardCode, onValueChange = { cardCode = it }, label = { Text("Kode Member (Digital Barcode)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = debtText, onValueChange = { debtText = it }, label = { Text("Catatan Piutang Toko (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (customer.id != 0L) {
                        IconButton(onClick = { onDelete(customer) }) {
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
                                    val finalCust = customer.copy(
                                        name = name,
                                        phone = phone,
                                        email = email,
                                        cardCode = cardCode,
                                        debtBalance = debtText.toDoubleOrNull() ?: 0.0
                                    )
                                    onSave(finalCust)
                                }
                            },
                            modifier = Modifier.testTag("crm_submit_btn")
                        ) {
                            Text("Simpan Member")
                        }
                    }
                }
            }
        }
    }
}

// Dialog Supplier Edit
@Composable
fun SupplierEditDialog(
    supplier: Supplier,
    onSave: (Supplier) -> Unit,
    onDelete: (Supplier) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(supplier.name) }
    var contactPerson by remember { mutableStateOf(supplier.contactPerson) }
    var phone by remember { mutableStateOf(supplier.phone) }
    var address by remember { mutableStateOf(supplier.address) }
    var debtText by remember { mutableStateOf(supplier.debtBalance.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (supplier.id == 0L) "Tambah Supplier Baru" else "Edit Profil Supplier",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Perusahaan Supplier") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("Contact Person") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("No. Telepon / Kantor") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = debtText, onValueChange = { debtText = it }, label = { Text("Catatan Hutang Usaha Toko (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (supplier.id != 0L) {
                        IconButton(onClick = { onDelete(supplier) }) {
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
                                    val finalSup = supplier.copy(
                                        name = name,
                                        contactPerson = contactPerson,
                                        phone = phone,
                                        address = address,
                                        debtBalance = debtText.toDoubleOrNull() ?: 0.0
                                    )
                                    onSave(finalSup)
                                }
                            }
                        ) {
                            Text("Simpan Supplier")
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Digital CRM Card layout
@Composable
fun DigitalMemberCardDialog(
    customer: Customer,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("KARTU MEMBER DIGITAL", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Icon(Icons.Default.CreditCard, "Card Icon", tint = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("KODE: ${customer.cardCode}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))

                Spacer(modifier = Modifier.height(24.dp))

                // Render barcodes mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // lines
                        val widths = listOf(2, 4, 1, 6, 2, 4, 1, 3, 2, 6, 2, 4, 1, 3, 2, 4, 1, 6, 2, 1, 4, 3, 1, 5, 2, 1, 3)
                        widths.forEach { wd ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(wd.dp)
                                    .background(Color.Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Level VIP: ${customer.memberLevel}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Poin: ${customer.points} PTS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer, contentColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesai & Scan")
                }
            }
        }
    }
}
