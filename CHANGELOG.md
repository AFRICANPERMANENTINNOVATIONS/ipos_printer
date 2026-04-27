## 0.1.2

* **Fix**: `printerSetAlignment` is at txn 6 (not 7) and `printerPrintBlankLines(int lines, int height, callback)` is at txn 8 (not `printerSetTextStyle`). The OEM's iPos AIDL has been rebuilt from parameter names recovered from the `LzyHardWareManager` proxy stub. All confirmed transaction codes (1, 6, 8, 10, 11, 15, 18) now match exactly.
* All async iPos AIDL methods now return `void` (matching the OEM stub which writes only `readException()` after each transaction).
* iPos backend now reuses a single long-lived `IPosPrinterCallback` stub for the lifetime of the connection (the OEM service appears to fire results on the most recently registered callback rather than the per-call parameter). Defaults typeface to `"ST"` to match the original LzyHardWareManager flow.
* `printBitmap` / `printBarcode` / `printRaw` now throw `UnsupportedOperationException` on iPos backend — these aren't exposed by this OEM's service. Sunmi still supports them.
* `feedPaper(dots)` on iPos is now mapped to `printerPrintBlankLines(1, dots, callback)`.

## 0.1.1

* **Fix**: realigned the iPos AIDL with the OEM service's transaction codes
  (`getPrinterVersion()` was speculatively included at txn 6, shifting every
  later method by one). Confirmed-against-smali codes 1, 6, 8, 10, 11, 15, 18
  now match exactly.
* `printerSetTextStyle` simplified to the 2-int variant (`textSize`,
  `alignment`) actually exposed by these devices.
* New `printerPrintText` AIDL entry (txn 10) for the simpler text-print
  variant; full-format `printSpecFormatText` stays at txn 11.
* `getFirmwareVersion()` now returns `null` (not exposed by this OEM AIDL).
* `IposBackend.callWithCallback` now propagates the underlying
  `RemoteException` instead of swallowing it as `false`, and includes the
  operation name in the log/error message.

## 0.1.0

* Initial release.
* Backend `ipos` (`com.iposprinter.iposprinterservice`) — full feature support: status, init, self-check, alignment, text style, text/columns, bitmap, barcode, QR code, raw bytes, feed, perform-print.
* Backend `sunmi` (`woyou.aidlservice.jiuiv5`) — text, columns, bitmap, barcode, QR, raw, line-wrap.
* Auto-detection of installed backend.
* Status `EventChannel` exposing iPos broadcasts (`PAPERLESS`, `BUSY`, `THP_HIGHTEMP`, `MOTOR_HIGHTEMP`, `NORMAL`).
