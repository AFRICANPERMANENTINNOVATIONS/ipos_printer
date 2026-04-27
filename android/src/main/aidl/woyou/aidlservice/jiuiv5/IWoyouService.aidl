package woyou.aidlservice.jiuiv5;

import woyou.aidlservice.jiuiv5.ICallback;
import android.graphics.Bitmap;

interface IWoyouService {
    void printerInit(ICallback callback);
    void printerSelfChecking(ICallback callback);
    String getPrinterSerialNo();
    String getPrinterVersion();
    String getPrinterModal();
    int updatePrinterState();
    void sendRAWData(in byte[] rawData, ICallback callback);
    void setAlignment(int alignment, ICallback callback);
    void setFontName(String typeface, ICallback callback);
    void setFontSize(float fontsize, ICallback callback);
    void printText(String text, ICallback callback);
    void printTextWithFont(String text, String typeface, float fontsize, ICallback callback);
    void printOriginalText(String text, ICallback callback);
    void printColumnsText(in String[] colsTextArr, in int[] colsWidthArr, in int[] colsAlign, ICallback callback);
    void printBitmap(in Bitmap bitmap, ICallback callback);
    void printBarCode(String data, int symbology, int height, int width, int textposition, ICallback callback);
    void printQRCode(String data, int modulesize, int errorlevel, ICallback callback);
    void lineWrap(int n, ICallback callback);
    void commitPrinterBuffer();
    void enterPrinterBuffer(boolean clean);
    void exitPrinterBuffer(boolean commit);
    void cutPaper(ICallback callback);
}
