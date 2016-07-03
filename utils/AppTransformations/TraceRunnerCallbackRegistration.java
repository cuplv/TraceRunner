package app.dinus.com.example;

import android.annotation.SuppressLint;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
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
            Object listenerInfo = readField(view, "mListenerInfo", true);
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

    private static Object readField(Object obj, String fieldName, boolean forceAccess) throws IllegalAccessException {
        Class objClass = obj.getClass();

        try {
            Field field = getFieldRec(objClass, fieldName);
            if (field == null) return null;

            if (forceAccess && !field.isAccessible()) {
                field.setAccessible(true);
            } else {
                setAccessibleWorkaround(field);
            }

            Object b = field.get(obj);
            return b;

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Field getFieldRec(Class clazz, String fieldName)
            throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            Class superClass = clazz.getSuperclass();
            if (superClass == null) {
                throw e;
            } else {
                return getFieldRec(superClass, fieldName);
            }
        }
    }


    static void setAccessibleWorkaround(AccessibleObject o) {
        if (o == null || o.isAccessible()) {
            return;
        }
        Member m = (Member) o;
        if (Modifier.isPublic(m.getModifiers())
                && isPackageAccess(m.getDeclaringClass().getModifiers())) {
            try {
                o.setAccessible(true);
            } catch (SecurityException e) {
                // ignore in favor of subsequent IllegalAccessException
            }
        }
    }

    private static final int ACCESS_TEST = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE;
    static boolean isPackageAccess(int modifiers) {
        return (modifiers & ACCESS_TEST) == 0;
    }

    private static boolean isFieldNull(Object obj, String fieldName) throws IllegalAccessException {
        assert null != obj;

        Object fieldObj = readField(obj, fieldName, true);
        return null == fieldObj;
    }

    private static boolean isCollectionFieldEmpty(Object obj, String fieldName) throws IllegalAccessException {
        assert null != obj;

        Object field = readField(obj, fieldName, true);
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
