package jail_locks;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing;

  // the dictionary encoding all declarations within the current environment:
  private final Map<String, Object> values = new HashMap<>();

  // symbolizes the value of a variable that hasn't been initialized yet:
  public static final Object UNINITIALIZED = new Object(); // =>
  // after implementing the static-analysis in the 'Resolver'-class, this =>
  // constant is only useful for dynamically checking variables that have =>
  // been declared but not initialized within the global-scope's environment.

  // constructors:
  //---
  Environment() { // for initiating the 'globals' - Environment.
    enclosing = null;
  }
  Environment(Environment enclosing) { // for initiating local Environments.
    this.enclosing = enclosing;
  }
  //---
//-----------------------------------------------------
  /// assigns a variable by name in the current scope's environment:
  void define(String name, Object value) {
    values.put(name, value);
  }
//-----------------------------------------------------

//-----------------------------------------------------
// GLOBAL-VARIABLES FETCHING AND ASSIGNMENT:
//-----------------------------------------------------
  /// fetches the value of a variable in the global environment:
  Object get(Token name) {
    if (values.containsKey(name.lexeme)) {
      Object value = values.get(name.lexeme);
      if (value != UNINITIALIZED) {return value;}
      throw new RuntimeError(name,
        "Tried accessing an uninitialized variable: '" + name.lexeme + "'.");
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
//-----------------------------------------------------
  /// assigns value to a variable from the global environment:
  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }

    throw new RuntimeError(name,
        "Undefined variable '" + name.lexeme + "'.");
  }
//-----------------------------------------------------

//-----------------------------------------------------
// LOCAL-VARIABLES FETCHING AND ASSIGNMENT:
//-----------------------------------------------------
  /// looks up the enclosing environment, corresponding to the provided
  /// 'distance' of lookup upwards, and returns the correct "ancestor"-environment.
  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // this warning is fine. =>
      // if everything is synced up between the interpreter and the =>
      // resolver, then the depth should always be accurate to the real =>
      // depth of the scopes-hierarchy.
    }

    return environment;
  }
//-----------------------------------------------------
  /// fetches a variable based on a specified hierarchical depth from the
  /// current scope's environment and upwards, without checking -> the check
  /// has already been done by the 'Resolver'.
  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name); // <- assuming ill change hte input of this method to receive a Token.
  }
//-----------------------------------------------------
  /// assigns a variable based on a specified hierarchical depth from the
  /// current scope's environment and upwards, without checking -> the check
  /// has already been done by the 'Resolver'.
  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value); // <- same thing here, a Token.
  }
//-----------------------------------------------------



// OLD:
// (get(), assign())-methods (before the 'Resolver'):
//  //-----------------------------------------------------
//  /// fetches the variable from the current environment, and if not found ->
//  /// iteratively makes that same lookup to the enclosing scope's environment.
//  Object get(Token name) {
//    if (values.containsKey(name.lexeme)) {
//      Object value = values.get(name.lexeme);
//      if (value != UNINITIALIZED) {return value;}
//      throw new RuntimeError(name,
//        "Tried accessing an uninitialized variable: '" + name.lexeme + "'.");
//    }
//
//    if (enclosing != null) return enclosing.get(name); // recursive lookup in the =>
//      // enclosing scope's environment to get an existing value from the outer scope.
//
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
//  }
//  //-----------------------------------------------------
//  /// assigns a variable from the current environment, and if not found ->
//  /// iteratively makes that same lookup to the enclosing scope's environment.
//  void assign(Token name, Object value) {
//    if (values.containsKey(name.lexeme)) {
//      values.put(name.lexeme, value);
//      return;
//    }
//
//    if (enclosing != null) {
//      enclosing.assign(name, value); // recursive lookup in the enclosing =>
//      // scope's environment to assign an existing value in the outer scope.
//      return;
//    }
//
//    throw new RuntimeError(name,
//        "Undefined variable '" + name.lexeme + "'.");
//  }
//  //-----------------------------------------------------
}
