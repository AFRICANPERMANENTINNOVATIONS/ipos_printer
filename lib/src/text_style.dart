/// Styling options applied to subsequent print operations.
class PrinterTextStyle {
  const PrinterTextStyle({
    this.textSize = 24,
    this.fillBlack = false,
    this.underline = false,
    this.italic = false,
    this.scaleX = 0,
    this.scaleY = 0,
  });

  final int textSize;
  final bool fillBlack;
  final bool underline;
  final bool italic;
  final int scaleX;
  final int scaleY;

  Map<String, Object?> toMap() => {
        'textSize': textSize,
        'fillBlack': fillBlack,
        'underline': underline,
        'italic': italic,
        'scaleX': scaleX,
        'scaleY': scaleY,
      };
}
