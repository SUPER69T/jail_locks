package jail_locks;

import java.util.List;

abstract class Expr {
//----------------------------------
  interface Visitor<R> {
    R visitAssignExpr(Assign expr);
    R visitBinaryExpr(Binary expr);
    R visitTernaryExpr(Ternary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitLogicalExpr(Logical expr);
    R visitPrefixUnaryExpr(PrefixUnary expr);
    R visitVariableExpr(Variable expr);
    R visitErrorExpr(Error expr);
  }

  abstract <R> R accept(Visitor<R> visitor);
//----------------------------------
//-------------Assign:
  static class Assign extends Expr {
    Assign(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Expr value;
  }
//----------------------------------
//-------------Binary:
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
//----------------------------------
//-------------Ternary:
  static class Ternary extends Expr {
    Ternary(Expr left, Token question, Expr middle, Token colon, Expr right) {
      this.left = left;
      this.question = question;
      this.middle = middle;
      this.colon = colon;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTernaryExpr(this);
    }

    final Expr left;
    final Token question;
    final Expr middle;
    final Token colon;
    final Expr right;
  }
//----------------------------------
//-------------Grouping:
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
//----------------------------------
//-------------Literal:
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
//----------------------------------
//-------------Logical:
  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
//----------------------------------
//-------------PrefixUnary:
  static class PrefixUnary extends Expr {
    PrefixUnary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrefixUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
//----------------------------------
//-------------Variable:
  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }
//----------------------------------
//-------------Error:
  static class Error extends Expr {
    Error(Token errToken, List<Expr> subExpressions) {
      this.errToken = errToken;
      this.subExpressions = subExpressions;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitErrorExpr(this);
    }

    final Token errToken;
    final List<Expr> subExpressions;
  }
//----------------------------------
}
