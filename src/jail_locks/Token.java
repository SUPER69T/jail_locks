package jail_locks;

class Token {
  final TokenType type; // TokenType Enum.
  final String lexeme; // terminal symbol, like "+" for the 'PLUS' Token.
  final Object literal; // only used by literal Token
  final int line; // the line in which the Token is found in the source lox code.

  Token(TokenType type, String lexeme, Object literal, int line) {
    this.type = type;
    this.lexeme = lexeme;
    this.literal = literal;
    this.line = line;
  }

  public String toString() {
    return type + " " + lexeme + " " + literal;
  }
}