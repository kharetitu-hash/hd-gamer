package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.PaymentApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PaymentViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Obtain the unifying ViewModel with Custom Factory injecting the android application
    val viewModel = ViewModelProvider(
      this, 
      PaymentViewModel.Factory(application)
    )[PaymentViewModel::class.java]

    setContent {
      MyApplicationTheme {
        PaymentApp(viewModel = viewModel)
      }
    }
  }
}
