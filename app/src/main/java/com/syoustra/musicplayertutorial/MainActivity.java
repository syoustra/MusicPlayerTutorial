package com.syoustra.musicplayertutorial;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import java.util.ArrayList;

/** MEDIA PLAYER:
 *
 * Find Media folder
 * Play, pause, forward, back
 * Display related info (Title, album cover, etc.)
 *
 * **/

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Adapter adapter;



    //TODO 13. Add global variables to bind mediaPlayer and Main Activity
    private MediaPlayerService player;
    boolean serviceBound = false;
    //TODO 19. Create global ArrayList of Audio objects
    ArrayList<Audio> audioList;

    //TODO 31. Create global static String, changing to use your package name; sends broadcast intents that user has updated cache of music to play
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.syoustra.musicplayertutorial.PlayNewAudio";
    //Change to your package name


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.main_recycler_view);
        setupList();


        //TODO 23. Adjust playAudio() to pull audio from arrayList ... MAKE SURE THERE'S MEDIA ON DEVICE
//        playAudio("https://upload.wikimedia.org/wikipedia/commons/6/6c/Grieg_Lyric_Pieces_Kobold.ogg");
        loadAudio();
        //play the first audio in the arrayList
        playAudio(audioList.get(0).getData());

    }

    private void setupList() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        adapter = new Adapter(audioList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    //TODO 16. Lifecycle methods save, restore, destroy to prevent service from crashing the app
    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
            savedInstanceState.putBoolean("Service state", serviceBound);
            super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

    //TODO 14. Bind Client to AudioPlayer Service
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    //TODO 15. Create new instance of MediaPlayerService and send media file to play
    //TODO 32. Replace playAudio() to draw from StorageUtil list of media
//    private void playAudio(String media) {
    private void playAudio(int audioIndex) {
        //Check if service is active
        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);


            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//            playerIntent.putExtra("media", media);
//            startService(playerIntent);
//            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service --> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    //TODO 20. Use contentResolver to create loadAudio method
    private void loadAudio() {
        //TODO 21. Create contentResolver cursor
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        //TODO 22. Cycle through data to add the media to the arrayList
        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                //Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        cursor.close();
    }

}
