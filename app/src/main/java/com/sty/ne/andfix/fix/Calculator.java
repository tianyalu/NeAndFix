package com.sty.ne.andfix.fix;

import android.content.Context;
import android.widget.Toast;

import com.sty.ne.andfix.MethodReplace;

public class Calculator {

    @MethodReplace(className = "com.sty.ne.andfix.Calculator", methodName = "calculator")
    public void calculator(Context context) {
        int a = 666;
        int b = 1;
        Toast.makeText(context, "计算a/b = " + a / b, Toast.LENGTH_SHORT).show();
    }

    public void aaa() {

    }

    public void bbb() {

    }
}
