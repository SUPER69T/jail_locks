package jail_locks;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
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

  private void execute(Stmt stmt) {
    stmt.accept(this); // 'this' refers to the 'Interpreter' instance.
  }

  private Object evaluate(Expr expr) { // 'evaluate' gets called by =>
    // the Stmt visitor methods, as described in lox's EBNF grammar rules.
    return expr.accept(this);
  }

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
//-----------------------------------------------------
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
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
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
//-----------------------------------------------------
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return environment.get(expr.name);
  }
//-----------------------------------------------------
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
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
          yield (double)left > (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String)right) > 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
        }
        case GREATER_EQUAL -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double)left > (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String)right) >= 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
        }
        case LESS -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double)left > (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String)right) < 0;
        }
        throw new RuntimeError(expr.operator, "Operands must both either be numbers or strings.");
        }
        case LESS_EQUAL -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double)left > (double) right;
        }
        else if (left instanceof String && right instanceof String) {
          yield ((String) left).compareTo((String)right) <= 0;
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
        yield (double)left - (double)right;
        }
        case PLUS -> {
        if (left instanceof Double && right instanceof Double) {
          yield (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          yield (String)left + (String)right;
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
          yield (String)left + right;
        }
        if (left instanceof Boolean && right instanceof String) {
          yield left + (String)right;
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
        if ((Double)left == 0 && (Double)right == 0) { // '0' / '0'
          throw new RuntimeError(expr.operator,
        "Tried dividing a '0' by '0'.");
        }
        if ((Double)right == 0) { // 'scalar' / '0'
          throw new RuntimeError(expr.operator,
        "Tried dividing a scalar number by '0'.");
        }
        //---
        yield (double)left / (double)right;
        }
        case STAR -> {
        checkNumberOperands(expr.operator, left, right);
        yield (double)left * (double)right;
        }
        //--------------------------
        default ->
        // Unreachable.
        null;
      };
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
//-----------------------------------------------------
}
