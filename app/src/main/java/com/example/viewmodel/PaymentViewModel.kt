package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.GeminiChatHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val sender: String, // "user" or "copilot"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val transferAction: TransferShortcut? = null
)

data class TransferShortcut(
    val amount: Double,
    val symbol: String,
    val contactName: String
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = PaymentRepository(database)
    private val chatHelper = GeminiChatHelper()

    // Database flows
    val fiatWallet: StateFlow<FiatWallet?> = repository.fiatWalletFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cryptoWallets: StateFlow<List<CryptoWallet>> = repository.cryptoWalletsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = repository.contactsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cryptoRates: StateFlow<Map<String, Double>> = repository.cryptoRates

    // Navigation and tab states (0: Dashboard, 1: P2P Transfer, 2: Crypto Terminal, 3: AI Co-pilot)
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // Notification states
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage = _notificationMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    // Interactive operational states (P2P page)
    private val _selectedP2PContact = MutableStateFlow<Contact?>(null)
    val selectedP2PContact = _selectedP2PContact.asStateFlow()

    private val _p2pAmount = MutableStateFlow("")
    val p2pAmount = _p2pAmount.asStateFlow()

    private val _p2pSymbol = MutableStateFlow("USD") // "USD", "BTC", "ETH", "SOL", "USDC"
    val p2pSymbol = _p2pSymbol.asStateFlow()

    // Interactive operational states (Exchange / Buy-Sell page)
    private val _selectedExchangeSymbol = MutableStateFlow("BTC") // "BTC", "ETH", "SOL", "USDC"
    val selectedExchangeSymbol = _selectedExchangeSymbol.asStateFlow()

    private val _exchangeAmount = MutableStateFlow("")
    val exchangeAmount = _exchangeAmount.asStateFlow()

    private val _isBuyMode = MutableStateFlow(true) // true to buy coin with USD, false to sell
    val isBuyMode = _isBuyMode.asStateFlow()

    // Faucet and External Send actions
    private val _faucetLoading = MutableStateFlow(false)
    val faucetLoading = _faucetLoading.asStateFlow()

    private val _externalSendAddress = MutableStateFlow("")
    val externalSendAddress = _externalSendAddress.asStateFlow()

    private val _externalSendAmount = MutableStateFlow("")
    val externalSendAmount = _externalSendAmount.asStateFlow()

    // AI Chat Copilot states
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput = _chatInput.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // First launch setup/prepopulation
            repository.prepopulateIfEmpty()
            
            // Add a friendly welcome message to Co-pilot
            _chatHistory.value = listOf(
                ChatMessage(
                    "copilot",
                    "Hi kharetitu@gmail.com! 👋 I am your specialized AI Finance and Crypto Co-Pilot. I can answer questions about exchange rates, portfolio splits, or help you perform instantaneous transfers. Just ask!"
                )
            )
        }
    }

    // --- Action Handlers ---

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun showNotification(msg: String) {
        _notificationMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3500)
            if (_notificationMessage.value == msg) {
                _notificationMessage.value = null
            }
        }
    }

    fun showError(msg: String) {
        _errorMessage.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3500)
            if (_errorMessage.value == msg) {
                _errorMessage.value = null
            }
        }
    }

    fun dismissNotification() {
        _notificationMessage.value = null
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    // Cash Deposit/Withdraw
    fun depositCash(amount: Double) {
        viewModelScope.launch {
            repository.depositFiat(amount)
            showNotification("Successfully deposited $$amount USD into your wallet.")
        }
    }

    fun withdrawCash(amount: Double) {
        viewModelScope.launch {
            val success = repository.withdrawFiat(amount)
            if (success) {
                showNotification("Successfully withdrew $$amount USD to your link bank account.")
            } else {
                showError("Insufficient cash balance to complete withdrawal.")
            }
        }
    }

    // P2P page setters
    fun setP2PSupply(contact: Contact?, amount: String, symbol: String) {
        if (contact != null) _selectedP2PContact.value = contact
        _p2pAmount.value = amount
        _p2pSymbol.value = symbol
    }

    fun executeP2PTransfer() {
        val contact = _selectedP2PContact.value
        val amount = _p2pAmount.value.toDoubleOrNull() ?: 0.0
        val symbol = _p2pSymbol.value

        if (contact == null) {
            showError("Please select a recipient contact first.")
            return
        }
        if (amount <= 0.0) {
            showError("Please enter a valid transfer amount.")
            return
        }

        viewModelScope.launch {
            val success = repository.sendP2P(contact, amount, symbol)
            if (success) {
                showNotification("Transferred $amount $symbol to ${contact.name} instantly!")
                _p2pAmount.value = ""
            } else {
                showError("Insufficient balance in $symbol to send specified transfer.")
            }
        }
    }

    // Exchange crypto setters
    fun setExchangeSymbol(symbol: String) {
        _selectedExchangeSymbol.value = symbol
    }

    fun setExchangeAmount(amount: String) {
        _exchangeAmount.value = amount
    }

    fun toggleBuySellMode() {
        _isBuyMode.value = !_isBuyMode.value
    }

    fun executeExchange() {
        val symbol = _selectedExchangeSymbol.value
        val amount = _exchangeAmount.value.toDoubleOrNull() ?: 0.0
        val isBuy = _isBuyMode.value

        if (amount <= 0.0) {
            showError("Please enter a valid currency exchange amount.")
            return
        }

        viewModelScope.launch {
            val success = if (isBuy) {
                repository.buyCrypto(symbol, amount)
            } else {
                repository.sellCrypto(symbol, amount)
            }

            if (success) {
                if (isBuy) {
                    showNotification("Purchase successful! Swapped $$amount USD for $symbol tokens.")
                } else {
                    showNotification("Sale successful! Swapped $amount $symbol tokens into USD.")
                }
                _exchangeAmount.value = ""
            } else {
                showError("Insufficient account reserves to settle cryptocurrency exchange.")
            }
        }
    }

    // Market rates refresher
    fun refreshMarketPrices() {
        repository.simulateMarketFluctuations()
        showNotification("Exchange rates updated successfully.")
    }

    // Faucet
    fun triggerFaucetRequest(symbol: String) {
        viewModelScope.launch {
            _faucetLoading.value = true
            kotlinx.coroutines.delay(1000) // Aesthetic delay for transaction
            val coinAmount = when (symbol) {
                "BTC" -> 0.005
                "ETH" -> 0.15
                "SOL" -> 2.5
                "USDC" -> 25.0
                else -> 1.0
            }
            repository.receiveFaucetCrypto(symbol, coinAmount)
            _faucetLoading.value = false
            showNotification("Received $coinAmount testnet $symbol tokens in faucet drop!")
        }
    }

    // Send External Wallet Address
    fun sendExternalCrypto() {
        val symbol = _selectedExchangeSymbol.value
        val amount = _externalSendAmount.value.toDoubleOrNull() ?: 0.0
        val address = _externalSendAddress.value

        if (address.isEmpty() || address.length < 10) {
            showError("Invalid destination crypto address format.")
            return
        }
        if (amount <= 0.0) {
            showError("Please enter a valid coin amount.")
            return
        }

        viewModelScope.launch {
            val success = repository.sendExternalCrypto(symbol, amount, address)
            if (success) {
                showNotification("Withdrew $amount $symbol tokens to address ${address.take(8)}...")
                _externalSendAddress.value = ""
                _externalSendAmount.value = ""
            } else {
                showError("Insufficient $symbol tokens to fulfill blockchain withdrawal.")
            }
        }
    }

    fun setExternalWithdrawFields(address: String, amount: String) {
        _externalSendAddress.value = address
        _externalSendAmount.value = amount
    }

    // Contact additions
    fun addNewContact(name: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            val listColors = listOf("#4CAF50", "#2196F3", "#9C27B0", "#E91E63", "#FF9800", "#FF5722", "#00BCD4")
            val selectedColorHex = listColors.random()
            repository.addContact(name, phone, email, address, selectedColorHex)
            showNotification("Added new contact: $name.")
        }
    }

    // AI Chat Copilot methods
    fun setChatInput(text: String) {
        _chatInput.value = text
    }

    fun sendCopilotMessage() {
        val text = _chatInput.value.trim()
        if (text.isEmpty()) return

        val userMsg = ChatMessage("user", text)
        _chatHistory.value = _chatHistory.value + userMsg
        _chatInput.value = ""
        _isChatLoading.value = true

        viewModelScope.launch {
            // Build rich database context helper to make the Gemini API smart
            val fiatBal = fiatWallet.value?.balance ?: 0.0
            val cryptoSummary = cryptoWallets.value.joinToString(", ") { "${it.symbol}: ${it.balance}" }
            val contactsSummary = contacts.value.joinToString(", ") { "${it.name} (${it.phone})" }
            val transactionsSummary = transactions.value.take(4).joinToString("; ") { "${it.type} of ${it.amount} ${it.symbol} to ${it.counterpartyName ?: "unknown"}" }
            
            val contextText = """
                User Email: kharetitu@gmail.com
                Cash Balance (USD): $$fiatBal
                Crypto Portfolios: $cryptoSummary
                Recent Operations: $transactionsSummary
                Recent Address Contacts: $contactsSummary
            """.trimIndent()

            val coPilotText = chatHelper.getCoPilotResponse(text, contextText)
            
            // Check for TRANSFER_ACTION short-cuts to construct the quick action buttons clickable in-app
            var shortcut: TransferShortcut? = null
            if (coPilotText.contains("[TRANSFER_ACTION]")) {
                try {
                    val instruction = coPilotText.substringAfter("[TRANSFER_ACTION]").trim().takeWhile { it != '\n' }
                    // e.g. "Send USD 15.00 to Alice Smith" or "Send BTC 0.05 to Bob Jones"
                    val tokens = instruction.split(" ")
                    val sym = tokens.getOrNull(1) ?: "USD"
                    val amt = tokens.getOrNull(2)?.toDoubleOrNull() ?: 15.0
                    val indexTo = instruction.indexOf(" to ")
                    val recipient = if (indexTo != -1) instruction.substring(indexTo + 4).trim() else "Alice Smith"
                    shortcut = TransferShortcut(amt, sym, recipient)
                } catch (e: Exception) {
                    // No action if parsing failed
                }
            }

            _chatHistory.value = _chatHistory.value + ChatMessage(
                sender = "copilot",
                text = coPilotText,
                transferAction = shortcut
            )
            _isChatLoading.value = false
        }
    }

    fun triggerQuickAction(shortcut: TransferShortcut) {
        // Attempt search for matching contact
        val matchingContact = contacts.value.find { it.name.lowercase().contains(shortcut.contactName.lowercase()) } 
            ?: contacts.value.firstOrNull()

        if (matchingContact != null) {
            setP2PSupply(
                contact = matchingContact,
                amount = shortcut.amount.toString(),
                symbol = shortcut.symbol
            )
            _currentTab.value = 1 // Switch automatically to P2P tab
            showNotification("Co-Pilot pre-filled checkout with ${matchingContact.name} for ${shortcut.amount} ${shortcut.symbol}!")
        } else {
            showError("Couldn't pre-fill transfer shortcut: Contact not found.")
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PaymentViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PaymentViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
