package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM fiat_wallets WHERE currency = :currency LIMIT 1")
    fun getFiatWalletFlow(currency: String = "USD"): Flow<FiatWallet?>

    @Query("SELECT * FROM fiat_wallets WHERE currency = :currency LIMIT 1")
    suspend fun getFiatWallet(currency: String = "USD"): FiatWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiatWallet(wallet: FiatWallet)

    @Update
    suspend fun updateFiatWallet(wallet: FiatWallet)

    @Query("SELECT * FROM crypto_wallets")
    fun getAllCryptoWalletsFlow(): Flow<List<CryptoWallet>>

    @Query("SELECT * FROM crypto_wallets")
    suspend fun getAllCryptoWallets(): List<CryptoWallet>

    @Query("SELECT * FROM crypto_wallets WHERE symbol = :symbol LIMIT 1")
    suspend fun getCryptoWallet(symbol: String): CryptoWallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptoWallet(wallet: CryptoWallet)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCryptoWallets(wallets: List<CryptoWallet>)

    @Update
    suspend fun updateCryptoWallet(wallet: CryptoWallet)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContacts(): List<Contact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Delete
    suspend fun deleteContact(contact: Contact)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)
}
