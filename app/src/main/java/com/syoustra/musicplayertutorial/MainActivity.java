package com.syoustra.musicplayertutorial;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/** MEDIA PLAYER:
 *
 * Find Media folder
 * Play, pause, forward, back
 * Display related info (Title, album cover, etc.)
 *
 * **/

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
