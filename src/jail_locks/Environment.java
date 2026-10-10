package jail_locks;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

abstract class Environment {
  final Environment enclosing;

  public Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  public abstract void define(String name, Object value);


  static class GlobalEnvironment extends Environment {

     // the dictionary encoding all declarations within the global environment:
     private final Map<String, Object> values;

    // symbolizes the value of a variable that hasn't been initialized yet:
    public static final Object UNINITIALIZED = new Object(); // =>
    // after implementing the static-analysis in the 'Resolver'-class, this =>
    // constant is only useful for dynamically checking variables that have =>
    // been declared but not iniZ
    GlobalEnvironment() {
      super(null); // the global environment has no enclosing environment.
      this.values = new HashMap<>();
    }

//------------------------------------------------------
// GLOBAL-VARIABLES DEFINITION, FETCHING AND ASSIGNMENT:
//------------------------------------------------------

    /// assigns a variable by name in the global-scope's environment:
    @Override
    public void define(String name, Object value) {
      values.put(name, value);
    }
//-----------------------------------------------------

    /// fetches the value of a variable in the global environment:
    Object get(Token name) {
      if (values.containsKey(name.lexeme)) {
        Object value = values.get(name.lexeme);
        if (value != UNINITIALIZED) {
          return value;
        }
        throw new RuntimeError(name,
                "Tried accessing an uninitialized variable: '" + name.lexeme + "'.");
      }

      throw new RuntimeError(name,
              "Undefined variable: '" + name.lexeme + "'.");
    }
//-----------------------------------------------------

    /// assigns value to a variable from the global environment:
    void assign(Token name, Object value) {
      if (values.containsKey(name.lexeme)) {
        values.put(name.lexeme, value);
        return;
      }

      throw new RuntimeError(name,
              "Undefined variable: '" + name.lexeme + "'.");
    }
//-----------------------------------------------------
  }


  static class LocalEnvironment extends Environment {
    final Environment enclosing;

    // the array of 'Object's encoding all values within a local environment:
    private final List<Object> values = new ArrayList<>(); // =>
    // each entry to this ArrayList represents a different variable, at =>
    // the same order of declarations as the 'Resolver' found them.
    // and on that same note - the AST-traversal sequence in both the =>
    // 'Resolver', the 'Interpreter', and any other class that traverses =>
    // the tree, is exactly matching, and allows for some cool and highly =>
    // useful assumptions of order, that cut down the need to recalculate =>
    // some information (like location, as seen in this case).

    // constructor:
    //---
    LocalEnvironment(Environment enclosing) {
      super(enclosing);
      this.enclosing = enclosing;
    }
    //---

//-----------------------------------------------------
// LOCAL-VARIABLES DEFINITION, FETCHING AND ASSIGNMENT:
//-----------------------------------------------------
    @Override
    public void define(String name, Object value) {
      values.add(value);
    }
//-----------------------------------------------------
    /// looks up the enclosing environment, corresponding to the provided
    /// 'distance' of lookup upwards, and returns the correct "ancestor"-environment.
    LocalEnvironment ancestor(int distance) {
      LocalEnvironment environment = this;
      for (int i = 0; i < distance; i++) {
        environment = (LocalEnvironment) environment.enclosing;
        // NOTE:
        // if everything is synced up between the interpreter and the =>
        // resolver, then the depth should always be accurate to the real =>
        // depth of the scopes-hierarchy - that means we can never somehow =>
        // magically land back onto the global-environment, and the =>
        // interpreter's visitAssignExpr(),lookUpVariable(), both enforce it.
      }
      return environment;
    }
//-----------------------------------------------------
    /// fetches a value based on a specified hierarchical depth and
    /// index from the current local-scope's environment and upwards,
    /// the indexing has already been done by the 'Resolver'.
    Object getAt(int distance, Integer index) {
      return ancestor(distance).values.get(index);
    }
//-----------------------------------------------------

    /// assigns a value based on a specified hierarchical depth and
    /// index from the current local-scope's environment and upwards,
    /// the indexing has already been done by the 'Resolver'.
    void assignAt(int distance, Integer index, Object value) {
      ancestor(distance).values.set(index, value);
    }
//-----------------------------------------------------
  }
}





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
