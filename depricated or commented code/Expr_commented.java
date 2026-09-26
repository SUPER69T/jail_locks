package jail_locks;

abstract class Expr_commented {
//----------------------------------
  interface Visitor<R> { // =>
    // R = 'Object' in Expr.Visitor's case, and 'Void' in Stmt.Visitor's case.
    R visitBinaryExpr(Binary expr);
    R visitTernaryExpr(Ternary expr);
    R visitGroupingExpr(Grouping expr);
    R visitLiteralExpr(Literal expr);
    R visitUnaryExpr(Unary expr);
  }

  abstract <R> R accept(Visitor<R> visitor); // enforces each subclass of the 'Expr' class =>
  // to override the 'accept' method that makes the call to the (also forced) construction =>
  // of a visitor method for that specific 'Expr' type.

//----------------------------------
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this); // 'this' here refers to the specific =>
      // (Expr's child-class)-object which made the 'accept' method call.
      // this object is the one that is holding the declared field values, which =>
      // the Interpreter then evaluates using it's implemented visitormethod.
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
//----------------------------------
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
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
//----------------------------------
}
