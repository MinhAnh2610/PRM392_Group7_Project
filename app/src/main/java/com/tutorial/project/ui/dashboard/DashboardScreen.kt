package com.tutorial.project.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(navController: NavController) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val dashboardViewModel: DashboardViewModel = viewModel(
    factory = GenericViewModelFactory {
      val client = SupabaseClientProvider.client
      val authRepository = AuthRepository(client.auth)
      DashboardViewModel(
        productRepository = ProductRepository(client),
        cartRepository = CartRepository(client = client, authRepository = authRepository)
      )
    }
  )
  val authViewModel: AuthViewModel = viewModel(
    factory = GenericViewModelFactory { AuthViewModel(AuthRepository(SupabaseClientProvider.client.auth)) }
  )

  val isLoggedIn by remember { mutableStateOf(authViewModel.isLoggedIn()) }
  val productList by dashboardViewModel.products.observeAsState(emptyList())
  val cartItemList by dashboardViewModel.cartItems.observeAsState(emptyList())
  val error by dashboardViewModel.error.observeAsState()
  val isLoading by dashboardViewModel.isLoading.observeAsState(true)
  val toastEvent by dashboardViewModel.toastEvent.observeAsState()
  val notificationRequest by dashboardViewModel.notificationRequest.observeAsState()

  var selectedCategory by remember { mutableStateOf("All") }
  var showLogoutDialog by remember { mutableStateOf(false) }
  var viewMode by remember { mutableStateOf(ViewMode.LIST) }

  val categories = listOf("All", "Electronics", "Clothing", "Food", "Books", "Sports")
  val filteredProducts = if (selectedCategory == "All") {
    productList
  } else {
    productList.filter { it.category?.equals(selectedCategory, ignoreCase = true) == true }
  }

  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
    if (isGranted) {
      dashboardViewModel.checkForNotification()
    } else {
      Toast.makeText(context, "Notification permission denied.", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(Unit) {
    dashboardViewModel.loadProducts()
    dashboardViewModel.loadCartItems()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        dashboardViewModel.checkForNotification()
      } else {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      dashboardViewModel.checkForNotification()
    }
  }

  LaunchedEffect(toastEvent) {
    toastEvent?.let {
      Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
      dashboardViewModel.onToastShown()
    }
  }

  LaunchedEffect(notificationRequest) {
    notificationRequest?.let { itemCount ->
      NotificationHelper.showCartNotification(context, itemCount)
      dashboardViewModel.onNotificationShown()
    }
  }

  if (showLogoutDialog) {
    ModernLogoutDialog(
      onConfirm = {
        scope.launch {
          authViewModel.logout()
          navController.navigate(Screen.Login.route) { popUpTo(0) }
        }
        showLogoutDialog = false
      },
      onDismiss = { showLogoutDialog = false }
    )
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      ModernTopAppBar(
        isLoggedIn = isLoggedIn,
        cartItemCount = cartItemList.size,
        onCartClick = { navController.navigate(Screen.Cart.route) },
        onMessagesClick = { navController.navigate(Screen.ConversationsList.route) },
        onOrderHistoryClick = { navController.navigate(Screen.OrderHistory.route) },
        onLogoutClick = { showLogoutDialog = true },
        onLoginClick = { navController.navigate(Screen.Login.route) }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      // Scam Banner
      ScamBanner()

      // Welcome Section
      if (isLoggedIn) {
        WelcomeSection()
      }

      // Category Filter with improved design
      ModernCategoryFilter(
        categories = categories,
        selectedCategory = selectedCategory,
        onCategorySelected = { selectedCategory = it }
      )

      // View Mode Toggle
      ViewModeToggle(
        viewMode = viewMode,
        onViewModeChanged = { viewMode = it }
      )

      when {
        isLoading -> {
          ModernLoadingState()
        }
        error != null -> {
          ModernErrorState(message = error)
        }
        filteredProducts.isEmpty() -> {
          ModernEmptyState(message = "No products found in this category.")
        }
        else -> {
          AnimatedContent(
            targetState = viewMode,
            transitionSpec = {
              fadeIn(animationSpec = tween(300)) with fadeOut(animationSpec = tween(300))
            }
          ) { mode ->
            when (mode) {
              ViewMode.LIST -> {
                LazyColumn(
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  items(filteredProducts, key = { it.id }) { product ->
                    ModernProductCard(
                      product = product,
                      onProductClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                      onAddToCart = {
                        if (isLoggedIn) {
                          dashboardViewModel.addToCart(product, 1)
                        } else {
                          navController.navigate(Screen.Login.route)
                        }
                      }
                    )
                  }
                }
              }
              ViewMode.GRID -> {
                LazyVerticalStaggeredGrid(
                  columns = StaggeredGridCells.Fixed(2),
                  modifier = Modifier.fillMaxSize(),
                  contentPadding = PaddingValues(16.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalItemSpacing = 12.dp
                ) {
                  items(filteredProducts, key = { it.id }) { product ->
                    GridProductCard(
                      product = product,
                      onProductClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                      onAddToCart = {
                        if (isLoggedIn) {
                          dashboardViewModel.addToCart(product, 1)
                        } else {
                          navController.navigate(Screen.Login.route)
                        }
                      }
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScamBanner() {
  var currentBannerIndex by remember { mutableStateOf(0) }
  var remainingCustomers by remember { mutableStateOf((15..97).random()) }

  val scamBanners = listOf(
    "🎉 FIRST 100 CUSTOMERS GET FREE IPHONE 15! Only $remainingCustomers spots left!",
    "⚡ FLASH SALE: 1-99% OFF EVERYTHING! Limited to next 50 buyers!",
    "🔥 CONGRATULATIONS! You're our 1,000,000th visitor! Claim your $1000 prize!",
    "💎 EXCLUSIVE: Buy 1 Get 10 FREE! This offer expires in 5 minutes!",
    "🚨 URGENT: Your account has been selected for FREE PREMIUM upgrade!",
    "🎁 MYSTERY BOX WORTH $500 - Only $9.99! Last $remainingCustomers boxes remaining!",
    "⭐ CELEBRITY ENDORSED: As seen on TV! 1000% satisfaction guaranteed!",
    "💰 MAKE $5000 A DAY FROM HOME! Click here to start your empire!",
    "🏆 YOU'VE WON! Claim your FREE Tesla Model S now! (No purchase necessary*)",
    "🔮 PSYCHIC PREDICTION: You will regret not buying this! 99.9% accurate!"
  )

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(4000)
      currentBannerIndex = (currentBannerIndex + 1) % scamBanners.size
      remainingCustomers = maxOf(1, remainingCustomers - (1..3).random())
      if (remainingCustomers <= 5) remainingCustomers = (15..97).random()
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(
      containerColor = Color(0xFFFF6B35)
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              Color(0xFFFF6B35),
              Color(0xFFFF8E53),
              Color(0xFFFF6B35)
            )
          )
        )
        .padding(16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Blinking star animation
        val infiniteTransition = rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
          )
        )

        Text(
          text = "⭐",
          fontSize = 24.sp,
          modifier = Modifier.graphicsLayer(alpha = alpha)
        )

        AnimatedContent(
          targetState = scamBanners[currentBannerIndex].replace("$remainingCustomers", remainingCustomers.toString()),
          transitionSpec = {
            slideInVertically { it } + fadeIn() with slideOutVertically { -it } + fadeOut()
          }
        ) { banner ->
          Text(
            text = banner,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
          )
        }

        Text(
          text = "🎯",
          fontSize = 24.sp,
          modifier = Modifier.graphicsLayer(alpha = alpha)
        )
      }
    }
  }
}

enum class ViewMode { LIST, GRID }

@Composable
fun WelcomeSection() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    shape = RoundedCornerShape(16.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.horizontalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
            )
          )
        )
        .padding(20.dp)
    ) {
      Column {
        Text(
          text = "Welcome back! 👋",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
          text = "Discover amazing products today",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopAppBar(
  isLoggedIn: Boolean,
  cartItemCount: Int,
  onCartClick: () -> Unit,
  onMessagesClick: () -> Unit,
  onOrderHistoryClick: () -> Unit,
  onLogoutClick: () -> Unit,
  onLoginClick: () -> Unit
) {
  TopAppBar(
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Default.ShoppingBag,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
          "ShopHub",
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.headlineSmall
        )
      }
    },
    actions = {
      if (isLoggedIn) {
        IconButton(onClick = onMessagesClick) {
          Icon(
            Icons.Outlined.Email,
            contentDescription = "Messages",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(onClick = onOrderHistoryClick) {
          Icon(
            Icons.Outlined.History,
            contentDescription = "Order History",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }

        IconButton(onClick = onCartClick) {
          BadgedBox(
            badge = {
              if (cartItemCount > 0) {
                Badge(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError
                ) {
                  Text("$cartItemCount")
                }
              }
            }
          ) {
            Icon(
              Icons.Outlined.ShoppingCart,
              contentDescription = "Shopping Cart",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        IconButton(onClick = onLogoutClick) {
          Icon(
            Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Logout",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      } else {
        FilledTonalButton(
          onClick = onLoginClick,
          shape = RoundedCornerShape(20.dp)
        ) {
          Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(4.dp))
          Text("Login")
        }
        Spacer(Modifier.width(8.dp))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
    )
  )
}

@Composable
fun ModernCategoryFilter(
  categories: List<String>,
  selectedCategory: String,
  onCategorySelected: (String) -> Unit
) {
  LazyRow(
    contentPadding = PaddingValues(horizontal = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.padding(vertical = 8.dp)
  ) {
    items(categories) { category ->
      val isSelected = selectedCategory == category
      FilterChip(
        selected = isSelected,
        onClick = { onCategorySelected(category) },
        label = {
          Text(
            category,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
          )
        },
        leadingIcon = if (isSelected) {
          { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = MaterialTheme.colorScheme.primary,
          selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
          selectedBorderColor = Color.Transparent
        )
      )
    }
  }
}

@Composable
fun ViewModeToggle(
  viewMode: ViewMode,
  onViewModeChanged: (ViewMode) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      "Products",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )

    Row {
      IconButton(
        onClick = { onViewModeChanged(ViewMode.LIST) }
      ) {
        Icon(
          Icons.Default.List,
          contentDescription = "List View",
          tint = if (viewMode == ViewMode.LIST) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }
      IconButton(
        onClick = { onViewModeChanged(ViewMode.GRID) }
      ) {
        Icon(
          Icons.Default.Apps,
          contentDescription = "Grid View",
          tint = if (viewMode == ViewMode.GRID) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }
    }
  }
}

@Composable
fun ModernProductCard(
  product: ProductWithStoreInfo,
  onProductClick: () -> Unit,
  onAddToCart: () -> Unit
) {
  var isPressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
  )

  Card(
    onClick = {
      isPressed = true
      onProductClick()
    },
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer(scaleX = scale, scaleY = scale),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(
      defaultElevation = 4.dp,
      pressedElevation = 8.dp
    )
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      AsyncImage(
        model = product.image_url,
        contentDescription = product.name,
        modifier = Modifier
          .size(100.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.ShoppingBag,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = product.store_name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stock indicator
        if (product.stock_quantity <= 5) {
          Text(
            text = if (product.stock_quantity == 0) "Out of stock" else "Only ${product.stock_quantity} left",
            style = MaterialTheme.typography.bodySmall,
            color = if (product.stock_quantity == 0) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "$${"%.2f".format(product.price)}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          FilledTonalButton(
            onClick = onAddToCart,
            enabled = product.stock_quantity > 0,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
          ) {
            Icon(
              Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Add", fontSize = 14.sp)
          }
        }
      }
    }
  }

  LaunchedEffect(isPressed) {
    if (isPressed) {
      kotlinx.coroutines.delay(100)
      isPressed = false
    }
  }
}

@Composable
fun GridProductCard(
  product: ProductWithStoreInfo,
  onProductClick: () -> Unit,
  onAddToCart: () -> Unit
) {
  Card(
    onClick = onProductClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      AsyncImage(
        model = product.image_url,
        contentDescription = product.name,
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = product.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = product.store_name,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "$${"%.2f".format(product.price)}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        IconButton(
          onClick = onAddToCart,
          enabled = product.stock_quantity > 0,
          modifier = Modifier
            .size(36.dp)
            .background(
              MaterialTheme.colorScheme.primaryContainer,
              CircleShape
            )
        ) {
          Icon(
            Icons.Default.Add,
            contentDescription = "Add to cart",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }
    }
  }
}

@Composable
fun ModernLoadingState() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        strokeWidth = 4.dp
      )
      Text(
        "Loading amazing products...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun ModernEmptyState(message: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        Icons.Default.Search,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
      )
      Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )
      Text(
        text = "Try selecting a different category",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun ModernErrorState(message: String?, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        Icons.Default.Error,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.error
      )
      Text(
        text = "Oops! Something went wrong",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
      )
      Text(
        text = message ?: "Unknown error occurred",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun ModernLogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        Icons.AutoMirrored.Filled.ExitToApp,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary
      )
    },
    title = {
      Text(
        "Sign Out",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Text(
        "Are you sure you want to sign out? You'll need to log in again to access your account.",
        style = MaterialTheme.typography.bodyMedium
      )
    },
    confirmButton = {
      FilledTonalButton(
        onClick = onConfirm,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
      ) {
        Text("Sign Out")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
    shape = RoundedCornerShape(20.dp)
  )
}
