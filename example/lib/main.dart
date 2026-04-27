import 'dart:async';

import 'package:flutter/material.dart';
import 'package:api_pos_printer/api_pos_printer.dart';

void main() => runApp(const DemoApp());

class DemoApp extends StatefulWidget {
  const DemoApp({super.key});
  @override
  State<DemoApp> createState() => _DemoAppState();
}

class _DemoAppState extends State<DemoApp> {
  final _printer = ApiPosPrinter.instance;

  PrinterBackend? _activeBackend;
  PrinterStatus _status = PrinterStatus.unknown;
  String _log = '';
  StreamSubscription<PrinterStatus>? _sub;

  @override
  void dispose() {
    _sub?.cancel();
    _printer.disconnect();
    super.dispose();
  }

  void _appendLog(String line) {
    setState(() => _log = '${DateTime.now().toIso8601String().substring(11, 19)}  $line\n$_log');
  }

  Future<void> _wrap(String label, Future<void> Function() action) async {
    try {
      await action();
      _appendLog('✓ $label');
    } catch (e) {
      _appendLog('✗ $label → $e');
    }
  }

  Future<void> _connect() async {
    final found = await _printer.listBackends();
    _appendLog('backends détectés: ${found.map((b) => b.id).join(', ')}');
    final backend = await _printer.connect();
    setState(() => _activeBackend = backend);
    _appendLog('connecté via ${backend.id}');
    _sub = _printer.statusStream.listen((s) {
      setState(() => _status = s);
      _appendLog('status broadcast: ${s.name}');
    });
    _status = await _printer.getStatus();
    setState(() {});
  }

  Future<void> _printDemoTicket() async {
    await _printer.printText('PERMANENT INNOVATIONS',
        fontSize: 48, alignment: PrintAlignment.center);
    await _printer.printText('Demo ticket',
        fontSize: 24, alignment: PrintAlignment.center);
    await _printer.feedPaper(dots: 8);
    await _printer.printText('************************',
        fontSize: 32, alignment: PrintAlignment.center);
    await _printer.printColumns(
      texts: ['Coffee', '2', '4.00'],
      widths: [16, 4, 8],
      aligns: [PrintAlignment.left, PrintAlignment.center, PrintAlignment.right],
      fontSize: 24,
    );
    await _printer.printColumns(
      texts: ['Croissant', '1', '2.50'],
      widths: [16, 4, 8],
      aligns: [PrintAlignment.left, PrintAlignment.center, PrintAlignment.right],
      fontSize: 24,
    );
    await _printer.printText('TOTAL: 6.50 EUR',
        fontSize: 32, alignment: PrintAlignment.right);
    await _printer.feedPaper(dots: 16);
    await _printer.printQrCode('https://permanentinnovations.africa',
        moduleSize: 10);
    await _printer.performPrint(feedLines: 80);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('api_pos_printer demo'),
          actions: [
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Center(
                child: Text(
                  '${_activeBackend?.id ?? '—'} • ${_status.name}',
                  style: const TextStyle(fontSize: 13),
                ),
              ),
            ),
          ],
        ),
        body: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  FilledButton(
                      onPressed: () => _wrap('connect', _connect),
                      child: const Text('Connect')),
                  OutlinedButton(
                      onPressed: () => _wrap('disconnect', _printer.disconnect),
                      child: const Text('Disconnect')),
                  OutlinedButton(
                      onPressed: () =>
                          _wrap('getStatus', () async => _status = await _printer.getStatus()),
                      child: const Text('Status')),
                  OutlinedButton(
                      onPressed: () => _wrap('init', _printer.init),
                      child: const Text('Init')),
                  OutlinedButton(
                      onPressed: () => _wrap('selfCheck', _printer.selfCheck),
                      child: const Text('Self check')),
                  FilledButton(
                      onPressed: () => _wrap('demo ticket', _printDemoTicket),
                      child: const Text('Print demo ticket')),
                  OutlinedButton(
                      onPressed: () => _wrap('feed', () => _printer.feedPaper(dots: 80)),
                      child: const Text('Feed 1cm')),
                ],
              ),
              const SizedBox(height: 12),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.black,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: SingleChildScrollView(
                    child: Text(
                      _log.isEmpty ? '— logs —' : _log,
                      style: const TextStyle(
                        color: Colors.greenAccent,
                        fontFamily: 'monospace',
                        fontSize: 12,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
