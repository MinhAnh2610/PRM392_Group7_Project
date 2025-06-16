// app/src/main/java/com/tutorial/project/ui/product/ProductDetailScreen.kt
package com.tutorial.project.ui.product

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.ProductWithStoreInfo
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.ProductDetailViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
  navController: NavController,
  productId: Int
) {
  val viewModel: ProductDetailViewModel = viewModel(
    key = "product_detail/$productId", // Ensure ViewModel is unique per product
    factory = GenericViewModelFactory {
      ProductDetailViewModel(
        productRepository = ProductRepository(SupabaseClientProvider.client),
        cartRepository = CartRepository(
          client = SupabaseClientProvider.client,
          authRepository = AuthRepository(SupabaseClientProvider.client.auth)
        ),
        authRepository = AuthRepository(SupabaseClientProvider.client.auth),
        productId = productId
      )
    }
  )

  val isLoggedIn = remember { viewModel.isUserLoggedIn() }

  val product by viewModel.product.observeAsState()
  val isLoading by viewModel.isLoading.observeAsState(false)
  val error by viewModel.error.observeAsState()
  val context = LocalContext.current

  val toastEvent by viewModel.toastEvent.observeAsState()
  val navigateToChat by viewModel.navigateToChat.observeAsState(null)

  LaunchedEffect(toastEvent) {
    toastEvent?.let { event ->
      Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
      viewModel.onToastShown()
    }
  }

  LaunchedEffect(navigateToChat) {
    val chatData = navigateToChat
    if (chatData != null) {
      navController.navigate(Screen.Chat.createRoute(chatData.first, chatData.second))
      viewModel.onChatNavigated() // Reset the event so it doesn't fire again
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Product Details") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      when {
        isLoading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        error != null -> {
          Text(
            text = "Error: $error",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.align(Alignment.Center)
          )
        }

        product != null -> {
          ProductDetailsContent(
            product = product!!,
            navController = navController,
            isLoggedIn = isLoggedIn,
            onAddToCart = { quantity ->
              if (isLoggedIn) {
                viewModel.addToCart(quantity)
              } else {
                // User is a guest, navigate them to login screen
                navController.navigate(Screen.Login.route)
              }
            },
            onChatClick = {
              if (isLoggedIn) {
                viewModel.onChatIconClick()
              } else {
                navController.navigate(Screen.Login.route)
              }
            }
          )
        }

        else -> {
          Text(
            text = "Product not found.",
            modifier = Modifier.align(Alignment.Center)
          )
        }
      }
    }
  }
}

@Composable
fun ProductDetailsContent(
  product: ProductWithStoreInfo,
  navController: NavController,
  isLoggedIn: Boolean,
  onAddToCart: (Int) -> Unit,
  onChatClick: () -> Unit
) {
  var quantity by remember { mutableStateOf("1") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(),
      elevation = CardDefaults.cardElevation(4.dp)
    ) {
      Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
          model = product.store_logo_url,
          contentDescription = "${product.store_name} logo",
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
          contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            product.store_name,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
          )
          product.description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
          }
        }
        // Add separate IconButtons for Map and Chat
        IconButton(onClick = {
          navController.navigate(
            Screen.Map.createRoute(
              lat = 37.7749,
              lon = -122.4194,
              name = product.store_name,
              address = "546 9th St, San Francisco, CA 94103" // Placeholder address
            )
          )
        }) {
          Icon(Icons.Default.Place, contentDescription = "View on Map")
        }
        IconButton(onClick = onChatClick) {
          Icon(Icons.Default.Email, contentDescription = "Chat with Seller")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    AsyncImage(
      model = product.image_url,
      contentDescription = product.name,
      modifier = Modifier
        .fillMaxWidth()
        .height(250.dp)
        .clip(RoundedCornerShape(12.dp)),
      contentScale = ContentScale.Crop
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = product.name,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    product.description?.let {
      Text(
        text = it,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.Gray
      )
      Spacer(modifier = Modifier.height(16.dp))
    }

    Text(
      text = "$${"%.2f".format(product.price)}",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "In Stock: ${product.stock_quantity}",
      style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      OutlinedTextField(
        value = quantity,
        onValueChange = {
          // Allow only digits and non-empty string for quantity
          if (it.isEmpty() || it.all { char -> char.isDigit() }) {
            quantity = it
          }
        },
        label = { Text("Qty") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(100.dp)
      )
      Spacer(modifier = Modifier.weight(1f))
      Button(
        onClick = { onAddToCart(quantity.toIntOrNull() ?: 1) },
        enabled = product.stock_quantity > 0 && (quantity.toIntOrNull() ?: 0) > 0,
        modifier = Modifier.height(56.dp)
      ) {
        Text(if (isLoggedIn) "Add to Cart" else "Login to Add to Cart")
      }
    }
  }
}