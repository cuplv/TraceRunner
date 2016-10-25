package plv.colorado.edu.returnvaluetest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplication(); //test return value from non used return
        TextView tv = (TextView)findViewById(R.id.h);
        tv.setText("goodbye world!");
        SomeClass s = new SomeClass();
        s.returnsString(3);
        s.returnsString(10);
        s.addFive(4);
        s.someVoid(8);
    }
}
