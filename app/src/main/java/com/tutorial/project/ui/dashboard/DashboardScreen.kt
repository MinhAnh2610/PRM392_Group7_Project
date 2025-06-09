package com.tutorial.project.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import com.tutorial.project.data.model.Product
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.ui.product.ProductDetailScreen
import com.tutorial.project.viewmodel.DashboardViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  navController: NavController
) {
  val viewModel: DashboardViewModel = viewModel(
    factory = GenericViewModelFactory {
      DashboardViewModel(productRepository = ProductRepository(SupabaseClientProvider.client),
        cartRepository = CartRepository(
          client = SupabaseClientProvider.client,
          authRepository = AuthRepository(SupabaseClientProvider.client.auth)
        ),)
    }
  )

  val productList by viewModel.products.observeAsState(emptyList())
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
      TopAppBar(title = { Text("Product Sale") })
    }
  ) { padding ->
    Column(modifier = Modifier.padding(padding)) {
      error?.let {
        Text("Error: $it", color = Color.Red, modifier = Modifier.padding(16.dp))
      }

      LazyColumn {
        itemsIndexed(productList) { _, product ->
          ProductCard(
            product = product,
            onProductClick = {
              navController.navigate(Screen.ProductDetail.createRoute(product.id))
            },
            onAddToCart = {
              viewModel.addToCart(product, 1)
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(product: Product, onProductClick: () -> Unit, onAddToCart: () -> Unit) {
  Card(
    onClick = onProductClick,
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Row(modifier = Modifier.padding(16.dp)) {
      AsyncImage(
        model = product.image_url,
        contentDescription = product.name,
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Price: $${"%.2f".format(product.price)}", color = Color.Gray)
        Text("In stock: ${product.stock_quantity}", fontSize = 12.sp)

        Button(
          onClick = { onAddToCart() },
          modifier = Modifier.padding(top = 8.dp),
          enabled = product.stock_quantity > 0
        ) {
          Text("Add to Cart")
        }
      }
    }
  }
}

