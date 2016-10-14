package plv.colorado.edu.testapp0;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;

import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TestClass t = new TestClass(this);
        t.thisCallShouldNotBeInstrumented();

    }
}
