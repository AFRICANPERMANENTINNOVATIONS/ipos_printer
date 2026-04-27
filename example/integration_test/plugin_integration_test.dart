import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:ipos_printer/ipos_printer.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('listBackends returns a list', (tester) async {
    final backends = await IposPrinter.instance.listBackends();
    expect(backends, isA<List<PrinterBackend>>());
  });
}
