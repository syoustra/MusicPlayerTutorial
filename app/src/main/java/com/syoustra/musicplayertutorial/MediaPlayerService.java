package com.syoustra.musicplayertutorial;

/**
 * TUTORIAL FROM https://www.sitepoint.com/a-step-by-step-guide-to-building-an-android-audio-player-app/
 **/

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

//TODO 2. Create the Media Player Service and basic shell of related methods/binder inner class
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

    //TODO 6. Add global variable to store pause/resume position
    private int resumePosition;

    //TODO 10. Add global instance of AudioManager
    private AudioManager audioManager;

    //TODO 27. Add global variables to listen for phone calls
    //Handle incoming phone calls
    private boolean onGoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    //TODO 33. Add global variables for the list of songs
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently-playing audio




    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    //TODO 35. Add registration calls for the BroadcastReceivers

    @Override
    public void onCreate() {
        super.onCreate();
        //Perform one-time setup procedures

        //Manage incoming phone calls during playback
        //Pause MediaPlayer on incoming call
        //Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();

    }

    //TODO 12. Set up the Service lifecycle methods
    //The system calls this method when an activity requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //An audio file is passed to the service through putExtra();
            mediaFile = intent.getExtras().getString("media");
        } catch (NullPointerException e){
            stopSelf();
        }

        if (requestAudioFocus() == false) {
            //Could not gain focus)
            stopSelf();
        }

        if (mediaFile != null && mediaFile != "") initMediaPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    //TODO 36. Update onDestroy to unregister BroadcastReceivers
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(PhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        //TODO 37. (removeNotification not created yet; ignore errors)
        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();

    }

    //TODO 9. Fill in the @Override methods with the actual code
    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of a media resource being streamed over the internet
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed
        stopMedia();
        //Stop the service
        stopSelf();
    }

    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("Media Player Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("Media Player Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("Media Player Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
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
        playMusic();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation
    }

    //TODO 11. Fill in code for onAudioFocusChange
    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                //resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //Lost focus for an unbounded amount of time; stop playback and release player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                //Lost focus for a short time, but we have to stop playback. We don't release the media player because playback is likely to resume.
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //Lost focus for a short time, but it's ok to keep playing at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    private boolean removeAudioFocus() { //TODO 9999 FIX THIS, AS abandonAudioFocusRequest requires API 26 and up
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocusRequest(this);
    }




    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }

    }

    //TODO 5. Initialize mediaPlayer
    private void initMediaPlayer() {
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

    //TODO 8. Add if statements to make sure there are no problems while playing media
    private void playMusic() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    //TODO 24 Create BroadcastReceiver to listen for headphones being removed
    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            //TODO 25 (buildNotification() not built yet, so don't worry about errors)
            buildNotification(PlaybackStatus.Paused);
        }
    };

    //TODO 26 Register headphones-removed BroadcastReceiver
    private void registerBecomingNoisyReceiver() {
        //register after receiving audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }


    //TODO 28. Address telephone call situations
    //Handle incoming calls
    private void callStateListener() {
        //Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Start listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            onGoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        //phone idle, start playing
                        if (mediaPlayer != null) {
                            if (onGoingCall) {
                                onGoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }

            }
        };

        //Register the listener with the telephony manager
        //Listen for changes to the device call state
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

    }

    //TODO 34. Create a second BroadcastReceiver to listen for user's request for a new song
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Get the new media index from SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopMedia();
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }

    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);

        registerReceiver(playNewAudio, filter);
    }



}
