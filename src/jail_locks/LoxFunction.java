package jail_locks;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;

  // constructor:
  LoxFunction(Stmt.Function declaration) { // used with 'visitFunctionStmt()'.
    this.declaration = declaration;
  }

  @Override
  public int arity() {
    return declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) { // used with 'visitCallExpr()'.
    Environment environment = new Environment(interpreter.globals);

    for (int i = 0; i < declaration.params.size(); i++) {
      environment.define(declaration.params.get(i).lexeme, arguments.get(i)); // =>
      // matching the parameter-name and argument-value by their exact places.
      //              | declaration |    |     call    |
    }

    interpreter.executeBlock(declaration.body, environment);
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}