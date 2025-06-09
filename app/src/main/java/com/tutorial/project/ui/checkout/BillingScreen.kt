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
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.BillingViewModel
import com.tutorial.project.viewmodel.OrderProcessState
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
        cartRepository = CartRepository(SupabaseClientProvider.client, authRepo)
      )
    }
  )

  val orderState by viewModel.orderProcessState.observeAsState(OrderProcessState.Idle)
  val cartItems by viewModel.cartItems.observeAsState(emptyList())
  val totalPrice by viewModel.totalPrice.observeAsState(0.0)

  val context = LocalContext.current
  var showSuccessDialog by remember { mutableStateOf<Boolean>(false) }

  // Mock fields for user info
  var name by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }

  LaunchedEffect(orderState) {
    when (val state = orderState) {
      is OrderProcessState.Success -> {
        showSuccessDialog = true
      }
      is OrderProcessState.Error -> {
        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
      }
      else -> {}
    }
  }

  if (showSuccessDialog) {
    AlertDialog(
      onDismissRequest = { },
      title = { Text("Order Placed!") },
      text = { Text("Your order has been successfully placed.") },
      confirmButton = {
        Button(onClick = {
          showSuccessDialog = false
          viewModel.onOrderProcessed()
          // Navigate to dashboard and clear the back stack
          navController.navigate(Screen.Dashboard.route) {
            popUpTo(Screen.Dashboard.route) { inclusive = true }
          }
        }) {
          Text("OK")
        }
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
      Button(
        onClick = { viewModel.placeOrder() },
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        enabled = orderState !is OrderProcessState.Processing && cartItems.isNotEmpty()
      ) {
        if (orderState is OrderProcessState.Processing) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
        } else {
          Text("Confirm & Pay $${"%.2f".format(totalPrice)}")
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

      if (cartItems.isNotEmpty()) {
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
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Total", fontWeight = FontWeight.Bold)
          Text("$${"%.2f".format(totalPrice)}", fontWeight = FontWeight.Bold)
        }
      } else if (orderState !is OrderProcessState.Processing) {
        Text("Your cart is empty or could not be loaded.")
      }

      if (orderState is OrderProcessState.Processing && cartItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
    }
  }
}