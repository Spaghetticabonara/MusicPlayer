package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.aware.DiscoverySession;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class PlayerActivity extends AppCompatActivity {

    Button btnplay, btnnext, btnprev;
    TextView txtsname, txtstart, txtstop;
    SeekBar seekmusic, volumebar;
    String sname;
    ImageView imageView, btnrep, btnshuffle;

    public static final String EXTRA_NAME = "song_name";
    static MediaPlayer mediaPlayer;
    int position;
    boolean repeat = false;
    boolean shuffle = false;
    ArrayList<File> mysongs;
    AudioManager audioManager;

    Thread updateseekbar;

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //set back to home button
        if (item.getItemId() == android.R.id.home) {onBackPressed();}
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        //back home button on top bar
        getSupportActionBar().setTitle("Now Playing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        btnprev = findViewById(R.id.prevbtn);
        btnnext = findViewById(R.id.nextbtn);
        btnplay = findViewById(R.id.playbtn);
        btnrep = findViewById(R.id.repeatbtn);
        btnshuffle = findViewById(R.id.shufflebtn);

        txtsname = findViewById(R.id.nametxt);
        txtstart = findViewById(R.id.txtstart);
        txtstop = findViewById(R.id.txtend);

        seekmusic = findViewById(R.id.seekbar);
        volumebar = findViewById(R.id.volumnbar);

        imageView = findViewById(R.id.imageview);

        //checking if media player is null or not
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        //set volume bar
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //get max volume
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //get current volume
        int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        volumebar.setMax(maxVolume);
        volumebar.setProgress(curVolume);
        volumebar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //we just want to set the volume so we just put a zero there: youtube said
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        //get songs from phone
        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        mysongs = (ArrayList) bundle.getParcelableArrayList("songs");
//        String songName = i.getStringExtra("songname");
        position = bundle.getInt("pos", 0);

        initPlayer(position);

        //---------play btn-------------------
        btnplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //when play pause button is clicked
                play();
            }
        });

        //---------next btn---------------------
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position < mysongs.size() - 1 && shuffle) {
                    position = getShuffle(mysongs.size() - 1);


                } else if (position < mysongs.size() - 1 && !shuffle){
                    position++;

                }else {
                    position = 0;
                }
                initPlayer(position);
            }
        });

        //---------previous btn-----------------------
        btnprev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position <= 0 && shuffle) {
                    position = getShuffle(mysongs.size() - 1);
//                    position = mysongs.size() - 1;
                } else if (position <= 0 && !shuffle){
                    position = mysongs.size() - 1;

                }else {
                    position--;
                }
                initPlayer(position);
            }
        });

        //------------repeat btn----------------------
        btnrep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                repeat = !repeat;
//                mediaPlayer.setLooping(repeat);

                if (repeat){
                    btnrep.setBackgroundResource(R.drawable.repeatbtn_click);
                }
                else {
                    btnrep.setBackgroundResource(R.drawable.repeatbtn);
                }
                initPlayer(position);
            }
        });

        //------------shuffle btn--------------------
        btnshuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shuffle = !shuffle;
                if (shuffle){
                    btnshuffle.setImageResource(R.drawable.shuffle_click);
                }
                else {
                    btnshuffle.setImageResource(R.drawable.shuffle);
                }
            }
        });
    }


    private void initPlayer(int position) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.reset();
        }

        sname = mysongs.get(position).getName().replace(".mp3", "");
        txtsname.setText(sname);

        Uri uri = Uri.parse(mysongs.get(position).toString());

        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                //set total time
                String totalTime = createTime(mediaPlayer.getDuration());
                txtstop.setText(totalTime);

                //set seekbar maximum duration
                seekmusic.setMax(mediaPlayer.getDuration());

                mediaPlayer.setLooping(repeat);

                //start the music player
                mediaPlayer.start();

                //set 1000 to pause
                btnplay.setBackgroundResource(R.drawable.ic_pause);

                //start animation
                startAnimation(imageView);
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                int curPosition = position;

                if (curPosition < mysongs.size() - 1) {
                    curPosition++;
                    initPlayer(curPosition);
                } else {
                    curPosition = 0;
                    initPlayer(curPosition);
                }
                //do something when the song is finished playing
                //for now we will just change the pause icon to play icon

                btnplay.setBackgroundResource(R.drawable.ic_play);
            }
        });

        //setting up seekbar
        seekmusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // do something when the seekbar is changed

                if (fromUser){
                    mediaPlayer.seekTo(progress); // seek the song
                    seekBar.setProgress(progress); //set the seekbar progress
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //set seekbar color
        seekmusic.getProgressDrawable().setColorFilter(getResources().getColor(R.color.black), PorterDuff.Mode.MULTIPLY);
        seekmusic.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        //setup seekbar to change with songs duration
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mediaPlayer != null) {
                    try {
                        if (mediaPlayer.isPlaying()){
                            Message message = new Message();
                            message.what = mediaPlayer.getCurrentPosition();
                            handler.sendMessage(message);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    //create handler to set progress
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int curPos = msg.what;
            seekmusic.setProgress(curPos);
            String curTime = createTime(curPos);
            txtstart.setText(curTime);
        }
    };


    public void startAnimation (View view){
        ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 360f);
        //set for one sec
        animator.setDuration(1000);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animator);
        animatorSet.start();
    }

    public String createTime (int duration) {
        String time = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        time += min + ":";

        if (sec < 10) time += "0";

        time += sec;
        return  time;
    }

    private void play() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            btnplay.setBackgroundResource(R.drawable.ic_play);
        }
        else {
            mediaPlayer.start();
            btnplay.setBackgroundResource(R.drawable.ic_pause);
        }
    }

    private int getShuffle(int i) {
        Random random = new Random();
        return random.nextInt(i + 1);
    }

}