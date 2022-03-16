package com.malio.gmsedemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import com.malio.basese.MalioSeImpl;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv_se);
        /*MalioSeImpl.openSe();
        tv.setText("version = " + MalioSeImpl.getSeVersion());
        MalioSeImpl.closeSe();*/
        tv.setText("version = " + MalioSeImpl.getText());

    }
}