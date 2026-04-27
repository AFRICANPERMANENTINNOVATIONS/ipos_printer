package com.iposprinter.iposprinterservice;

import com.iposprinter.iposprinterservice.IPosPrinterCallback;

// Method signatures and transaction codes recovered from the LzyHardWareManager
// proxy stub (`y6/b$a$a.smali`). All callback methods return VOID (not boolean)
// — the OEM service writes only `readException()` after each transaction.
//
// Signatures marked CONFIRMED match parameter names extracted from `.param`
// directives in the proxy. Slots between confirmed methods are filled with
// `_reservedNN` placeholders so the transaction codes line up; do NOT call
// these placeholders — their real OEM signatures are unknown.
interface IPosPrinterService {
    /* txn  1 */ int getPrinterStatus();                                                                                              // CONFIRMED
    /* txn  2 */ void _reserved2(IPosPrinterCallback callback);
    /* txn  3 */ void _reserved3(IPosPrinterCallback callback);
    /* txn  4 */ void _reserved4(IPosPrinterCallback callback);
    /* txn  5 */ void _reserved5(IPosPrinterCallback callback);
    /* txn  6 */ void printerSetAlignment(int alignment, IPosPrinterCallback callback);                                                // CONFIRMED
    /* txn  7 */ void _reserved7(IPosPrinterCallback callback);
    /* txn  8 */ void printerPrintBlankLines(int lines, int height, IPosPrinterCallback callback);                                     // CONFIRMED
    /* txn  9 */ void _reserved9(IPosPrinterCallback callback);
    /* txn 10 */ void printerPrintText(String text, String typeface, int fontsize, IPosPrinterCallback callback);                      // CONFIRMED
    /* txn 11 */ void printSpecFormatText(String text, String typeface, int fontsize, int alignment, IPosPrinterCallback callback);    // CONFIRMED
    /* txn 12 */ void _reserved12(IPosPrinterCallback callback);
    /* txn 13 */ void _reserved13(IPosPrinterCallback callback);
    /* txn 14 */ void _reserved14(IPosPrinterCallback callback);
    /* txn 15 */ void printQRCode(String data, int modulesize, int mErrorCorrectionLevel, IPosPrinterCallback callback);               // CONFIRMED
    /* txn 16 */ void _reserved16(IPosPrinterCallback callback);
    /* txn 17 */ void _reserved17(IPosPrinterCallback callback);
    /* txn 18 */ void printerPerformPrint(int feedlines, IPosPrinterCallback callback);                                                // CONFIRMED
}
