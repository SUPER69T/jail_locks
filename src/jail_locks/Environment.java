package jail_locks;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing;
  private final Map<String, Object> values = new HashMap<>();

  // symbolizes the value of a variable that hasn't been initialized yet:
  public static final Object UNINITIALIZED = new Object();

  Environment() {
    enclosing = null;
  }
  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      Object value = values.get(name.lexeme);
      if (value != UNINITIALIZED) {return value;}
      throw new RuntimeError(name,
        "Tried accessing an uninitialized variable '" + name.lexeme + "'.");
    }

    if (enclosing != null) return enclosing.get(name); // recursive lookup in the =>
      // enclosing scope's environment to get an existing value from the outer scope.

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

   void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    if (enclosing != null) {
      enclosing.assign(name, value); // recursive lookup in the enclosing =>
      // scope's environment to assign an existing value in the outer scope.
      return;
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }

  void define(String name, Object value) {
    values.put(name, value);
  }
}
