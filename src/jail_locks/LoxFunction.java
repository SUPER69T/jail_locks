package jail_locks;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure; // =>
  // the 'Resolver' did resolve every single one of a function's =>
  // parameters and arguments, which allows us to create a =>
  // LocalEnvironment and access variables in the fully-indexed way.


  // constructor:
  LoxFunction(Stmt.Function declaration, Environment closure) {  // used with 'visitFunctionStmt()'.
    this.declaration = declaration;
    this.closure = closure;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) { // used with 'visitCallExpr()'.
    Environment environment = new Environment.LocalEnvironment(closure); // using the closure's environment scope.

    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i)); // =>
      // matching the parameter-name and argument-value by their exact places.
      //              | declaration |    |     call    |
    }

    try {
      interpreter.executeBlock(declaration.body, environment); // executing =>
      // the body in the same environment where the parameters were defined.
    } catch (Return returnValue) {
      return returnValue.value;
    }
    // default (no return):
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}