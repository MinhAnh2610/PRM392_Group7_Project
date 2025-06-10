package com.tutorial.project.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.Product
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.ProductRepository
import com.tutorial.project.viewmodel.AuthViewModel
import com.tutorial.project.viewmodel.DashboardViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  navController: NavController
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val dashboardViewModel: DashboardViewModel = viewModel(
    factory = GenericViewModelFactory {
      val repository = ProductRepository(SupabaseClientProvider.client)
      DashboardViewModel(repository)
    }
  )

  val authViewModel: AuthViewModel = viewModel(
    factory = GenericViewModelFactory {
      val repository = AuthRepository(SupabaseClientProvider.client.auth)
      AuthViewModel(repository)
    }
  )

  val productList by dashboardViewModel.products.observeAsState(emptyList())
  val error by dashboardViewModel.error.observeAsState()
  val isLoading by dashboardViewModel.isLoading.observeAsState(false)
  var selectedCategory by remember { mutableStateOf("All") }
  var showLogoutDialog by remember { mutableStateOf(false) }

  val categories = listOf("All", "Electronics", "Clothing", "Food", "Books", "Sports")

  val filteredProducts = if (selectedCategory == "All") {
    productList
  } else {
    productList.filter { it.category?.equals(selectedCategory, ignoreCase = true) == true }
  }

  LaunchedEffect(Unit) {
    dashboardViewModel.loadProducts()
  }

  if (showLogoutDialog) {
    AlertDialog(
      onDismissRequest = { showLogoutDialog = false },
      title = { Text("Confirm Logout") },
      text = { Text("Are you sure you want to logout?") },
      confirmButton = {
        TextButton(
          onClick = {
            scope.launch {
              authViewModel.logout()
              navController.navigate("login") {
                popUpTo(0) { inclusive = true }
              }
            }
            showLogoutDialog = false
          }
        ) {
          Text("Logout", color = Color.Red)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            "Store Dashboard",
            fontWeight = FontWeight.Bold
          )
        },
        actions = {
          IconButton(onClick = {
            // Navigate to cart screen (implement later)
          }) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
          }

          IconButton(onClick = {
            // Navigate to profile screen (implement later)
          }) {
            Icon(Icons.Default.Person, contentDescription = "Profile")
          }

          IconButton(onClick = { showLogoutDialog = true }) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer
        )
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      CategoryFilterSection(
        categories = categories,
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it }
      )

      error?.let { errorMessage ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
        ) {
          Text(
            text = "Error: $errorMessage",
            modifier = Modifier.padding(16.dp),
            color = Color.Red
          )
        }
      }

      if (isLoading) {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      } else {
        if (filteredProducts.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                "No products found",
                fontSize = 18.sp,
                color = Color.Gray
              )
              if (selectedCategory != "All") {
                Text(
                  "Try selecting a different category",
                  fontSize = 14.sp,
                  color = Color.Gray
                )
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(filteredProducts) { product ->
              ProductCard(
                product = product,
                onProductClick = {
                  // Navigate to product detail (implement later)
                },
                onAddToCart = {
                  // Add to cart logic (implement later)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun CategoryFilterSection(
  categories: List<String>,
  selectedCategory: String,
  onCategorySelected: (String) -> Unit
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(categories) { category ->
      FilterChip(
        onClick = { onCategorySelected(category) },
        label = { Text(category) },
        selected = selectedCategory == category,
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = Color.White
        )
      )
    }
  }
}

@Composable
fun ProductCard(
  product: Product,
  onProductClick: () -> Unit,
  onAddToCart: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onProductClick() },
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Image Box
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color.Gray.copy(alpha = 0.1f))
      ) {
        if (product.imageUrl?.isNotEmpty() == true) {
          AsyncImage(
            model = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        } else {
          Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
              .size(40.dp)
              .align(Alignment.Center),
            tint = Color.Gray
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Product Details Column - FIXED
      Column(
        modifier = Modifier.weight(1f), // Now correctly in RowScope
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Product Info
        Column {
          Text(
            text = product.name ?: "Unknown Product",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          product.description?.let { description ->
            Text(
              text = description,
              fontSize = 14.sp,
              color = Color.Gray,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(top = 4.dp)
            )
          }

          product.category?.let { category ->
            Text(
              text = category,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Price and Add Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "$${product.price}",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Stock: ${product.stockQuantity ?: 0}",
              fontSize = 12.sp,
              color = if ((product.stockQuantity ?: 0) > 0) Color.Green else Color.Red
            )
          }

          Button(
            onClick = onAddToCart,
            enabled = (product.stockQuantity ?: 0) > 0,
            modifier = Modifier.height(36.dp)
          ) {
            Icon(
              Icons.Default.Add,
              contentDescription = "Add to cart",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add", fontSize = 12.sp)
          }
        }
      }
    }
  }
}
