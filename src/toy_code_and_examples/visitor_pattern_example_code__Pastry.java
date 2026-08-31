package toy_code_and_examples;


/**
 * the double-dispatch technique combines dynamic-dispatch (resolved =>
 * at runtime) - for instance: an OVERRIDDEN object's class-type =>
 * (same name, same parameters, parent/child classes),
 * with a static-dispatch (resolved at compile-time) - for instance: =>
 * calling an OVERLOADED function/method on the (now type-resolved) object.
 */
public class visitor_pattern_example_code__Pastry {
    public static void main(String[] args) {
        //---
        // initialized data object:
        Pastry b = new Beignet(5, 300); // p is the data (class-type).

        // initialized behavior object:
        PastryVisitor eater = new PastryEater(); // 'eater' is the (visitor-function-class type).

        // performing double-dispatch:
        b.accept(eater);
        //---

        // initializing another data object:
        Pastry c = new Cruller(8, 225);

        // activating that same initialized 'behavior' (function-class), using =>
        // 'double-dispatch' lookup again:
        c.accept(eater);
        //---
    }
}

//-----
abstract class Pastry {
    abstract void accept(PastryVisitor visitor); // =>
    // the abstract base 'element'-class forces all inheriting 'element'-classes =>
    // to inherit the 'accept' overridden runtime-resolved method.
}
//---
class Beignet extends Pastry {
    final int sugar_amount;
    final int calories;

    Beignet(int sugar_amount, int calories) {
        this.sugar_amount = sugar_amount;
        this.calories = calories;
    }
    @Override
    void accept(PastryVisitor visitor) {
        visitor.visitBeignet(this); // 'this' refers to the current 'Beignet'-class instance.
    }
}
class Cruller extends Pastry {
    final int crispiness_level;
    final int calories;

    Cruller(int crispiness_level, int calories) {
        this.crispiness_level = crispiness_level;
        this.calories = calories;
    }
    @Override
    void accept(PastryVisitor visitor) {
        visitor.visitCruller(this); // 'this' refers to the current 'Cruller'-class instance.
    }
}
//-----

//-----
interface PastryVisitor { // =>
    // an interface for all 'visitor-function'-classes, that's forcing =>
    // the overloading behavior of resolving exact overloaded functions =>
    // for all possible passed 'element'-classes at compile time (vtable =>
    // 'array of function-pointers'-offset resolution at compile time).
    void visitBeignet(Beignet beignet);
    void visitCruller(Cruller cruller);
}
//---
// visitor-class declaration example:
class PastryEater implements PastryVisitor {
    // runtime-evaluated functions (of type-'PastryEater', matching each =>
    // object inheriting from "Pastry"):
    @Override
    public void visitBeignet(Beignet beignet) {
        System.out.println("Eating a powdered Beignet with " + beignet.sugar_amount + " amounts of sugar!");
        System.out.println("The user has consumed: " + beignet.calories + " calories.\n");
    }

    @Override
    public void visitCruller(Cruller cruller) {
        System.out.println("Eating a twisted Cruller with a " + cruller.crispiness_level + " crispiness level!");
        System.out.println("The user has consumed: " + cruller.calories + " calories.\n");
    }
}
//-----
