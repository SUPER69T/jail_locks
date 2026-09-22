package jail_locks;

import java.util.List;

abstract class Stmt {
//----------------------------------
  interface Visitor<R> {
    R visitBlockStmt(Block stmt);
    R visitExpressionStmt(Expression stmt);
    R visitIfStmt(If stmt);
    R visitPrintStmt(Print stmt);
    R visitExitStmt(Exit stmt);
    R visitVarStmt(Var stmt);
    R visitWhileStmt(While stmt);
    R visitForStmt(For stmt);
    R visitLoopFlowCtrlStmt(LoopFlowCtrl stmt);
  }

  abstract <R> R accept(Visitor<R> visitor);
//----------------------------------
//-------------Block:
  static class Block extends Stmt {
    Block(List<Stmt> statements, Boolean CreateNestedEnv) {
      this.statements = statements;
      this.CreateNestedEnv = CreateNestedEnv;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBlockStmt(this);
    }

    final List<Stmt> statements;
    final Boolean CreateNestedEnv;
  }
//----------------------------------
//-------------Expression:
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
//----------------------------------
//-------------If:
  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIfStmt(this);
    }

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }
//----------------------------------
//-------------Print:
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }
//----------------------------------
//-------------Exit:
  static class Exit extends Stmt {
    Exit() {
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitExitStmt(this);
    }

  }
//----------------------------------
//-------------Var:
  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVarStmt(this);
    }

    final Token name;
    final Expr initializer;
  }
//----------------------------------
//-------------While:
  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitWhileStmt(this);
    }

    final Expr condition;
    final Stmt body;
  }
//----------------------------------
//-------------For:
  static class For extends Stmt {
    For(Stmt initializer, Expr condition, Expr increment, Stmt body) {
      this.initializer = initializer;
      this.condition = condition;
      this.increment = increment;
      this.body = body;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitForStmt(this);
    }

    final Stmt initializer;
    final Expr condition;
    final Expr increment;
    final Stmt body;
  }
//----------------------------------
//-------------LoopFlowCtrl:
  static class LoopFlowCtrl extends Stmt {
    LoopFlowCtrl(Token instruction) {
      this.instruction = instruction;
    }

    @Override
    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLoopFlowCtrlStmt(this);
    }

    final Token instruction;
  }
//----------------------------------
}
