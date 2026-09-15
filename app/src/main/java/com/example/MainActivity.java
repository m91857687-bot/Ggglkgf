package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;
import com.startapp.sdk.adsbase.adlisteners.VideoListener;

public class MainActivity extends Activity {

    private TextView tvQuestion;
    private TextView tvCoins;
    private Button btnNext;
    private Button btnWatchAd;

    private int currentPuzzleIndex = 0;
    private int coins = 0;
    private StartAppAd rewardedVideo;

    private String[] puzzles = {
            "ما هو الشيء الذي يكتب ولا يقرأ؟\n\n(القلم)",
            "ما هو الشيء الذي له أوراق وليس شجرة؟\n\n(الكتاب)",
            "يمشي بلا رجلين ويبكي بلا عينين.. فما هو؟\n\n(السحاب)",
            "ما هو الشيء الذي كلما أخذت منه كبر؟\n\n(الحفرة)",
            "له أسنان ولا يعض، ما هو؟\n\n(المشط)"
    };

    private Handler handler = new Handler();
    private Runnable adRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialize StartApp SDK
        StartAppSDK.init(this, "208214327", true);
        
        // 2. Show a splash ad on app start
        StartAppAd.showAd(this);

        setContentView(R.layout.activity_main);

        tvQuestion = findViewById(R.id.tv_question);
        tvCoins = findViewById(R.id.tv_coins);
        btnNext = findViewById(R.id.btn_next);
        btnWatchAd = findViewById(R.id.btn_watch_ad);

        updatePuzzle();
        updateCoins();

        // إعداد إعلان المكافأة
        rewardedVideo = new StartAppAd(this);
        loadRewardedVideo();

        // مشاهدة إعلان المكافأة
        btnWatchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rewardedVideo.isReady()) {
                    rewardedVideo.setVideoListener(new VideoListener() {
                        @Override
                        public void onVideoCompleted() {
                            // إضافة عملة للمستخدم عند إكمال الفيديو
                            coins++;
                            updateCoins();
                            Toast.makeText(MainActivity.this, "لقد ربحت عملة جديدة!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    rewardedVideo.showAd();
                    
                    // تحميل إعلان جديد للمرة القادمة
                    loadRewardedVideo();
                } else {
                    Toast.makeText(MainActivity.this, "الإعلان غير جاهز بعد، حاول مرة أخرى", Toast.LENGTH_SHORT).show();
                    loadRewardedVideo();
                }
            }
        });

        // Show ad on next button click (moving to next level)
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartAppAd.showAd(MainActivity.this);

                currentPuzzleIndex++;
                if (currentPuzzleIndex >= puzzles.length) {
                    currentPuzzleIndex = 0; 
                }
                updatePuzzle();
            }
        });

        // 3. Show an interstitial ad automatically every 60 seconds
        adRunnable = new Runnable() {
            @Override
            public void run() {
                StartAppAd.showAd(MainActivity.this);
                handler.postDelayed(this, 60000); 
            }
        };
        handler.postDelayed(adRunnable, 60000);
    }

    private void loadRewardedVideo() {
        rewardedVideo.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                // الإعلان جاهز
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {
                // فشل تحميل الإعلان
            }
        });
    }

    private void updatePuzzle() {
        tvQuestion.setText("لغز " + (currentPuzzleIndex + 1) + ":\n\n" + puzzles[currentPuzzleIndex]);
    }

    private void updateCoins() {
        tvCoins.setText("العملات: " + coins);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && adRunnable != null) {
            handler.removeCallbacks(adRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        // 4. Handle back press to show ad before exiting
        StartAppAd.onBackPressed(this);
        super.onBackPressed();
    }
}
