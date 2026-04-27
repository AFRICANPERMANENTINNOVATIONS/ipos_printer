package africa.permanentinnovations.ipos_printer.backend

import android.graphics.Bitmap
import africa.permanentinnovations.ipos_printer.models.TextStyle

interface PrinterBackend {
    val name: String

    fun isAvailable(): Boolean
    suspend fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean

    suspend fun getStatus(): Int
    suspend fun getSerialNumber(): String?
    suspend fun getModel(): String?
    suspend fun getFirmwareVersion(): String?

    suspend fun init()
    suspend fun selfCheck()

    suspend fun setAlignment(alignment: Int)
    suspend fun setTextStyle(style: TextStyle)
    suspend fun printText(text: String, typeface: String?, fontSize: Int, alignment: Int)
    suspend fun printColumns(texts: Array<String>, widths: IntArray, aligns: IntArray, typeface: String?, fontSize: Int)

    suspend fun printBitmap(bitmap: Bitmap, alignment: Int, size: Int)
    suspend fun printBarcode(data: String, symbology: Int, height: Int, width: Int, textPosition: Int)
    suspend fun printQrCode(data: String, moduleSize: Int, errorLevel: Int)

    suspend fun printRaw(data: ByteArray)
    suspend fun feedPaper(dots: Int)
    suspend fun performPrint(feedLines: Int)

    /** Listener invoked when a status broadcast (paperless, busy, overheat, …) is received. */
    fun setStatusListener(listener: ((Int) -> Unit)?)
}
