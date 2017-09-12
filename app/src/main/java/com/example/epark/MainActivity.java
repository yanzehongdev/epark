package com.example.epark;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button outdoorPark;
    private Button indoorPark;
    private Button nearBottomBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outdoorPark = (Button) findViewById(R.id.btn_outdoor);
        indoorPark = (Button) findViewById(R.id.btn_indoor);
        nearBottomBtn = (Button) findViewById(R.id.bottom_btn_near);
        outdoorPark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapMainActivity.class);
                startActivity(intent);
            }
        });
        indoorPark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapMainActivity.class);
                startActivity(intent);
            }
        });
        nearBottomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapMainActivity.class);
                startActivity(intent);
            }
        });
    }

}
