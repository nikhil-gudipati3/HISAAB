package app.hisaab

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.hisaab.core.design.*
import app.hisaab.feature.activity.ActivityScreen
import app.hisaab.feature.buckets.BucketDetailScreen
import app.hisaab.feature.buckets.BucketsScreen
import app.hisaab.feature.home.AppViewModel
import app.hisaab.feature.home.HomeScreen
import app.hisaab.feature.insights.InsightsScreen
import app.hisaab.feature.onboarding.OnboardingScreen
import app.hisaab.feature.pay.PayScreen
import app.hisaab.feature.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { HisaabRoot(this) }
    }
}

@Composable
fun HisaabRoot(activity: FragmentActivity) {
    val vm: AppViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    var unlocked by remember { mutableStateOf(false) }
    var lockAttempted by remember { mutableStateOf(false) }

    if (ui.settings.hideInRecents) {
        DisposableEffect(Unit) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        }
    }
    // Biometric lock: prompt once per launch when enabled.
    LaunchedEffect(ui.settings.appLockEnabled, lockAttempted) {
        if (ui.settings.appLockEnabled && !unlocked && !lockAttempted) {
            lockAttempted = true
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked = true }
                    override fun onAuthenticationError(code: Int, msg: CharSequence) { /* stays locked; user can retry via button */ }
                })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hisaab").setSubtitle("Use your fingerprint, face, or device credential")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build())
        }
    }
    if (ui.settings.appLockEnabled && !unlocked) {
        Box(Modifier.fillMaxSize().background(HColors.Void), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                Text("Hisaab is locked", style = HType.Headline, color = HColors.TextPrimary)
                Spacer(Modifier.height(16.dp))
                SecondaryButton("Unlock", onClick = { lockAttempted = false })
            }
        }
        return
    }

    val start = if (ui.settings.onboardingDone) "home" else "onboarding"
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: start
    val showBar = route in listOf("home", "buckets", "insights", "activity", "pay")
    Box(Modifier.fillMaxSize().background(HColors.Void)) {
        NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize()) {
            composable("onboarding") { OnboardingScreen(vm) { nav.navigate("home") { popUpTo("onboarding") { inclusive = true } } } }
            composable("home") { HomeScreen(vm, { nav.navigate("settings") }, { id -> nav.navigate("bucket/$id") }, { nav.navigate("pay") }) }
            composable("buckets") { BucketsScreen(vm) { id -> nav.navigate("bucket/$id") } }
            composable("bucket/{id}") { e ->
                BucketDetailScreen(vm, e.arguments?.getString("id")?.toLongOrNull() ?: 0L) { nav.popBackStack() }
            }
            composable("insights") { InsightsScreen(vm) }
            composable("activity") { ActivityScreen(vm) }
            composable("pay") { PayScreen(vm) { nav.navigate("home") { popUpTo("pay") { inclusive = true } } } }
            composable("settings") { SettingsScreen(vm) { nav.popBackStack() } }
        }
        if (showBar) BottomBar(nav, route, Modifier.align(Alignment.BottomCenter))
        // PENDING recovery prompt: any PENDING APP_PAY older than a few minutes is shown in Activity as "Needs check".
        // On start, if pending items exist, surface a quiet banner on Home via Activity screen detail sheets.
    }
}

@Composable
fun BottomBar(nav: NavHostController, route: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(32.dp)
    Row(modifier.padding(bottom = 16.dp).height(64.dp).fillMaxWidth(0.92f)
        .glow0(shape).background(HColors.Surface, shape).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BarItem("Home", route == "home") { nav.navigate("home") { popUpTo("home") { inclusive = false }; launchSingleTop = true } }
        BarItem("Buckets", route == "buckets") { nav.navigate("buckets") { launchSingleTop = true } }
        Box(Modifier.size(52.dp).glow(RoundedCornerShape(26.dp), stroke = 1.5.dp,
            halos = listOf(4.dp to 0.25f, 10.dp to 0.12f, 20.dp to 0.06f))
            .background(HColors.Accent, RoundedCornerShape(26.dp)).clickable {
                nav.navigate("pay") { launchSingleTop = true }
            }, contentAlignment = Alignment.Center) {
            Text("Pay", style = HType.Button, color = HColors.OnAccent)
        }
        BarItem("Insights", route == "insights") { nav.navigate("insights") { launchSingleTop = true } }
        BarItem("Activity", route == "activity") { nav.navigate("activity") { launchSingleTop = true } }
    }
}

@Composable
fun BarItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable { onClick() }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = HType.Label, color = if (selected) HColors.Accent else HColors.TextSecondary)
        if (selected) Box(Modifier.width(16.dp).height(2.dp).background(HColors.Accent, RoundedCornerShape(1.dp)))
        else Spacer(Modifier.height(2.dp))
    }
}
