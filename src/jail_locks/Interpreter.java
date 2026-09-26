package jail_locks;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  // defining the global-scope on interpreter's initialization:
  final Environment globals = new Environment();
  private Environment environment = globals;
  //
  private final boolean isRepl;

  // constructor:
  Interpreter(boolean isRepl) {
    this.isRepl = isRepl;

    // defined native functions:
    //---
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
    //---
  }
//-----------------------------------------------------
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  /**
  * execute a Stmt.
  */
  private void execute(Stmt stmt) {
    stmt.accept(this); // 'this' refers to the 'Interpreter' instance himself. =>
    // that in terms calls the '(Expr/Stmt).java'-subclass's own .accept()-method, which =>
    // "rewires" the call back to the correct shape call:
    // InterpreterInstance.ImplementedVisitorMethod((Expr/Stmt)subclassInstance)
    // example being: passing: expr = Expr.Literal literal, this = Interpreter interpreter
    // 'literal.call(interpreter)' turns into: 'interpreter.visitLiteralExpr(literal)'
    // to be clear: in this case ImplementedVisitorMethod = visitLiteralExpr().
  }

  /**
  * evaluate an Expr.
  */
  protected Object evaluate(Expr expr) { // 'evaluate' gets called when an =>
    // expression's value is required, as described in lox's EBNF grammar rules.
    return expr.accept(this);
  }
//-----------------------------------------------------
  /**
  * in REPL mode: evaluates the statement-expression and prints it to stdout.
  * in non-REPL mode (script): only evaluates the expression.
  */
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    Object temp = evaluate(stmt.expression);
    if (isRepl && temp != null) {
      System.out.println(stringify(temp));
    }

    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // conversion of the Stmt.Function to a full LoxFunction-object =>
    // that implements: constructor (dah), arity(), call(), toString():
    LoxFunction function = new LoxFunction(stmt);

    // saving that function object in the current environment:
    environment.define(stmt.name.lexeme, function); // 'name' is the IDENTIFIER
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }
//-----------------------------------------------------

  /**
  * for defining new variables in the current environment.
  */
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    else {
      value = Environment.UNINITIALIZED;
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
      while (isTruthy(evaluate(stmt.condition))) {
        try {
          execute(stmt.body);
        } catch (BREAK e) { // detected a 'break'-statement:
          break;
        } catch (CONTINUE e) { // detected a 'continue'-statement:
          continue;
        }
      }
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitForStmt(Stmt.For stmt) {
    if (stmt.initializer != null) execute(stmt.initializer);

    while (stmt.condition == null || isTruthy(evaluate(stmt.condition))) {
      try {
          if (stmt.body != null) execute(stmt.body);
          if (stmt.increment != null) evaluate(stmt.increment);
        } catch (BREAK e) { // detected a 'break'-statement:
          break;
        } catch (CONTINUE e) { // detected a 'continue'-statement:
          if (stmt.increment != null) evaluate(stmt.increment);
          continue;
        }
    }
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitLoopFlowCtrlStmt(Stmt.LoopFlowCtrl stmt) {
    switch (stmt.instruction.type) {
    case BREAK -> throw new BREAK();
    case CONTINUE -> throw new CONTINUE();
    }
    return null;
  }
//-----------------------------------------------------
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    if (stmt.CreateNestedEnv) {
      executeBlock(stmt.statements, new Environment(environment));
    }
    else {
      executeStmtList(stmt.statements);
    }

    return null;
  }
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      executeStmtList(statements);

    } finally {
      this.environment = previous;
    }
  }
//-----------------------------------------------------
  @Override
    public Void visitExitStmt(Stmt.Exit stmt) {
        System.exit(0);
        return null;
    }
//-----------------------------------------------------
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }
//-----------------------------------------------------
  /**
  * for fetching an existing variable's value.
  */
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }
//-----------------------------------------------------
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
//-----------------------------------------------------
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }

    return evaluate(expr.right);
  }
//-----------------------------------------------------
  @Override
  public Object visitPrefixUnaryExpr(Expr.PrefixUnary expr) {
    Object right = evaluate(expr.right);

    return switch (expr.operator.type) {
      case BANG -> !isTruthy(right);
      case MINUS -> {
        checkNumberOperand(expr.operator, right);
        yield -(double)right;
      }
        default ->
          // Unreachable.
          null;
    };
  }
//-----------------------------------------------------
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }
//-----------------------------------------------------
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    return switch (expr.operator.type) {
        //--------------------------
        // challenge:
        // implementing lexical string comparisons in lox:
        //---
      case GREATER -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left > (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String) right) > 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
      }
      case GREATER_EQUAL -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left >= (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String) right) >= 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
      }
      case LESS -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left < (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String) right) < 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
      }
      case LESS_EQUAL -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left <= (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String) right) <= 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
      }
      //---
      //--------------------------
      case BANG_EQUAL -> !isEqual(left, right);
      case EQUAL_EQUAL -> isEqual(left, right);
      //--------------------------
      case MINUS -> {
        checkNumberOperands(expr.operator, left, right);
        yield (double) left - (double) right;
      }
      case PLUS -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double) left + (double) right;
        }
        if (left instanceof String && right instanceof String) {
          yield (String) left + (String) right;
        }
        // challenge:
        // auto-conversion on: string + (otherType) concatenation.
        // java already support this type of auto-conversion on "+" =>
        // operations with a string operand...:
        //---
        if (left instanceof String && right instanceof Double) {
          yield left + stringify(right);
        }
        if (left instanceof Double && right instanceof String) {
          yield stringify(left) + right;
        }
        if (left instanceof String && right instanceof Boolean) {
          yield (String) left + right;
        }
        if (left instanceof Boolean && right instanceof String) {
          yield left + (String) right;
        }
        //
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
      }
      //---
      case SLASH -> {
        checkNumberOperands(expr.operator, left, right);
        // challenge:
        // implemented both cases of division by - '0' just in case =>
        // we would want to return different values or different error messages:
        //---
        if ((Double) left == 0 && (Double) right == 0) { // '0' / '0'
          throw new RuntimeError(expr.operator, "Tried dividing a '0' by '0'.");
        }
        if ((Double) right == 0) { // 'scalar' / '0'
          throw new RuntimeError(expr.operator, "Tried dividing a scalar number by '0'.");
        }
        //---
        yield (double) left / (double) right;
      }
      case STAR -> {
        checkNumberOperands(expr.operator, left, right);
        yield (double) left * (double) right;
      }
      case MODULO -> {
        checkNumberOperands(expr.operator, left, right);
        yield (double) left % (double) right;
      }
      case EXPONENT -> {
        checkNumberOperands(expr.operator, left, right);
        yield Math.pow((double) left, (double) right);
      }
      case COMMA -> right; // C/C++ ','-operator functionality.
      //--------------------------
      default ->
        // Unreachable.
        null;
    };
  }
//-----------------------------------------------------
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    // fetching the LoxFunction object from the environment =>
    // using it's assigned IDENTIFIER.name string as the key:
    Object callee = evaluate(expr.callee);

    // evaluating the arguments and adding them to the List:
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // robert chose to evaluate the arguments before evaluating =>
    // the 'function' expression itself. an architectural decision =>
    // that makes sense because Lox does support side effects taking =>
    // place inside the 'arguments' block, before the function call =>
    // evaluates, making it a natural decision:
    //---
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
          "Can only call functions and classes");
    }
    LoxCallable function = (LoxCallable)callee;
    //---

    // arity check: ( (arguments.arity == parameters.arity)? ):
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, "Expected " +
          function.arity() + " arguments but got " +
          arguments.size());
    }

    return function.call(this, arguments);
  }
//-----------------------------------------------------
  @Override
  public Object visitTernaryExpr(Expr.Ternary expr) {
    try {
      Boolean left = (Boolean) evaluate(expr.left);
      Object middle = evaluate(expr.middle);
      Object right = evaluate(expr.right);
      if (left) {
        return middle;
      } else {
        return right;
      }
    } catch (ClassCastException e) {
      throw new RuntimeError(expr.question,
              "left-side of the ternary operator must evaluate to a boolean value");
    }
  }

  // implementing dummy visitError methods that will never be called cause the parser =>
  // signals an errors that prevent the AST from being evaluated by the Interpreter:
  public Object visitErrorExpr(Expr.Error err_expr) {return null;}
    
  //
//-----------------------------------------------------
// HELPER FUNCTIONS:
//-----------------------------------------------------
  private String stringify(Object object) {
    if (object == null) return "nil";

    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }

  /**
  * defines what boolean values does jlox primitives / objects(?) return.
  */
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;

    throw new RuntimeError(operator, "Operands must both either be numbers or strings.");
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  void executeStmtList(List<Stmt> statements) {
    for (Stmt statement : statements) {
        execute(statement);
      }
  }
//-----------------------------------------------------
}
