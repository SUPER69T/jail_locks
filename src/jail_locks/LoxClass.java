package jail_locks;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
  final String name;
  private final Map<String, LoxFunction> methods;

  LoxClass(String name, Map<String, LoxFunction> methods) {
    this.name = name;
    this.methods = methods;
  }

  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }

    return null;
  }

  /// here is where the 'class()' notation for class-instance initiation =>
  /// is implemented in Lox: as an implementation of the 'call()' - visitor.
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);

    LoxFunction initializer = findMethod("init");
    if (initializer != null) { // if the class declared an =>
      // 'init()'-method, we bind that method to the new instance of =>
      // the LoxClass, and execute it's body with the arguments that =>
      // were passed into the initializer-method's '.call()'.
      initializer.bind(instance).call(interpreter, arguments);
    }

    return instance;
  }

  @Override
  public int arity() { // the 'init()'-method might have =>
    // a non-0 arity, which is pretty intuitive to return =>
    // for the 'arity()' check of the LoxClass itself:
    LoxFunction initializer = findMethod("init");
    if (initializer == null) return 0;
    return initializer.arity();
  }

  @Override
  public String toString() {
    return name;
  }
}