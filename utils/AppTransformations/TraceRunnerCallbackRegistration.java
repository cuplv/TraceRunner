package app.dinus.com.example;


// WARNING: the class uses commons-lang
// Need to import this in gradle: compile group: 'org.apache.commons', name: 'commons-lang3', version: '3.4'

import android.annotation.SuppressLint;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by s on 7/2/16.
 */
public class TraceRunnerCallbackRegistration {
    @SuppressLint("NewApi")
    public static View registerForView(View view){
        Listener listener = null;

        if (null == view) return view;

        // WARNING: calling the hasOnClickListener ensures that the mListenerInfo of view has been
        // created
        if (! view.hasOnClickListeners()) {
            listener = getListener(listener);
            view.setOnClickListener(listener);
        }

        if (null == view.getOnFocusChangeListener()) {
            view.setOnFocusChangeListener(new Listener());
        }

        try {
            // Access the mListenerInfo object of view
            // Need to break visibility restrictions (boolean flag set to true)
            Object listenerInfo = FieldUtils.readField(view, "mListenerInfo", true);
            if (null == listenerInfo) {
                System.err.println("mListenerInfo is null!");
                return view;
            }

            if (isCollectionFieldEmpty(listenerInfo, "mOnLayoutChangeListeners")) {
                listener = getListener(listener);
                view.addOnLayoutChangeListener(listener);
            }

            if (isCollectionFieldEmpty(listenerInfo, "mOnAttachStateChangeListeners")) {
                listener = getListener(listener);
                view.addOnAttachStateChangeListener(listener);
            }

            if (isFieldNull(listenerInfo, "mOnFocusChangeListener")) {
                listener = getListener(listener);
                view.setOnFocusChangeListener(listener);
            }
//            if (! isFieldNull(listenerInfo, "mOnScrollChangeListener")) {
//                listener = getListener(listener);
//                view.oChangeListener(listener);
//            }
            if (isFieldNull(listenerInfo, "mOnClickListener")) {
                listener = getListener(listener);
                view.setOnClickListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnLongClickListener")) {
                listener = getListener(listener);
                view.setOnLongClickListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnCreateContextMenuListener")) {
                listener = getListener(listener);
                view.setOnCreateContextMenuListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnKeyListener")) {
                listener = getListener(listener);
                view.setOnKeyListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnTouchListener")) {
                listener = getListener(listener);
                view.setOnTouchListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnHoverListener")) {
                listener = getListener(listener);
                view.setOnHoverListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnGenericMotionListener")) {
                listener = getListener(listener);
                view.setOnGenericMotionListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnDragListener")) {
                listener = getListener(listener);
                view.setOnDragListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnSystemUiVisibilityChangeListener")) {
                listener = getListener(listener);
                view.setOnSystemUiVisibilityChangeListener(listener);
            }
            if (isFieldNull(listenerInfo, "mOnApplyWindowInsetsListener")) {
                listener = getListener(listener);
                view.setOnApplyWindowInsetsListener(listener);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return view;
    }

    private static boolean fieldExist(Object obj, String fieldName) throws IllegalAccessException {
        Field field = FieldUtils.getField(obj.getClass(), fieldName, true);
        return null == field;
    }

    private static boolean isFieldNull(Object obj, String fieldName) throws IllegalAccessException {
        assert null != obj;

        Object fieldObj = FieldUtils.readField(obj, fieldName, true);
        return null == fieldObj;
    }

    private static boolean isCollectionFieldEmpty(Object obj, String fieldName) throws IllegalAccessException {
        assert null != obj;

        Object field = FieldUtils.readField(obj, fieldName, true);
        if (field != null) {
            Collection<Object> arrayObj = (Collection<Object>) field;
            return arrayObj.size() == 0;
        }
        else {
            return true;
        }
    }


    private static Listener getListener(Listener listener) {
        return listener == null ? new Listener() : listener;
    }

    @SuppressLint("NewApi")
    public static class Listener implements View.OnAttachStateChangeListener,
            View.OnFocusChangeListener,
            View.OnClickListener,
            View.OnLongClickListener,
            View.OnCreateContextMenuListener,
            View.OnKeyListener,
            View.OnTouchListener,
            View.OnHoverListener,
            View.OnGenericMotionListener,
            View.OnDragListener,
            View.OnSystemUiVisibilityChangeListener,
            View.OnApplyWindowInsetsListener,
            View.OnLayoutChangeListener
    {

        @Override
        public void onViewAttachedToWindow(View v) {

        }

        @Override
        public void onViewDetachedFromWindow(View v) {

        }

        @Override
        public void onFocusChange(View v, boolean hasFocus) {

        }

        @Override
        public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
            return null;
        }

        @Override
        public void onClick(View v) {

        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            return false;
        }

        @Override
        public boolean onGenericMotion(View v, MotionEvent event) {
            return false;
        }

        @Override
        public boolean onHover(View v, MotionEvent event) {
            return false;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            return false;
        }


        @Override
        public void onSystemUiVisibilityChange(int visibility) {

        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return false;
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {

        }
    }
}
