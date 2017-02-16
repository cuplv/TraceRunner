package plv.colorado.edu.returnvaluetest;

import android.animation.ObjectAnimator;
import android.app.FragmentManager;
import android.content.UriMatcher;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

//public class MainActivity extends AppCompatActivity implements View.OnClickListener{
public class MainActivity extends SomeClass{
    static UriMatcher meh = getit();
    static UriMatcher getit(){
        return new UriMatcher(3);
    }
//    MainActivity(){
//        Log.i("","");
//    }


    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
//        ThreadPool p = Executors.newFixedThreadPool(1);
        SomeClass s = new SomeClass();
	
    }



}
