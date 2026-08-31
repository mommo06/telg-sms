package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.StatusPulseIndicator
import com.example.ui.screens.CarbonCopyScreen
import com.example.ui.screens.CommandsAndManualScreen
import com.example.ui.screens.ConfigScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TelegramAccent
import com.example.ui.theme.TelegramBlue
import com.example.ui.viewmodel.TelegramSmsViewModel

data class NavTabItem(
  val title: String,
  val icon: ImageVector,
  val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: TelegramSmsViewModel,
  modifier: Modifier = Modifier
) {
  var selectedTabIndex by remember { mutableIntStateOf(0) }
  val isServiceActive by viewModel.isServiceActive.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // Permission Launcher
  val permissionsToRequest = remember {
    val list = mutableListOf(
      Manifest.permission.RECEIVE_SMS,
      Manifest.permission.READ_SMS,
      Manifest.permission.SEND_SMS,
      Manifest.permission.READ_PHONE_STATE,
      Manifest.permission.READ_CALL_LOG
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    list.toTypedArray()
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) {
    viewModel.refreshDeviceStatus()
  }

  LaunchedEffect(Unit) {
    val needsRequest = permissionsToRequest.any {
      ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
    }
    if (needsRequest) {
      permissionLauncher.launch(permissionsToRequest)
    }
  }

  val navItems = listOf(
    NavTabItem("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    NavTabItem("Logs", Icons.Default.ListAlt, "nav_logs"),
    NavTabItem("Bot Config", Icons.Default.Settings, "nav_config"),
    NavTabItem("Carbon Copy", Icons.Default.CloudUpload, "nav_carbon_copy"),
    NavTabItem("Terminal", Icons.Default.Terminal, "nav_terminal")
  )

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.ic_telegram_sms_logo_1788184749788),
              contentDescription = "Telegram SMS Logo",
              modifier = Modifier.size(28.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Telegram SMS",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          Row(
            modifier = Modifier.padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            StatusPulseIndicator(isActive = isServiceActive)
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
              color = if (isServiceActive) StatusSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = if (isServiceActive) "ACTIVE" else "IDLE",
                color = if (isServiceActive) StatusSuccess else Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        navItems.forEachIndexed { index, item ->
          NavigationBarItem(
            selected = selectedTabIndex == index,
            onClick = { selectedTabIndex = index },
            icon = { Icon(item.icon, contentDescription = item.title) },
            label = { Text(item.title, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = TelegramBlue,
              selectedTextColor = TelegramBlue,
              indicatorColor = TelegramBlue.copy(alpha = 0.15f)
            ),
            modifier = Modifier.testTag(item.testTag)
          )
        }
      }
    }
  ) { paddingValues ->
    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      when (selectedTabIndex) {
        0 -> DashboardScreen(
          viewModel = viewModel,
          onNavigateToLogs = { selectedTabIndex = 1 },
          onNavigateToConfig = { selectedTabIndex = 2 }
        )
        1 -> LogsScreen(viewModel = viewModel)
        2 -> ConfigScreen(viewModel = viewModel)
        3 -> CarbonCopyScreen(viewModel = viewModel)
        4 -> CommandsAndManualScreen(viewModel = viewModel)
      }
    }
  }
}
