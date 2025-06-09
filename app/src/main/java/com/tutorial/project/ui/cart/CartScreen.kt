package com.tutorial.project.ui.cart

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.CartViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController) {
  val viewModel: CartViewModel = viewModel(
    factory = GenericViewModelFactory {
      val authRepo = AuthRepository(SupabaseClientProvider.client.auth)
      val cartRepo = CartRepository(SupabaseClientProvider.client, authRepo)
      CartViewModel(cartRepo)
    }
  )

  val cartItems by viewModel.cartItems.observeAsState(emptyList())
  val totalPrice by viewModel.totalPrice.observeAsState(0.0)
  val isLoading by viewModel.isLoading.observeAsState(false)
  val error by viewModel.error.observeAsState()
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.loadCartItems()
  }

  LaunchedEffect(error) {
    error?.let {
      Toast.makeText(context, it, Toast.LENGTH_LONG).show()
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("My Cart") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        CartSummary(totalPrice = totalPrice, onCheckout = {
          navController.navigate(Screen.Billing.route)
        })
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when {
        isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        cartItems.isEmpty() -> {
          Text(
            "Your cart is empty.",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.bodyLarge
          )
        }

        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
          ) {
            items(cartItems, key = { it.cart_item_id }) { item ->
              CartItemCard(
                item = item,
                onQuantityChange = { newQuantity ->
                  viewModel.updateQuantity(item.cart_item_id, newQuantity)
                },
                onRemoveItem = {
                  viewModel.removeItem(item.cart_item_id)
                }
              )
              Spacer(modifier = Modifier.height(12.dp))
            }
          }
        }
      }
    }
  }
}

@Composable
fun CartItemCard(
  item: CartItemWithProductDetails,
  onQuantityChange: (Int) -> Unit,
  onRemoveItem: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(4.dp)
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = item.product_image_url,
        contentDescription = item.product_name,
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(item.product_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("$${"%.2f".format(item.product_price)}", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        QuantitySelector(
          quantity = item.quantity,
          onQuantityChange = onQuantityChange
        )
      }
      IconButton(onClick = onRemoveItem) {
        Icon(
          Icons.Default.Delete,
          contentDescription = "Remove Item",
          tint = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}

@Composable
fun QuantitySelector(quantity: Int, onQuantityChange: (Int) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = { onQuantityChange(quantity - 1) }, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Default.ArrowBack, contentDescription = "Decrease Quantity")
    }
    Text(
      text = quantity.toString(),
      modifier = Modifier.padding(horizontal = 8.dp),
      fontWeight = FontWeight.Bold
    )
    IconButton(onClick = { onQuantityChange(quantity + 1) }, modifier = Modifier.size(32.dp)) {
      Icon(Icons.Default.ArrowForward, contentDescription = "Increase Quantity")
    }
  }
}

@Composable
fun CartSummary(totalPrice: Double, onCheckout: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(8.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Total:", style = MaterialTheme.typography.titleLarge)
        Text(
          "$${"%.2f".format(totalPrice)}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Button(
        onClick = onCheckout,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("PROCEED TO CHECKOUT")
      }
    }
  }
}