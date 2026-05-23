package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Contact
import com.example.data.Transaction
import com.example.data.CryptoWallet
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.PaymentViewModel
import com.example.viewmodel.TransferShortcut
import java.text.SimpleDateFormat
import java.util.*

// Space Titanium Dark Theme values local for consistency
object PayUiTheme {
    val DarkSlateBg = Color(0xFF0F1218)
    val CardNavy = Color(0xFF161C26)
    val GlassOverlay = Color(0x1FDDDDDD)
    
    val NeonEmerald = Color(0xFF00E676)
    val NeonBlue = Color(0xFF2979FF)
    val BrightPurple = Color(0xFFD500F9)
    val GoldAmber = Color(0xFFFFD600)
    
    val LightGray = Color(0xFFECEFF1)
    val TextMuted = Color(0xFF90A4AE)
    val DarkDivider = Color(0xFF263238)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentApp(viewModel: PaymentViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val notificationMessage by viewModel.notificationMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Retrieve database observers
    val fiatWallet by viewModel.fiatWallet.collectAsState()
    val cryptoWallets by viewModel.cryptoWallets.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cryptoRates by viewModel.cryptoRates.collectAsState()

    // Dialog state for cash edits (Deposit / Withdraw details)
    var showCashDialog by remember { mutableStateOf(false) }
    var dialogIsDeposit by remember { mutableStateOf(true) }

    // Dialog state for new contact creation
    var showNewContactDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = PayUiTheme.DarkSlateBg,
        bottomBar = {
            NavigationBar(
                containerColor = PayUiTheme.CardNavy,
                tonalElevation = 8.dp
            ) {
                val menuItems = listOf(
                    Triple("Dashboard", Icons.Default.Wallet, 0),
                    Triple("P2P Pay", Icons.Default.SwapHoriz, 1),
                    Triple("Crypto", Icons.Default.TrendingUp, 2),
                    Triple("AI Advisor", Icons.Default.SmartToy, 3)
                )

                menuItems.forEach { (label, icon, index) ->
                    val selected = currentTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(index) },
                        icon = { 
                            Icon(
                                imageVector = icon, 
                                contentDescription = label,
                                tint = if (selected) PayUiTheme.NeonEmerald else PayUiTheme.TextMuted
                            ) 
                        },
                        label = { 
                            Text(
                                text = label, 
                                color = if (selected) Color.White else PayUiTheme.TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = PayUiTheme.DarkDivider
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen router
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "TabTransition"
            ) { activeIndex ->
                when (activeIndex) {
                    0 -> DashboardScreen(
                        fiatWallet = fiatWallet,
                        cryptoWallets = cryptoWallets,
                        cryptoRates = cryptoRates,
                        transactions = transactions,
                        contacts = contacts,
                        onTransferNow = { contact ->
                            viewModel.setP2PSupply(contact, "", "USD")
                            viewModel.selectTab(1)
                        },
                        onOpenCashAdjust = { isDep ->
                            dialogIsDeposit = isDep
                            showCashDialog = true
                        }
                    )
                    1 -> P2PTransferScreen(
                        contacts = contacts,
                        selectedContact = viewModel.selectedP2PContact.collectAsState().value,
                        amountValue = viewModel.p2pAmount.collectAsState().value,
                        symbolSelected = viewModel.p2pSymbol.collectAsState().value,
                        fiatWallet = fiatWallet,
                        cryptoWallets = cryptoWallets,
                        onContactSelect = { viewModel.setP2PSupply(it, viewModel.p2pAmount.value, viewModel.p2pSymbol.value) },
                        onAmountChanged = { viewModel.setP2PSupply(viewModel.selectedP2PContact.value, it, viewModel.p2pSymbol.value) },
                        onSymbolChanged = { viewModel.setP2PSupply(viewModel.selectedP2PContact.value, viewModel.p2pAmount.value, it) },
                        onAddContactClicked = { showNewContactDialog = true },
                        onExecuteTransfer = { viewModel.executeP2PTransfer() }
                    )
                    2 -> CryptoTerminalScreen(
                        cryptoWallets = cryptoWallets,
                        fiatWallet = fiatWallet,
                        cryptoRates = cryptoRates,
                        currentSymbol = viewModel.selectedExchangeSymbol.collectAsState().value,
                        exchangeAmount = viewModel.exchangeAmount.collectAsState().value,
                        isBuyMode = viewModel.isBuyMode.collectAsState().value,
                        onSymbolChanged = { viewModel.setExchangeSymbol(it) },
                        onAmountChanged = { viewModel.setExchangeAmount(it) },
                        onToggleMode = { viewModel.toggleBuySellMode() },
                        onExecuteExchange = { viewModel.executeExchange() },
                        onRefreshPrices = { viewModel.refreshMarketPrices() },
                        onTriggerFaucet = { viewModel.triggerFaucetRequest(it) },
                        faucetLoading = viewModel.faucetLoading.collectAsState().value,
                        externalAddress = viewModel.externalSendAddress.collectAsState().value,
                        externalAmount = viewModel.externalSendAmount.collectAsState().value,
                        onExternalFieldsChanged = { addr, amt -> viewModel.setExternalWithdrawFields(addr, amt) },
                        onExecuteExternalSend = { viewModel.sendExternalCrypto() }
                    )
                    3 -> AiAdvisorScreen(
                        chatHistory = viewModel.chatHistory.collectAsState().value,
                        chatInput = viewModel.chatInput.collectAsState().value,
                        isChatLoading = viewModel.isChatLoading.collectAsState().value,
                        onInputChange = { viewModel.setChatInput(it) },
                        onSendMessage = { viewModel.sendCopilotMessage() },
                        onQuickActionClick = { viewModel.triggerQuickAction(it) }
                    )
                }
            }

            // In-app Notification overlays (Toasts)
            notificationMessage?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PayUiTheme.NeonBlue),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = msg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.dismissNotification() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }

            errorMessage?.let { err ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = err, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.dismissError() }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Cash Adjust Dialog (Deposit / Withdraw sandbox helper)
    if (showCashDialog) {
        var inputVal by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showCashDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (dialogIsDeposit) Icons.Default.FileDownload else Icons.Default.FileUpload,
                        contentDescription = "Cash Operation",
                        tint = PayUiTheme.NeonEmerald,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (dialogIsDeposit) "Deposit Cash Reserves" else "Withdraw Out Cash",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulating ACH Bank balance transfers on sandbox.",
                        color = PayUiTheme.TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputVal,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) inputVal = it },
                        label = { Text("Amount ($)", color = PayUiTheme.TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(color = Color.White),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick tags presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(50, 200, 500, 1000).forEach { qty ->
                            Button(
                                onClick = { inputVal = qty.toString() },
                                colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.DarkDivider),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Text(text = "$$qty", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCashDialog = false }) {
                            Text("Cancel", color = PayUiTheme.TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                val amtNum = inputVal.toDoubleOrNull() ?: 0.0
                                if (amtNum > 0) {
                                    if (dialogIsDeposit) {
                                        viewModel.depositCash(amtNum)
                                    } else {
                                        viewModel.withdrawCash(amtNum)
                                    }
                                    showCashDialog = false
                                } else {
                                    viewModel.showError("Please enter a valid cash amount.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonEmerald)
                        ) {
                            Text(text = if (dialogIsDeposit) "Deposit" else "Withdraw", color = PayUiTheme.DarkSlateBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // New Contact Dial
    if (showNewContactDialog) {
        var addName by remember { mutableStateOf("") }
        var addPhone by remember { mutableStateOf("") }
        var addEmail by remember { mutableStateOf("") }
        var addAddress by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showNewContactDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Contact", tint = PayUiTheme.NeonBlue, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Add Peer Contact", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Full Name", color = PayUiTheme.TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = addPhone,
                        onValueChange = { addPhone = it },
                        label = { Text("Phone Number", color = PayUiTheme.TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = addEmail,
                        onValueChange = { addEmail = it },
                        label = { Text("Email Address", color = PayUiTheme.TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    OutlinedTextField(
                        value = addAddress,
                        onValueChange = { addAddress = it },
                        label = { Text("Wallet Addr (Optional)", color = PayUiTheme.TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showNewContactDialog = false }) {
                            Text("Discard", color = PayUiTheme.TextMuted)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                if (addName.isNotBlank() && addPhone.isNotBlank()) {
                                    viewModel.addNewContact(addName, addPhone, addEmail, addAddress)
                                    showNewContactDialog = false
                                } else {
                                    viewModel.showError("Name and Phone fields are mandatory.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonBlue)
                        ) {
                            Text(text = "Save", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// SCREENS
// ============================================

@Composable
fun DashboardScreen(
    fiatWallet: com.example.data.FiatWallet?,
    cryptoWallets: List<CryptoWallet>,
    cryptoRates: Map<String, Double>,
    transactions: List<Transaction>,
    contacts: List<Contact>,
    onTransferNow: (Contact) -> Unit,
    onOpenCashAdjust: (Boolean) -> Unit
) {
    // Math calculating balance aggregates
    val cashValue = fiatWallet?.balance ?: 0.0
    val cryptoValueSum = cryptoWallets.sumOf { wallet ->
        val activeRate = cryptoRates[wallet.symbol] ?: wallet.usdRateOnLaunch
        wallet.balance * activeRate
    }
    val aggregateNetWorth = cashValue + cryptoValueSum

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, Partner",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "kharetitu@gmail.com",
                        color = PayUiTheme.TextMuted,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PayUiTheme.NeonEmerald, PayUiTheme.NeonBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "K", color = PayUiTheme.DarkSlateBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        // Net Aggregate Portfolio Card (Futuristic Look)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Subtle background graphics for credit-card look
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2979FF).copy(0.12f), Color.Transparent),
                                    center = Offset(this.size.width * 0.85f, this.size.height * 0.15f),
                                    radius = this.size.width * 0.5f
                                )
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "TOTAL ASSETS NET WORTH",
                            color = PayUiTheme.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%,.2f", aggregateNetWorth)}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Divider(color = PayUiTheme.DarkDivider, thickness = 1.dp)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "CASH (USD)", color = PayUiTheme.TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "$${String.format("%,.2f", cashValue)}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "BLOCKCHAIN (USD)", color = PayUiTheme.TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "$${String.format("%,.2f", cryptoValueSum)}",
                                    color = PayUiTheme.NeonEmerald,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cash Deposit/Withdraw Quick triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onOpenCashAdjust(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonEmerald),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Deposit", tint = PayUiTheme.DarkSlateBg, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Deposit", color = PayUiTheme.DarkSlateBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onOpenCashAdjust(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.DarkDivider),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Withdraw", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Withdraw", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Crypto Portfolio breakdown list
        item {
            Text(
                text = "My Crypto Balances",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cryptoWallets.forEach { cw ->
                    val rate = cryptoRates[cw.symbol] ?: cw.usdRateOnLaunch
                    val tokenValue = cw.balance * rate

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(getCoinColor(cw.symbol).copy(0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cw.symbol.take(2),
                                        color = getCoinColor(cw.symbol),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = cw.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Rate: $${String.format("%,.2f", rate)}",
                                        color = PayUiTheme.TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format("%,.2f", tokenValue)}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cw.balance} ${cw.symbol}",
                                    color = PayUiTheme.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Transfer horizontal bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Send",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No contacts saved yet.", color = PayUiTheme.TextMuted, fontSize = 13.sp)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(contacts) { peer ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onTransferNow(peer) }
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(peer.avatarColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = peer.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = peer.name.substringBefore(" "),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(56.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Transactions list section
        item {
            Text(
                text = "Recent Transactions",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy)
                ) {
                    Text(
                        text = "History empty. Tap deposit to get started!",
                        color = PayUiTheme.TextMuted,
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(transactions.take(8)) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(getTransactionBgColor(tx.type).copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getTransactionIcon(tx.type),
                        contentDescription = tx.type,
                        tint = getTransactionBgColor(tx.type),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = getTransactionTitle(tx),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(170.dp)
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(tx.timestamp)),
                        color = PayUiTheme.TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = getTransactionValueText(tx),
                    color = getTransactionBgColor(tx.type),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                if (tx.symbol != "USD") {
                    Text(
                        text = "$${String.format("%,.2f", tx.usdValue)}",
                        color = PayUiTheme.TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// P2P TRANSFER PAGE
@Composable
fun P2PTransferScreen(
    contacts: List<Contact>,
    selectedContact: Contact?,
    amountValue: String,
    symbolSelected: String,
    fiatWallet: com.example.data.FiatWallet?,
    cryptoWallets: List<CryptoWallet>,
    onContactSelect: (Contact) -> Unit,
    onAmountChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onAddContactClicked: () -> Unit,
    onExecuteTransfer: () -> Unit
) {
    var queryStr by remember { mutableStateOf("") }
    val filteredContacts = contacts.filter { it.name.contains(queryStr, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "Send Peer-to-Peer", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = "Transfer assets instantaneously to save friction.", color = PayUiTheme.TextMuted, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented wallet balance visual helper
        val activeBal = if (symbolSelected == "USD") {
            fiatWallet?.balance ?: 0.0
        } else {
            cryptoWallets.find { it.symbol == symbolSelected }?.balance ?: 0.0
        }
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "YOUR AVAILABLE BALANCE", color = PayUiTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (symbolSelected == "USD") "$${String.format("%,.2f", activeBal)}" else "$activeBal $symbolSelected",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PayUiTheme.DarkDivider)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = symbolSelected, color = getCoinColor(symbolSelected), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Picker for transfer symbol
        Text(text = "Transfer Currency", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("USD", "BTC", "ETH", "SOL", "USDC").forEach { sym ->
                val chosen = symbolSelected == sym
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (chosen) getCoinColor(sym) else PayUiTheme.CardNavy)
                        .clickable { onSymbolChanged(sym) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sym,
                        color = if (chosen) PayUiTheme.DarkSlateBg else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipient contacts search & grid picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Select Recipient", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAddContactClicked) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp), tint = PayUiTheme.NeonBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add Peer", color = PayUiTheme.NeonBlue, fontSize = 13.sp)
            }
        }

        OutlinedTextField(
            value = queryStr,
            onValueChange = { queryStr = it },
            placeholder = { Text("Search by email or phone...", color = PayUiTheme.TextMuted) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PayUiTheme.TextMuted) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                focusedContainerColor = PayUiTheme.CardNavy,
                unfocusedContainerColor = PayUiTheme.CardNavy
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                if (filteredContacts.isEmpty()) {
                    Text(
                        text = "No compatible peers found.",
                        color = PayUiTheme.TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    filteredContacts.forEach { peer ->
                        val active = selectedContact?.id == peer.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (active) PayUiTheme.DarkDivider else Color.Transparent)
                                .clickable { onContactSelect(peer) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(peer.avatarColorHex))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = peer.name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = peer.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = peer.phone, color = PayUiTheme.TextMuted, fontSize = 11.sp)
                            }

                            if (active) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = PayUiTheme.NeonEmerald)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Enter Transfer Value Input
        Text(text = "Specify Amount", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amountValue,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onAmountChanged(it) },
            placeholder = { Text("0.00", color = PayUiTheme.TextMuted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Text(text = if (symbolSelected == "USD") "$" else "Ξ", color = PayUiTheme.TextMuted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                focusedContainerColor = PayUiTheme.CardNavy,
                unfocusedContainerColor = PayUiTheme.CardNavy
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Button(
            onClick = onExecuteTransfer,
            colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonEmerald),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = PayUiTheme.DarkSlateBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (selectedContact != null) "Send to ${selectedContact.name}" else "Execute Transfer",
                color = PayUiTheme.DarkSlateBg,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// CRYPTO TERMINAL SCREEN (MARKET PRICES AND TRADING)
@Composable
fun CryptoTerminalScreen(
    cryptoWallets: List<CryptoWallet>,
    fiatWallet: com.example.data.FiatWallet?,
    cryptoRates: Map<String, Double>,
    currentSymbol: String,
    exchangeAmount: String,
    isBuyMode: Boolean,
    onSymbolChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onToggleMode: () -> Unit,
    onExecuteExchange: () -> Unit,
    onRefreshPrices: () -> Unit,
    onTriggerFaucet: (String) -> Unit,
    faucetLoading: Boolean,
    externalAddress: String,
    externalAmount: String,
    onExternalFieldsChanged: (String, String) -> Unit,
    onExecuteExternalSend: () -> Unit
) {
    var terminalSubTab by remember { mutableStateOf(0) } // 0: Exchange buy/sell, 1: Outer Blockchain Send

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Crypto Terminal", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(text = "Analyze live-simulated rates and swap portfolio models.", color = PayUiTheme.TextMuted, fontSize = 12.sp)
            }

            IconButton(onClick = onRefreshPrices) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = PayUiTheme.NeonEmerald)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Rates Grid with elegant Custom Sparkline Charts!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("BTC", "ETH", "SOL", "USDC").forEach { sym ->
                val isSelected = currentSymbol == sym
                val currentRate = cryptoRates[sym] ?: 1.0

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clickable { onSymbolChanged(sym) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PayUiTheme.DarkDivider else PayUiTheme.CardNavy
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sym, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(getCoinColor(sym)))
                        }

                        // Unique Custom Canvas Graphic Sparkline rendering simulated asset price curves!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .padding(vertical = 4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val path = Path()
                                val points = getSparklinePoints(sym, currentRate)
                                if (points.size >= 2) {
                                    val stepX = this.size.width / (points.size - 1)
                                    val maxPrice = points.maxOrNull() ?: 1.0
                                    val minPrice = points.minOrNull() ?: 0.0
                                    val priceRange = maxPrice - minPrice
                                    val safeRange = if (priceRange == 0.0) 1.0 else priceRange

                                    points.forEachIndexed { idx, point ->
                                        // Map point to canvas coordinate structure
                                        val x = idx * stepX
                                        val y = this.size.height - (((point - minPrice) / safeRange) * this.size.height).toFloat()
                                        if (idx == 0) {
                                            path.moveTo(x, y)
                                        } else {
                                            path.lineTo(x, y)
                                        }
                                    }

                                    drawPath(
                                        path = path,
                                        color = getCoinColor(sym),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (sym == "USDC") "$$currentRate" else "$${String.format("%,.1f", currentRate)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Tabs (Buy/Sell Swap vs Withdraw/Withdraw External)
        TabRow(
            selectedTabIndex = terminalSubTab,
            containerColor = PayUiTheme.CardNavy,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[terminalSubTab]),
                    color = PayUiTheme.NeonEmerald
                )
            }
        ) {
            Tab(selected = terminalSubTab == 0, onClick = { terminalSubTab = 0 }, text = { Text("Local Exchange", fontSize = 13.sp, color = Color.White) })
            Tab(selected = terminalSubTab == 1, onClick = { terminalSubTab = 1 }, text = { Text("On-Chain Send", fontSize = 13.sp, color = Color.White) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (terminalSubTab == 0) {
            // Option 0: Exchange Terminal
            Card(
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "SWAP INSTANTLY", color = PayUiTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBuyMode) "Buy $currentSymbol tokens" else "Sell $currentSymbol tokens",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onToggleMode) {
                            Icon(imageVector = Icons.Default.SwapVert, contentDescription = "Toggle swap direction", tint = PayUiTheme.NeonEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val cashAvailable = fiatWallet?.balance ?: 0.0
                    val cryptoWallet = cryptoWallets.find { it.symbol == currentSymbol }
                    val cryptoAvailable = cryptoWallet?.balance ?: 0.0
                    val symbolRate = cryptoRates[currentSymbol] ?: 1.0

                    // Mode text
                    if (isBuyMode) {
                        Text(text = "Settles in USD. Cash reserves: $$cashAvailable", color = PayUiTheme.TextMuted, fontSize = 11.sp)
                    } else {
                        Text(text = "Settles in USD. Crypto reserves: $cryptoAvailable $currentSymbol", color = PayUiTheme.TextMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = exchangeAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onAmountChanged(it) },
                        label = {
                            Text(
                                text = if (isBuyMode) "Purchase USD Value ($)" else "Crypto Quantity ($currentSymbol)",
                                color = PayUiTheme.TextMuted
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = PayUiTheme.DarkSlateBg,
                            unfocusedContainerColor = PayUiTheme.DarkSlateBg
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Conversion calculator review details
                    val enteredVal = exchangeAmount.toDoubleOrNull() ?: 0.0
                    val reviewString = if (isBuyMode) {
                        val tokenQty = if (symbolRate > 0.0) enteredVal / symbolRate else 0.0
                        "You will receive approx: ~${String.format("%.5f", tokenQty)} $currentSymbol"
                    } else {
                        val usdValue = enteredVal * symbolRate
                        "You will get back approx: ~$$String.format(\"%,.2f\", usdValue) USD"
                    }

                    if (enteredVal > 0) {
                        Text(text = reviewString, color = PayUiTheme.NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onExecuteExchange,
                        colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBuyMode) "Confirm Buy Block" else "Confirm Liquidation",
                            color = PayUiTheme.DarkSlateBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sandbox Faucet module
            Card(
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SANDBOX FAUCET DROP", color = PayUiTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Request Testnet Tokens", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Claims instantly inject mock cryptocurrency into your balances to enable full P2P sandbox testing.", color = PayUiTheme.TextMuted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BTC", "ETH", "SOL", "USDC").forEach { claimToken ->
                            Button(
                                onClick = { onTriggerFaucet(claimToken) },
                                colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.DarkDivider),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                enabled = !faucetLoading
                            ) {
                                Text(text = "+$claimToken", fontSize = 11.sp, color = getCoinColor(claimToken))
                            }
                        }
                    }
                }
            }
        } else {
            // Option 1: On-Chain Send/Withdraw
            Card(
                colors = CardDefaults.cardColors(containerColor = PayUiTheme.CardNavy),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "WITHDRAW VIA BLOCKCHAIN", color = PayUiTheme.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Send $currentSymbol Coin Outward", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = externalAddress,
                        onValueChange = { onExternalFieldsChanged(it, externalAmount) },
                        label = { Text("Recipient Native Crypto Address", color = PayUiTheme.TextMuted) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = PayUiTheme.DarkSlateBg,
                            unfocusedContainerColor = PayUiTheme.DarkSlateBg
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = externalAmount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onExternalFieldsChanged(externalAddress, it) },
                        label = { Text("Token Quantity ($currentSymbol)", color = PayUiTheme.TextMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            focusedContainerColor = PayUiTheme.DarkSlateBg,
                            unfocusedContainerColor = PayUiTheme.DarkSlateBg
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onExecuteExternalSend,
                        colors = ButtonDefaults.buttonColors(containerColor = PayUiTheme.NeonBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Sign & Transmit Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// AI COPILOT CHAT SCREEN
@Composable
fun AiAdvisorScreen(
    chatHistory: List<ChatMessage>,
    chatInput: String,
    isChatLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onQuickActionClick: (TransferShortcut) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PayUiTheme.NeonBlue.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.SmartToy, contentDescription = "AI", tint = PayUiTheme.NeonBlue, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "PayCrypto Co-Pilot", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(text = "Generative financial assistant and transfer draftsman.", color = PayUiTheme.TextMuted, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Messages scrolling log
        val scrollState = rememberScrollState()
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = false
        ) {
            items(chatHistory) { msg ->
                val isUsr = msg.sender == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUsr) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp, 
                                    topEnd = 16.dp, 
                                    bottomStart = if (isUsr) 16.dp else 4.dp, 
                                    bottomEnd = if (isUsr) 4.dp else 16.dp
                                )
                            )
                            .background(if (isUsr) PayUiTheme.NeonBlue else PayUiTheme.CardNavy)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .widthIn(max = 280.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    // Quick confirmation CTA details under messages mapping [TRANSFER_ACTION]
                    msg.transferAction?.let { shortcut ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PayUiTheme.DarkDivider),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clickable { onQuickActionClick(shortcut) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Receipt, contentDescription = "Receipt", tint = PayUiTheme.NeonEmerald, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Confirm $${shortcut.amount} to ${shortcut.contactName}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(150.dp)
                                    )
                                }
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Go", tint = PayUiTheme.NeonEmerald, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PayUiTheme.CardNavy)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = "Co-pilot formulating insights...", color = PayUiTheme.TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick query tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Help me" to "Help me understand this interface.",
                "Portfolio Splts" to "Suggest a standard cryptocurrency portfolio split.",
                "Send $15 Alice" to "Draft a transfer of $15 to Alice."
            ).forEach { (lbl, qry) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PayUiTheme.CardNavy)
                        .clickable { onInputChange(qry) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = lbl, color = PayUiTheme.NeonEmerald, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat text controller Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onInputChange,
                placeholder = { Text("Ask your Co-pilot financial advices...", color = PayUiTheme.TextMuted, fontSize = 13.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    focusedContainerColor = PayUiTheme.CardNavy,
                    unfocusedContainerColor = PayUiTheme.CardNavy
                ),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendMessage,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(PayUiTheme.NeonBlue)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "Send prompt", tint = Color.White)
            }
        }
    }
}

// ============================================
// COLOR AND HELPER UTILS
// ============================================

fun getCoinColor(symbol: String): Color = when (symbol) {
    "BTC" -> Color(0xFFFF9800)
    "ETH" -> Color(0xFF9C27B0)
    "SOL" -> Color(0xFF00E5FF)
    "USDC" -> Color(0xFF2196F3)
    else -> PayUiTheme.NeonEmerald
}

fun getSparklinePoints(symbol: String, curRate: Double): List<Double> {
    // Generates a mock curve representing history based on the current rates to look hyper real
    return when (symbol) {
        "BTC" -> listOf(curRate * 0.985, curRate * 0.99, curRate * 0.978, curRate * 0.995, curRate * 1.01, curRate * 0.992, curRate)
        "ETH" -> listOf(curRate * 1.02, curRate * 1.01, curRate * 0.98, curRate * 0.99, curRate * 0.995, curRate * 1.005, curRate)
        "SOL" -> listOf(curRate * 0.94, curRate * 0.96, curRate * 1.03, curRate * 0.99, curRate * 1.04, curRate * 0.98, curRate)
        "USDC" -> listOf(1.0001, 0.9999, 1.0002, 0.9998, 1.0000, 1.0001, curRate)
        else -> listOf(curRate, curRate, curRate, curRate, curRate, curRate, curRate)
    }
}

fun getTransactionIcon(type: String): ImageVector = when (type) {
    "DEPOSIT" -> Icons.Default.ArrowDownward
    "WITHDRAW" -> Icons.Default.ArrowUpward
    "P2P_SEND" -> Icons.Default.ArrowForward
    "P2P_RECEIVE" -> Icons.Default.ArrowBack
    "CRYPTO_BUY" -> Icons.Default.ShoppingCart
    "CRYPTO_SELL" -> Icons.Default.Sell
    "CRYPTO_SEND" -> Icons.Default.Output
    "CRYPTO_RECEIVE" -> Icons.Default.Input
    else -> Icons.Default.AccountBalanceWallet
}

fun getTransactionBgColor(type: String): Color = when {
    type.contains("RECEIVE") || type == "DEPOSIT" || type == "CRYPTO_BUY" -> PayUiTheme.NeonEmerald
    type.contains("SEND") || type == "WITHDRAW" || type == "CRYPTO_SELL" -> PayUiTheme.NeonBlue
    else -> PayUiTheme.GoldAmber
}

fun getTransactionTitle(tx: Transaction): String = when (tx.type) {
    "DEPOSIT" -> "Deposit Settled"
    "WITHDRAW" -> "ACH Withdraw"
    "P2P_SEND" -> "Paid ${tx.counterpartyName ?: "Partner"}"
    "P2P_RECEIVE" -> "Received from ${tx.counterpartyName ?: "Partner"}"
    "CRYPTO_BUY" -> "Acquired ${tx.symbol}"
    "CRYPTO_SELL" -> "Liquidated ${tx.symbol}"
    "CRYPTO_SEND" -> "Transmitted ${tx.symbol} Out"
    "CRYPTO_RECEIVE" -> "Claimed Faucet ${tx.symbol}"
    else -> "Vault Transfer"
}

fun getTransactionValueText(tx: Transaction): String {
    val sign = if (tx.type.contains("RECEIVE") || tx.type == "DEPOSIT" || tx.type == "CRYPTO_BUY") "+" else "-"
    return if (tx.symbol == "USD") {
        "$sign$${String.format("%,.2f", tx.amount)}"
    } else {
        "$sign${tx.amount} ${tx.symbol}"
    }
}
