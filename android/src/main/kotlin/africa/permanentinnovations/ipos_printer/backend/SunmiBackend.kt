package africa.permanentinnovations.ipos_printer.backend

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import africa.permanentinnovations.ipos_printer.models.PrinterStatus
import africa.permanentinnovations.ipos_printer.models.TextStyle
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import woyou.aidlservice.jiuiv5.ICallback
import woyou.aidlservice.jiuiv5.IWoyouService

class SunmiBackend(private val context: Context) : PrinterBackend {

    override val name = "sunmi"

    companion object {
        private const val TAG = "SunmiBackend"
        const val SERVICE_PACKAGE = "woyou.aidlservice.jiuiv5"
        const val SERVICE_ACTION = "woyou.aidlservice.jiuiv5.IWoyouService"
    }

    private var service: IWoyouService? = null
    private var connectCont: ((Boolean) -> Unit)? = null
    private var statusListener: ((Int) -> Unit)? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IWoyouService.Stub.asInterface(binder)
            connectCont?.invoke(true)
            connectCont = null
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    override fun isAvailable(): Boolean {
        val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
        return context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
    }

    override suspend fun connect(): Boolean = withTimeoutOrNull(5000) {
        if (service != null) return@withTimeoutOrNull true
        suspendCancellableCoroutine<Boolean> { cont ->
            connectCont = { ok -> if (cont.isActive) cont.resume(ok) }
            val intent = Intent(SERVICE_ACTION).setPackage(SERVICE_PACKAGE)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                connectCont = null
                if (cont.isActive) cont.resume(false)
            }
        }
    } ?: false

    override fun disconnect() {
        if (service != null) {
            runCatching { context.unbindService(serviceConnection) }
            service = null
        }
    }

    override fun isConnected(): Boolean = service != null

    override fun setStatusListener(listener: ((Int) -> Unit)?) {
        statusListener = listener
    }

    private fun requireService(): IWoyouService =
        service ?: throw IllegalStateException("Sunmi printer service not connected.")

    private suspend fun callWithCallback(block: (ICallback) -> Unit): Boolean =
        withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine<Boolean> { cont ->
                val cb = object : ICallback.Stub() {
                    override fun onRunResult(isSuccess: Boolean) {
                        if (cont.isActive) cont.resume(isSuccess)
                    }
                    override fun onReturnString(result: String?) {}
                    override fun onRaiseException(code: Int, msg: String?) {
                        Log.w(TAG, "Sunmi exception $code: $msg")
                        if (cont.isActive) cont.resume(false)
                    }
                    override fun onPrintResult(code: Int, msg: String?) {}
                }
                runCatching { block(cb) }.onFailure {
                    Log.e(TAG, "AIDL call failed", it)
                    if (cont.isActive) cont.resume(false)
                }
            }
        } ?: false

    override suspend fun getStatus(): Int = runCatching {
        // Sunmi: 1 = ready normal. We map to our enum.
        when (requireService().updatePrinterState()) {
            1 -> PrinterStatus.NORMAL
            3 -> PrinterStatus.BUSY
            4 -> PrinterStatus.PAPERLESS
            5 -> PrinterStatus.THP_HIGH_TEMP
            else -> PrinterStatus.UNKNOWN
        }
    }.getOrDefault(PrinterStatus.UNKNOWN)

    override suspend fun getSerialNumber() = runCatching { requireService().printerSerialNo }.getOrNull()
    override suspend fun getModel() = runCatching { requireService().printerModal }.getOrNull()
    override suspend fun getFirmwareVersion() = runCatching { requireService().printerVersion }.getOrNull()

    override suspend fun init() { callWithCallback { requireService().printerInit(it) } }
    override suspend fun selfCheck() { callWithCallback { requireService().printerSelfChecking(it) } }

    override suspend fun setAlignment(alignment: Int) {
        callWithCallback { requireService().setAlignment(alignment, it) }
    }

    override suspend fun setTextStyle(style: TextStyle) {
        callWithCallback { requireService().setFontSize(style.textSize.toFloat(), it) }
    }

    override suspend fun printText(text: String, typeface: String?, fontSize: Int, alignment: Int) {
        callWithCallback { requireService().setAlignment(alignment, it) }
        callWithCallback {
            requireService().printTextWithFont(text + "\n", typeface ?: "", fontSize.toFloat(), it)
        }
    }

    override suspend fun printColumns(
        texts: Array<String>,
        widths: IntArray,
        aligns: IntArray,
        typeface: String?,
        fontSize: Int,
    ) {
        callWithCallback { requireService().printColumnsText(texts, widths, aligns, it) }
    }

    override suspend fun printBitmap(bitmap: Bitmap, alignment: Int, size: Int) {
        callWithCallback { requireService().setAlignment(alignment, it) }
        callWithCallback { requireService().printBitmap(bitmap, it) }
    }

    override suspend fun printBarcode(
        data: String,
        symbology: Int,
        height: Int,
        width: Int,
        textPosition: Int,
    ) {
        callWithCallback {
            requireService().printBarCode(data, symbology, height, width, textPosition, it)
        }
    }

    override suspend fun printQrCode(data: String, moduleSize: Int, errorLevel: Int) {
        callWithCallback { requireService().printQRCode(data, moduleSize, errorLevel, it) }
    }

    override suspend fun printRaw(data: ByteArray) {
        callWithCallback { requireService().sendRAWData(data, it) }
    }

    override suspend fun feedPaper(dots: Int) {
        // Sunmi takes lines, not dots. Approximate: 1 line ~= 24 dots.
        callWithCallback { requireService().lineWrap((dots / 24).coerceAtLeast(1), it) }
    }

    override suspend fun performPrint(feedLines: Int) {
        callWithCallback { requireService().lineWrap(feedLines, it) }
    }
}
