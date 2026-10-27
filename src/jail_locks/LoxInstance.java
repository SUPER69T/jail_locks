package jail_locks;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  protected LoxClass klass;
  private final Map<String, Object> fields = new HashMap<>(); // =>
  // fields in Lox represent both

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  Object get(Token name) {
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null) return method.bind(this);

    throw new RuntimeError(name,
        "Undefined property '" + name.lexeme + "'");
  }

  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return "'" + klass.name + "' - instance.";
  }
}
