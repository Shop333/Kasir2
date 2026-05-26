package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String, // simple plain hashes for local login
    val pin: String, // 4-6 digit quick PIN
    val role: String, // "Admin", "Kasir", "Supervisor", "Gudang"
    val name: String,
    val email: String = "",
    val phone: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "activity_logs")
data class UserActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val username: String,
    val action: String, // e.g., "LOGIN", "BUKA_KAS", "TRANSAKSI", "UPDATE_PRODUK"
    val details: String
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentCategory: String = "" // sub-category
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String = "",
    val sku: String = "",
    val name: String,
    val categoryId: Int = 0,
    val unit: String = "Pcs", // custom unit e.g. Pcs, Box, Kg
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val memberPrice: Double = 0.0, // multi price (normal/member/grosir)
    val grosirPrice: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 5, // minimum stock alert
    val variantName: String = "", // e.g. "Merah", "L" (for product variants)
    val imageUrl: String = "",
    val status: String = "ACTIVE"
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val cardCode: String = "", // Member Card Code QR/Barcode
    val points: Int = 0,
    val memberLevel: String = "Bronze", // Bronze, Silver, Gold, Platinum
    val joinDate: Long = System.currentTimeMillis(),
    val debtBalance: Double = 0.0 // data piutang pelanggan
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactPerson: String = "",
    val phone: String = "",
    val address: String = "",
    val debtBalance: Double = 0.0 // hutang toko kepada supplier
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val transactionId: String, // Format: TX-YYYYMMDD-XXXX
    val timestamp: Long = System.currentTimeMillis(),
    val userId: Int,
    val cashierName: String,
    val customerId: Long? = null,
    val customerName: String = "Pelanggan Umum",
    val subTotal: Double,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0, // PPN setup
    val totalAmount: Double,
    val paymentMethod: String, // "TUNAI", "QRIS", "BANK_TRANSFER", "DEBIT_KREDIT", "SPLIT"
    val paymentMethodDetails: String = "", // splits info, or card numbers
    val paidAmount: Double,
    val changeAmount: Double,
    val pointsEarned: Int = 0,
    val voucherCode: String = "",
    val debtAmount: Double = 0.0, // amount recorded as customer's debt
    val shiftId: Long,
    val notes: String = ""
)

@Entity(tableName = "transaction_items")
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val productId: Long,
    val productName: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val qty: Int,
    val discountPerItem: Double = 0.0,
    val subTotal: Double
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openTime: Long = System.currentTimeMillis(),
    val closeTime: Long? = null,
    val userId: Int,
    val userName: String,
    val startingCash: Double, // modal awal
    val cashSales: Double = 0.0,
    val qrisSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val bankSales: Double = 0.0,
    val debtSales: Double = 0.0,
    val totalCashIn: Double = 0.0, // starting + petty_cash_masuk + cashSales
    val totalCashOut: Double = 0.0, // petty_cash_keluar
    val expectedCash: Double = 0.0, // calculated theoretical cash
    val actualCash: Double = 0.0, // verified physical cash
    val cashDifference: Double = 0.0, // actual - expected
    val notes: String = "",
    val isClosed: Boolean = false
)

@Entity(tableName = "petty_cash")
data class PettyCash(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val shiftId: Long,
    val amount: Double,
    val type: String, // "IN" or "OUT" (pemasukan / pengeluaran operasional)
    val description: String
)

@Entity(tableName = "store_profile")
data class StoreProfile(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val name: String = "Toko Saya",
    val logoUrl: String = "",
    val address: String = "Jalan POS No. 1, Jakarta",
    val phone: String = "08123456789",
    val npwp: String = "01.234.567.8-999.000",
    val taxPercentage: Double = 11.0, // 11% PPN standard Indonesia
    val isTaxEnabled: Boolean = true
)
