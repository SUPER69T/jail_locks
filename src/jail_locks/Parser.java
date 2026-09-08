package jail_locks;

import java.util.List;

// importing all TokenTypes:
import static jail_locks.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  Expr parse() {
    try {
      return expression();
    } catch (ParseError error) {
      return null;
    }
  }

    private Expr expression() {
    return comma();
  }
  // challenges:
  // comma (left-associative) + ternary (right-associative).
  //---
  private Expr comma() {
    Token ProductionErrorToken = match(COMMA) ? previous() : null;

    Expr expr = ternary();

    while (match(COMMA)) {
      Token operator = previous();
      Expr right = ternary();
      expr = new Expr.Binary(expr, operator, right);
    }

    if (ProductionErrorToken != null) {
      Lox.error(ProductionErrorToken,
              "Expected a left-expression before the " + "'" + ProductionErrorToken + "' Token");
    }

    return expr;
  }

  private Expr ternary() { // tried to add error-production to the ternary operator, but it's just =>
    // way to complicated to keep both expressions around the ':' token, and make it still make =>
    // sense for a future optimizer / resolver to use it properly... I GIVE UP!

    Expr expr = equality();
    if (match(QUESTION)) {
      Token question = previous();
      Expr middle = expression();
      Token colon = consume(COLON, "Expected a colon: ':', after question-operator: '?'");
      Expr right = ternary();
      return new Expr.Ternary(expr, question, middle, colon, right);
    }
    return expr;
  }
  //---

  private Expr equality() {
    Token ProductionErrorToken = match(BANG_EQUAL, EQUAL_EQUAL) ? previous() : null;

    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    if (ProductionErrorToken != null) {
      Lox.error(ProductionErrorToken,
              "Expected a left-expression before the " + "'" + ProductionErrorToken + "' Token");
    }

    return expr; // returning 'expr' even if an error-production was found, for the =>
    // 'resolver' section to have a bigger AST to check scopes.
  }

  private Expr comparison() {
    Token ProductionErrorToken = match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL) ? previous() : null;

    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    if (ProductionErrorToken != null) {
      Lox.error(ProductionErrorToken,
              "Expected a left-expression before the " + "'" + ProductionErrorToken + "' Token");
    }

    return expr;
  }

  private Expr term() {
    Token ProductionErrorToken = match(MINUS, PLUS) ? previous() : null;

    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    if (ProductionErrorToken != null) {
      Lox.error(ProductionErrorToken,
              "Expected a left-expression before the " + "'" + ProductionErrorToken + "' Token");
    }

    return expr;
  }

  private Expr factor() {
    Token ProductionErrorToken = match(SLASH, STAR) ? previous() : null;

    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    if (ProductionErrorToken != null) {
      Lox.error(ProductionErrorToken,
              "Expected a left-expression before the " + "'" + ProductionErrorToken + "' Token");
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expected ')'");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expected an expression");
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  /**
  * 'consume' is mainly used to advance, but also throw errors with specific messages:
  */
  @SuppressWarnings("CanIgnoreReturnValue") // istg bro, i ain't learning no java for the death of me.
   private Token consume(TokenType type, String message) { // ignore
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

}


