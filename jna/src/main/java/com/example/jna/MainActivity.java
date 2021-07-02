package com.example.jna;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jna.api.JnaTools;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv_jna = findViewById(R.id.tv_jna);
        tv_jna.setText("jna add result = " + JnaTools.INSTANCE.add(1, 2));
        Button mButton =  findViewById(R.id.btn_change_color);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.liner_01).setBackground(getResources().getDrawable(R.drawable.frame_green));
            }
        });

    }
}
