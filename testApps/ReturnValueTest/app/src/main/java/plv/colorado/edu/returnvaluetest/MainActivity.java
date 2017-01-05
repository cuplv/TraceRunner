package plv.colorado.edu.returnvaluetest;

import android.animation.ObjectAnimator;
import android.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.Callable;

//public class MainActivity extends AppCompatActivity implements View.OnClickListener{
public class MainActivity extends AppCompatActivity{
    MainActivity(){
        Log.i("init", "this is the <init> method");
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Integer i = new Integer(42);
        foo(i);
	
    }
    static void foo(int i){
        
    }


}
