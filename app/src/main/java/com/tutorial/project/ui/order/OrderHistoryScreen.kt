// app/src/main/java/com/tutorial/project/ui/orders/OrderHistoryScreen.kt
package com.tutorial.project.ui.order

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay

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
  var showContent by remember { mutableStateOf(false) }
  
  LaunchedEffect(Unit) {
    delay(300)
    showContent = true
  }

  Scaffold(
    topBar = {
      AnimatedVisibility(
        visible = showContent,
        enter = slideInVertically(
          initialOffsetY = { -it },
          animationSpec = tween(600, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(600))
      ) {
        TopAppBar(
          title = { 
            Text(
              "My Orders", 
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 20.sp
            ) 
          },
          navigationIcon = {
            IconButton(
              onClick = { navController.popBackStack() },
              modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(4.dp)
            ) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Back",
                tint = Color.White
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
          )
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color(0xFF667eea),
              Color(0xFF764ba2),
              Color(0xFFf093fb)
            ),
            center = androidx.compose.ui.geometry.Offset(0f, 0f),
            radius = 800f,
            tileMode = TileMode.Clamp
          )
        )
        .padding(padding)
    ) {
      when (val state = ordersState) {
        is UiState.Loading -> {
          OrderHistoryLoadingAnimation()
        }
        is UiState.Error -> {
          OrderHistoryErrorCard(message = state.message, showContent = showContent)
        }
        is UiState.Success -> {
          if (state.data.isEmpty()) {
            EmptyOrdersCard(showContent = showContent)
          } else {
            OrdersList(
              orders = state.data,
              onOrderClick = { orderId ->
                navController.navigate(Screen.OrderDetail.createRoute(orderId))
              },
              showContent = showContent
            )
          }
        }
      }
    }
  }
}

@Composable
fun OrderHistoryLoadingAnimation() {
  val infiniteTransition = rememberInfiniteTransition(label = "loading")
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )
  
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing)
    ),
    label = "rotation"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .size(120.dp)
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.White.copy(alpha = 0.3f),
              Color.Transparent
            )
          ),
          CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.Default.ShoppingCart,
        contentDescription = "Loading",
        modifier = Modifier
          .size(60.dp)
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            rotationZ = rotation
          ),
        tint = Color.White
      )
    }
    Spacer(modifier = Modifier.height(24.dp))
    Text(
      "Loading your order history...",
      style = MaterialTheme.typography.headlineSmall,
      color = Color.White,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun OrderHistoryErrorCard(message: String, showContent: Boolean) {
  AnimatedVisibility(
    visible = showContent,
    enter = slideInVertically(
      initialOffsetY = { it },
      animationSpec = tween(800, easing = EaseOutCubic)
    ) + fadeIn(animationSpec = tween(800))
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .shadow(16.dp, RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(
        containerColor = Color.White.copy(alpha = 0.95f)
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      shape = RoundedCornerShape(24.dp)
    ) {
      Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color(0xFFff6b6b),
                  Color(0xFFee5a52)
                )
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Error",
            modifier = Modifier.size(40.dp),
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          "Oops! Something went wrong",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF2d3436),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          message,
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFF636e72),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun EmptyOrdersCard(showContent: Boolean) {
  AnimatedVisibility(
    visible = showContent,
    enter = slideInVertically(
      initialOffsetY = { it },
      animationSpec = tween(800, easing = EaseOutCubic)
    ) + fadeIn(animationSpec = tween(800))
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp)
        .shadow(16.dp, RoundedCornerShape(24.dp)),
      colors = CardDefaults.cardColors(
        containerColor = Color.White.copy(alpha = 0.95f)
      ),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      shape = RoundedCornerShape(24.dp)
    ) {
      Column(
        modifier = Modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(80.dp)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color(0xFF74b9ff),
                  Color(0xFF0984e3)
                )
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.ShoppingCart,
            contentDescription = "No Orders",
            modifier = Modifier.size(40.dp),
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          "No Orders Yet",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF2d3436),
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          "Start shopping to see your order history here!",
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFF636e72),
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun OrdersList(
  orders: List<Order>,
  onOrderClick: (Int) -> Unit,
  showContent: Boolean
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header Card
    item {
      AnimatedVisibility(
        visible = showContent,
        enter = slideInVertically(
          initialOffsetY = { -it },
          animationSpec = tween(800, delayMillis = 200, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(800, delayMillis = 200))
      ) {
        OrdersHeaderCard(orderCount = orders.size)
      }
    }
    
    // Order Cards with staggered animation
    items(
      items = orders,
      key = { it.id!! }
    ) { order ->
      val index = orders.indexOf(order)
      AnimatedVisibility(
        visible = showContent,
        enter = slideInVertically(
          initialOffsetY = { it },
          animationSpec = tween(600, delayMillis = 400 + (index * 100), easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(600, delayMillis = 400 + (index * 100)))
      ) {
        EnhancedOrderHistoryCard(
          order = order,
          onClick = { onOrderClick(order.id!!) }
        )
      }
    }
  }
}

@Composable
fun OrdersHeaderCard(orderCount: Int) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(20.dp, RoundedCornerShape(24.dp)),
    colors = CardDefaults.cardColors(
      containerColor = Color.White.copy(alpha = 0.95f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    shape = RoundedCornerShape(24.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          brush = Brush.linearGradient(
            colors = listOf(
              Color(0xFF667eea),
              Color(0xFF764ba2)
            )
          )
        )
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Floating icon with glow effect
        Box(
          modifier = Modifier
            .size(80.dp)
            .background(
              brush = Brush.radialGradient(
                colors = listOf(
                  Color.White.copy(alpha = 0.3f),
                  Color.Transparent
                )
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.ShoppingCart,
            contentDescription = "Order History",
            modifier = Modifier.size(48.dp),
            tint = Color.White
          )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
          "Order History",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
          "$orderCount orders found",
          style = MaterialTheme.typography.bodyLarge,
          color = Color.White.copy(alpha = 0.9f)
        )
      }
    }
  }
}

@Composable
fun EnhancedOrderHistoryCard(order: Order, onClick: () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulse by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.02f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

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

  val statusColor = when (order.status.lowercase()) {
    "completed" -> Color(0xFF00b894)
    "pending" -> Color(0xFFfdcb6e)
    "cancelled" -> Color(0xFFe17055)
    else -> Color(0xFF74b9ff)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .graphicsLayer(
        scaleX = pulse,
        scaleY = pulse
      )
      .shadow(12.dp, RoundedCornerShape(20.dp))
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(
      containerColor = Color.White.copy(alpha = 0.95f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    shape = RoundedCornerShape(20.dp)
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      // Order ID with special styling
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            Icons.Default.ShoppingCart,
            contentDescription = "Order",
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF667eea)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "Order #${order.id}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF2d3436)
          )
        }
        
        // Status indicator
        Box(
          modifier = Modifier
            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            order.status.replaceFirstChar { it.uppercase() },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor
          )
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      
      // Date with icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .background(
            Color(0xFF74b9ff).copy(alpha = 0.1f),
            RoundedCornerShape(8.dp)
          )
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        Icon(
          Icons.Default.Email,
          contentDescription = "Date",
          modifier = Modifier.size(16.dp),
          tint = Color(0xFF0984e3)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          "Date: $formattedDate",
          fontSize = 14.sp,
          color = Color(0xFF0984e3),
          fontWeight = FontWeight.Medium
        )
      }
      
      Spacer(modifier = Modifier.height(8.dp))
      
      // Total amount with special styling
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = Color(0xFFfdcb6e).copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.ShoppingCart,
              contentDescription = "Total",
              modifier = Modifier.size(20.dp),
              tint = Color(0xFFe17055)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Total:",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFe17055)
            )
          }
          Text(
            "$${"%.2f".format(order.total_amount)}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFe17055)
          )
        }
      }
    }
  }
}