package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fiat_wallets")
data class FiatWallet(
    @PrimaryKey val currency: String = "USD",
    val balance: Double = 1500.00
)

@Entity(tableName = "crypto_wallets")
data class CryptoWallet(
    @PrimaryKey val symbol: String, // e.g., "BTC", "ETH", "SOL", "USDC"
    val name: String,
    val balance: Double,
    val address: String,
    val usdRateOnLaunch: Double
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val email: String,
    val walletAddress: String,
    val avatarColorHex: String // For visuals, e.g. "#4CAF50"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "DEPOSIT", "WITHDRAW", "P2P_SEND", "P2P_RECEIVE", "CRYPTO_BUY", "CRYPTO_SELL", "CRYPTO_SEND", "CRYPTO_RECEIVE"
    val symbol: String, // "USD", "BTC", "ETH", etc.
    val amount: Double, // Native amount (e.g., 0.05 BTC or 150.0 USD)
    val usdValue: Double, // Equivalent value in USD at time of transaction
    val counterpartyName: String? = null, // Contact name if transferred
    val counterpartyAddress: String? = null, // External crypto address or phone
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED" // "COMPLETED", "PENDING", "FAILED"
)
