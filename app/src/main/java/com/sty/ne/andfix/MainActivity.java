package com.sty.ne.andfix;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private Button btnCalculate;
    private Button btnFix;
    private Calculator calculator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCalculate = findViewById(R.id.btn_calculate);
        btnFix = findViewById(R.id.btn_fix);

        calculator = new Calculator();

        btnCalculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calculator.calculator(MainActivity.this);
            }
        });
        btnFix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DexManager.getInstance().setContext(MainActivity.this);
                DexManager.getInstance().loadDex(new File(Environment.getExternalStorageDirectory(),
                        "/sty/hotfix.dex"));
            }
        });
    }


}
