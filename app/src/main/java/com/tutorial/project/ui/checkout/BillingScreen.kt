// app/src/main/java/com/tutorial/project/ui/billing/BillingScreen.kt
package com.tutorial.project.ui.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.repository.StripeRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.BillingViewModel
import com.tutorial.project.viewmodel.PaymentStatus
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(navController: NavController) {
  val viewModel: BillingViewModel = viewModel(
    factory = GenericViewModelFactory {
      val authRepo = AuthRepository(SupabaseClientProvider.client.auth)
      BillingViewModel(
        orderRepository = OrderRepository(SupabaseClientProvider.client, authRepo),
        cartRepository = CartRepository(SupabaseClientProvider.client, authRepo),
        stripeRepository = StripeRepository(SupabaseClientProvider.client)
      )
    }
  )

  val paymentStatus by viewModel.paymentStatus.observeAsState(PaymentStatus.Idle)
  val cartItems by viewModel.cartItems.observeAsState(emptyList())
  val totalPrice by viewModel.totalPrice.observeAsState(0.0)

  val paymentSheet = rememberPaymentSheet(
    paymentResultCallback = { result ->
      when (result) {
        is PaymentSheetResult.Completed -> viewModel.onPaymentSuccess()
        is PaymentSheetResult.Canceled -> viewModel.onPaymentError("Payment canceled by user.")
        is PaymentSheetResult.Failed -> viewModel.onPaymentError(result.error.message ?: "Payment failed.")
      }
    }
  )

  val context = LocalContext.current
  var showSuccessDialog by remember { mutableStateOf(false) }

  LaunchedEffect(paymentStatus) {
    when (val status = paymentStatus) {
      is PaymentStatus.Success -> {
        showSuccessDialog = true
      }

      is PaymentStatus.Error -> {
        Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
      }

      else -> {}
    }
  }

  if (showSuccessDialog) {
    AlertDialog(
      onDismissRequest = { /* Prevent dismissing */ },
      title = { Text("Payment Successful!") },
      text = { Text("Your order has been confirmed.") },
      confirmButton = {
        Button(onClick = {
          showSuccessDialog = false
          navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Dashboard.route) { inclusive = true }
          }
        }) { Text("OK") }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Checkout") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      val currentStatus = paymentStatus
      Button(
        onClick = {
          if (currentStatus is PaymentStatus.ReadyToPay) {
            paymentSheet.presentWithPaymentIntent(
              currentStatus.clientSecret,
              PaymentSheet.Configuration(
                merchantDisplayName = "My Awesome Store", // Change this
                customer = currentStatus.customerConfig
              )
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        enabled = currentStatus is PaymentStatus.ReadyToPay
      ) {
        if (paymentStatus is PaymentStatus.Processing || paymentStatus is PaymentStatus.Idle) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text("Proceed to Pay $${"%.2f".format(totalPrice)}")
        }
      }
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .padding(16.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Text("Order Summary", style = MaterialTheme.typography.titleLarge)
      Spacer(modifier = Modifier.height(16.dp))

      cartItems.forEach { item ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("${item.product_name} (x${item.quantity})")
          Text("$${"%.2f".format(item.product_price * item.quantity)}")
        }
      }
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Total", fontWeight = FontWeight.Bold)
        Text("$${"%.2f".format(totalPrice)}", fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(24.dp))

      if (paymentStatus is PaymentStatus.Error) {
        Text(
          text = (paymentStatus as PaymentStatus.Error).message,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
      }
    }
  }
}