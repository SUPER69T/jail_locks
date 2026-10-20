package jail_locks;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
  final String name;

  LoxClass(String name) {
    this.name = name;
  }

  /// here is where the 'class()' notation for class-instance initiation =>
  /// is implemented in Lox: as an implementation of the 'call()' - visitor.
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);
    return instance;
  }

  @Override
  public int arity() {
    return 0;
  }

  @Override
  public String toString() {
    return name;
  }
}