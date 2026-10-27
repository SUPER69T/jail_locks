package jail_locks;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

// importing all TokenTypes:
import static jail_locks.TokenType.*;

class Parser {
  private static class ParseError extends RuntimeException {}

  private final List<Token> tokens;
  private int current = 0;
  private int loopDepth = 0; // a simulation of the nested depth inside the =>
  // (while/for)-loops, to indicate whether the Stmt.LoopFlowCtrl should generate =>
  // the proper (BREAK/CONTINUE)-Token if currently nested within a loop, or throw =>
  // a ParseError for violating the Token's proper location rules.

  // constructor:
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  record FunctionParamsBodyisGetterTRIPLET (List<Token> params, List<Stmt> body, boolean isGetter) {} // =>
  // this record is used by the finishFunction() helper method, for =>
  // constructing both functions, methods, and lambda-functions.

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
      if (match(CLASS)) return classDeclaration();
      if (check(FUN) && checkNext(IDENTIFIER)){ // ensuring the next =>
        // Token is an IDENTIFIER, allows the declaration of lambda-func.
        advance();
        return function("function");
      }
      if (match(VAR)) return varDeclaration();
      return statement();
    } catch (ParseError e) {
      synchronize();
      return null;
    }
  }
//-----------------------------------------------------
  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name");

    if (check(LEFT_PAREN)) { // this is the production-rule that enforces =>
      // static-methods from never being declared outside a class's body.
      throw error(peek(),"cannot declare static-methods outside of a class's body");
    }

    consume(LEFT_BRACE, "Expect '{' before class body");

    List<Stmt.Function> methods = new ArrayList<>();
    List<Stmt.Function> staticMethods = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      if (match(CLASS)) { // the parser coming into this clause, means =>
        // it detected an src-sequence like: "class name1 { class..." =>
        // matching the ambiguity of declaring classes and static-methods =>
        // with that same 'CLASS'-Token recycled.
        staticMethods.add(function("method")); // felt like the =>
        // 'kind' should stay "method" cause the 'kind' of the method =>
        // makes minimal difference in the error messages where it is used.

      } else {
        methods.add(function("method"));
      }
    }

    consume(RIGHT_BRACE, "Expect '}' after class body");

    return new Stmt.Class(name, methods, staticMethods);
  }
//-----------------------------------------------------
  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name");
    FunctionParamsBodyisGetterTRIPLET paramsANDbodyPAIR = finishFunction(kind, name);
    return new Stmt.Function(name, paramsANDbodyPAIR.params, paramsANDbodyPAIR.body, paramsANDbodyPAIR.isGetter);
  }

  /// this one helper is used by both function() and lambdaFunction() production-rules.
  private FunctionParamsBodyisGetterTRIPLET finishFunction(String kind, Token name) {
    List<Token> parameters = new ArrayList<>();
    boolean isGetter = false;

    if (check(LEFT_BRACE)) { // =>
      if (!kind.equals("lambda-function")) {
        // if we immediately encounter a left-brace right after parsing =>
        // the method-IDENTIFIER, we will assume that it's the parsing =>
        // of a Getter-method we are dealing with, in which case, we =>
        // should update the Stmt.Function's isGetter-flag, to signal =>
        // the Interpreter that it should be executed before returned.
        // NOTE: so far lambda-functions are the only kind of function =>
        // that Lox shouldn't allow to be declared as class-getters.
        // even static-methods can be declared as getters.
        if (!name.lexeme.equals("init")) {
          isGetter = true;
        } else {
          // NOTE: nvm on the last note, I decided on disallowing the =>
          // "init"-constructor-method from being declared as a getter- =>
          // -method for good. the less ambiguity I allow to keep in =>
          // the dynamic implementation of the jlox language, the less =>
          // prone for bugs it will become.
          throw error(peek(),
"constructor-'init'-methods cannot be declared as getter-methods in Lox");
        }
      } else { throw error(peek(),
"lambda-functions cannot be declared as getter-methods in Lox");
      }
    } else {
      consume(LEFT_PAREN, "Expect '(' after " + kind + " name");
      if (!check(RIGHT_PAREN)) {
        do {
          if (parameters.size() >= 255) {
            error(peek(), "Can't have more than 255 parameters");
          }

          parameters.add(
                  consume(IDENTIFIER, "Expect parameter name"));
        } while (match(COMMA));
      }
      consume(RIGHT_PAREN, "Expect ')' after parameters");
    }

    consume(LEFT_BRACE, "Expect '{' before " + kind + " body");
    List<Stmt> body = block();
    return new FunctionParamsBodyisGetterTRIPLET(parameters, body, isGetter);
  }

//  NOTE: THIS IS PART OF BUILDING THE DEFAULT-PARAMETERS FEATURE.
//  private Stmt.Function function(String kind) { // kind is ("function"/"method")
//    Token name = consume(IDENTIFIER, "Expect " + kind + " name");
//    Token parameter;
//    Token rvalue_tkn;
//    boolean defaults = false;
//
//    consume(LEFT_PAREN, "Expect '(' after " + kind + " name");
//    List<Token> parameters = new ArrayList<>();
//    if (!check(RIGHT_PAREN)) { // if 'false', the function was declared with 0 parameters.
//      // else:
//      do { // example: fun dosomtng(A, B = 5);
//        if (parameters.size() >= 255) {
//          error(peek(), "Can't have more than 255 parameters");
//        } // A,B|
//        parameter = consume(IDENTIFIER, "Expect parameter name");
//
//        if (peek().type == EQUAL) { // =>
//        // detected the start of the default parameters
//          defaults = true;
//          break;
//        }
//        parameters.add(parameter); // parameters = [IDENTIFIER(A)]
//
//      } while (match(COMMA));
//    }
//
//    if (defaults) {
//      // assigning default parameters:
//      //---
//      current--; // unwinding back to the 'IDENTIFIER' right before the first '='.
//      do {
//        if (parameters.size() >= 255) {
//          error(peek(), "Can't have more than 255 parameters");
//        } // B  |
//        parameter = consume(IDENTIFIER, "Expect parameter name");
//
//        if (match(EQUAL)) {
//          if (match(NUMBER, STRING)) {
//            rvalue_tkn = previous(); //  |       B       |  |        5       |  |      1      |
//            parameter = new Token(EQUAL, parameter.lexeme, rvalue_tkn.literal, rvalue_tkn.line);
//          }
//          error(parameter, "default parameters can only be assigned rvalues of types: 'NUMBER' / 'STRING'.");
//        }
//        else error(peek(), "cannot declare positional arguments after defaults");
//
//        parameters.add(parameter); // parameters = [tkn(A),tkn(B,5)]
//
//      } while (match(COMMA));
//      //---
//    }
//    consume(RIGHT_PAREN, "Expect ')' after parameters");
//
//    consume(LEFT_BRACE, "Expect '{' before " + kind + " body");
//
//    List<Stmt> body = block();
//    return new Stmt.Function(name, parameters, body);
//  }
//-----------------------------------------------------
  /**
  * @RETURNS: a node representing the declaration-statement
  * of a new variable in the current environment.
  */
  private Stmt varDeclaration() {

    Token name;
    Expr initializer;
    List<Stmt> varDecs = new ArrayList<>();

    // I was just thinking about how I shit on do-while loops all the time =>
    // cause of how little use I ever got out of them and how I should def =>
    // not implement it in Lox, and now I stumbled on the most perfect use =>
    // case for a do-while loop I think I have ever seen in my life. ggs:
    //---
    do {
      name = consume(IDENTIFIER, "Expect variable name");
      initializer = null;
      if (match(EQUAL)) {
      initializer = assignment();
      }
      varDecs.add(new Stmt.Var(name, initializer));
    } while (match(COMMA));
    //---

    consume(SEMICOLON, "Expect ';' after variable declaration");
    return new Stmt.Block(varDecs, false);
  }
//-----------------------------------------------------
  private Stmt statement() {
    return switch (peek().type) {
      case FOR -> {
        advance();
        yield forStatement();
      } case IF -> {
          advance();
          yield ifStatement();
        } case PRINT -> {
            advance();
            yield printStatement();
          } case RETURN -> {
              advance();
              yield returnStatement();
            } case WHILE -> {
                advance();
                yield whileStatement();
              } case LEFT_BRACE -> {
                  advance();
                  yield new Stmt.Block(block(), true);
                } case EXIT, QUIT -> { // desugaring the 'QUIT'-Token to =>
                  // perform the same action as the Lox-'exit'-statement, =>
                  // by too- constructing and returning a Stmt.Exit-AST-node:
                    advance();
                    yield new Stmt.Exit();
                  } case BREAK, CONTINUE -> {
                      advance();
                      yield loopFlowStatement();
                    }

                    default -> expressionStatement();
    };
    // robert was using a bunch of 'if' conditions, which may be due to =>
    // him writing this code early-on, before the enhanced-switch came =>
    // to java, which was around 2020 if google is right.
    // robert's old code (+my additions to jlox):
    //---
//    if (match(FOR)) return forStatement();
//    if (match(IF)) return ifStatement();
//    if (match(PRINT)) return printStatement();
//    if (match(WHILE)) return whileStatement();
//    if (match(LEFT_BRACE)) return new Stmt.Block(block(), true);
//    if (match(EXIT)) return new Stmt.Exit();
//    if (match(BREAK, CONTINUE)) return loopFlowStatement(); // 'break' / 'continue'.
//
//    return expressionStatement();
    //---
  }
//-----------------------------------------------------
  private Stmt forStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = expression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    Expr increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = expression();
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");
    //
    loopDepth += 1;
    Stmt body = statement();
    loopDepth -= 1;
    //

    return new Stmt.Block(List.of(new Stmt.For(initializer, condition, increment, body)), true);
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
    private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = expression();
    }

    consume(SEMICOLON, "Expect ';' after return value");
    return new Stmt.Return(keyword, value);
  }
//-----------------------------------------------------
  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'");
    Expr condition = expression();
    consume(RIGHT_PAREN, "Expect ')' after condition");
    //---
    loopDepth+=1;
    Stmt body = statement(); // <-- --- --- --- --- --- --- --- --- ---| these 'loopFlowStatement()' - Stmts
    loopDepth-=1; //                                                   | are also being parsed and returned
    //---                                                              | inside the 'forStatement()' - rule,
    return new Stmt.While(condition, body); //                         | all while updating the 'loopDepth'-
  } //                                                                 | counter to match the current nesting-
//-----------------------------------------------------                | depth for enforcing correct loopFlow-
  private Stmt loopFlowStatement() { // --> --- --- --- --- --- --- ---| Tokens appearance - locations.
    Token brkORcntTkn = previous();
    String name = brkORcntTkn.lexeme;
    consume(SEMICOLON, "Expect ';' after '" + name + "'");
    if (0 < loopDepth) {
      return new Stmt.LoopFlowCtrl(brkORcntTkn);
    }
    throw error(brkORcntTkn, "The '" + name + "' statement cannot appear outside of an any enclosing loops.");
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
  * @RETURNS: an AST node representing the assignment of a value
  * to an existing variable within the current Environment.
  */
  private Expr assignment() { // (right-associative).
    Expr expr = ternary();

    if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, MUL_EQUAL, DIV_EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;

        if (equals.type.equals(EQUAL)) {
          return new Expr.Assign(name, value);
        }
        // this is my own implementation of the '+=' / '-=' operators:
        //---
        else if (equals.type.equals(PLUS_EQUAL)) {
          return new Expr.Assign(name, new Expr.Binary(expr, new Token(PLUS, "+", null, equals.line), value));
        } else if (equals.type.equals(MINUS_EQUAL)) {
          return new Expr.Assign(name, new Expr.Binary(expr, new Token(MINUS, "-", null, equals.line), value));
        } else if (equals.type.equals(MUL_EQUAL)) {
          return new Expr.Assign(name, new Expr.Binary(expr, new Token(STAR, "*", null, equals.line), value));
        } else if (equals.type.equals(DIV_EQUAL)) {
          return new Expr.Assign(name, new Expr.Binary(expr, new Token(SLASH, "/", null, equals.line), value));
        }
        //---
      } else if (expr instanceof Expr.Get) {
          Expr.Get get = (Expr.Get) expr;
          return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target before the '" + equals.lexeme + "' operator");
    }

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

    Expr expr = or();
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
  private Expr or() { // (left-associative).
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//-----------------------------------------------------
  private Expr and() { // (left-associative).
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//-----------------------------------------------------
  private Expr equality() { // (left-associative).
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
  private Expr comparison() { // (left-associative).
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr term() { // (left-associative).
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr factor() { // (left-associative).
    Expr expr = exponent();

    while (match(SLASH, STAR, MODULO)) {
      Token operator = previous();
      Expr right = exponent();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr exponent() { // (right-associative).
    Expr expr = prefix_unary();

    if (match(EXPONENT)) {
      Token exponent = previous();
      Expr power = exponent();
      expr = new Expr.Binary(expr, exponent, power);
    }

    return expr;
  }
//-----------------------------------------------------
  private Expr prefix_unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = prefix_unary();
      return new Expr.PrefixUnary(operator, right);
    }

    return postfix_unary();
  }
//-----------------------------------------------------
  private Expr postfix_unary() {
    Expr expr = call();

    while (match(INCREMENT, DECREMENT)) {
      Token operator = previous();
      Token binary_op = operator.type.equals(INCREMENT) ?
      new Token(PLUS, "+", null, operator.line) :
      new Token(MINUS, "-", null, operator.line);

      // in case of the left expression being a variable that requires reassignment:
      if (expr instanceof Expr.Variable) {
      Token name = ((Expr.Variable)expr).name; //      |(PLUS/MINUS)|
      expr = new Expr.Assign(name, new Expr.Binary(expr, binary_op, new Expr.Literal(1.0)));
      } else { // in case of a regular AST expression node we follow C++'s steps and throw:
        error(operator, "Invalid assignment target before the '" + operator.lexeme + "' operator");
      }
    }
    return expr;
  }
//-----------------------------------------------------
  private Expr call() {
    Expr expr = lambdaFunction(); // to parse a valid function, the =>
    // given 'expr' should be a Token of type: 'IDENTIFIER'.

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER,
            "Expect property name after '.'");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }

    return expr;
  }
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments");
        }
        // building an AST subtree for each argument passed to the call.
        // Lox allows a function argument to be of any evaluated type:
        arguments.add(assignment()); // =>
      } while (match(COMMA)); // compared to the function() parsing rule =>
      // the finishCall() method makes an additional call to parse any =>
      // additional expressions that are being passed-in as arguments, which =>
      // in robert's original code meant calling the expression()-rule that =>
      // (SURPRISE SURPRISE) - holds the (challenge's) comma()-rule right =>
      // beneath it (expression < comma) and higher in precedence, which meant =>
      // that anytime a function call with multiple arguments would be passed =>
      // the 'COMMA' Tokens that separate the arguments would themselves be =>
      // parsed into the AST-node, building a single larger AST node, that =>
      // evaluates to the furthest-more argument's value, and violating the =>
      // (params.arity == args.arity) check at 'visitCallExpr()', throwing a =>
      // RuntimeError with a massage like "Expected 2 arguments but got 1".
    }

    Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments");


    return new Expr.Call(callee, paren, arguments);
  }
//-----------------------------------------------------
  private Expr lambdaFunction() {
    if (match(FUN)) {
      Token keyword = previous();
      FunctionParamsBodyisGetterTRIPLET paramsANDbodyPAIR = finishFunction("lambda-function", keyword);
      return new Expr.LambdaFunction(keyword, paramsANDbodyPAIR.params, paramsANDbodyPAIR.body);
    }
    return primary();
  }
//-----------------------------------------------------
  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

    if (match(THIS)) return new Expr.This(previous());

    if (match(IDENTIFIER)) return new Expr.Variable(previous());


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
      case EQUAL, PLUS_EQUAL, MINUS_EQUAL -> { // assignment().
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

  /// comparing the passed-in token-type, with the current token-type (without advancing).
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }
  /// comparing the passed-in token-type, with the next token-type (without advancing).
  private boolean checkNext(TokenType type) {
    if (isAtEnd() | tokens.get(current+1).type == EOF) return false;
    return tokens.get(current+1).type == type;
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

      // NOTE: renting about the types of valid error-recovery anchors:
      //---------------------------------------------------------------------
      // something that I only noticed much later is the presence of the =>
      // separation of concerns between the Parser's syntactic checks =>
      // and the Resolver's semantic checks, even in this synchronize()- =>
      // method itself, with the types of IDENTIFIERS we choose as valid =>
      // anchor-points to recovery from panic-mode errors and avoidance of =>
      // cascading errors, is the level of abstraction, which is expressed =>
      // here by the size of the concern of the recovery itself.
      // what I'm talking about is the choice to use recovery-anchors such =>
      // as Lox's statement-IDENTIFIERS, which are responsible to a much =>
      // narrower sequence of input than checking an entire scoped-body, =>
      // which could be the case for analysing correct bracket closures in =>
      // the 'Resolver', but rather focusing on recovery on the level of =>
      // each line/expression/statement for itself...
      // which makes sense, and explains why the usage of IDENTIFIERS such =>
      // as the 'RETURN'-Token is a valid anchor, being that it represents =>
      // the start of a statement, and a valid parsing-rule in the Parser, =>
      // even though we would always expect it to be found within a larger =>
      // expression such as a function's body, which I could only guess =>
      // could be used for extra error-recovery-unwinding for larger rules =>
      // that belong to the 'Resolver'...
      //---------------------------------------------------------------------

      switch (peek().type) {
        case CLASS:
        case FUN:
        case RETURN:
        case VAR:
        case WHILE:
        case FOR:
        case BREAK:
        case CONTINUE:
        case IF:
        case PRINT:
        case EXIT:
        case QUIT:
          return;
      }

      advance();
    }
  }
//-----------------------------------------------------
}


