// app/src/main/java/com/tutorial/project/ui/billing/BillingScreen.kt
package com.tutorial.project.ui.checkout

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.tutorial.project.R
import com.tutorial.project.data.api.SupabaseClientProvider
import com.tutorial.project.data.model.CartItemWithProductDetails
import com.tutorial.project.data.repository.AuthRepository
import com.tutorial.project.data.repository.CartRepository
import com.tutorial.project.data.repository.OrderRepository
import com.tutorial.project.data.repository.StripeRepository
import com.tutorial.project.navigation.Screen
import com.tutorial.project.viewmodel.BillingViewModel
import com.tutorial.project.viewmodel.PaymentStatus
import com.tutorial.project.viewmodel.factory.GenericViewModelFactory
import io.github.jan.supabase.auth.auth

// A nice orange color inspired by the design
val OrangeAccent = Color(0xFFF96821)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(navController: NavController) {
  val viewModel: BillingViewModel = viewModel(
    factory = GenericViewModelFactory {
      val authRepo = AuthRepository(SupabaseClientProvider.client.auth)
      BillingViewModel(
        orderRepository = OrderRepository(SupabaseClientProvider.client, authRepo),
        cartRepository = CartRepository(SupabaseClientProvider.client, authRepo),
        stripeRepository = StripeRepository(SupabaseClientProvider.client)
      )
    }
  )

  val paymentStatus by viewModel.paymentStatus.observeAsState(PaymentStatus.Idle)
  val cartItems by viewModel.cartItems.observeAsState(emptyList())
  val totalPrice by viewModel.totalPrice.observeAsState(0.0)
  val context = LocalContext.current
  var showSuccessDialog by remember { mutableStateOf(false) }

  val paymentSheet = rememberPaymentSheet(
    paymentResultCallback = { result ->
      when (result) {
        is PaymentSheetResult.Completed -> viewModel.onPaymentSuccess()
        is PaymentSheetResult.Canceled -> viewModel.onPaymentError("Payment canceled by user.")
        is PaymentSheetResult.Failed -> viewModel.onPaymentError(
          result.error.message ?: "Payment failed."
        )
      }
    }
  )

  LaunchedEffect(paymentStatus) {
    when (val status = paymentStatus) {
      is PaymentStatus.Success -> showSuccessDialog = true
      is PaymentStatus.Error -> Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
      else -> Unit
    }
  }

  if (showSuccessDialog) {
    PaymentSuccessDialog(onConfirm = {
      showSuccessDialog = false
      navController.navigate(Screen.Dashboard.route) {
        popUpTo(Screen.Dashboard.route) { inclusive = true }
      }
    })
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Order") },
        navigationIcon = {
          IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = { /* TODO */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      CheckoutBottomBar(
        paymentStatus = paymentStatus,
        onPayClicked = { status ->
          if (status is PaymentStatus.ReadyToPay) {
            paymentSheet.presentWithPaymentIntent(
              status.clientSecret,
              PaymentSheet.Configuration("My Awesome Store", customer = status.customerConfig)
            )
          }
        }
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
      // Spacer to add some breathing room at the top
      Spacer(modifier = Modifier.height(0.dp))

      ItemsSection(cartItems = cartItems)
      AddressSection()
      OrderSummarySection(subtotal = totalPrice)

      val currentStatus = paymentStatus
      if (currentStatus is PaymentStatus.Error) {
        ErrorBanner(message = currentStatus.message)
      }
      // Spacer to ensure content doesn't sit directly on the bottom bar
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun ItemsSection(cartItems: List<CartItemWithProductDetails>) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text("Items", style = MaterialTheme.typography.titleLarge)
    if (cartItems.isNotEmpty()) {
      Column {
        cartItems.forEachIndexed { index, item ->
          OrderItemRow(item = item)
          if (index < cartItems.lastIndex) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
          }
        }
      }
    } else {
      Text(
        "Your cart is empty.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun OrderItemRow(item: CartItemWithProductDetails) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // --- To use real images, replace this Box with AsyncImage from Coil ---
    AsyncImage(
      model = item.product_image_url,
      contentDescription = item.product_name,
      modifier = Modifier
        .size(64.dp)
        .clip(RoundedCornerShape(12.dp)),
      contentScale = ContentScale.Crop,
      placeholder = painterResource(id = R.drawable.ic_launcher_foreground), // Add a placeholder
      error = painterResource(id = R.drawable.ic_launcher_foreground)
    )
//    Box(
//      modifier = Modifier
//        .size(64.dp)
//        .clip(RoundedCornerShape(12.dp))
//        .background(MaterialTheme.colorScheme.surfaceVariant),
//      contentAlignment = Alignment.Center
//    ) {
//      Icon(
//        imageVector = Icons.Default.Check,
//        contentDescription = null,
//        tint = MaterialTheme.colorScheme.onSurfaceVariant
//      )
//    }

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        item.product_name,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        "Price: $${"%.2f".format(item.product_price)} (x${item.quantity})",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Text(
      text = "$${"%.2f".format(item.product_price * item.quantity)}",
      style = MaterialTheme.typography.bodyLarge,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun AddressSection() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Address", style = MaterialTheme.typography.titleLarge)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "1770 S Harbor Blvd, Ste 140, Anaheim, CA",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.weight(1f)
      )
      Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = "Edit Address",
        tint = MaterialTheme.colorScheme.primary
      )
    }
    TextButton(onClick = { /*TODO: Navigate to add address screen*/ }) {
      Text("+ Add new address")
    }
  }
}

@Composable
private fun OrderSummarySection(subtotal: Double) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Order Summary", style = MaterialTheme.typography.titleLarge)
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      SummaryRow("Subtotal", subtotal)
      HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp), thickness = 0.5.dp)
      SummaryRow("Total", subtotal, isTotal = true)
    }
  }
}

@Composable
private fun SummaryRow(label: String, amount: Double, isTotal: Boolean = false) {
  val fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
  val style =
    if (isTotal) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = style, fontWeight = fontWeight)
    Text("$${"%.2f".format(amount)}", style = style, fontWeight = fontWeight)
  }
}

@Composable
private fun CheckoutBottomBar(
  paymentStatus: PaymentStatus,
  onPayClicked: (PaymentStatus) -> Unit
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 8.dp // Add a subtle shadow to separate from content
  ) {
    Button(
      onClick = { onPayClicked(paymentStatus) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .height(52.dp),
      enabled = paymentStatus is PaymentStatus.ReadyToPay,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
    ) {
      val isLoading =
        paymentStatus is PaymentStatus.Processing || paymentStatus is PaymentStatus.Idle
      AnimatedContent(targetState = isLoading, label = "PayButtonAnimation") { loading ->
        if (loading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White,
            strokeWidth = 3.dp
          )
        } else {
          Text(
            "Process Transaction",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun ErrorBanner(message: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.errorContainer,
    shape = MaterialTheme.shapes.medium
  ) {
    Text(
      text = message,
      color = MaterialTheme.colorScheme.onErrorContainer,
      modifier = Modifier.padding(16.dp),
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun PaymentSuccessDialog(onConfirm: () -> Unit) {
  AlertDialog(
    onDismissRequest = { },
    title = { Text("Payment Successful!") },
    text = { Text("Your order has been confirmed and is being prepared.") },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
      ) {
        Text("Awesome!")
      }
    }
  )
}