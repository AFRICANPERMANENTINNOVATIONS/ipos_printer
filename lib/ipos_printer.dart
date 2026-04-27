import 'dart:async';

import 'package:flutter/services.dart';

import 'src/enums.dart';
import 'src/text_style.dart';

export 'src/enums.dart';
export 'src/text_style.dart';

/// High-level façade over the native multi-backend printer plugin.
///
/// Currently supports:
///  * iPosPrinter (`com.iposprinter.iposprinterservice`) — used by Telpo,
///    Centerm, MTwo and many Chinese-OEM Android POS terminals.
///  * Sunmi InnerPrinter (`woyou.aidlservice.jiuiv5`) — V1, V2, P1, P2…
///
/// All operations are asynchronous. The native side serialises calls and
/// resolves them when the device fires the AIDL `onRunResult` callback.
class IposPrinter {
  IposPrinter._();
  static final IposPrinter instance = IposPrinter._();

  static const _methods = MethodChannel('africa.permanentinnovations.ipos_printer/methods');
  static const _statusEvents = EventChannel('africa.permanentinnovations.ipos_printer/status');

  Stream<PrinterStatus>? _statusStream;

  /// Lists every backend whose service is currently installed on the device.
  Future<List<PrinterBackend>> listBackends() async {
    final raw = await _methods.invokeListMethod<String>('listBackends') ?? const [];
    return raw
        .map((id) => PrinterBackend.values.firstWhere(
              (b) => b.id == id,
              orElse: () => PrinterBackend.auto,
            ))
        .where((b) => b != PrinterBackend.auto)
        .toList();
  }

  /// Binds to the printer service. Pass [PrinterBackend.auto] (default) to
  /// let the plugin pick the first available backend.
  ///
  /// Returns the backend that was actually bound.
  Future<PrinterBackend> connect({PrinterBackend backend = PrinterBackend.auto}) async {
    final res = await _methods.invokeMapMethod<String, Object?>(
      'connect',
      {'backend': backend == PrinterBackend.auto ? null : backend.id},
    );
    if (res == null || res['connected'] != true) {
      throw const IposPrinterException('Failed to connect to printer service');
    }
    final id = res['backend'] as String?;
    return PrinterBackend.values.firstWhere(
      (b) => b.id == id,
      orElse: () => PrinterBackend.auto,
    );
  }

  Future<void> disconnect() => _methods.invokeMethod<void>('disconnect');

  Future<bool> isConnected() async =>
      (await _methods.invokeMethod<bool>('isConnected')) ?? false;

  Future<PrinterBackend?> currentBackend() async {
    final id = await _methods.invokeMethod<String?>('currentBackend');
    if (id == null) return null;
    return PrinterBackend.values.firstWhere(
      (b) => b.id == id,
      orElse: () => PrinterBackend.auto,
    );
  }

  /// Stream of status broadcasts (paper-less, busy, overheat, …). Status is
  /// also reflected in [getStatus] but the stream is event-driven.
  Stream<PrinterStatus> get statusStream {
    return _statusStream ??= _statusEvents.receiveBroadcastStream().map((e) {
      final code = (e is Map) ? (e['status'] as int? ?? -1) : -1;
      return PrinterStatus.fromCode(code);
    });
  }

  Future<PrinterStatus> getStatus() async {
    final raw = await _methods.invokeMethod<int>('getStatus');
    return PrinterStatus.fromCode(raw ?? -1);
  }

  Future<String?> getSerialNumber() => _methods.invokeMethod<String?>('getSerialNumber');
  Future<String?> getModel() => _methods.invokeMethod<String?>('getModel');
  Future<String?> getFirmwareVersion() => _methods.invokeMethod<String?>('getFirmwareVersion');

  Future<void> init() => _methods.invokeMethod<void>('init');
  Future<void> selfCheck() => _methods.invokeMethod<void>('selfCheck');

  Future<void> setAlignment(PrintAlignment alignment) =>
      _methods.invokeMethod<void>('setAlignment', {'alignment': alignment.code});

  Future<void> setTextStyle(PrinterTextStyle style) =>
      _methods.invokeMethod<void>('setTextStyle', style.toMap());

  /// Print a string, terminated with a line feed.
  Future<void> printText(
    String text, {
    int fontSize = 24,
    PrintAlignment alignment = PrintAlignment.left,
    String? typeface,
  }) =>
      _methods.invokeMethod<void>('printText', {
        'text': text.endsWith('\n') ? text : '$text\n',
        'typeface': typeface,
        'fontSize': fontSize,
        'alignment': alignment.code,
      });

  /// Print a row split into columns. Lengths of [texts], [widths] and
  /// [aligns] must match.
  Future<void> printColumns({
    required List<String> texts,
    required List<int> widths,
    required List<PrintAlignment> aligns,
    int fontSize = 24,
    String? typeface,
  }) {
    assert(texts.length == widths.length && widths.length == aligns.length,
        'texts/widths/aligns must have the same length');
    return _methods.invokeMethod<void>('printColumns', {
      'texts': texts,
      'widths': widths,
      'aligns': aligns.map((a) => a.code).toList(),
      'typeface': typeface,
      'fontSize': fontSize,
    });
  }

  /// Decode and print a bitmap from PNG/JPEG bytes.
  ///
  /// [size] is interpreted by the native side: on iPos it controls the
  /// rendering width; on Sunmi it is ignored.
  Future<void> printBitmap(
    Uint8List bytes, {
    PrintAlignment alignment = PrintAlignment.center,
    int size = 0,
  }) =>
      _methods.invokeMethod<void>('printBitmap', {
        'bytes': bytes,
        'alignment': alignment.code,
        'size': size,
      });

  Future<void> printBarcode(
    String data, {
    BarcodeSymbology symbology = BarcodeSymbology.code128,
    int height = 60,
    int width = 2,
    BarcodeTextPosition textPosition = BarcodeTextPosition.below,
  }) =>
      _methods.invokeMethod<void>('printBarcode', {
        'data': data,
        'symbology': symbology.code,
        'height': height,
        'width': width,
        'textPosition': textPosition.code,
      });

  Future<void> printQrCode(
    String data, {
    int moduleSize = 8,
    QrErrorLevel errorLevel = QrErrorLevel.h,
  }) =>
      _methods.invokeMethod<void>('printQrCode', {
        'data': data,
        'moduleSize': moduleSize,
        'errorLevel': errorLevel.code,
      });

  /// Send raw ESC/POS bytes. Useful for advanced commands not exposed
  /// directly by the plugin.
  Future<void> printRaw(Uint8List bytes) =>
      _methods.invokeMethod<void>('printRaw', {'bytes': bytes});

  /// Feed paper by [dots] (≈ 8 dots/mm on most thermal heads).
  Future<void> feedPaper({int dots = 80}) =>
      _methods.invokeMethod<void>('feedPaper', {'dots': dots});

  /// Flush the iPos print buffer and feed [feedLines] empty lines.
  /// On Sunmi this is mapped to `lineWrap(feedLines)`.
  Future<void> performPrint({int feedLines = 4}) =>
      _methods.invokeMethod<void>('performPrint', {'feedLines': feedLines});
}

class IposPrinterException implements Exception {
  const IposPrinterException(this.message);
  final String message;
  @override
  String toString() => 'IposPrinterException: $message';
}
