package plv.colorado.edu.testapp0;

import android.content.Context;

/**
 * Created by s on 10/12/16.
 */

public class TestClass {
    private final Context context;

    TestClass(Context context){
        this.context = context;
    }
    public int thisCallShouldNotBeInstrumented(){
        context.getFilesDir(); //This should be instrumented
        return 0;
    }
}
