package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

class PaymentRepository(private val database: AppDatabase) {

    private val walletDao = database.walletDao()
    private val contactDao = database.contactDao()
    private val transactionDao = database.transactionDao()

    // Expose database Flows
    val fiatWalletFlow: Flow<FiatWallet?> = walletDao.getFiatWalletFlow()
    val cryptoWalletsFlow: Flow<List<CryptoWallet>> = walletDao.getAllCryptoWalletsFlow()
    val contactsFlow: Flow<List<Contact>> = contactDao.getAllContactsFlow()
    val transactionsFlow: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()

    // Simulated volatile real-time prices for cryptocurrency
    private val _cryptoRates = MutableStateFlow(
        mapOf(
            "BTC" to 68520.0,
            "ETH" to 3480.0,
            "SOL" to 142.5,
            "USDC" to 1.0
        )
    )
    val cryptoRates: StateFlow<Map<String, Double>> = _cryptoRates.asStateFlow()

    // Random walk price simulator
    fun simulateMarketFluctuations() {
        val current = _cryptoRates.value
        val btcDelta = current["BTC"]!! * (1.0 + (Random.nextDouble(-0.015, 0.015)))
        val ethDelta = current["ETH"]!! * (1.0 + (Random.nextDouble(-0.018, 0.018)))
        val solDelta = current["SOL"]!! * (1.0 + (Random.nextDouble(-0.035, 0.035)))
        val usdcDelta = 1.0 + Random.nextDouble(-0.0005, 0.0005) // Stable token

        _cryptoRates.value = mapOf(
            "BTC" to Math.round(btcDelta * 100.0) / 100.0,
            "ETH" to Math.round(ethDelta * 100.0) / 100.0,
            "SOL" to Math.round(solDelta * 100.0) / 100.0,
            "USDC" to Math.round(usdcDelta * 10000.0) / 10000.0
        )
    }

    // Prepopulate DB with initial wallet balances, contacts, and transactions if empty
    suspend fun prepopulateIfEmpty() {
        // Checking if wallets exist
        val existingFiat = walletDao.getFiatWallet()
        if (existingFiat == null) {
            walletDao.insertFiatWallet(FiatWallet("USD", 1850.50))
        }

        val existingCrypto = walletDao.getAllCryptoWallets()
        if (existingCrypto.isEmpty()) {
            val initialCryptoWallets = listOf(
                CryptoWallet("BTC", "Bitcoin", 0.0352, "bc1qxy2kg3ut76dfsw8rwe2p36sh68qw", 68520.0),
                CryptoWallet("ETH", "Ethereum", 0.85, "0x71C7656EC7ab88b098defB751B7401B5f6d8976F", 3480.0),
                CryptoWallet("SOL", "Solana", 5.4, "HN7cABvi3M46GfWfP4C7ib93P26ghXJ", 142.5),
                CryptoWallet("USDC", "USD Coin", 210.0, "0x3901b8b097deff88a0adefb751b7401b5f6d8977", 1.0)
            )
            walletDao.insertCryptoWallets(initialCryptoWallets)
        }

        val existingContacts = contactDao.getAllContacts()
        if (existingContacts.isEmpty()) {
            val initialContacts = listOf(
                Contact(0, "Alice Smith", "+1 (555) 019-2834", "alice.smith@paycrypto.com", "bc1q78p9szl9shqwkeuf45pjh72qwle72s", "#FF5722"),
                Contact(0, "Bob Jones", "+1 (555) 014-9988", "bob.jones@paycrypto.com", "0x56a623088defb751b7401b5f6d8971a238b0976", "#2196F3"),
                Contact(0, "Charlie Brown", "+1 (555) 012-3456", "charlie@peanuts.org", "HN7c2bFwi3M46GfWfP4C7ib93P26g4fe8A", "#4CAF50"),
                Contact(0, "Diana Prince", "+1 (555) 018-7711", "diana@themyscira.gov", "0x12b056e7abcd88b098defb751b7401b5f2bcde8", "#9C27B0")
            )
            contactDao.insertContacts(initialContacts)
        }

        val existingTxs = transactionDao.getAllTransactions()
        if (existingTxs.isEmpty()) {
            val initialTxs = listOf(
                Transaction(0, "DEPOSIT", "USD", 2000.0, 2000.0, "Bank Account", "Instant Transfer", System.currentTimeMillis() - 86400000 * 3),
                Transaction(0, "CRYPTO_BUY", "BTC", 0.01, 680.0, "Exchange", "Bought 0.01 BTC", System.currentTimeMillis() - 86400000 * 2),
                Transaction(0, "P2P_SEND", "USD", 150.0, 150.0, "Alice Smith", "+1 (555) 019-2834", System.currentTimeMillis() - 36000000),
                Transaction(0, "CRYPTO_RECEIVE", "SOL", 1.5, 213.75, "External Faucet", "Devnet Faucet Claims", System.currentTimeMillis() - 7200000)
            )
            transactionDao.insertTransactions(initialTxs)
        }
    }

    // --- Core Operations ---

    suspend fun depositFiat(amount: Double) {
        val currentWallet = walletDao.getFiatWallet() ?: FiatWallet("USD", 0.0)
        val updated = currentWallet.copy(balance = currentWallet.balance + amount)
        walletDao.insertFiatWallet(updated)

        transactionDao.insertTransaction(
            Transaction(
                type = "DEPOSIT",
                symbol = "USD",
                amount = amount,
                usdValue = amount,
                counterpartyName = "Bank Deposit",
                counterpartyAddress = "ACH Transfer"
            )
        )
    }

    suspend fun withdrawFiat(amount: Double): Boolean {
        val currentWallet = walletDao.getFiatWallet() ?: return false
        if (currentWallet.balance < amount) return false

        val updated = currentWallet.copy(balance = currentWallet.balance - amount)
        walletDao.insertFiatWallet(updated)

        transactionDao.insertTransaction(
            Transaction(
                type = "WITHDRAW",
                symbol = "USD",
                amount = amount,
                usdValue = amount,
                counterpartyName = "Bank Withdraw",
                counterpartyAddress = "ACH Bank Out"
            )
        )
        return true
    }

    suspend fun sendP2P(contact: Contact, amount: Double, symbol: String): Boolean {
        if (amount <= 0.0) return false

        if (symbol == "USD") {
            val currentFiat = walletDao.getFiatWallet() ?: return false
            if (currentFiat.balance < amount) return false

            // Subtract from sender's Fiat
            walletDao.insertFiatWallet(currentFiat.copy(balance = currentFiat.balance - amount))

            // Log Transaction
            transactionDao.insertTransaction(
                Transaction(
                    type = "P2P_SEND",
                    symbol = "USD",
                    amount = amount,
                    usdValue = amount,
                    counterpartyName = contact.name,
                    counterpartyAddress = contact.phone
                )
            )
            return true
        } else {
            // Crypto Transfer
            val currentCrypto = walletDao.getCryptoWallet(symbol) ?: return false
            if (currentCrypto.balance < amount) return false

            // Subtract from crypto wallet
            walletDao.insertCryptoWallet(currentCrypto.copy(balance = currentCrypto.balance - amount))

            val currentRate = _cryptoRates.value[symbol] ?: currentCrypto.usdRateOnLaunch
            val usdVal = amount * currentRate

            // Log Transaction
            transactionDao.insertTransaction(
                Transaction(
                    type = "P2P_SEND",
                    symbol = symbol,
                    amount = amount,
                    usdValue = usdVal,
                    counterpartyName = contact.name,
                    counterpartyAddress = contact.walletAddress
                )
            )
            return true
        }
    }

    suspend fun buyCrypto(symbol: String, usdAmount: Double): Boolean {
        if (usdAmount <= 0.0) return false
        val currentFiat = walletDao.getFiatWallet() ?: return false
        if (currentFiat.balance < usdAmount) return false

        val rate = _cryptoRates.value[symbol] ?: return false
        val coinAmount = usdAmount / rate

        // Sub Fiat
        walletDao.insertFiatWallet(currentFiat.copy(balance = currentFiat.balance - usdAmount))

        // Add Crypto
        val currentCrypto = walletDao.getCryptoWallet(symbol) ?: CryptoWallet(symbol, getCoinName(symbol), 0.0, generateMockAddress(symbol), rate)
        walletDao.insertCryptoWallet(currentCrypto.copy(balance = currentCrypto.balance + coinAmount))

        // Log
        transactionDao.insertTransaction(
            Transaction(
                type = "CRYPTO_BUY",
                symbol = symbol,
                amount = coinAmount,
                usdValue = usdAmount,
                counterpartyName = "Exchange Terminal",
                counterpartyAddress = "Direct buy structure"
            )
        )
        return true
    }

    suspend fun sellCrypto(symbol: String, coinAmount: Double): Boolean {
        if (coinAmount <= 0.0) return false
        val currentCrypto = walletDao.getCryptoWallet(symbol) ?: return false
        if (currentCrypto.balance < coinAmount) return false

        val rate = _cryptoRates.value[symbol] ?: return false
        val usdValue = coinAmount * rate

        // Sub Crypto
        walletDao.insertCryptoWallet(currentCrypto.copy(balance = currentCrypto.balance - coinAmount))

        // Add Fiat
        val currentFiat = walletDao.getFiatWallet() ?: FiatWallet("USD", 0.0)
        walletDao.insertFiatWallet(currentFiat.copy(balance = currentFiat.balance + usdValue))

        // Log
        transactionDao.insertTransaction(
            Transaction(
                type = "CRYPTO_SELL",
                symbol = symbol,
                amount = coinAmount,
                usdValue = usdValue,
                counterpartyName = "Exchange Terminal",
                counterpartyAddress = "Direct sell structure"
            )
        )
        return true
    }

    suspend fun receiveFaucetCrypto(symbol: String, coinAmount: Double) {
        val rate = _cryptoRates.value[symbol] ?: 1.0
        val currentCrypto = walletDao.getCryptoWallet(symbol) ?: CryptoWallet(symbol, getCoinName(symbol), 0.0, generateMockAddress(symbol), rate)
        
        walletDao.insertCryptoWallet(currentCrypto.copy(balance = currentCrypto.balance + coinAmount))

        transactionDao.insertTransaction(
            Transaction(
                type = "CRYPTO_RECEIVE",
                symbol = symbol,
                amount = coinAmount,
                usdValue = coinAmount * rate,
                counterpartyName = "Testnet Faucet",
                counterpartyAddress = "Mock Faucet Protocol"
            )
        )
    }

    suspend fun sendExternalCrypto(symbol: String, coinAmount: Double, targetAddress: String): Boolean {
        if (coinAmount <= 0.0) return false
        val currentCrypto = walletDao.getCryptoWallet(symbol) ?: return false
        if (currentCrypto.balance < coinAmount) return false

        val rate = _cryptoRates.value[symbol] ?: 1.0
        val usdValue = coinAmount * rate

        // Sub Crypto
        walletDao.insertCryptoWallet(currentCrypto.copy(balance = currentCrypto.balance - coinAmount))

        // Log Transaction
        transactionDao.insertTransaction(
            Transaction(
                type = "CRYPTO_SEND",
                symbol = symbol,
                amount = coinAmount,
                usdValue = usdValue,
                counterpartyName = "External Address",
                counterpartyAddress = targetAddress
            )
        )
        return true
    }

    suspend fun addContact(name: String, phone: String, email: String, walletAddress: String, avatarColorHex: String) {
        contactDao.insertContact(
            Contact(
                name = name,
                phone = phone,
                email = email,
                walletAddress = walletAddress.ifEmpty { generateMockAddress("BTC") },
                avatarColorHex = avatarColorHex.ifEmpty { "#9E9E9E" }
            )
        )
    }

    suspend fun deleteContact(contact: Contact) {
        contactDao.deleteContact(contact)
    }

    // Helper utilities
    private fun getCoinName(symbol: String): String = when (symbol) {
        "BTC" -> "Bitcoin"
        "ETH" -> "Ethereum"
        "SOL" -> "Solana"
        "USDC" -> "USD Coin"
        else -> "Crypto Coin"
    }

    private fun generateMockAddress(symbol: String): String = when (symbol) {
        "BTC" -> "bc1q" + List(28) { (('a'..'z') + ('0'..'9')).random() }.joinToString("")
        "ETH", "USDC" -> "0x" + List(40) { (('a'..'f') + ('0'..'9')).random() }.joinToString("")
        "SOL" -> List(44) { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")
        else -> "0x" + List(40) { (('a'..'f') + ('0'..'9')).random() }.joinToString("")
    }
}
