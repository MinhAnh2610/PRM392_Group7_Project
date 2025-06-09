// app/src/main/java/com/tutorial/project/ui/product/ProductDetailScreen.kt
package com.tutorial.project.ui.product

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.tutorial.project.data.model.Product
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
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
    key = "product_detail_$productId", // Ensure ViewModel is unique per product
    factory = GenericViewModelFactory {
      ProductDetailViewModel(
        productRepository = ProductRepository(SupabaseClientProvider.client),
        cartRepository = CartRepository(
          client = SupabaseClientProvider.client,
          authRepository = AuthRepository(SupabaseClientProvider.client.auth)
        ),
        productId = productId
      )
    }
  )

  val product by viewModel.product.observeAsState()
  val isLoading by viewModel.isLoading.observeAsState(false)
  val error by viewModel.error.observeAsState()
  val context = LocalContext.current

  val toastEvent by viewModel.toastEvent.observeAsState()

  LaunchedEffect(toastEvent) {
    toastEvent?.let { event ->
      Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
      viewModel.onToastShown()
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
          ProductDetailsContent(product!!, onAddToCart = { quantity ->
            viewModel.addToCart(quantity)
          })
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
fun ProductDetailsContent(product: Product, onAddToCart: (Int) -> Unit) {
  var quantity by remember { mutableStateOf("1") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
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
        Text("Add to Cart")
      }
    }
  }
}