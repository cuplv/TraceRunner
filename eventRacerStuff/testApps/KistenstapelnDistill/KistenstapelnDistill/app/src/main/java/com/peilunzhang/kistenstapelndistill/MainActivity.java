package com.peilunzhang.kistenstapelndistill;


// import android.support.v7.app.AppCompatActivity;

import android.support.v7.app.ActionBarActivity;

import android.os.Bundle;
import android.app.FragmentManager;
import android.view.View;
import android.widget.Button;

public class MainActivity extends ActionBarActivity {


    FragmentManager fragmentManager = getFragmentManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button firstFragmentBtn = (Button) findViewById(R.id.fragmentBtn1);
        Button secondFragmentBtn = (Button) findViewById(R.id.fragmentBtn2);

        firstFragmentBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view){
                fragmentManager.beginTransaction().
                        replace(R.id.fragment_container, new CountdownFragment()).commit();

            };
        });
        secondFragmentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                fragmentManager.beginTransaction().
                        replace(R.id.fragment_container, new EmptyFragment()).commit();

            };
        });

    }
}
