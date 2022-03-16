package com.example.jna;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jna.api.JnaTools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout mLinearLayout;
    TextView tv_jna;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_jna = findViewById(R.id.tv_jna);
        tv_jna.setText("jna add result = " + JnaTools.INSTANCE.add(1, 2));
        Button mButton =  findViewById(R.id.btn_change_color);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.liner_01).setBackground(getResources().getDrawable(R.drawable.frame_green));
                ConnectivityManager mCM = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
            }
        });
    }

}
