package app.hisaab.feature.pay

import android.Manifest
import androidx.compose.foundation.background
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hisaab.core.design.*
import app.hisaab.core.money.formatPaise
import app.hisaab.core.money.parsePaise
import app.hisaab.data.BudgetMode
import app.hisaab.data.TxnEntity
import app.hisaab.domain.*
import app.hisaab.feature.home.AppViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun PayScreen(vm: AppViewModel, onDone: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanned by remember { mutableStateOf<UpiPayee?>(null) }
    var manualVpa by remember { mutableStateOf("") }
    var manualMode by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingId by remember { mutableStateOf<Long?>(null) }
    var showResolve by remember { mutableStateOf(false) }
    var scanDone by remember { mutableStateOf(false) }

    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamera = granted
        if (!granted) manualMode = true
    }
    LaunchedEffect(Unit) { if (!hasCamera) permLauncher.launch(Manifest.permission.CAMERA) }

    // UPI result launcher: save PENDING before launch (in confirm sheet), resolve on return.
    val upiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val extras = mutableMapOf<String, String?>()
        result.data?.extras?.keySet()?.forEach { k -> extras[k] = result.data?.extras?.get(k)?.toString() }
        // Also check data string (some apps return upi response in data uri)
        val dataStr = result.data?.dataString ?: ""
        if (dataStr.isNotBlank()) extras["response"] = dataStr
        when (interpretUpiResult(extras)) {
            true -> { pendingId?.let { vm.resolveTxn(it, true) }; onDone() }
            false -> { pendingId?.let { vm.resolveTxn(it, false) }; onDone() }
            null -> showResolve = true
        }
    }

    fun launchUpi(payee: UpiPayee, amountPaise: Long, bucketId: Long?, accountId: Long?, category: String) {
        scope.launch {
            val id = vm.addTxnNow(TxnEntity(amountPaise = amountPaise, payeeName = payee.name,
                payeeVpa = payee.vpa, note = payee.note, categoryKey = category, bucketId = bucketId,
                accountId = accountId, timestamp = System.currentTimeMillis(), status = "PENDING",
                source = "APP_PAY", upiTxnRef = payee.ref))
            pendingId = id
            // Payee memory: updated on success resolution via last txn lookup; also preselect next time from this txn.
            val uri = Uri.parse(buildUpiUri(payee.vpa, payee.name, amountPaise, payee.note))
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (intent.resolveActivity(ctx.packageManager) == null) {
                error = "No UPI app found. Install one to pay."
                vm.resolveTxn(id, false)
                return@launch
            }
            upiLauncher.launch(Intent.createChooser(intent, "Pay with"))
        }
    }

    Column(Modifier.fillMaxSize().background(HColors.Void).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Pay", style = HType.Headline, color = HColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        if (scanned == null && !manualMode) {
            Text("Point the camera at a UPI QR code.", style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            if (hasCamera) {
                ScannerView(onScanned = { raw ->
                    if (!scanDone) {
                        val parsed = parseUpi(raw)
                        if (parsed != null) { scanDone = true; scanned = parsed }
                        else error = "That QR isn't a UPI payment code."
                    }
                }, modifier = Modifier.fillMaxWidth().height(320.dp))
            } else {
                Text("Camera access is off. You can still enter a UPI ID.", style = HType.Body, color = HColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            SecondaryButton("Enter UPI ID") { manualMode = true }
            error?.let { Text(it, style = HType.Caption, color = HColors.TextSecondary) }
        } else if (scanned == null && manualMode) {
            Text("Enter the payee UPI ID, for example name@okhdfcbank.", style = HType.Body, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(manualVpa, { manualVpa = it }, label = { Text("UPI ID") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Continue") {
                val p = parseUpi("upi://pay?pa=${manualVpa.trim()}&cu=INR")
                if (p != null) scanned = p else error = "That doesn't look like a valid UPI ID."
            }
            error?.let { Text(it, style = HType.Caption, color = HColors.TextSecondary) }
            Spacer(Modifier.height(12.dp))
            SecondaryButton("Scan a QR instead") { manualMode = false; scanDone = false }
        }
    }
    scanned?.let { payee ->
        ConfirmSheet(vm, payee, onDismiss = { scanned = null; scanDone = false }, onPay = { amount, bucketId, accountId, category ->
            launchUpi(payee, amount, bucketId, accountId, category)
        })
    }
    if (showResolve && pendingId != null) {
        AlertDialog(onDismissRequest = {}, title = { Text("Did the payment go through?", color = HColors.TextPrimary) },
            text = { Text("Hisaab never assumes a payment succeeded. Tell us what happened in your UPI app.", color = HColors.TextSecondary) },
            containerColor = HColors.SurfaceHigh,
            confirmButton = { TextButton(onClick = { vm.resolveTxn(pendingId!!, true); showResolve = false; onDone() }) { Text("Yes, it went through", color = HColors.Accent) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.resolveTxn(pendingId!!, false); showResolve = false; onDone() }) { Text("No, it failed", color = HColors.TextPrimary) }
                    TextButton(onClick = { showResolve = false; onDone() }) { Text("Not sure yet", color = HColors.TextPrimary) }
                }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmSheet(vm: AppViewModel, payee: UpiPayee, onDismiss: () -> Unit, onPay: (Long, Long?, Long?, String) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf(payee.amountPaise?.let { formatPaise(it, detail = true) } ?: "") }
    var accountId by remember { mutableStateOf<Long?>(ui.accounts.firstOrNull()?.id) }
    var bucketId by remember { mutableStateOf<Long?>(null) }
    var category by remember { mutableStateOf(categoryForMcc(payee.mcc)) }
    // Payee memory preselect
    LaunchedEffect(payee.vpa) {
        vm.memoryFor(payee.vpa)?.let { mem ->
            if (mem.bucketId != null) bucketId = mem.bucketId
            if (mem.categoryKey != null) category = mem.categoryKey!!
        }
        if (bucketId == null) bucketId = ui.buckets.firstOrNull()?.id
    }
    val amount = parsePaise(amountText) ?: 0L
    val bucketUi = ui.bucketUis().firstOrNull { it.bucket.id == bucketId }
    val projected = bucketUi?.let { it.remaining - amount }
    val over = if (ui.settings.mode == BudgetMode.BUCKETS) (projected ?: 0L) < 0 else {
        val d = ui.daily(); amount > d.remainingToday
    }
    val strictBlocked = over && ui.settings.strictMode
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = HColors.SurfaceHigh) {
        Column(Modifier.padding(20.dp)) {
            Text(payee.name ?: payee.vpa, style = HType.Headline, color = HColors.TextPrimary)
            Text(payee.vpa, style = HType.Caption, color = HColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(amountText, { amountText = it }, label = { Text("Amount (₹)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("Pay from", style = HType.Label, color = HColors.TextPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.accounts) { a -> Chip2(a.name, a.last4?.let { "••$it" } ?: "", accountId == a.id, { accountId = a.id }) }
            }
            if (ui.settings.mode == BudgetMode.BUCKETS) {
                Spacer(Modifier.height(16.dp))
                Text("Bucket", style = HType.Label, color = HColors.TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ui.bucketUis()) { b ->
                        Chip2(b.bucket.name, "₹${formatPaise(b.remaining)} left", bucketId == b.bucket.id, { bucketId = b.bucket.id })
                    }
                }
                if (bucketUi != null && amount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(if (over) "Over by ₹${formatPaise(-(projected ?: 0L))}. This payment takes ${bucketUi.bucket.name} below zero."
                        else "₹${formatPaise(bucketUi.remaining)} → ₹${formatPaise(projected ?: 0L)}",
                        style = HType.Caption, color = HColors.TextSecondary)
                }
            } else {
                val d = ui.daily()
                if (amount > 0) Text("Today: ₹${formatPaise(d.remainingToday)} → ₹${formatPaise((d.remainingToday - amount).coerceAtLeast(0))}",
                    style = HType.Caption, color = HColors.TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            if (strictBlocked) {
                Text("Strict mode is on, so payments past the remaining amount are blocked here. You can still log it manually after paying.",
                    style = HType.Caption, color = HColors.TextSecondary)
                Spacer(Modifier.height(12.dp))
            }
            PrimaryButton(if (over && !strictBlocked) "Pay anyway ₹${formatPaise(amount)}" else "Pay ₹${formatPaise(amount)}",
                enabled = amount > 0 && !strictBlocked) {
                onPay(amount, if (ui.settings.mode == BudgetMode.BUCKETS) bucketId else null, accountId, category)
                onDismiss()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ScannerView(onScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(factory = { c ->
        val previewView = PreviewView(c).apply { layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) }
        val providerFuture = ProcessCameraProvider.getInstance(c)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val scanner = BarcodeScanning.getClient()
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            val executor = Executors.newSingleThreadExecutor()
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(image).addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { onScanned(it) }
                    }.addOnCompleteListener { imageProxy.close() }
                } else imageProxy.close()
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) { /* camera unavailable: manual path remains */ }
        }, ContextCompat.getMainExecutor(c))
        previewView
    }, modifier = modifier)
}
