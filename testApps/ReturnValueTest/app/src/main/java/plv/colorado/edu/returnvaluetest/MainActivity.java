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
//    private SomeClass object = new SomeClass();
//    private SomeClass nullval = null;

    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        throw new RuntimeException("meh");
//        Log.i("", "" + foo());
//        Callable<String> c = new Callable<String>() {
//            @Override
//            public String call() throws Exception {
//                return null;
//            }
//        };

//        try {
//            throw new Exception();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        setContentView(R.layout.activity_main);
//        getApplication(); //test return value from non used return
//        TextView tv = (TextView) findViewById(R.id.h);
//        tv.setText("goodbye world!");
//        tv.setOnClickListener(this);
//        SomeClass s = new SomeClass();
//        s.returnsString(3);
//        s.returnsString(10);
//        s.addFive(4);
//        s.someVoid(8);
//        s.takesArray(new int[]{1,2,3});
//        s.returnsArray();
//        tv.setOnClickListener(null);
//        Log.i("",this.getObject(3).toString());
//        FragmentManager fragmentManager = getFragmentManager();
    }

//    protected SomeClass getObject(int i){
//        SomeClass o = null;
//        switch (i) {
//            case 3 :
//                o = new SomeClass();
//                break;
//            case 4 :
////                o = this.object;
//                break;
//
//        }
//        return o;
//    }
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        return super.onCreateOptionsMenu(menu);
//    }
//    @Override
//    protected void onPause(){
//        super.onPause();
//    }
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//    }
//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//    }
//
//    @Override
//    public void onClick(View v) {
//        Log.i("====clicked","Clicked!!!");
//
//    }
//    public boolean foo(/*SomeClass s*/){
//        Log.i("",s.toString());
        //return s.hashCode() > 0;
//        return true;
//    }
//    private View.OnClickListener onClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//
//        }
//    };
}
