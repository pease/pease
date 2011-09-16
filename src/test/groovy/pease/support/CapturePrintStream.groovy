package pease.support

class CapturePrintStream extends PrintStream {
  ByteArrayOutputStream byteArrayOutputStream

  CapturePrintStream() {
    this(new ByteArrayOutputStream())
  }

  CapturePrintStream(ByteArrayOutputStream byteArrayOutputStream) {
    super(byteArrayOutputStream)
    this.byteArrayOutputStream = byteArrayOutputStream
  }

  @Override
  String toString() {
    byteArrayOutputStream.toString()
  }
}
