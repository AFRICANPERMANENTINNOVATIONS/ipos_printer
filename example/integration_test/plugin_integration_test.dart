import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:api_pos_printer/api_pos_printer.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('listBackends returns a list', (tester) async {
    final backends = await ApiPosPrinter.instance.listBackends();
    expect(backends, isA<List<PrinterBackend>>());
  });
}
