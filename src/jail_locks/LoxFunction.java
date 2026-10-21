package jail_locks;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final boolean isInitializer; // used to signify that the =>
  // LoxFunction is an initializer ('init()')-function.

  private final Stmt.Function declaration;
  private final Environment closure; // =>
  // the 'Resolver' did resolve every single one of a function's =>
  // parameters and arguments, which allows us to create a =>
  // LocalEnvironment and access variables in the fully-indexed way.


  // constructor:
  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.declaration = declaration;
    this.closure = closure;
  }

  LoxFunction bind(LoxInstance instance) {
    // WARNING: go over this part again:
    Environment environment = new Environment.LocalEnvironment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer); // =>
    // passing the same 'isInitializer' value into the new, now bounded =>
    // LoxFunction.
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
      // allowing the return of 'this' (a reference to the class-instance) =>
      // on empty 'return'-expressions within the initializer-method.
      if (isInitializer) return closure.getAt(0, "this");
      return returnValue.value;
    }

    if (isInitializer) return closure.getAt(0, "this");

    // default (no return):
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + declaration.name.lexeme + ">";
  }
}