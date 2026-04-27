package africa.permanentinnovations.api_pos_printer.models

object PrinterStatus {
    const val NORMAL = 0
    const val PAPERLESS = 1
    const val THP_HIGH_TEMP = 2
    const val MOTOR_HIGH_TEMP = 3
    const val BUSY = 4
    const val UNKNOWN = -1
}

object Alignment {
    const val LEFT = 0
    const val CENTER = 1
    const val RIGHT = 2
}

object BarcodeSymbology {
    const val UPC_A = 0
    const val UPC_E = 1
    const val JAN13_EAN13 = 2
    const val JAN8_EAN8 = 3
    const val CODE39 = 4
    const val ITF = 5
    const val CODABAR = 6
    const val CODE93 = 7
    const val CODE128 = 8
}

object BarcodeTextPosition {
    const val NONE = 0
    const val ABOVE = 1
    const val BELOW = 2
    const val BOTH = 3
}

data class TextStyle(
    val textSize: Int = 24,
    val fillBlack: Boolean = false,
    val underline: Boolean = false,
    val italic: Boolean = false,
    val scaleX: Int = 0,
    val scaleY: Int = 0,
)
