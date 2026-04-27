import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:api_pos_printer/api_pos_printer.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('africa.permanentinnovations.api_pos_printer/methods');
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  setUp(() {
    messenger.setMockMethodCallHandler(channel, (call) async {
      switch (call.method) {
        case 'listBackends':
          return <String>['ipos'];
        case 'connect':
          return {'connected': true, 'backend': 'ipos'};
        case 'getStatus':
          return 0;
        default:
          return null;
      }
    });
  });

  tearDown(() => messenger.setMockMethodCallHandler(channel, null));

  test('listBackends maps strings to enum', () async {
    final backends = await ApiPosPrinter.instance.listBackends();
    expect(backends, [PrinterBackend.ipos]);
  });

  test('connect returns active backend', () async {
    final backend = await ApiPosPrinter.instance.connect();
    expect(backend, PrinterBackend.ipos);
  });

  test('getStatus maps to enum', () async {
    final status = await ApiPosPrinter.instance.getStatus();
    expect(status, PrinterStatus.normal);
  });
}
