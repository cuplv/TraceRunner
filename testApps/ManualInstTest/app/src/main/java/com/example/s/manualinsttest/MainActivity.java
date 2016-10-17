package com.example.s.manualinsttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraceRunnerRuntimeInstrumentation.logCallin("hi!","there",new Object[]{"hi"},"lksdjf");
        setContentView(R.layout.activity_main);
    }
}
