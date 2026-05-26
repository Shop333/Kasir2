package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        UserActivityLog::class,
        Category::class,
        Product::class,
        Customer::class,
        Supplier::class,
        Transaction::class,
        TransactionItem::class,
        Shift::class,
        PettyCash::class,
        StoreProfile::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasir_pos_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.posDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: PosDao) {
                // Populate default store profile
                dao.updateStoreProfile(
                    StoreProfile(
                        id = 1,
                        name = "Sentosa Minimarket",
                        logoUrl = "",
                        address = "Jl. Sudirman No. 45, Jakarta Selatan",
                        phone = "0812-3456-7890",
                        npwp = "12.345.678.9-012.000",
                        taxPercentage = 11.0,
                        isTaxEnabled = true
                    )
                )

                // Populate default users
                val adminId = dao.insertUser(
                    User(
                        username = "admin",
                        passwordHash = "admin", // simple demo hash
                        pin = "1234",
                        role = "Admin",
                        name = "Firdaus (Admin)",
                        email = "admin@sentosapos.com",
                        phone = "0811223344"
                    )
                )
                val kasirId = dao.insertUser(
                    User(
                        username = "kasir",
                        passwordHash = "kasir",
                        pin = "5678",
                        role = "Kasir",
                        name = "Siti (Kasir)",
                        email = "siti@sentosapos.com",
                        phone = "0855667788"
                    )
                )
                dao.insertUser(
                    User(
                        username = "gudang",
                        passwordHash = "gudang",
                        pin = "0000",
                        role = "Gudang",
                        name = "Budi (Logistik)",
                        email = "budi@sentosapos.com",
                        phone = "0899887766"
                    )
                )

                // Add default categories
                dao.insertCategory(Category(name = "Makanan"))
                dao.insertCategory(Category(name = "Minuman"))
                dao.insertCategory(Category(name = "Snack"))
                dao.insertCategory(Category(name = "Rokok"))
                dao.insertCategory(Category(name = "Sembako"))

                // Add default products
                dao.insertProduct(
                    Product(
                        name = "Indomie Goreng Spesial",
                        barcode = "89686016074",
                        sku = "IND-GOR-001",
                        categoryId = 1,
                        unit = "Bks",
                        purchasePrice = 2500.0,
                        sellingPrice = 3200.0,
                        memberPrice = 3000.0,
                        grosirPrice = 2900.0,
                        stock = 150,
                        minStock = 10,
                        variantName = "Original"
                    )
                )
                dao.insertProduct(
                    Product(
                        name = "Teh Pucuk Harum 350ml",
                        barcode = "8992222050021",
                        sku = "TEH-PUC-002",
                        categoryId = 2,
                        unit = "Botol",
                        purchasePrice = 2300.0,
                        sellingPrice = 3500.0,
                        memberPrice = 3300.0,
                        grosirPrice = 3100.0,
                        stock = 80,
                        minStock = 15,
                        variantName = "Original"
                    )
                )
                dao.insertProduct(
                    Product(
                        name = "Kopi Kenangan Mantan Can 250ml",
                        barcode = "8992222012012",
                        sku = "KOP-KEN-003",
                        categoryId = 2,
                        unit = "Kaleng",
                        purchasePrice = 7500.0,
                        sellingPrice = 9800.0,
                        memberPrice = 9500.0,
                        grosirPrice = 9000.0,
                        stock = 45,
                        minStock = 8,
                        variantName = "Sweet"
                    )
                )
                dao.insertProduct(
                    Product(
                        name = "Chitato Sapi Panggang 68g",
                        barcode = "89686043135",
                        sku = "CHI-SAP-004",
                        categoryId = 3,
                        unit = "Pcs",
                        purchasePrice = 8500.0,
                        sellingPrice = 11500.0,
                        memberPrice = 11000.0,
                        grosirPrice = 10500.0,
                        stock = 30,
                        minStock = 5,
                        variantName = "Medium"
                    )
                )
                dao.insertProduct(
                    Product(
                        name = "Beras Sentosa Sejahtera 5Kg",
                        barcode = "8993202102102",
                        sku = "BER-SEN-005",
                        categoryId = 5,
                        unit = "Karung",
                        purchasePrice = 58000.0,
                        sellingPrice = 68000.0,
                        memberPrice = 66000.0,
                        grosirPrice = 65000.0,
                        stock = 12,
                        minStock = 3,
                        variantName = "Pandan Wangi"
                    )
                )
                // Low stock product for testing notifications
                dao.insertProduct(
                    Product(
                        name = "Minyak Goreng Bimoli 2L",
                        barcode = "8992102202202",
                        sku = "MIN-BIM-006",
                        categoryId = 5,
                        unit = "Pouch",
                        purchasePrice = 28000.0,
                        sellingPrice = 34000.0,
                        memberPrice = 33000.0,
                        grosirPrice = 32500.0,
                        stock = 2, // low stock!
                        minStock = 8,
                        variantName = "Refill"
                    )
                )

                // Add default customers
                dao.insertCustomer(
                    Customer(
                        name = "Ahmad Sobari",
                        email = "ahmad@gmail.com",
                        phone = "081299998888",
                        cardCode = "MEM-001",
                        points = 350,
                        memberLevel = "Bronze"
                    )
                )
                dao.insertCustomer(
                    Customer(
                        name = "Dewi Fortuna",
                        email = "dewi@gmail.com",
                        phone = "085611112222",
                        cardCode = "MEM-002",
                        points = 1200,
                        memberLevel = "Gold"
                    )
                )

                // Add default suppliers
                dao.insertSupplier(
                    Supplier(
                        name = "PT Indofood Sukses Makmur",
                        contactPerson = "Hendra",
                        phone = "021-555123",
                        address = "Kawasan Industri Ancol, Jakarta"
                    )
                )
                dao.insertSupplier(
                    Supplier(
                        name = "CV Sembako Abadi",
                        contactPerson = "Sandi",
                        phone = "0819123456",
                        address = "Grogol, Jakarta Barat"
                    )
                )
            }
        }
    }
}
