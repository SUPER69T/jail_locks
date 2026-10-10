package jail_locks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Queue;

public class Lox {
    protected static Interpreter interpreter;
    final static int interpreter_strictness = 0; // 0 being the least strict.

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    static boolean hadWarning = false;
    static boolean hadRuntimeWarning = false;
    static Queue<String> warningsQueue = new java.util.LinkedList<>();

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

   private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));

    interpreter = new Interpreter(false);

    run(new String(bytes, Charset.defaultCharset()));

    while (!warningsQueue.isEmpty()) {
      System.err.println(warningsQueue.poll());
    }

    // Indicate an error in the exit code.
    if (hadError | hadWarning) System.exit(65);
    if (hadRuntimeError | hadRuntimeWarning) System.exit(70);
  }

   private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    interpreter = new Interpreter(true);

    for (;;) {
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line);
      hadError = false;

      while (!warningsQueue.isEmpty()) {
          System.err.println(warningsQueue.poll());
      }
      hadWarning = false;
    }
  }

  private static void run(String source) {

//-----------------------------------------------|
    Scanner scanner = new Scanner(source); //----|
    List<Token> tokens = scanner.scanTokens(); //|
//-----------------------------------------------|

//--------------------------------------------|
    Parser parser = new Parser(tokens); //----|
    List<Stmt> statements = parser.parse(); //|
//--------------------------------------------|

    // Stop if there was a syntax error.
    if (hadError) return;
    // stop if a syntax warning occurred:
    if (hadWarning) {
      if (0 < interpreter_strictness) return;
      else hadWarning = false;
    }

//---------------------------------------------------|
    Resolver resolver = new Resolver(interpreter); //|
    resolver.resolve(statements); //-----------------|
//---------------------------------------------------|

    // Stop if there was a resolution error.
    if (hadError) return;
    // stop if a resolution warning occurred:
    if (hadWarning) {
      if (0 < interpreter_strictness) return;
      else hadWarning = false;
    }

// NOTE: run-time starts here..:
//---------------------------------------|
    interpreter.interpret(statements); //|
//---------------------------------------|

    // stop if a runtime warning occurred:
    if (hadRuntimeWarning) {
      if (0 < interpreter_strictness) return;
      else hadWarning = false;
    }
  }

//-----------------------------------------------------
// (Error/Warning)s HANDLING HELPERS:
//-----------------------------------------------------
// Errors:
//-----------------------------------------------------
  static void error(int line, String message) {
    reportError(line, "", message);
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      reportError(token.line, " at end", message);
    } else {
      reportError(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  private static void reportError(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message + ".");
    hadError = true;
  }

  static void runtimeError(RuntimeError error) {
    System.err.println("[line " + error.token.line + "] " +
            error.getMessage());

    hadRuntimeError = true;
  }
//-----------------------------------------------------
// Warnings:
//-----------------------------------------------------

  /// reporting compile-time warnings:
  static void warning(Token token, String message) {
    warningsQueue.offer("[line " + token.line + "] Warning: " + message + ".");
    hadWarning = true;
  }

  /// reporting runtime warnings:
  static void runtimeWarning(Token token, String message) {
    warningsQueue.offer("[line " + token.line + "] Warning: " + message + ".");
    hadRuntimeWarning = true;
  }
//-----------------------------------------------------
}