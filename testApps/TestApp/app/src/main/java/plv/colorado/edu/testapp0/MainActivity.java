package plv.colorado.edu.testapp0;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.fakeLibrary.FakeLibraryClass;

import java.io.IOException;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TestClass t = new TestClass(this);
        t.thisCallShouldNotBeInstrumented();

        final Button button = (Button)findViewById(R.id.button);
        FakeLibraryClass.thisMethodTakesAFloat(2.3f);
        FakeLibraryClass.thisMethodTakesALong(2);
        FakeLibraryClass.thisMethodTakesADouble(32);
        getApplication();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                button.setEnabled(false);
            }
        });

        //TODO: add more test cases here

    }
}
