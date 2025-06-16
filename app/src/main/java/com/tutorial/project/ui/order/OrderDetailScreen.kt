// app/src/main/java/com/tutorial/project/ui/orders/OrderDetailScreen.kt
package com.tutorial.project.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.OrderItemsWithProductDetails
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.dto.UiState
import com.tutorial.project.viewmodel.OrderDetailViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, orderId: Int) {
  val viewModel: OrderDetailViewModel =
    viewModel(key = "order_detail_$orderId", factory = GenericViewModelFactory {
      OrderDetailViewModel(
        OrderRepository(
          SupabaseClientProvider.client,
          AuthRepository(SupabaseClientProvider.client.auth)
        ),
        orderId = orderId
      )
    })

  val state by viewModel.orderDetailState.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Order #$orderId") },
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
      when (val uiState = state) {
        is UiState.Loading -> CircularProgressIndicator()
        is UiState.Error -> Text("Error: ${uiState.message}")
        is UiState.Success -> {
          LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(uiState.data, key = { it.id }) { item ->
              OrderItemCard(item = item)
              Spacer(modifier = Modifier.height(12.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun OrderItemCard(item: OrderItemsWithProductDetails) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
      AsyncImage(
        model = item.product_image_url,
        contentDescription = item.product_name,
        modifier = Modifier
          .size(64.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.product_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Quantity: ${item.quantity}")
        Text("Price each: $${"%.2f".format(item.price)}")
      }
    }
  }
}