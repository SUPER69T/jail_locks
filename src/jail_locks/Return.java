package jail_locks;

class Return extends RuntimeException {
  final Object value;

  Return(Object value) {
    super(null, null, false, false); // Params:
    // [0]: message - no message, no need.
    // [1]: cause - no need to provide.
    // [2]: enableSuppression - disables recording *suppressed-exceptions.
    // [3]: writableStackTrace - we don't want the stack trace to write =>
    // anything to the terminal about the use of our face Return exception.

    this.value = value; // the value which the return-statement returns.
  }
}


// *[suppressed-exceptions]:
// in Java, a suppressed exception is an additional error that is saved =>
// inside a primary exception when multiple errors happen at the same =>
// time, usually during a try-with-resources statement.
// How It WorksPrimary Exception: An error happens inside the main try =>
// block and is thrown.
// Secondary Exception: Another error happens when Java automatically =>
// tries to close the resource (like a file or database connection).
// Suppression: Instead of losing the second error or letting it hide =>
// the first one, Java attaches it to the primary exception as a =>
// "suppressed" exception.