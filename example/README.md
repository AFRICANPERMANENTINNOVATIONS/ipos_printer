# api_pos_printer example

Minimal Flutter app that exercises every operation exposed by the
[`api_pos_printer`](https://pub.dev/packages/api_pos_printer) plugin: backend
auto-detection, status polling, status broadcasts, text printing, multi-column
rows, QR code, paper feed and final perform-print.

## Run

```bash
cd example
flutter run -d <android-device-id>
```

The app boots on the first connected Android POS terminal that exposes either
`com.iposprinter.iposprinterservice` or `woyou.aidlservice.jiuiv5`.

## What the screen shows

* **Connect / Disconnect**: bind / unbind the printer service.
* **Status**: latest `PrinterStatus` (normal, paperless, busy, …).
* **Init / Self check**: on iPos these are no-ops (not exposed by the OEM).
* **Print demo ticket**: end-to-end flow printing a header, two priced rows, a
  total line, a QR code and a paper feed.
* **Feed 1cm**: small blank-line feed.
* **Logs panel**: timestamps every operation result and every status
  broadcast received from the printer.
