package jail_locks;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final boolean isInitializer; // used to signify that the =>
  // LoxFunction is an initializer ('init()')-function.

  protected final boolean isGetter;

  private final Stmt.Function declaration;
  private final Environment closure; // =>
  // the 'Resolver' did resolve every single one of a function's =>
  // parameters and arguments, which allows us to create a =>
  // LocalEnvironment and access variables in the fully-indexed way.


  // constructor:
  LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, boolean isGetter) {
    this.isInitializer = isInitializer;
    this.isGetter = isGetter;
    this.declaration = declaration;
    this.closure = closure;
  }

  /// the method for binding the class-instance's environment (LoxInstance) =>
  /// to the specific method when it's being called, allowing it to =>
  /// use 'this' and other class-local variables within it's body.
  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment.LocalEnvironment(closure);
    environment.define("this", instance);
    return new LoxFunction(declaration, environment, isInitializer, isGetter); // =>
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

      // in cases of the 'Return'-calling-method being the 'init()' method, we =>
      // simply return the 'this'-reference to the class instance itself:
      if (isInitializer) return ((Environment.LocalEnvironment)closure).getAt(0, 0); // =>
      // indexed lookup of the 'this'-localVariable, which is simply saved =>
      // as the first variable ever declared inside a freshly created class- =>
      // scope, which is why we can look it up using the indexes: 0 for- =>
      // depth, and 0 for its index.

      return returnValue.value;
    }
    if (isInitializer) return ((Environment.LocalEnvironment)closure).getAt(0, 0);
    //if (isInitializer) return closure.getAt(0, "this");

    // default (no return):
    return null;
  }

  @Override
  public String toString() {
    return "'" + declaration.name.lexeme + "' - function.";
  }
}