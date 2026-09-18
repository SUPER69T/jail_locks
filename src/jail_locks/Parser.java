package jail_locks;

import java.sql.Types;
import java.util.ArrayList;
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

//---------------------------------------------------------------
// HERE STARTS THE 'program -> expression' AST abstraction layer:
//---------------------------------------------------------------
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration()); // start of recursive descent.
    }

    return statements;
  }
//-----------------------------------------------------
  private Stmt declaration() {
    try {
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError e) {
      synchronize();
      return null;
    }
  }
//-----------------------------------------------------
  private Stmt statement() {
    if (match(IF)) return ifStatement();
    if (match(PRINT)) return printStatement();
    if (match(LEFT_BRACE)) return new Stmt.Block(block());
    if (match(EXIT)) return new Stmt.Exit();
    return expressionStatement();
  }
//-----------------------------------------------------
  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after if condition");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
//-----------------------------------------------------
  private Stmt printStatement() {
    Expr value = expression();
    consume(SEMICOLON, "Expect ';' after value");
    return new Stmt.Print(value);
  }
//-----------------------------------------------------
  /**
  * @RETURNS: a node representing the declaration-statement
  * of a new variable in the environments-hierarchy.
  */
  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = expression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration");
    return new Stmt.Var(name, initializer);
  }
//-----------------------------------------------------
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(SEMICOLON, "Expect ';' after expression");
    return new Stmt.Expression(expr);
  }
//-----------------------------------------------------

  /**
   * @return a list of all declarations within the new "{'...'}" scope =>
   * for the visitBlockStmt to execute within the new inner-environment.
   */
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block");
    return statements;
  }

//---------------------------------------------------------------
// HERE STARTS THE 'expression -> primary' AST abstraction layer:
//---------------------------------------------------------------
  private Expr expression() {
    return comma();
  }
//-----------------------------------------------------
  // challenge - comma (left-associative):
  //---
  private Expr comma() {
    Expr expr = assignment();

    while (match(COMMA)) {
      Token operator = previous();
      Expr right = assignment();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
  //---
//-----------------------------------------------------
  /**
  * @RETURNS: a node representing an assignment of a value to an
  * existing variable within the environments-hierarchy.
  */
  private Expr assignment() {
    Expr expr = ternary();

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }

      error(equals, "Invalid assignment target before the '=' operator");
    }

    // this is my own implementation of the '+=' operator:
    //---
    else if (match(PLUS_EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, new Expr.Binary(expr, new Token(PLUS, "+", null, equals.line), value));
      }

      error(equals, "Invalid assignment target before the '+=' operator");
    }
    //---

    return expr;
  }
//-----------------------------------------------------
  // challenge - ternary (right-associative):
  //---
  private Expr ternary() {
    //-------------------------------------------------------------------------------------------------------------|
    // OLD COMMENT:                                                                                                |
    // tried to add error-production to the ternary operator, but it's just =>                                     |
    // way to complicated to keep both expressions around the ':' token, and make it still make =>                 |
    // sense for a future optimizer / resolver to use it properly... I GIVE UP!                                    |
    // NEW COMMENT:                                                                                                |
    // added my own Expr.Error expression-type class for error-productions, and completely =>                      |
    // upgraded the error-production method robert initially wanted for his jlox interpreter =>                    |
    // (thanks to gemini's effort of explaining and my own struggle with this insane code project).                |
    // awesome stuff, and robert deserves  all the credit for making me fall in love with this =>                  |
    // stuff and really appreciate the complexity, methodology, and beauty of designing and building interpreters. |
    //-------------------------------------------------------------------------------------------------------------|

    Expr expr = equality();
    if (match(QUESTION)) {
      Token question = previous();
      Expr middle = expression();
      Token colon = consume(COLON, "Expected ':' after '?' in the ternary operator");
      Expr right = ternary();
      return new Expr.Ternary(expr, question, middle, colon, right);
    }
    return expr;
  }
  //---
//-----------------------------------------------------
  private Expr equality() {
    Expr expr = comparison();
    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr; // returning 'expr' even if an error-production was found, for the =>
    // 'resolver' section to have a bigger AST to check scopes.
  }
//-----------------------------------------------------
  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) { // 5 + 4 + 7++ + 7;
      Token operator = previous();
      if (match(operator.type)) {// means an increment / decrement operation has been detected.
        // in case of the left expression being a variable that requires reassignment:
        if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        expr = new Expr.Assign(name, new Expr.Binary(expr, operator, new Expr.Literal(Double.valueOf(1))));
        } else { // in case of a regular AST expression node we follow C++'s steps and throw:
          error(operator, "Invalid assignment target before the '" + operator.lexeme + operator.lexeme + "' operator");
        }
      } else {
        Expr right = factor();
        expr = new Expr.Binary(expr, operator, right);
      }
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }
//-----------------------------------------------------
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().literal);
    }

    //
    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(LEFT_PAREN)) {
      Expr expr = expression();
      consume(RIGHT_PAREN, "Expected ')'");
      return new Expr.Grouping(expr);
    }

    // challenge: proper error-productions:
    //---
    return switch (peek().type) {
      case COMMA -> { // comma().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the ',' (comma)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case QUESTION -> { // ternary().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '?' (ternary)-token");

        Expr middle = expression();
        consume(COLON, "Expected ':' after '?' in the ternary operator");
        Expr right= expression();

        List<Expr> subExpressions = List.of(middle, right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case COLON -> { // ternary().
        Token errToken = advance();
        error(errToken, "Unexpected ':' (ternary)-Token located outside of a ternary expression");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case EQUAL, PLUS_EQUAL -> { // assignment().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '" + peek().lexeme + "' (assignment)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case BANG_EQUAL, EQUAL_EQUAL -> { // equality().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '" + peek().lexeme + "' (equality)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case GREATER, GREATER_EQUAL, LESS, LESS_EQUAL -> { //comparison().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '" + peek().lexeme + "' (comparison)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case MINUS, PLUS -> { //term().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '" + peek().lexeme + "' (term)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }
      case SLASH, STAR -> { //factor().
        Token errToken = advance();
        error(errToken, "Expected a left-expression before the '" + peek().lexeme + "' (factor)-operator");

        Expr right= expression();

        List<Expr> subExpressions = List.of(right);
        yield new Expr.Error(errToken, subExpressions);
      }

      // entering panic mode:
      default -> throw error(peek(), "Expected an expression");
    };
    //---
  }
//-----------------------------------------------------

//-----------------------------------------------------
// HELPER FUNCTIONS:
//-----------------------------------------------------

  /// scanning all passed token-types for whether they equal to the current token-type (advances in case of equal).
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  /// 'consume' compares the current token to the expected Token type that's passed
  /// as a parameter and returns this token on successful comparison, while also
  /// advancing. on unsuccessful comparison it throws an error with passed message parameter.
  @SuppressWarnings("CanIgnoreReturnValue") // istg bro, I ain't learning no java for the death of me.
   private Token consume(TokenType type, String message) { // ignore
    if (check(type)) return advance();

    throw error(peek(), message);
  }

  /// @return a comparison of the passed-in token-type, with the current token-type (without advancing).
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  /// advances the token and returns the previous token (the token which just was the current).
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == EOF;
  }

  /// @return the current token (does not advances).
  private Token peek() {
    return tokens.get(current);
  }

  /// @return the previous token (does not advances).
  private Token previous() {
    return tokens.get(current - 1);
  }

  /// @return ParseError().
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /// winds forward until the first ';' or statement is matched.
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
        case EXIT:
          return;
      }

      advance();
    }
  }
//-----------------------------------------------------
}


