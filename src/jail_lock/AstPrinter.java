package jail_lock;

/**
* print the AST in polish-notation.
*/
class AstPrinter implements Expr.Visitor<String> {
  String print_in_PN(Expr expr) {
    return expr.accept(this);
  }

   @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme,
                        expr.left, expr.right);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();

    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }
    builder.append(")");

    return builder.toString();
  }

  public static void main(String[] args) {
    //region expression
     Expr expression = new Expr.Binary(
        new Expr.Unary(
            new Token(TokenType.MINUS, "-", null, 1),
            new Expr.Literal(123)),
        new Token(TokenType.STAR, "*", null, 1),
        new Expr.Grouping(
            new Expr.Literal(45.67)));
    //endregion

    //region expression2
    Expr expression2 = new Expr.Binary(
            new Expr.Grouping(
                    new Expr.Binary(
                            new Expr.Literal(1),
                            new Token(TokenType.PLUS, "+", null, 1),
                            new Expr.Literal(2))),

            new Token(TokenType.STAR, "*", null, 1),

            new Expr.Grouping(new Expr.Binary(
                            new Expr.Literal(4),
                            new Token(TokenType.MINUS, "-", null, 1),
                            new Expr.Literal(3))));
    //endregion

    System.out.println("expression:\n" + new AstPrinter().print_in_PN(expression));
    System.out.println("expression2:\n" + new AstPrinter().print_in_PN(expression2));
  }
}