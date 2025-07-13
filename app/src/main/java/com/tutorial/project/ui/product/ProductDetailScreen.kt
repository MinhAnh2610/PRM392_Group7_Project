package com.tutorial.project.ui.product

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    key = "product_detail/$productId",
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
      viewModel.onChatNavigated()
    }
  }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      ModernProductTopAppBar(
        onBackClick = { navController.popBackStack() },
        productName = product?.name
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
          ModernLoadingState()
        }
        error != null -> {
          ModernErrorState(message = error!!)
        }
        product != null -> {
          ModernProductDetailsContent(
            product = product!!,
            navController = navController,
            isLoggedIn = isLoggedIn,
            onAddToCart = { quantity ->
              if (isLoggedIn) {
                viewModel.addToCart(quantity)
              } else {
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
          ModernEmptyState(message = "Product not found")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProductTopAppBar(
  onBackClick: () -> Unit,
  productName: String?
) {
  TopAppBar(
    title = {
      Text(
        text = productName ?: "Product Details",
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        style = MaterialTheme.typography.titleLarge
      )
    },
    navigationIcon = {
      IconButton(onClick = onBackClick) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  )
}

@Composable
fun ModernProductDetailsContent(
  product: ProductWithStoreInfo,
  navController: NavController,
  isLoggedIn: Boolean,
  onAddToCart: (Int) -> Unit,
  onChatClick: () -> Unit
) {
  var quantity by remember { mutableStateOf("1") }
  var showQuantityError by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    ResponsiveProductImageSection(product = product)

    CountdownSaleTimer()

    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      ModernStoreCard(
        product = product,
        navController = navController,
        onChatClick = onChatClick
      )

      Spacer(modifier = Modifier.height(24.dp))

      ProductInfoSection(product = product)

      Spacer(modifier = Modifier.height(24.dp))

      EnhancedPriceAndStockSection(product = product)

      Spacer(modifier = Modifier.height(32.dp))

      AddToCartSection(
        product = product,
        quantity = quantity,
        onQuantityChange = { newQuantity ->
          quantity = newQuantity
          showQuantityError = false
        },
        onAddToCart = {
          val qty = quantity.toIntOrNull()
          if (qty != null && qty > 0 && qty <= product.stock_quantity) {
            onAddToCart(qty)
          } else {
            showQuantityError = true
          }
        },
        isLoggedIn = isLoggedIn,
        showError = showQuantityError
      )
    }
  }
}

@Composable
fun ResponsiveProductImageSection(product: ProductWithStoreInfo) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(20.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      AsyncImage(
        model = product.image_url,
        contentDescription = product.name,
        modifier = Modifier
          .fillMaxSize()
          .padding(8.dp),
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
      )
    }
  }
}

@Composable
fun CountdownSaleTimer() {
  var timeLeft by remember { mutableStateOf(calculateTimeLeft()) }

  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(1000)
      timeLeft = calculateTimeLeft()
    }
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          Icons.Default.AccessTime,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.size(20.dp)
        )
        Text(
          text = "⚡ FLASH SALE ENDS IN",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onErrorContainer
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        TimeUnit(timeLeft.hours, "HRS")
        TimeSeparator()
        TimeUnit(timeLeft.minutes, "MIN")
        TimeSeparator()
        TimeUnit(timeLeft.seconds, "SEC")
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "🔥 Limited time offer - Don't miss out!",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun TimeUnit(value: Int, label: String) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.error
      ),
      shape = RoundedCornerShape(8.dp)
    ) {
      Text(
        text = "%02d".format(value),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onError
      )
    }
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onErrorContainer,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun TimeSeparator() {
  Text(
    text = ":",
    style = MaterialTheme.typography.headlineMedium,
    fontWeight = FontWeight.Bold,
    color = MaterialTheme.colorScheme.onErrorContainer
  )
}

data class TimeLeft(
  val hours: Int,
  val minutes: Int,
  val seconds: Int
)

fun calculateTimeLeft(): TimeLeft {
  val now = System.currentTimeMillis()
  val hourInMillis = 60 * 60 * 1000
  val remainingInHour = hourInMillis - (now % hourInMillis)

  val hours = (remainingInHour / (60 * 60 * 1000)).toInt()
  val minutes = ((remainingInHour % (60 * 60 * 1000)) / (60 * 1000)).toInt()
  val seconds = ((remainingInHour % (60 * 1000)) / 1000).toInt()

  return TimeLeft(hours, minutes, seconds)
}

@Composable
fun ModernStoreCard(
  product: ProductWithStoreInfo,
  navController: NavController,
  onChatClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = product.store_logo_url,
        contentDescription = "${product.store_name} logo",
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surface),
        contentScale = ContentScale.Crop
      )

      Spacer(modifier = Modifier.width(16.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = product.store_name,
          fontWeight = FontWeight.Bold,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        product.description?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 2
          )
        }
      }

      Row {
        FilledTonalIconButton(
          onClick = {
            navController.navigate(
              Screen.Map.createRoute(
                lat = 37.7749,
                lon = -122.4194,
                name = product.store_name,
                address = "546 9th St, San Francisco, CA 94103"
              )
            )
          }
        ) {
          Icon(
            Icons.Default.Place,
            contentDescription = "View on Map",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        FilledTonalIconButton(onClick = onChatClick) {
          Icon(
            Icons.Default.Email,
            contentDescription = "Chat with Seller",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
    }
  }
}

@Composable
fun ProductInfoSection(product: ProductWithStoreInfo) {
  Column {
    Text(
      text = product.name,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    product.description?.let { description ->
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = description,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        lineHeight = 24.sp
      )
    }
  }
}

@Composable
fun EnhancedPriceAndStockSection(product: ProductWithStoreInfo) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ),
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Badge(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError
        ) {
          Text(
            "FLASH SALE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }

        Text(
          text = "25% OFF",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.error
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "$${"%.2f".format(product.price * 1.33)}",
          style = MaterialTheme.typography.titleMedium.copy(
            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
          ),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Text(
          text = "$${"%.2f".format(product.price)}",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Inventory,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = when {
            product.stock_quantity == 0 -> MaterialTheme.colorScheme.error
            product.stock_quantity <= 5 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
          }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when {
            product.stock_quantity == 0 -> "Out of Stock"
            product.stock_quantity <= 5 -> "Only ${product.stock_quantity} left in stock"
            else -> "${product.stock_quantity} in stock"
          },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = when {
            product.stock_quantity == 0 -> MaterialTheme.colorScheme.error
            product.stock_quantity <= 5 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurface
          }
        )
      }
    }
  }
}

@Composable
fun AddToCartSection(
  product: ProductWithStoreInfo,
  quantity: String,
  onQuantityChange: (String) -> Unit,
  onAddToCart: () -> Unit,
  isLoggedIn: Boolean,
  showError: Boolean
) {
  Column {
    Text(
      text = "Quantity",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      OutlinedTextField(
        value = quantity,
        onValueChange = { newValue ->
          if (newValue.isEmpty() || (newValue.all { it.isDigit() } && newValue.length <= 3)) {
            onQuantityChange(newValue)
          }
        },
        label = { Text("Qty") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(120.dp),
        isError = showError,
        supportingText = if (showError) {
          { Text("Invalid quantity", color = MaterialTheme.colorScheme.error) }
        } else null,
        shape = RoundedCornerShape(12.dp)
      )

      Button(
        onClick = onAddToCart,
        enabled = product.stock_quantity > 0 && (quantity.toIntOrNull() ?: 0) > 0,
        modifier = Modifier
          .weight(1f)
          .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Icon(
          Icons.Default.ShoppingCart,
          contentDescription = null,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when {
            !isLoggedIn -> "Login to Add to Cart"
            product.stock_quantity == 0 -> "Out of Stock"
            else -> "Add to Cart"
          },
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (!isLoggedIn) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Please login to add items to your cart",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
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
        "Loading product details...",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun ModernErrorState(message: String) {
  Box(
    modifier = Modifier
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
        text = "Something went wrong",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
      )
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun ModernEmptyState(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Icon(
        Icons.Default.ShoppingBag,
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
    }
  }
}
