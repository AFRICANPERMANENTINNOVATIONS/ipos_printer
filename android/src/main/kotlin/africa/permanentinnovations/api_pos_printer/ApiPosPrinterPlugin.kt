package africa.permanentinnovations.api_pos_printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import africa.permanentinnovations.api_pos_printer.backend.BackendDetector
import africa.permanentinnovations.api_pos_printer.backend.PrinterBackend
import africa.permanentinnovations.api_pos_printer.models.TextStyle
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ApiPosPrinterPlugin : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var context: Context

    private var backend: PrinterBackend? = null
    private var statusSink: EventChannel.EventSink? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        context = binding.applicationContext
        methodChannel = MethodChannel(binding.binaryMessenger, "africa.permanentinnovations.api_pos_printer/methods")
        methodChannel.setMethodCallHandler(this)
        eventChannel = EventChannel(binding.binaryMessenger, "africa.permanentinnovations.api_pos_printer/status")
        eventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        backend?.disconnect()
        backend = null
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        scope.cancel()
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        statusSink = events
        backend?.setStatusListener { code ->
            statusSink?.success(mapOf("status" to code))
        }
    }

    override fun onCancel(arguments: Any?) {
        statusSink = null
        backend?.setStatusListener(null)
    }

    private fun requireBackend(): PrinterBackend =
        backend ?: throw IllegalStateException("Printer not connected. Call connect() first.")

    override fun onMethodCall(call: MethodCall, result: Result) {
        scope.launch {
            try {
                val response = handle(call)
                withContext(Dispatchers.Main) { result.success(response) }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    result.error(t.javaClass.simpleName, t.message, null)
                }
            }
        }
    }

    private suspend fun handle(call: MethodCall): Any? = when (call.method) {
        "listBackends" -> BackendDetector.listAvailable(context)

        "connect" -> {
            val preferred = call.argument<String>("backend")
            val b = BackendDetector.create(context, preferred)
            val ok = b.connect()
            if (ok) {
                backend = b
                b.setStatusListener { code -> statusSink?.success(mapOf("status" to code)) }
            }
            mapOf("connected" to ok, "backend" to b.name)
        }

        "disconnect" -> { backend?.disconnect(); backend = null; null }
        "isConnected" -> backend?.isConnected() ?: false
        "currentBackend" -> backend?.name

        "getStatus" -> requireBackend().getStatus()
        "getSerialNumber" -> requireBackend().getSerialNumber()
        "getModel" -> requireBackend().getModel()
        "getFirmwareVersion" -> requireBackend().getFirmwareVersion()

        "init" -> { requireBackend().init(); null }
        "selfCheck" -> { requireBackend().selfCheck(); null }

        "setAlignment" -> {
            requireBackend().setAlignment(call.argument<Int>("alignment") ?: 0); null
        }

        "setTextStyle" -> {
            requireBackend().setTextStyle(
                TextStyle(
                    textSize = call.argument<Int>("textSize") ?: 24,
                    fillBlack = call.argument<Boolean>("fillBlack") ?: false,
                    underline = call.argument<Boolean>("underline") ?: false,
                    italic = call.argument<Boolean>("italic") ?: false,
                    scaleX = call.argument<Int>("scaleX") ?: 0,
                    scaleY = call.argument<Int>("scaleY") ?: 0,
                ),
            )
            null
        }

        "printText" -> {
            requireBackend().printText(
                text = call.argument<String>("text") ?: "",
                typeface = call.argument<String>("typeface"),
                fontSize = call.argument<Int>("fontSize") ?: 24,
                alignment = call.argument<Int>("alignment") ?: 0,
            )
            null
        }

        "printColumns" -> {
            val texts = (call.argument<List<String>>("texts") ?: emptyList()).toTypedArray()
            val widths = (call.argument<List<Int>>("widths") ?: emptyList()).toIntArray()
            val aligns = (call.argument<List<Int>>("aligns") ?: emptyList()).toIntArray()
            requireBackend().printColumns(
                texts, widths, aligns,
                typeface = call.argument<String>("typeface"),
                fontSize = call.argument<Int>("fontSize") ?: 24,
            )
            null
        }

        "printBitmap" -> {
            val bytes = call.argument<ByteArray>("bytes")
                ?: throw IllegalArgumentException("bytes is required")
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: throw IllegalArgumentException("Could not decode bitmap bytes")
            requireBackend().printBitmap(
                bitmap,
                alignment = call.argument<Int>("alignment") ?: 1,
                size = call.argument<Int>("size") ?: 0,
            )
            null
        }

        "printBarcode" -> {
            requireBackend().printBarcode(
                data = call.argument<String>("data") ?: "",
                symbology = call.argument<Int>("symbology") ?: 8,
                height = call.argument<Int>("height") ?: 60,
                width = call.argument<Int>("width") ?: 2,
                textPosition = call.argument<Int>("textPosition") ?: 2,
            )
            null
        }

        "printQrCode" -> {
            requireBackend().printQrCode(
                data = call.argument<String>("data") ?: "",
                moduleSize = call.argument<Int>("moduleSize") ?: 8,
                errorLevel = call.argument<Int>("errorLevel") ?: 3,
            )
            null
        }

        "printRaw" -> {
            val bytes = call.argument<ByteArray>("bytes")
                ?: throw IllegalArgumentException("bytes is required")
            requireBackend().printRaw(bytes); null
        }

        "feedPaper" -> {
            requireBackend().feedPaper(call.argument<Int>("dots") ?: 80); null
        }

        "performPrint" -> {
            requireBackend().performPrint(call.argument<Int>("feedLines") ?: 4); null
        }

        else -> throw NoSuchMethodException("Method ${call.method} not implemented")
    }
}
