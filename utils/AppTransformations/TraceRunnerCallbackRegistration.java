package de.danoeh.antennapod;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.widget.Button;

/**
 * Created by s on 7/2/16.
 */
public class TraceRunnerCallbackRegistration {
    @SuppressLint("NewApi")
    public static View registerForView(View view){
        if(view instanceof Button){
            Button button = (Button)view;
            Listener listener = new Listener();
            button.addOnAttachStateChangeListener(listener);

        }
        return view;

    }

    @SuppressLint("NewApi")
    public static class Listener implements View.OnAttachStateChangeListener {

        @Override
        public void onViewAttachedToWindow(View v) {

        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }
    }
}
