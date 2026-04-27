package africa.permanentinnovations.api_pos_printer.backend

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import africa.permanentinnovations.api_pos_printer.models.PrinterStatus
import africa.permanentinnovations.api_pos_printer.models.TextStyle
import com.iposprinter.iposprinterservice.IPosPrinterCallback
import com.iposprinter.iposprinterservice.IPosPrinterService
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class IposBackend(private val context: Context) : PrinterBackend {

    override val name = "ipos"

    companion object {
        private const val TAG = "IposBackend"
        const val SERVICE_PACKAGE = "com.iposprinter.iposprinterservice"
        const val SERVICE_ACTION = "com.iposprinter.iposprinterservice.IPosPrintService"
        const val DEFAULT_TYPEFACE = "ST"

        const val ACTION_NORMAL = "com.iposprinter.iposprinterservice.NORMAL_ACTION"
        const val ACTION_PAPERLESS = "com.iposprinter.iposprinterservice.PAPERLESS_ACTION"
        const val ACTION_PAPER_EXISTS = "com.iposprinter.iposprinterservice.PAPEREXISTS_ACTION"
        const val ACTION_THP_HIGHTEMP = "com.iposprinter.iposprinterservice.THP_HIGHTEMP_ACTION"
        const val ACTION_THP_NORMALTEMP = "com.iposprinter.iposprinterservice.THP_NORMALTEMP_ACTION"
        const val ACTION_MOTOR_HIGHTEMP = "com.iposprinter.iposprinterservice.MOTOR_HIGHTEMP_ACTION"
        const val ACTION_BUSY = "com.iposprinter.iposprinterservice.BUSY_ACTION"
    }

    private var service: IPosPrinterService? = null
    private var connectCont: ((Boolean) -> Unit)? = null
    private var statusListener: ((Int) -> Unit)? = null

    /** Latest in-flight async result waiter. The OEM service can fire callbacks
     *  on a global registered binder, so we keep a single long-lived stub and
     *  reroute results to whichever coroutine is currently waiting. */
    private val pendingResult = AtomicReference<((Boolean) -> Unit)?>(null)

    private val callbackStub = object : IPosPrinterCallback.Stub() {
        override fun onRunResult(isSuccess: Boolean) {
            pendingResult.getAndSet(null)?.invoke(isSuccess)
        }
        override fun onReturnString(result: String?) {
            pendingResult.getAndSet(null)?.invoke(true)
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IPosPrinterService.Stub.asInterface(binder)
            connectCont?.invoke(true)
            connectCont = null
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val code = when (intent?.action) {
                ACTION_NORMAL, ACTION_PAPER_EXISTS, ACTION_THP_NORMALTEMP -> PrinterStatus.NORMAL
                ACTION_PAPERLESS -> PrinterStatus.PAPERLESS
                ACTION_THP_HIGHTEMP -> PrinterStatus.THP_HIGH_TEMP
                ACTION_MOTOR_HIGHTEMP -> PrinterStatus.MOTOR_HIGH_TEMP
                ACTION_BUSY -> PrinterStatus.BUSY
                else -> return
            }
            statusListener?.invoke(code)
        }
    }
    private var receiverRegistered = false

    override fun isAvailable(): Boolean {
        val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
        return context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
    }

    override suspend fun connect(): Boolean = withTimeoutOrNull(5000) {
        if (service != null) return@withTimeoutOrNull true
        suspendCancellableCoroutine<Boolean> { cont ->
            connectCont = { success ->
                if (success) registerReceiver()
                if (cont.isActive) cont.resume(success)
            }
            val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                connectCont = null
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation { runCatching { context.unbindService(serviceConnection) } }
        }
    } ?: false

    override fun disconnect() {
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(statusReceiver) }
            receiverRegistered = false
        }
        if (service != null) {
            runCatching { context.unbindService(serviceConnection) }
            service = null
        }
    }

    override fun isConnected(): Boolean = service != null

    private fun registerReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(ACTION_NORMAL)
            addAction(ACTION_PAPERLESS)
            addAction(ACTION_PAPER_EXISTS)
            addAction(ACTION_THP_HIGHTEMP)
            addAction(ACTION_THP_NORMALTEMP)
            addAction(ACTION_MOTOR_HIGHTEMP)
            addAction(ACTION_BUSY)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(statusReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(statusReceiver, filter)
        }
        receiverRegistered = true
    }

    override fun setStatusListener(listener: ((Int) -> Unit)?) {
        statusListener = listener
    }

    private fun requireService(): IPosPrinterService =
        service ?: throw IllegalStateException("Printer service not connected. Call connect() first.")

    /** Run an AIDL call and suspend until [callbackStub] reports a result. */
    private suspend fun callAsync(
        operation: String,
        block: (IPosPrinterCallback) -> Unit,
    ): Boolean = withTimeoutOrNull(15_000) {
        suspendCancellableCoroutine<Boolean> { cont ->
            pendingResult.set { ok -> if (cont.isActive) cont.resume(ok) }
            runCatching { block(callbackStub) }.onFailure { err ->
                pendingResult.set(null)
                Log.e(TAG, "$operation failed", err)
                if (cont.isActive) cont.resumeWith(Result.failure(err))
            }
        }
    } ?: throw IllegalStateException("$operation timed out after 15s")

    override suspend fun getStatus(): Int =
        runCatching { requireService().printerStatus }.getOrDefault(PrinterStatus.UNKNOWN)

    // The OEM AIDL on these devices does not expose serial/model/firmware
    // strings synchronously. Returning null keeps the API consistent without
    // hitting an unconfirmed transaction code.
    override suspend fun getSerialNumber(): String? = null
    override suspend fun getModel(): String? = null
    override suspend fun getFirmwareVersion(): String? = null

    override suspend fun init() { /* no-op: OEM service does not require explicit init */ }
    override suspend fun selfCheck() { /* no-op: not exposed by this OEM */ }

    override suspend fun setAlignment(alignment: Int) {
        callAsync("printerSetAlignment") { requireService().printerSetAlignment(alignment, it) }
    }

    /** This OEM's printer does not expose a generic text-style setter; styling
     *  happens per-call through [printText]. We map [TextStyle.textSize] to the
     *  fontSize parameter on the next print. */
    override suspend fun setTextStyle(style: TextStyle) {
        nextFontSize = style.textSize
    }
    private var nextFontSize = 24

    override suspend fun printText(text: String, typeface: String?, fontSize: Int, alignment: Int) {
        val tf = typeface?.takeIf { it.isNotEmpty() } ?: DEFAULT_TYPEFACE
        callAsync("printSpecFormatText") {
            requireService().printSpecFormatText(text, tf, fontSize, alignment, it)
        }
    }

    /** Bypasses the alignment parameter — matches OEM `printerPrintText` (txn 10). */
    suspend fun printPlainText(text: String, typeface: String?, fontSize: Int) {
        val tf = typeface?.takeIf { it.isNotEmpty() } ?: DEFAULT_TYPEFACE
        callAsync("printerPrintText") {
            requireService().printerPrintText(text, tf, fontSize, it)
        }
    }

    override suspend fun printColumns(
        texts: Array<String>,
        widths: IntArray,
        aligns: IntArray,
        typeface: String?,
        fontSize: Int,
    ) {
        // No confirmed multi-column AIDL on this OEM. Render a single line by
        // padding each column to its requested width.
        val padded = buildString {
            texts.forEachIndexed { i, s ->
                val w = widths.getOrNull(i) ?: s.length
                val a = aligns.getOrNull(i) ?: 0
                val padCount = (w - s.length).coerceAtLeast(0)
                when (a) {
                    1 -> { append(" ".repeat(padCount / 2)); append(s); append(" ".repeat(padCount - padCount / 2)) }
                    2 -> { append(" ".repeat(padCount)); append(s) }
                    else -> { append(s); append(" ".repeat(padCount)) }
                }
            }
            append('\n')
        }
        printText(padded, typeface, fontSize, 0)
    }

    override suspend fun printBitmap(bitmap: Bitmap, alignment: Int, size: Int) {
        throw UnsupportedOperationException(
            "Bitmap printing is not exposed by this OEM's iPosPrinterService."
        )
    }

    override suspend fun printBarcode(
        data: String,
        symbology: Int,
        height: Int,
        width: Int,
        textPosition: Int,
    ) {
        throw UnsupportedOperationException(
            "Barcode printing is not exposed by this OEM's iPosPrinterService."
        )
    }

    override suspend fun printQrCode(data: String, moduleSize: Int, errorLevel: Int) {
        callAsync("printQRCode") { requireService().printQRCode(data, moduleSize, errorLevel, it) }
    }

    override suspend fun printRaw(data: ByteArray) {
        throw UnsupportedOperationException(
            "Raw byte printing is not exposed by this OEM's iPosPrinterService."
        )
    }

    /** [dots] is converted into a single blank line of `dots`-pixel height,
     *  matching the OEM `printerPrintBlankLines(lines, height, callback)` call
     *  pattern observed in the smali. */
    override suspend fun feedPaper(dots: Int) {
        callAsync("printerPrintBlankLines") {
            requireService().printerPrintBlankLines(1, dots, it)
        }
    }

    override suspend fun performPrint(feedLines: Int) {
        callAsync("printerPerformPrint") { requireService().printerPerformPrint(feedLines, it) }
    }
}
