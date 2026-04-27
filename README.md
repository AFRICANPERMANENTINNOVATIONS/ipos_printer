# ipos_printer

[![pub package](https://img.shields.io/pub/v/ipos_printer.svg)](https://pub.dev/packages/ipos_printer)
[![pub points](https://img.shields.io/pub/points/ipos_printer)](https://pub.dev/packages/ipos_printer/score)

Flutter plugin that drives the **integrated thermal printer** on Android POS
terminals. Two backends are supported out of the box and the plugin
auto-detects which one is installed at connect time:

| Backend  | Service package                          | Devices                                                     |
| -------- | ---------------------------------------- | ----------------------------------------------------------- |
| `ipos`   | `com.iposprinter.iposprinterservice`     | Telpo, Centerm, MTwo, iPos and most Chinese-OEM POS units.  |
| `sunmi`  | `woyou.aidlservice.jiuiv5`               | Sunmi V1 / V2 / P1 / P2 / T1 / T2.                          |

Android only. There is no equivalent service on iOS.

## Installation

```yaml
dependencies:
  ipos_printer: ^0.1.2
```

```dart
import 'package:ipos_printer/ipos_printer.dart';
```

The plugin already declares the required `<queries>` entries in its manifest;
no extra Android configuration is needed in your host app.

## Quick start

```dart
final printer = IposPrinter.instance;

// 1. Discover and connect.
final available = await printer.listBackends();
final backend = await printer.connect();           // auto-detects
print('Connected via ${backend.id}');

// 2. Subscribe to hardware events.
printer.statusStream.listen((s) => print('printer: ${s.name}'));

// 3. Print a ticket.
await printer.printText('PERMANENT INNOVATIONS',
    fontSize: 48, alignment: PrintAlignment.center);
await printer.printText('Demo ticket',
    fontSize: 24, alignment: PrintAlignment.center);
await printer.feedPaper(dots: 8);

await printer.printColumns(
  texts: ['Coffee', '2', '4.00'],
  widths: [16, 4, 8],
  aligns: [PrintAlignment.left, PrintAlignment.center, PrintAlignment.right],
);

await printer.printQrCode('https://permanentinnovations.africa', moduleSize: 10);
await printer.performPrint(feedLines: 80);

await printer.disconnect();
```

## API surface

| Dart method                          | iPos AIDL                            | Sunmi AIDL                        |
| ------------------------------------ | ------------------------------------ | --------------------------------- |
| `getStatus()`                        | `getPrinterStatus`                   | `updatePrinterState` (re-mapped)  |
| `setAlignment()`                     | `printerSetAlignment`                | `setAlignment`                    |
| `printText()`                        | `printSpecFormatText`                | `printTextWithFont`               |
| `printColumns()`                     | rendered into a single padded line   | `printColumnsText`                |
| `printQrCode()`                      | `printQRCode`                        | `printQRCode`                     |
| `printBitmap(Uint8List)` ¹           | _not exposed_                        | `printBitmap(Bitmap)`             |
| `printBarcode()` ¹                   | _not exposed_                        | `printBarCode`                    |
| `printRaw(Uint8List)` ¹              | _not exposed_                        | `sendRAWData`                     |
| `feedPaper(dots)`                    | `printerPrintBlankLines(1, dots)`    | `lineWrap(dots / 24)`             |
| `performPrint()`                     | `printerPerformPrint`                | `lineWrap`                        |

¹ The iPos AIDL exposed by these LzyHardWareManager-class devices does not
include barcode, bitmap or raw byte methods. Calls throw
`UnsupportedOperationException` on the iPos backend.

## Status broadcasts (iPos only)

The iPos service broadcasts status changes; they are surfaced through
`statusStream` as `PrinterStatus` values:

| Broadcast action                                                    | `PrinterStatus`     |
| ------------------------------------------------------------------- | ------------------- |
| `…NORMAL_ACTION` / `…PAPEREXISTS_ACTION` / `…THP_NORMALTEMP_ACTION` | `normal`            |
| `…PAPERLESS_ACTION`                                                 | `paperless`         |
| `…THP_HIGHTEMP_ACTION`                                              | `thpHighTemp`       |
| `…MOTOR_HIGHTEMP_ACTION`                                            | `motorHighTemp`     |
| `…BUSY_ACTION`                                                      | `busy`              |

## Picking a specific backend

```dart
await printer.connect(backend: PrinterBackend.sunmi);
```

`PrinterBackend.auto` is the default and picks the first installed service in
this order: `ipos`, then `sunmi`.

## Adding a new backend

1. Drop the vendor AIDL into `android/src/main/aidl/<vendor>/…`.
2. Implement the `PrinterBackend` Kotlin interface in
   `android/src/main/kotlin/africa/permanentinnovations/ipos_printer/backend/`.
3. Register the implementation in `BackendDetector.autoDetect`.

Dart and Kotlin layers are decoupled: Dart only knows about operations, not
about which AIDL is being driven.

## Limitations

* Android only.
* Bluetooth ESC/POS support is **not** included; this plugin targets devices
  with an integrated thermal head exposed through a system service.
* On Sunmi, `feedPaper(dots)` is approximated to `lineWrap(dots / 24)` since
  Sunmi's API only feeds whole lines.

## License

MIT — see [LICENSE](LICENSE).
