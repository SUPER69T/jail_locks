package jail_locks;

// TODO: implement this Parameter class as the parameters that functions =>
// take for the sake of default parameters to work properly.
// 'evaluate_default_expression()' is called inside the LoxFunction class
// whenever a specific argument hasn't fed a default parameter, and is =>
// immediately assigned right there.
// also implement arity-range and new arity checks.
public record Parameter(Token name, Expr def_val) {
  private Object evaluate_default_expression() {
    return Lox.interpreter.evaluate(def_val);
  }
}
