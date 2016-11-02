package com.example.s.manualinsttest;

import android.os.BaseBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.colorado.plv.tracerunner_runtime_instrumentation.TraceRunnerRuntimeInstrumentation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        class foo{}

        Class<?> declaringClassobj = Object.class.getSuperclass();

        Class<?> declaringClass = String.class.getSuperclass();


        Set<Method> overrideHierarchy = MethodUtils.getOverrideHierarchy(foo.class.getEnclosingMethod(), ClassUtils.Interfaces.INCLUDE);

        Method[] declaredMethods = this.getClass().getSuperclass().getDeclaredMethods();
        List<Method> onCreateDeclared = new ArrayList<>();
        for(Method declaredMethod : declaredMethods){
            if(declaredMethod.getName().equals("onCreate"))
                onCreateDeclared.add(declaredMethod);
        }
        Method[] methods = this.getClass().getSuperclass().getMethods();
        List<Method> onCreateGetMethods = new ArrayList<>();
        for(Method method : methods){
            if(method.getName().equals("onCreate"))
                onCreateGetMethods.add(method);
        }

//        Set<Method> stringOverride = MethodUtils.getOverrideHierarchy(declaredMethods[0], ClassUtils.Interfaces.INCLUDE);


        TraceRunnerRuntimeInstrumentation.logCallbackEntry("com.example.s.manualinsttest","onCreate", new String[]{"android.os.Bundle"},"void" ,new Object[]{this,savedInstanceState});
        setContentView(R.layout.activity_main);

    }
}
