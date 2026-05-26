package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MainApplication
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PosViewModel(
    application: Application,
    private val repository: PosRepository
) : AndroidViewModel(application) {

    // --- AUTHENTICATION STATE ---
    var currentUser by mutableStateOf<User?>(null)
        private set

    var authError by mutableStateOf<String?>(null)

    var isLocked by mutableStateOf(false)
        private set

    var autoLockTimerMinutes by mutableStateOf(5) // default 5 mins

    // --- POS TRANSACTION STATE ---
    var cartItems = mutableStateOf<List<Pair<Product, Int>>>(emptyList()) // Pair of Product and Quantity
    var cartItemDiscounts = mutableStateOf<Map<Long, Double>>(emptyMap()) // productId -> discount per single unit
    var selectedCustomer by mutableStateOf<Customer?>(null)
    var activeVoucherCode by mutableStateOf("")
    var activeVoucherDiscount by mutableStateOf(0.0)

    // --- POS SEARCH & FILTERS ---
    var productSearchQuery by mutableStateOf("")
    var posSelectedCategoryId by mutableStateOf(0) // 0 means "Semua"

    // --- SHIFT STATE ---
    var activeShift by mutableStateOf<Shift?>(null)
        private set

    // --- PRINTER & CONFIGURATION PREFERENCES ---
    var storeName by mutableStateOf("Sentosa Minimarket")
    var storeAddress by mutableStateOf("Jl. Sudirman No. 45, Jakarta Selatan")
    var storePhone by mutableStateOf("0812-3456-7890")
    var storeNpwp by mutableStateOf("12.345.678.9-012.000")
    var taxPercentage by mutableStateOf(11.0)
    var isTaxEnabled by mutableStateOf(true)
    var activePrinterSize by mutableStateOf(58) // 58 or 80 mm
    var activePrinterType by mutableStateOf("Thermal Bluetooth")
    var isDarkMode by mutableStateOf(false)

    // --- OBSERVABLE DATABASE LISTS ---
    val productsFlow = repository.allProducts
    val categoriesFlow = repository.allCategories
    val customersFlow = repository.allCustomers
    val suppliersFlow = repository.allSuppliers
    val transactionsFlow = repository.allTransactions
    val shiftsFlow = repository.allShifts
    val activityLogsFlow = repository.activityLogs
    val lowStockProductsFlow = repository.lowStockProducts

    init {
        // Observe Store Profile
        viewModelScope.launch {
            repository.storeProfile.collect { profile ->
                profile?.let {
                    storeName = it.name
                    storeAddress = it.address
                    storePhone = it.phone
                    storeNpwp = it.npwp
                    taxPercentage = it.taxPercentage
                    isTaxEnabled = it.isTaxEnabled
                }
            }
        }
        // Observe Current Active Shift
        viewModelScope.launch {
            checkActiveShift()
        }
    }

    suspend fun checkActiveShift() {
        activeShift = repository.getActiveShift()
    }

    // --- LOGIN ACTIONS ---
    fun loginWithPassword(username: String, passwordHash: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.loginWithPassword(username, passwordHash)
            if (user != null) {
                currentUser = user
                isLocked = false
                authError = null
                onSuccess()
            } else {
                authError = "Username atau password salah!"
            }
        }
    }

    fun loginWithPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.loginWithPin(pin)
            if (user != null) {
                currentUser = user
                isLocked = false
                authError = null
                onSuccess()
            } else {
                authError = "PIN salah!"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            currentUser?.let {
                repository.logActivity(it.username, "LOGOUT", "User melakukan logout")
            }
            currentUser = null
            cartItems.value = emptyList()
            selectedCustomer = null
            activeVoucherCode = ""
            activeVoucherDiscount = 0.0
        }
    }

    fun lockApp() {
        isLocked = true
        currentUser?.let {
            viewModelScope.launch {
                repository.logActivity(it.username, "AUTO_LOCK", "Sistem dikunci otomatis/manual")
            }
        }
    }

    fun unlockApp(pin: String): Boolean {
        val user = currentUser
        if (user != null && user.pin == pin) {
            isLocked = false
            return true
        }
        return false
    }

    // --- MASTER CRUD: PRODUCTS ---
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            if (product.id == 0L) {
                repository.addProduct(product, operator)
            } else {
                repository.updateProduct(product, operator)
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.deleteProduct(product, operator)
        }
    }

    fun adjustStock(productId: Long, qty: Int, reason: String) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.adjustStock(productId, qty, reason, operator)
        }
    }

    // --- MASTER CRUD: CATEGORIES ---
    fun addCategory(name: String) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.addCategory(name, operator)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.deleteCategory(category, operator)
        }
    }

    // --- MASTER CRUD: CUSTOMERS ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            if (customer.id == 0L) {
                repository.addCustomer(customer, operator)
            } else {
                repository.updateCustomer(customer, operator)
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.deleteCustomer(customer, operator)
        }
    }

    // --- MASTER CRUD: SUPPLIERS ---
    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            if (supplier.id == 0L) {
                repository.addSupplier(supplier, operator)
            } else {
                repository.updateSupplier(supplier, operator)
            }
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.deleteSupplier(supplier, operator)
        }
    }

    // --- USER / EMPLOYEE MANAGEMENT ---
    fun saveEmployee(user: User) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            if (user.id == 0) {
                repository.addUser(user, operator)
            } else {
                repository.updateUser(user, operator)
            }
        }
    }

    fun deleteEmployee(user: User) {
        viewModelScope.launch {
            val operator = currentUser?.name ?: "Sistem"
            repository.deleteUser(user, operator)
        }
    }

    // --- SHIFT MANAGEMENT ---
    fun openShift(startingCash: Double) {
        viewModelScope.launch {
            val user = currentUser ?: return@launch
            repository.openShift(user.id, user.name, startingCash)
            checkActiveShift()
        }
    }

    fun closeShift(actualCash: Double, notes: String) {
        viewModelScope.launch {
            val shift = activeShift ?: return@launch
            val operator = currentUser?.name ?: "Sistem"
            repository.closeShift(shift.id, actualCash, notes, operator)
            checkActiveShift()
        }
    }

    // --- PETTY CASH ---
    fun recordPettyCash(amount: Double, type: String, description: String) {
        viewModelScope.launch {
            val shift = activeShift ?: return@launch
            val operator = currentUser?.name ?: "Sistem"
            repository.recordPettyCash(shift.id, amount, type, description, operator)
            checkActiveShift() // verify totals updated
        }
    }

    fun getPettyCashForCurrentShift(): Flow<List<PettyCash>> {
        val shiftId = activeShift?.id ?: return flowOf(emptyList())
        return repository.getPettyCashForShift(shiftId)
    }

    // --- POS CART ACTIONS ---
    fun addToCart(product: Product) {
        val existing = cartItems.value.find { it.first.id == product.id }
        if (existing != null) {
            cartItems.value = cartItems.value.map {
                if (it.first.id == product.id) Pair(it.first, it.second + 1) else it
            }
        } else {
            cartItems.value = cartItems.value + Pair(product, 1)
        }
    }

    fun updateCartQty(product: Product, newQty: Int) {
        if (newQty <= 0) {
            removeFromCart(product)
        } else {
            cartItems.value = cartItems.value.map {
                if (it.first.id == product.id) Pair(it.first, newQty) else it
            }
        }
    }

    fun removeFromCart(product: Product) {
        cartItems.value = cartItems.value.filter { it.first.id != product.id }
        cartItemDiscounts.value = cartItemDiscounts.value - product.id
    }

    fun setItemDiscount(productId: Long, discount: Double) {
        cartItemDiscounts.value = cartItemDiscounts.value + (productId to discount)
    }

    fun clearCart() {
        cartItems.value = emptyList()
        cartItemDiscounts.value = emptyMap()
        selectedCustomer = null
        activeVoucherCode = ""
        activeVoucherDiscount = 0.0
    }

    // Apply specific membership prices depending on levels
    fun getProductSellingPrice(product: Product): Double {
        val cust = selectedCustomer ?: return product.sellingPrice
        return when (cust.memberLevel) {
            "Gold", "Platinum" -> {
                if (product.grosirPrice > 0.0) product.grosirPrice else product.memberPrice
            }
            "Silver" -> {
                if (product.memberPrice > 0.0) product.memberPrice else product.sellingPrice
            }
            else -> product.sellingPrice
        }
    }

    // --- POS BILL CALCULATION ---
    fun getCartSummary(): CartSummary {
        var subtotal = 0.0
        var totalItemDiscounts = 0.0

        cartItems.value.forEach { (prod, qty) ->
            val finalPrice = getProductSellingPrice(prod)
            val disc = cartItemDiscounts.value[prod.id] ?: 0.0
            subtotal += finalPrice * qty
            totalItemDiscounts += disc * qty
        }

        val totalDiscount = totalItemDiscounts + activeVoucherDiscount
        val finalSubtotal = (subtotal - totalDiscount).coerceAtLeast(0.0)
        val taxAmount = if (isTaxEnabled) finalSubtotal * (taxPercentage / 100.0) else 0.0
        val totalAmount = finalSubtotal + taxAmount
        val loyaltyPointEarned = (totalAmount / 10000.0).toInt()

        return CartSummary(
            subTotal = subtotal,
            discountAmount = totalDiscount,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            pointsEarned = loyaltyPointEarned
        )
    }

    // --- CHECKOUT TRANSACTION ---
    fun executeCheckout(
        paymentMethod: String,
        paidAmount: Double,
        isDebt: Boolean,
        onSuccess: (Transaction) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val shift = activeShift
        if (shift == null) {
            onFailure("Gagal: Kasir/Shift belum dibuka!")
            return
        }
        val user = currentUser
        if (user == null) {
            onFailure("Gagal: Pengguna tidak teridentifikasi!")
            return
        }
        if (cartItems.value.isEmpty()) {
            onFailure("Gagal: Keranjang belanja kosong!")
            return
        }

        val summary = getCartSummary()
        if (!isDebt && paymentMethod == "TUNAI" && paidAmount < summary.totalAmount) {
            onFailure("Gagal: Uang tunai yang dibayarkan kurang!")
            return
        }

        viewModelScope.launch {
            try {
                // Adapt cart list to transaction requirements
                val cartList = cartItems.value.map { (prod, qty) ->
                    // Set active price depending on member level
                    val adaptedProd = prod.copy(sellingPrice = getProductSellingPrice(prod))
                    Pair(adaptedProd, qty)
                }

                // Gather item discounts from cartItemDiscounts
                val finalItemDiscounts = cartItemDiscounts.value.mapValues { entry ->
                    val qty = cartItems.value.find { it.first.id == entry.key }?.second ?: 1
                    entry.value * qty
                }

                val tx = repository.checkout(
                    cashierId = user.id,
                    cashierName = user.name,
                    customerId = selectedCustomer?.id,
                    customerName = selectedCustomer?.name ?: "Pelanggan Umum",
                    cartItems = cartList,
                    itemDiscounts = finalItemDiscounts,
                    voucherCode = activeVoucherCode,
                    voucherDiscount = activeVoucherDiscount,
                    taxesEnabled = isTaxEnabled,
                    taxPercentage = taxPercentage,
                    paymentMethod = paymentMethod,
                    paymentMethodDetails = if (selectedCustomer != null) "Member: ${selectedCustomer?.name}" else "Non-Member",
                    paidAmount = paidAmount,
                    isDebt = isDebt,
                    activeShiftId = shift.id
                )
                clearCart()
                onSuccess(tx)
            } catch (e: Exception) {
                onFailure("Gagal memproses transaksi: ${e.message}")
            }
        }
    }

    // --- SETTINGS PREFERENCES ACTIONS ---
    fun saveStoreConfig(name: String, address: String, phone: String, npwp: String, tax: Double, isTax: Boolean) {
        viewModelScope.launch {
            val currentProfile = StoreProfile(
                id = 1,
                name = name,
                address = address,
                phone = phone,
                npwp = npwp,
                taxPercentage = tax,
                isTaxEnabled = isTax
            )
            repository.saveStoreProfile(currentProfile, currentUser?.name ?: "Admin")
        }
    }

    // Create custom invoice format for mock bluetooth printer preview
    fun buildInvoiceString(tx: Transaction, items: List<TransactionItem>): String {
        val width = activePrinterSize
        val lineChar = "-"
        val border = lineChar.repeat(if (width == 58) 32 else 40)

        val sb = StringBuilder()
        sb.append(centerText(storeName, if (width == 58) 32 else 40)).append("\n")
        sb.append(centerText(storeAddress, if (width == 58) 32 else 40)).append("\n")
        sb.append(centerText("Telp: $storePhone", if (width == 58) 32 else 40)).append("\n")
        if (storeNpwp.isNotEmpty()) {
            sb.append(centerText("NPWP: $storeNpwp", if (width == 58) 32 else 40)).append("\n")
        }
        sb.append(border).append("\n")

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.append("No    : ").append(tx.transactionId).append("\n")
        sb.append("Wkt   : ").append(sdf.format(Date(tx.timestamp))).append("\n")
        sb.append("Kasir : ").append(tx.cashierName).append("\n")
        sb.append("Cust  : ").append(tx.customerName).append("\n")
        sb.append(border).append("\n")

        items.forEach { item ->
            sb.append(item.productName).append("\n")
            val priceStr = "  ${item.qty} x Rp${String.format("%,.0f", item.sellingPrice)}"
            val subStr = "Rp${String.format("%,.0f", item.subTotal)}"
            val spaces = (if (width == 58) 32 else 40) - priceStr.length - subStr.length
            if (spaces > 0) {
                sb.append(priceStr).append(" ".repeat(spaces)).append(subStr).append("\n")
            } else {
                sb.append(priceStr).append(" ").append(subStr).append("\n")
            }
            if (item.discountPerItem > 0.0) {
                sb.append("    Diskon item: -Rp${String.format("%,.0f", item.discountPerItem * item.qty)}\n")
            }
        }
        sb.append(border).append("\n")

        val subStr = "SUBTOTAL:"
        val subVal = "Rp${String.format("%,.0f", tx.subTotal)}"
        sb.append(subStr).append(" ".repeat(((if (width == 58) 32 else 40) - subStr.length - subVal.length).coerceAtLeast(1))).append(subVal).append("\n")

        if (tx.discountAmount > 0) {
            val discStr = "DISKON:"
            val discVal = "-Rp${String.format("%,.0f", tx.discountAmount)}"
            sb.append(discStr).append(" ".repeat(((if (width == 58) 32 else 40) - discStr.length - discVal.length).coerceAtLeast(1))).append(discVal).append("\n")
        }

        if (tx.taxAmount > 0) {
            val taxStr = "PPN (${taxPercentage}%):"
            val taxVal = "Rp${String.format("%,.0f", tx.taxAmount)}"
            sb.append(taxStr).append(" ".repeat(((if (width == 58) 32 else 40) - taxStr.length - taxVal.length).coerceAtLeast(1))).append(taxVal).append("\n")
        }

        sb.append(border).append("\n")

        val totalStr = "TOTAL AKHIR:"
        val totalVal = "Rp${String.format("%,.0f", tx.totalAmount)}"
        sb.append(totalStr).append(" ".repeat(((if (width == 58) 32 else 40) - totalStr.length - totalVal.length).coerceAtLeast(1))).append(totalVal).append("\n")

        val bayarStr = "METODE BAYAR:"
        val bayarVal = tx.paymentMethod
        sb.append(bayarStr).append(" ".repeat(((if (width == 58) 32 else 40) - bayarStr.length - bayarVal.length).coerceAtLeast(1))).append(bayarVal).append("\n")

        if (tx.debtAmount > 0) {
            val hutStr = "HUTANG (PIUTANG):"
            val hutVal = "Rp${String.format("%,.0f", tx.debtAmount)}"
            sb.append(hutStr).append(" ".repeat(((if (width == 58) 32 else 40) - hutStr.length - hutVal.length).coerceAtLeast(1))).append(hutVal).append("\n")
        } else {
            val payStr = "TUNAI DIBAYAR:"
            val payVal = "Rp${String.format("%,.0f", tx.paidAmount)}"
            sb.append(payStr).append(" ".repeat(((if (width == 58) 32 else 40) - payStr.length - payVal.length).coerceAtLeast(1))).append(payVal).append("\n")

            val kemStr = "KEMBALIAN:"
            val kemVal = "Rp${String.format("%,.0f", tx.changeAmount)}"
            sb.append(kemStr).append(" ".repeat(((if (width == 58) 32 else 40) - kemStr.length - kemVal.length).coerceAtLeast(1))).append(kemVal).append("\n")
        }

        if (tx.pointsEarned > 0) {
            sb.append(border).append("\n")
            sb.append(centerText("Poin Diperoleh: +${tx.pointsEarned} Poin", if (width == 58) 32 else 40)).append("\n")
        }

        sb.append(border).append("\n")
        sb.append(centerText("TERIMA KASIH", if (width == 58) 32 else 40)).append("\n")
        sb.append(centerText("SUDAH BERBELANJA", if (width == 58) 32 else 40)).append("\n")
        sb.append(centerText("LAYANAN KONSUMEN: $storePhone", if (width == 58) 32 else 40)).append("\n")
        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.substring(0, width)
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text + " ".repeat(width - text.length - padding)
    }

    class Factory(
        private val application: Application,
        private val repository: PosRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
                return PosViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class CartSummary(
    val subTotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val pointsEarned: Int
)
