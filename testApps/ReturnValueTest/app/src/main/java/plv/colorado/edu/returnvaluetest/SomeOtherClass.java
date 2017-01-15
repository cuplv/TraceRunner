package plv.colorado.edu.returnvaluetest;

/**
 * Created by s on 1/14/17.
 */

public class SomeOtherClass {
    static SomeClass sc = foo();
    static SomeClass foo() {
        return new SomeClass();
    }
}
