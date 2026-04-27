import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:api_pos_printer_example/main.dart';

void main() {
  testWidgets('Demo app renders Connect button', (tester) async {
    await tester.pumpWidget(const DemoApp());
    expect(find.widgetWithText(FilledButton, 'Connect'), findsOneWidget);
  });
}
