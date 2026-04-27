/// Backends supported by the plugin. Use [PrinterBackend.auto] to let the
/// plugin pick the first one available on the device.
enum PrinterBackend {
  auto('auto'),
  ipos('ipos'),
  sunmi('sunmi');

  const PrinterBackend(this.id);
  final String id;
}

/// Raw status values reported by the printer service.
enum PrinterStatus {
  normal(0),
  paperless(1),
  thpHighTemp(2),
  motorHighTemp(3),
  busy(4),
  unknown(-1);

  const PrinterStatus(this.code);
  final int code;

  static PrinterStatus fromCode(int code) =>
      PrinterStatus.values.firstWhere((s) => s.code == code, orElse: () => PrinterStatus.unknown);
}

enum PrintAlignment {
  left(0),
  center(1),
  right(2);

  const PrintAlignment(this.code);
  final int code;
}

/// Barcode symbologies (iPosPrinter convention).
enum BarcodeSymbology {
  upcA(0),
  upcE(1),
  ean13(2),
  ean8(3),
  code39(4),
  itf(5),
  codabar(6),
  code93(7),
  code128(8);

  const BarcodeSymbology(this.code);
  final int code;
}

enum BarcodeTextPosition {
  none(0),
  above(1),
  below(2),
  both(3);

  const BarcodeTextPosition(this.code);
  final int code;
}

/// QR error correction. Lower = more data, less recovery.
enum QrErrorLevel {
  l(0),
  m(1),
  q(2),
  h(3);

  const QrErrorLevel(this.code);
  final int code;
}
