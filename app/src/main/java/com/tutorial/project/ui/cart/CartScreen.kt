package com.tutorial.project.ui.cart

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BadgedBox(
              badge = {
                if (cartItems.isNotEmpty()) {
                  Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ) {
                    Text(text = cartItems.size.toString())
                  }
                }
              }
            ) {
              Icon(
                Icons.Default.ShoppingCart,
                contentDescription = "Shopping Cart",
                modifier = Modifier.size(24.dp)
              )
            }
            Text(
              "My Cart",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        )
      )
    },
    bottomBar = {
      if (cartItems.isNotEmpty()) {
        EnhancedCartSummary(
          cartItems = cartItems,
          totalPrice = totalPrice,
          onCheckout = {
            navController.navigate(Screen.Billing.route)
          }
        )
      }
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(MaterialTheme.colorScheme.background)
    ) {
      when {
        isLoading -> {
          Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp),
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              "Loading your cart...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
          }
        }
        cartItems.isEmpty() -> {
          EmptyCartContent(
            onContinueShopping = { navController.popBackStack() }
          )
        }
        else -> {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              CartHeader(itemCount = cartItems.size)
            }

            items(cartItems, key = { it.cart_item_id }) { item ->
              EnhancedCartItemCard(
                item = item,
                onQuantityChange = { newQuantity ->
                  viewModel.updateQuantity(item.cart_item_id, newQuantity)
                },
                onRemoveItem = {
                  viewModel.removeItem(item.cart_item_id)
                }
              )
            }

            item {
              Spacer(modifier = Modifier.height(100.dp)) // Space for bottom bar
            }
          }
        }
      }
    }
  }
}

@Composable
fun CartHeader(itemCount: Int) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        Icons.Default.ShoppingCart,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = if (itemCount == 1) "1 item in your cart" else "$itemCount items in your cart",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

@Composable
fun EnhancedCartItemCard(
  item: CartItemWithProductDetails,
  onQuantityChange: (Int) -> Unit,
  onRemoveItem: () -> Unit
) {
  ElevatedCard(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(
        elevation = 4.dp,
        shape = RoundedCornerShape(16.dp),
        ambientColor = Color.Black.copy(alpha = 0.1f)
      ),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
      ) {
        // Product Image
        Surface(
          modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp)),
          shadowElevation = 4.dp
        ) {
          AsyncImage(
            model = item.product_image_url,
            contentDescription = item.product_name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Product Details
        Column(
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = item.product_name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Unit Price: $${String.format("%.2f", item.product_price)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Quantity and Total Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernQuantitySelector(
              quantity = item.quantity,
              onQuantityChange = onQuantityChange
            )

            Column(
              horizontalAlignment = Alignment.End
            ) {
              Text(
                text = "Total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
              Text(
                text = "$${String.format("%.2f", item.product_price * item.quantity)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Actions Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Qty: ${item.quantity} × $${String.format("%.2f", item.product_price)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        OutlinedButton(
          onClick = onRemoveItem,
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error
          ),
          modifier = Modifier.height(36.dp)
        ) {
          Icon(
            Icons.Default.Delete,
            contentDescription = "Remove Item",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Remove",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
fun ModernQuantitySelector(
  quantity: Int,
  onQuantityChange: (Int) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    FloatingActionButton(
      onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
      modifier = Modifier.size(32.dp),
      containerColor = if (quantity > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      contentColor = if (quantity > 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    ) {
      Icon(
        Icons.Default.Delete,
        contentDescription = "Decrease Quantity",
        modifier = Modifier.size(16.dp)
      )
    }

    Surface(
      modifier = Modifier
        .width(48.dp)
        .height(32.dp),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Box(
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = quantity.toString(),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    FloatingActionButton(
      onClick = { onQuantityChange(quantity + 1) },
      modifier = Modifier.size(32.dp),
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
      Icon(
        Icons.Default.Add,
        contentDescription = "Increase Quantity",
        modifier = Modifier.size(16.dp)
      )
    }
  }
}

@Composable
fun EnhancedCartSummary(
  cartItems: List<CartItemWithProductDetails>,
  totalPrice: Double,
  onCheckout: () -> Unit
) {
  val itemCount = cartItems.size
  val totalQuantity = cartItems.sumOf { it.quantity }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(
        elevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
      ),
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    color = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      // Summary Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Order Summary",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer
        ) {
          Text(
            text = "$itemCount items",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Price Breakdown
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Subtotal ($totalQuantity items)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )
          Text(
            text = "$${String.format("%.2f", totalPrice)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Delivery Fee",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          )
          Text(
            text = if (totalPrice >= 50.0) "FREE" else "$5.99",
            style = MaterialTheme.typography.bodyMedium,
            color = if (totalPrice >= 50.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (totalPrice >= 50.0) FontWeight.Bold else FontWeight.Normal,
            textDecoration = if (totalPrice >= 50.0) TextDecoration.LineThrough else TextDecoration.None
          )
        }

        if (totalPrice < 50.0) {
          Text(
            text = "Add $${String.format("%.2f", 50.0 - totalPrice)} more for FREE delivery!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Total
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Total",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "$${String.format("%.2f", totalPrice + if (totalPrice >= 50.0) 0.0 else 5.99)}",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Checkout Button
      Button(
        onClick = onCheckout,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
      ) {
        Text(
          text = "PROCEED TO CHECKOUT",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

@Composable
fun EmptyCartContent(onContinueShopping: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Surface(
      modifier = Modifier.size(120.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surfaceVariant
    ) {
      Box(
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.ShoppingCart,
          contentDescription = "Empty Cart",
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "Your cart is empty",
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = "Add some items to get started!",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
      textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onContinueShopping,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = "Continue Shopping",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )
    }
  }
}