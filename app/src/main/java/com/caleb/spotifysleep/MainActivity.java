package com.caleb.spotifysleep;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;


public class MainActivity extends ActionBarActivity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "6ac9d5f8c22648e0905ccd1a70e02d37";
    private static final String REDIRECT_URI = "spotify-sleep-protocol://callback";

    private static final int REQUEST_CODE = 17;

    public static final int DEFAULT_TIME = 20;

    private TextView mSongName;
    private TextView mTimeText;
    private Button mPlayButton;
    private Button mPrevButton;
    private Button mNextButton;
    private Button mSubtractionButton;
    private Button mAdditionButton;


    private Player mPlayer;

    private CountDownTimer mTimer;

    private int mStartingVolume;

    private boolean isPaused = false;
    private boolean hasStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSongName = (TextView) findViewById(R.id.songNameText);
        mTimeText = (TextView) findViewById(R.id.timeText);
        mTimeText.setText(Integer.toString(getDefaultTimeFromPreferences()));

        mPlayButton = (Button) findViewById(R.id.playButton);
        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mSubtractionButton.setEnabled(false);
                mAdditionButton.setEnabled(false);

                if(!isPaused) {
                    mPlayer.pause();
                    isPaused = true;
                    mPlayButton.setText("Play");
                } else if(isPaused && hasStarted){
                    mPlayer.resume();
                    isPaused = false;
                    mPlayButton.setText("Pause");
                }

                if(!hasStarted) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String playlist = sp.getString(PreferencesActivity.PLAYLIST_KEY, "");
                    mPlayer.play(playlist);
                    mPlayer.setShuffle(true);
                    mPlayer.skipToNext();
                    hasStarted = true;
                    isPaused = false;
                    mPlayButton.setText("Pause");
                }


                int time;
                try {
                    time = Integer.parseInt(mTimeText.getText().toString());
                } catch(Exception e) {
                    Toast.makeText(MainActivity.this, "Try again", Toast.LENGTH_SHORT).show();
                    return;
                }


                //Fades out during the last 60 seconds, but turns the volume back to original position after the last song
                final int timeInMillis = time * 60000;
                if(mTimer == null) {
                    mTimer = new CountDownTimer(timeInMillis, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            int time = (int) Math.ceil(millisUntilFinished / 60000.0);
                            mTimeText.setText("" + time);
                            if (millisUntilFinished > 65000 && millisUntilFinished < 120000) {
                                mStartingVolume = getAudioManager().getStreamVolume(AudioManager.STREAM_MUSIC);
                            }
                            if (millisUntilFinished < 60000) {
                                if(mStartingVolume == 0)
                                    return;
                                double percent = millisUntilFinished / 60000.0;
                                if (mStartingVolume * percent < getAudioManager().getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                                    getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, (int) (mStartingVolume * percent), AudioManager.FLAG_SHOW_UI);
                            }

                        }

                        @Override
                        public void onFinish() {
                            MainActivity.this.finish();
                        }
                    }.start();
                }


            }
        });


        //subtracts 5 minutes from the starting timer
        mSubtractionButton = (Button) findViewById(R.id.subtractTimeButton);
        mSubtractionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int time = Integer.parseInt(mTimeText.getText().toString());
                    time -= 5;
                    if(time <= 0) {
                        time = 0;
                        mSubtractionButton.setEnabled(false);
                        mPlayButton.setEnabled(false);
                    }
                    mTimeText.setText(Integer.toString(time));
                } catch(Exception e) {}

            }
        });

        //adds 5 minutes to the starting timer
        mAdditionButton = (Button) findViewById(R.id.additionTimeButton);
        mAdditionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int time = Integer.parseInt(mTimeText.getText().toString());
                    time += 5;
                    mTimeText.setText(Integer.toString(time));
                    mSubtractionButton.setEnabled(true);
                    mPlayButton.setEnabled(true);
                } catch(Exception e){}
            }
        });

        //starts playing the previous song
        mPrevButton = (Button) findViewById(R.id.prevSongButton);
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPlayer != null) {
                    mPlayer.skipToPrevious();
                    mPlayButton.setText("Pause");
                }
            }
        });

        //skips to the next song
        mNextButton = (Button) findViewById(R.id.nextSongButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mPlayer != null) {
                    mPlayer.skipToNext();
                    mPlayButton.setText("Pause");
                }
            }
        });



        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"playlist-read-private", "playlist-read-collaborative", "streaming"});

        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if(response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addPlayerNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d("Fuck", throwable.getMessage());
                    }
                });

            }
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        mTimer.cancel();
        getAudioManager().setStreamVolume(AudioManager.STREAM_MUSIC, mStartingVolume, AudioManager.FLAG_SHOW_UI);
        super.onDestroy();
    }

    private AudioManager getAudioManager() {
        return (AudioManager) getApplication().getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_settings:
                    openOptions();
                    return true;

            case R.id.reset:
                    reset();
                    return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void openOptions() {
        Intent i = new Intent(getApplicationContext(), PreferencesActivity.class);
        startActivity(i);
    }

    //Retrieves default time from the settings
    public int getDefaultTimeFromPreferences() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String timeString = sp.getString(PreferencesActivity.DEFAULT_TIME_KEY, "");
        int time;
        try {
            time = Integer.parseInt(timeString);
        } catch(Exception e) {
            return DEFAULT_TIME;
        }
        return time;

    }

    //pauses the player and resets the timer
    public void reset() {
        mPlayer.skipToNext();
        mPlayer.pause();
        hasStarted = false;
        isPaused = false;

        mPlayButton.setText("Play");

        mAdditionButton.setEnabled(true);
        mSubtractionButton.setEnabled(true);

        mTimeText.setText(Integer.toString(getDefaultTimeFromPreferences()));

        if(mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mTimer == null) {
            mTimeText.setText(Integer.toString(getDefaultTimeFromPreferences()));
        }
    }
}