package plv.colorado.edu.returnvaluetest;

import android.util.Log;

/**
 * Created by s on 10/25/16.
 */

public class SomeClass {
    public String returnsString(int i){
        if(i<10)
            return "hi";
        else
            return "bye";
    }
    public int addFive(int i){
        return i+5;
    }
    public void someVoid(int i){
        if(i > 3)
            return;
        Log.i("meh","meh");
    }
    public void takesArray(int[] i){
        Log.i("meh","" + i[0]);
    }

    public Object[] returnsArray() {
        return new Object[]{new Object()};
    }
}
