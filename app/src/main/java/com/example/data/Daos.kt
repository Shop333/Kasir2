package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // --- USERS ---
    @Query("SELECT * FROM users ORDER BY username ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash LIMIT 1")
    suspend fun loginWithPassword(username: String, passwordHash: String): User?

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun loginWithPin(pin: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    // --- ACTIVITY LOGS ---
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 200")
    fun getActivityLogs(): Flow<List<UserActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UserActivityLog)

    // --- CATEGORIES ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE stock <= minStock")
    fun getLowStockProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @androidx.room.Transaction
    suspend fun updateStock(productId: Long, qtySold: Int) {
        val prod = getProductById(productId)
        if (prod != null) {
            val newStock = (prod.stock - qtySold).coerceAtLeast(0)
            updateProduct(prod.copy(stock = newStock))
        }
    }

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE cardCode = :code LIMIT 1")
    suspend fun getCustomerByMemberCode(code: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // --- SUPPLIERS ---
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    // --- TRANSACTIONS ---
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE transactionId = :id LIMIT 1")
    suspend fun getTransactionById(id: String): Transaction?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    suspend fun getItemsForTransaction(txId: String): List<TransactionItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItem>)

    @androidx.room.Transaction
    suspend fun saveTransaction(transaction: Transaction, items: List<TransactionItem>) {
        insertTransaction(transaction)
        insertTransactionItems(items)
        // Adjust stock & points
        items.forEach { item ->
            updateStock(item.productId, item.qty)
        }
        if (transaction.customerId != null && transaction.customerId > 0) {
            val cust = getCustomerByIdDirect(transaction.customerId)
            if (cust != null) {
                val newPoints = cust.points + transaction.pointsEarned
                val newDebt = cust.debtBalance + transaction.debtAmount
                // level progression
                val level = when {
                    newPoints >= 2000 -> "Platinum"
                    newPoints >= 1000 -> "Gold"
                    newPoints >= 500 -> "Silver"
                    else -> "Bronze"
                }
                updateCustomer(cust.copy(points = newPoints, debtBalance = newDebt, memberLevel = level))
            }
        }
    }

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    // --- SHIFTS ---
    @Query("SELECT * FROM shifts ORDER BY id DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE isClosed = 0 LIMIT 1")
    suspend fun getActiveShift(): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    // --- PETTY CASH ---
    @Query("SELECT * FROM petty_cash WHERE shiftId = :shiftId ORDER BY timestamp DESC")
    fun getPettyCashForShift(shiftId: Long): Flow<List<PettyCash>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPettyCash(pettyCash: PettyCash): Long

    // --- STORE PROFILE ---
    @Query("SELECT * FROM store_profile WHERE id = 1 LIMIT 1")
    fun getStoreProfile(): Flow<StoreProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStoreProfile(profile: StoreProfile)
}
