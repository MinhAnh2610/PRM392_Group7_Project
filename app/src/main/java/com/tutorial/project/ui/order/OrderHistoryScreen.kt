// app/src/main/java/com/tutorial/project/ui/orders/OrderHistoryScreen.kt
package com.tutorial.project.ui.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.Order
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.viewmodel.OrderHistoryViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(navController: NavController) {
  val viewModel: OrderHistoryViewModel = viewModel(factory = GenericViewModelFactory {
    OrderHistoryViewModel(
      OrderRepository(
        SupabaseClientProvider.client,
        AuthRepository(SupabaseClientProvider.client.auth)
      )
    )
  })

  val ordersState by viewModel.ordersState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("My Orders") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Box(modifier = Modifier
      .fillMaxSize()
      .padding(padding), contentAlignment = Alignment.Center) {
      when (val state = ordersState) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Error -> Text("Error: ${state.message}")
        is UiState.Success -> {
          if (state.data.isEmpty()) {
            Text("You have no past orders.")
          } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
              items(state.data, key = { it.id!! }) { order ->
                OrderHistoryCard(order = order, onClick = {
                  navController.navigate(Screen.OrderDetail.createRoute(order.id!!))
                })
                Spacer(modifier = Modifier.height(12.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun OrderHistoryCard(order: Order, onClick: () -> Unit) {
  val formattedDate = remember(order.created_at) {
    try {
      val dateString = order.created_at?.split("T")?.firstOrNull() ?: ""
      val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      val date = simpleDateFormat.parse(dateString)
      SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date ?: Date())
    } catch (e: Exception) {
      "Unknown Date"
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    elevation = CardDefaults.cardElevation(4.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("Order #${order.id}", fontWeight = FontWeight.Bold)
      Text("Date: $formattedDate")
      Text("Total: $${"%.2f".format(order.total_amount)}")
      Text("Status: ${order.status.replaceFirstChar { it.uppercase() }}")
    }
  }
}