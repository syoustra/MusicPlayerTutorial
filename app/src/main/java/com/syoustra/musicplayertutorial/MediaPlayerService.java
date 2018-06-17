package com.syoustra.musicplayertutorial;

/** TUTORIAL FROM https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-app/ **/

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;

import java.io.IOException;

//TODO 2. Create the Media Player Service and related methods/binder inner class
public class MediaPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    //Binder given to clients
    private final IBinder iBinder = new LocalBinder();

    //TODO 4. Create global instances of mediaPlayer and mediaFile
    private MediaPlayer mediaPlayer;
    private String mediaFile;

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of a media resource being streamed over the internet
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed
    }


    //Handle errors

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated
    }

    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }

    }

    //TODO 5. Initialize mediaPlayer
    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the media player is not pointing to another data source
        mediaPlayer.reset();

        //TODO 9999. FIX THIS, BECAUSE setAudioStreamType IS NEWLY DEPRECATED
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            //Set the data source to the mediaFile location
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }
}
