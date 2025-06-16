package com.tutorial.project.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.dto.NotificationHelper
import com.tutorial.project.data.model.ProductWithStoreInfo
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.ProductRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.AuthViewModel
import com.tutorial.project.viewmodel.DashboardViewModel
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // --- ViewModels ---
  // Use a single, comprehensive DashboardViewModel
  val dashboardViewModel: DashboardViewModel = viewModel(
    factory = GenericViewModelFactory {
      val client = SupabaseClientProvider.client
      val authRepository = AuthRepository(client.auth)
      DashboardViewModel(
        productRepository = ProductRepository(client),
        cartRepository = CartRepository(
          client = client,
          authRepository = authRepository
        )
      )
    }
  )

  // AuthViewModel for logout logic
  val authViewModel: AuthViewModel = viewModel(
    factory = GenericViewModelFactory {
      AuthViewModel(AuthRepository(SupabaseClientProvider.client.auth))
    }
  )

  // --- State Observation ---
  val productList by dashboardViewModel.products.observeAsState(emptyList())
  val cartItemList by dashboardViewModel.cartItems.observeAsState(emptyList())
  val error by dashboardViewModel.error.observeAsState()
  val isLoading by dashboardViewModel.isLoading.observeAsState(true)
  val toastEvent by dashboardViewModel.toastEvent.observeAsState()
  val notificationRequest by dashboardViewModel.notificationRequest.observeAsState()

  var selectedCategory by remember { mutableStateOf("All") }
  var showLogoutDialog by remember { mutableStateOf(false) }

  val categories = listOf("All", "Electronics", "Clothing", "Food", "Books", "Sports")
  val filteredProducts = if (selectedCategory == "All") {
    productList
  } else {
    productList.filter { it.category?.equals(selectedCategory, ignoreCase = true) == true }
  }

  // --- Permission Handling ---
  val launcher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      dashboardViewModel.checkForNotification()
    } else {
      Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
    }
  }

  // --- LaunchedEffects for side-effects ---
  LaunchedEffect(Unit) {
    dashboardViewModel.loadProducts()
    dashboardViewModel.loadCartItems()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      ) {
        dashboardViewModel.checkForNotification()
      } else {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      dashboardViewModel.checkForNotification()
    }
  }

  LaunchedEffect(toastEvent) {
    toastEvent?.let { event ->
      Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
      dashboardViewModel.onToastShown()
    }
  }

  LaunchedEffect(notificationRequest) {
    notificationRequest?.let { itemCount ->
      NotificationHelper.showCartNotification(context, itemCount)
      dashboardViewModel.onNotificationShown()
    }
  }

  // --- UI Composition ---
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
              navController.navigate(Screen.Login.route) { popUpTo(0) }
            }
            showLogoutDialog = false
          }
        ) { Text("Logout", color = Color.Red) }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
      }
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Store Dashboard", fontWeight = FontWeight.Bold) },
        actions = {
          IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
            BadgedBox(
              badge = {
                if (cartItemList.isNotEmpty()) {
                  Badge { Text(cartItemList.size.toString()) }
                }
              }
            ) {
              Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping Cart")
            }
          }
          IconButton(onClick = { navController.navigate(Screen.ConversationsList.route) }) {
            Icon(Icons.Default.Email, contentDescription = "Messages")
          }
          IconButton(onClick = { navController.navigate(Screen.OrderHistory.route) }) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Order History")
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
    Column(modifier = Modifier
      .fillMaxSize()
      .padding(padding)) {
      CategoryFilterSection(
        categories = categories,
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it }
      )

      error?.let {
        Text(
          text = "Error: $it",
          color = Color.Red,
          modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
        )
      }

      if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      } else {
        if (filteredProducts.isEmpty()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text("No products found", fontSize = 18.sp, color = Color.Gray)
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // ...
          ) {
            items(filteredProducts, key = { it.id }) { product ->
              ProductCard(
                product = product,
                onProductClick = {
                  navController.navigate(Screen.ProductDetail.createRoute(product.id))
                },
                onAddToCart = {
                  dashboardViewModel.addToCart(product, 1)
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
  product: ProductWithStoreInfo,
  onProductClick: () -> Unit,
  onAddToCart: () -> Unit
) {
  Card(
    onClick = onProductClick,
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(modifier = Modifier
      .padding(12.dp)
      .fillMaxWidth()) {
      AsyncImage(
        model = product.image_url,
        contentDescription = product.name,
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color.Gray.copy(alpha = 0.1f)),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Sold by ${product.store_name}",
          fontSize = 12.sp,
          color = Color.Gray,
          maxLines = 1
        )
        product.description?.let {
          Text(
            text = it,
            fontSize = 14.sp,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$${product.price}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
          )
          Button(
            onClick = onAddToCart,
            enabled = product.stock_quantity > 0,
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