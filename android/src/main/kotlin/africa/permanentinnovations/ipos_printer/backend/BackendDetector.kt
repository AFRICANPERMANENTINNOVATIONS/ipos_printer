package africa.permanentinnovations.ipos_printer.backend

import android.content.Context

object BackendDetector {

    fun create(context: Context, preferred: String?): PrinterBackend {
        val ctx = context.applicationContext
        return when (preferred) {
            "ipos" -> IposBackend(ctx)
            "sunmi" -> SunmiBackend(ctx)
            else -> autoDetect(ctx)
        }
    }

    private fun autoDetect(context: Context): PrinterBackend {
        val candidates = listOf<PrinterBackend>(
            IposBackend(context),
            SunmiBackend(context),
        )
        return candidates.firstOrNull { it.isAvailable() }
            ?: throw IllegalStateException(
                "No supported printer service detected on this device. " +
                    "Looked for: ${candidates.joinToString { it.name }}.",
            )
    }

    fun listAvailable(context: Context): List<String> {
        val ctx = context.applicationContext
        return buildList {
            if (IposBackend(ctx).isAvailable()) add("ipos")
            if (SunmiBackend(ctx).isAvailable()) add("sunmi")
        }
    }
}
