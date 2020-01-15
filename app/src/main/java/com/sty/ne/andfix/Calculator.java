package com.sty.ne.andfix;

import android.content.Context;
import android.widget.Toast;

public class Calculator {
    public void calculator(Context context) {
        int a = 666;
        int b = 0;
        Toast.makeText(context, "计算a/b = " + a / b, Toast.LENGTH_SHORT).show();
    }
}
