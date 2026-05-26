package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class PosRepository(private val dao: PosDao) {

    // --- USERS ---
    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val activityLogs: Flow<List<UserActivityLog>> = dao.getActivityLogs()

    suspend fun loginWithPassword(username: String, passwordHash: String): User? {
        val user = dao.loginWithPassword(username, passwordHash)
        if (user != null) {
            logActivity(user.username, "LOGIN", "Login berhasil menggunakan password (Role: ${user.role})")
        }
        return user
    }

    suspend fun loginWithPin(pin: String): User? {
        val user = dao.loginWithPin(pin)
        if (user != null) {
            logActivity(user.username, "LOGIN", "Login PIN berhasil (Role: ${user.role})")
        }
        return user
    }

    suspend fun addUser(user: User, adminUser: String): Long {
        val id = dao.insertUser(user)
        logActivity(adminUser, "TAMBAH_USER", "Menambahkan user baru: ${user.username} (${user.role})")
        return id
    }

    suspend fun updateUser(user: User, adminUser: String) {
        dao.updateUser(user)
        logActivity(adminUser, "UPDATE_USER", "Mengubah data user: ${user.username}")
    }

    suspend fun deleteUser(user: User, adminUser: String) {
        dao.deleteUser(user)
        logActivity(adminUser, "HAPUS_USER", "Menghapus user: ${user.username}")
    }

    suspend fun logActivity(username: String, action: String, details: String) {
        dao.insertLog(UserActivityLog(username = username, action = action, details = details))
    }

    // --- CATEGORIES ---
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun addCategory(name: String, operator: String): Long {
        val cat = Category(name = name)
        val id = dao.insertCategory(cat)
        logActivity(operator, "TAMBAH_KATEGORI", "Menambahkan kategori: $name")
        return id
    }

    suspend fun deleteCategory(category: Category, operator: String) {
        dao.deleteCategory(category)
        logActivity(operator, "HAPUS_KATEGORI", "Menghapus kategori: ${category.name}")
    }

    // --- PRODUCTS ---
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = dao.getLowStockProducts()

    suspend fun getProductByBarcode(barcode: String): Product? = dao.getProductByBarcode(barcode)
    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)

    suspend fun addProduct(product: Product, operator: String): Long {
        val id = dao.insertProduct(product)
        logActivity(operator, "TAMBAH_PRODUK", "Menambahkan produk: ${product.name} (SKU: ${product.sku})")
        return id
    }

    suspend fun updateProduct(product: Product, operator: String) {
        dao.updateProduct(product)
        logActivity(operator, "UPDATE_PRODUK", "Mengubah data produk: ${product.name}")
    }

    suspend fun deleteProduct(product: Product, operator: String) {
        dao.deleteProduct(product)
        logActivity(operator, "HAPUS_PRODUK", "Menghapus produk: ${product.name}")
    }

    suspend fun adjustStock(productId: Long, qtyAdjust: Int, reason: String, operator: String) {
        val prod = dao.getProductById(productId)
        if (prod != null) {
            val newStock = (prod.stock + qtyAdjust).coerceAtLeast(0)
            dao.updateProduct(prod.copy(stock = newStock))
            logActivity(operator, "MUTASI_STOK", "Mutasi ${if (qtyAdjust >= 0) "+" else ""}$qtyAdjust untuk ${prod.name}. Alasan: $reason")
        }
    }

    // --- CUSTOMERS ---
    val allCustomers: Flow<List<Customer>> = dao.getAllCustomers()

    suspend fun addCustomer(customer: Customer, operator: String): Long {
        val id = dao.insertCustomer(customer)
        logActivity(operator, "TAMBAH_PELANGGAN", "Menambahkan pelanggan: ${customer.name}")
        return id
    }

    suspend fun updateCustomer(customer: Customer, operator: String) {
        dao.updateCustomer(customer)
        logActivity(operator, "UPDATE_PELANGGAN", "Mengubah pelanggan: ${customer.name}")
    }

    suspend fun deleteCustomer(customer: Customer, operator: String) {
        dao.deleteCustomer(customer)
        logActivity(operator, "HAPUS_PELANGGAN", "Menghapus pelanggan: ${customer.name}")
    }

    // --- SUPPLIERS ---
    val allSuppliers: Flow<List<Supplier>> = dao.getAllSuppliers()

    suspend fun addSupplier(supplier: Supplier, operator: String): Long {
        val id = dao.insertSupplier(supplier)
        logActivity(operator, "TAMBAH_SUPPLIER", "Menambahkan supplier: ${supplier.name}")
        return id
    }

    suspend fun updateSupplier(supplier: Supplier, operator: String) {
        dao.updateSupplier(supplier)
        logActivity(operator, "UPDATE_SUPPLIER", "Mengubah supplier: ${supplier.name}")
    }

    suspend fun deleteSupplier(supplier: Supplier, operator: String) {
        dao.deleteSupplier(supplier)
        logActivity(operator, "HAPUS_SUPPLIER", "Menghapus supplier: ${supplier.name}")
    }

    // --- TRANSACTIONS ---
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()

    suspend fun getTransactionDetails(txId: String): Pair<Transaction?, List<TransactionItem>> {
        val tx = dao.getTransactionById(txId)
        val items = dao.getItemsForTransaction(txId)
        return Pair(tx, items)
    }

    suspend fun checkout(
        cashierId: Int,
        cashierName: String,
        customerId: Long?,
        customerName: String,
        cartItems: List<Pair<Product, Int>>, // Product and quantity
        itemDiscounts: Map<Long, Double>,   // discounts applied per item productId
        voucherCode: String,
        voucherDiscount: Double,
        taxesEnabled: Boolean,
        taxPercentage: Double,
        paymentMethod: String,
        paymentMethodDetails: String,
        paidAmount: Double,
        isDebt: Boolean,
        activeShiftId: Long
    ): Transaction {
        val timestamp = System.currentTimeMillis()
        val randomNum = (1000..9999).random()
        val simpleDate = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        val transactionId = "TX-$simpleDate-$randomNum"

        var subTotal = 0.0
        val items = cartItems.map { (product, qty) ->
            val disc = itemDiscounts[product.id] ?: 0.0
            val singleSub = (product.sellingPrice - disc) * qty
            subTotal += (product.sellingPrice * qty)
            TransactionItem(
                transactionId = transactionId,
                productId = product.id,
                productName = product.name,
                purchasePrice = product.purchasePrice,
                sellingPrice = product.sellingPrice,
                qty = qty,
                discountPerItem = disc,
                subTotal = singleSub
            )
        }

        val discTotal = itemDiscounts.values.sum() + voucherDiscount
        val subtotalPostItemDisc = subTotal - itemDiscounts.entries.sumOf { it.value }
        val taxAmount = if (taxesEnabled) {
            (subtotalPostItemDisc - voucherDiscount) * (taxPercentage / 100.0)
        } else {
            0.0
        }
        val netAmount = (subtotalPostItemDisc - voucherDiscount) + taxAmount

        val debtAmt = if (isDebt) netAmount else 0.0
        val changeAmt = if (!isDebt && paymentMethod == "TUNAI") {
            (paidAmount - netAmount).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // 1 point per 10.000 net spending
        val pointsEarned = (netAmount / 10000.0).toInt()

        val tx = Transaction(
            transactionId = transactionId,
            timestamp = timestamp,
            userId = cashierId,
            cashierName = cashierName,
            customerId = customerId,
            customerName = customerName,
            subTotal = subTotal,
            discountAmount = discTotal,
            taxAmount = taxAmount,
            totalAmount = netAmount,
            paymentMethod = paymentMethod,
            paymentMethodDetails = paymentMethodDetails,
            paidAmount = if (isDebt) 0.0 else paidAmount,
            changeAmount = changeAmt,
            pointsEarned = pointsEarned,
            voucherCode = voucherCode,
            debtAmount = debtAmt,
            shiftId = activeShiftId
        )

        dao.saveTransaction(tx, items)

        // Increment active shift numbers
        val activeShift = dao.getActiveShift()
        if (activeShift != null) {
            var cashAdd = 0.0
            var qrisAdd = 0.0
            var cardAdd = 0.0
            var bankAdd = 0.0
            var debtAdd = 0.0

            if (isDebt) {
                debtAdd = netAmount
            } else {
                when (paymentMethod) {
                    "TUNAI" -> cashAdd = netAmount
                    "QRIS" -> qrisAdd = netAmount
                    "DEBIT_KREDIT" -> cardAdd = netAmount
                    "BANK_TRANSFER" -> bankAdd = netAmount
                }
            }

            dao.updateShift(
                activeShift.copy(
                    cashSales = activeShift.cashSales + cashAdd,
                    qrisSales = activeShift.qrisSales + qrisAdd,
                    cardSales = activeShift.cardSales + cardAdd,
                    bankSales = activeShift.bankSales + bankAdd,
                    debtSales = activeShift.debtSales + debtAdd,
                    expectedCash = activeShift.expectedCash + cashAdd
                )
            )
        }

        logActivity(cashierName, "TRANSAKSI", "Transaksi berhasil: $transactionId (Rp ${String.format("%,.0f", netAmount)})")
        return tx
    }

    // --- SHIFTS & CASH MANAGER ---
    val allShifts: Flow<List<Shift>> = dao.getAllShifts()

    suspend fun getActiveShift(): Shift? = dao.getActiveShift()

    suspend fun openShift(userId: Int, userName: String, startingCash: Double): Long {
        val active = getActiveShift()
        if (active != null) return active.id // Shift already open

        val newShift = Shift(
            userId = userId,
            userName = userName,
            startingCash = startingCash,
            expectedCash = startingCash // theoretical is starting cash initially
        )
        val id = dao.insertShift(newShift)
        logActivity(userName, "BUKA_KAS", "Membuka shift kasir baru dengan modal Rp ${String.format("%,.0f", startingCash)}")
        return id
    }

    suspend fun closeShift(shiftId: Long, actualCash: Double, notes: String, operator: String) {
        val shift = dao.getAllShifts().firstOrNull()?.find { it.id == shiftId } ?: return
        val difference = actualCash - shift.expectedCash

        dao.updateShift(
            shift.copy(
                closeTime = System.currentTimeMillis(),
                actualCash = actualCash,
                cashDifference = difference,
                notes = notes,
                isClosed = true
            )
        )
        logActivity(operator, "TUTUP_KAS", "Menutup shift kasir #$shiftId. Selisih kas: Rp ${String.format("%,.2f", difference)}")
    }

    // --- PETTY CASH ---
    fun getPettyCashForShift(shiftId: Long): Flow<List<PettyCash>> = dao.getPettyCashForShift(shiftId)

    suspend fun recordPettyCash(shiftId: Long, amount: Double, type: String, description: String, operator: String): Long {
        val petty = PettyCash(shiftId = shiftId, amount = amount, type = type, description = description)
        val id = dao.insertPettyCash(petty)

        // Adjust theoretical expected shift cash if it is small cash entry
        val activeShift = dao.getActiveShift()
        if (activeShift != null && activeShift.id == shiftId) {
            val isAdd = type == "IN"
            val signMultiplier = if (isAdd) 1 else -1
            val delta = amount * signMultiplier
            val expectedNext = (activeShift.expectedCash + delta).coerceAtLeast(0.0)

            val nextIn = if (isAdd) activeShift.totalCashIn + amount else activeShift.totalCashIn
            val nextOut = if (!isAdd) activeShift.totalCashOut + amount else activeShift.totalCashOut

            dao.updateShift(
                activeShift.copy(
                    expectedCash = expectedNext,
                    totalCashIn = nextIn,
                    totalCashOut = nextOut
                )
            )
        }

        logActivity(operator, "KAS_KECIL", "Pencatatan kas kecil ${if (type == "IN") "masuk" else "keluar"}: Rp ${String.format("%,.0f", amount)} - $description")
        return id
    }

    // --- STORE PROFILE ---
    val storeProfile: Flow<StoreProfile?> = dao.getStoreProfile()

    suspend fun saveStoreProfile(profile: StoreProfile, operator: String) {
        dao.updateStoreProfile(profile)
        logActivity(operator, "UPDATE_PROFIL_TOKO", "Memperbarui profil toko: ${profile.name}")
    }
}
