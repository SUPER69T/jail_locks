package jail_locks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

  private final Interpreter interpreter;
  private final Stack<Map<String, VariableState>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;

  // constructor:
  //---
  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
  //---

  private enum FunctionType {
    NONE,
    FUNCTION,
    METHOD,
    LAMBDA
  }

  /// this class is used within the 'scopes'-stack, to track whether a
  /// variable has been initialized and used at stages in the scope's life.
  private static class VariableState {
    final Token token;
    final Integer index;
    boolean defined;
    boolean used;

    VariableState(Token token, Integer index, boolean defined) {
        this.token = token;
        this.index = index;
        this.defined = defined;
        this.used = false;
    }
}

//-----------------------------------------------------
  /// resolves every of the program's parsed statements
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  /// resolves a statement. the process of 'resolution' in this context
  /// means to make the recursive AST-traversal calls in order to track each
  /// (statement/expression)'s usage to enforce static-semantic-rules,
  /// not covered by the parser itself, like certain statement-usages only in
  /// certain locations (return inside a function), and variable usage after
  /// declaration, and so on...
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  /// resolves an expression. the process of 'resolution' in this context
  /// means to make the recursive AST-traversal calls in order to track each
  /// (statement/expression)'s usage to enforce static-semantic-rules,
  /// not covered by the parser itself, like certain statement-usages only in
  /// certain locations (return inside a function), and variable usage after
  /// declaration, and so on...
  private void resolve(Expr expr) {
    expr.accept(this);
  }

  /// resolves local variables, which means calculating the depth-difference,
  /// within the local-environments-tree, from the place where they were
  /// called/used, to the place where they were declared, and saving that
  /// distance in the interpreter class, for it to fetch/assign without the
  /// need to recalculate that distance or recursively look it up at runtime.
  /// NOTE: the 'index' is calculated once at variable-declaration inside
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      Map<String, VariableState> scope = scopes.get(i);
      VariableState state = scope.get(name.lexeme);

      if (state != null) {
        // saving the scope depth and the pre-declared index for the interpreter:
        interpreter.resolve(expr, scopes.size() - 1 - i, state.index);

        // marking the variable as used:
        state.used = true;
        return;
      }
    }
  }
//-----------------------------------------------------
  /// mimicking the start (opening) of a new scope inside the current one (the previous stack entry).
  private void beginScope() {
    scopes.push(new HashMap<String, VariableState>());
  }

  /// mimicking the end (closing) of the current scope + checking and reporting
  /// whether any unused variables were declared, and making a Warning for that case.
  private void endScope() {
    Map<String, VariableState> popped_scope = scopes.pop();
    for (Map.Entry<String, VariableState> entry : popped_scope.entrySet()) {
      VariableState state = entry.getValue();
      if (!state.used) {
        Lox.warning(state.token, "Local variable '" + entry.getKey() + "' is never used");
      }
    }
  }
//-----------------------------------------------------
  /// tracks the sequence of declaration in the current-(innermost)-scope
  /// and reports whether the variable has been instantiated, using a flag.
  private void declare(Token name) {
    if (scopes.isEmpty()) return; // <- if the scopes-stack is empty =>
    // that means we are declaring in the global-scope.

    Map<String, VariableState> scope = scopes.peek();

    // Lox disallows 'variable-shadowing' within the same scope:
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Already a variable with this name in this scope");
    // bad example:
    // var a = 1;
    // var a = 2; <- this is considered wrong Lox-syntax.
    }

    scope.put(name.lexeme, new VariableState(name, scope.size(), false));
    //                                             |           |
    // this index should start at 0, which also matches the number of elements in an empty scope.
  }

  /// updating the flag to 'true', to mimic a properly declared
  /// variable-statement, after the initializer has been fully-resolved.
  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().get(name.lexeme).defined = true;
  }
//-----------------------------------------------------
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    if (stmt.CreateNestedEnv) {
      beginScope();
      resolve(stmt.statements);
      endScope();
    } else {resolve(stmt.statements);}
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt.params, stmt.body, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitLambdaFunctionExpr(Expr.LambdaFunction expr) {
    // not declaring or defining a name for a lambda-function.

    resolveFunction(expr.params, expr.body, FunctionType.FUNCTION);
    return null;
  }

  private void resolveFunction(List<Token> params, List<Stmt> body, FunctionType type) {
    FunctionType enclosingFunction = currentFunction; // ~FOR-FUTURE-USE~
    currentFunction = type; // ~FOR-FUTURE-USE~

    beginScope();

    for (Token param : params) {
      declare(param);
      define(param);
    }
    resolve(body);

    endScope();

    currentFunction = enclosingFunction; // ~FOR-FUTURE-USE~
  }
//-----------------------------------------------------
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code");
    }

    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  @Override
  public Void visitForStmt(Stmt.For stmt) {
    resolve(stmt.initializer);
    resolve(stmt.condition);
    resolve(stmt.increment);
    resolve(stmt.body);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitLoopFlowCtrlStmt(Stmt.LoopFlowCtrl stmt) { // =>
    // have already implemented the loop-depth checks in the parser, =>
    // before implementing the 'Resolver', so no use rechecking it here.
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitExitStmt(Stmt.Exit stmt) { // =>
    // the exit statement has no rules or semantics - wherever it is =>
    // thrown, the program immediately stop running.
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitTernaryExpr(Expr.Ternary expr) {
    resolve(expr.left);
    resolve(expr.middle);
    resolve(expr.right);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitPrefixUnaryExpr(Expr.PrefixUnary expr) {
    resolve(expr.right);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    Token token = expr.name;

    // Lox disallows 'variable-self-referencing':
    if (!scopes.isEmpty()) {
      VariableState state = scopes.peek().get(token.lexeme);
      if (state != null && !state.defined) {
        Lox.error(token, "detected a cyclic-referencing variable");
      }
    }
    // bad examples:
    // 1. var a = a; <- even if 'a' could be allowed to redeclare itself =>
    // again within that same scope, and it would have been declared =>
    // before self-referencing, this syntax should not be allowed within =>
    // Lox, and rightfully so...
    //
    // 2. var a = {var b = a;}; <- this type of chained assignment does =>
    // not cause self-referencing, but rather the pure use of a variable =>
    // before it has been fully initialized.

    resolveLocal(expr, token);

    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitErrorExpr(Expr.Error expr) { // =>
    // in case the parser has created an Error-node, no additional work =>
    // is expected from the Resolver or the Interpreter, cause Lox.java =>
    // will stop the program from resolving/interpreting the source-code.
    return null;
  }
}
//-----------------------------------------------------