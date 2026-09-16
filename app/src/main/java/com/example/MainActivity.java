package com.example;

import android.app.Activity;
import android.os.Bundle;
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

    private TextView tvCoins;
    private Button btnWatchAd;
    private Button btnInterstitial;

    private int coins = 0;
    private StartAppAd rewardedVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Initialize StartApp SDK
        StartAppSDK.setTestAdsEnabled(true);
        StartAppSDK.init(this, "208214327", true);
        
        // Disable splash ad if user just wants simple UI
        StartAppAd.disableSplash();

        setContentView(R.layout.activity_main);

        tvCoins = findViewById(R.id.tv_coins);
        btnWatchAd = findViewById(R.id.btn_watch_ad);
        btnInterstitial = findViewById(R.id.btn_interstitial);

        updateCoins();

        // Setup Rewarded Video
        rewardedVideo = new StartAppAd(this);
        loadRewardedVideo();

        btnWatchAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rewardedVideo.isReady()) {
                    rewardedVideo.setVideoListener(new VideoListener() {
                        @Override
                        public void onVideoCompleted() {
                            coins++;
                            updateCoins();
                            Toast.makeText(MainActivity.this, "تمت إضافة عملة!", Toast.LENGTH_SHORT).show();
                        }
                    });
                    rewardedVideo.showAd();
                    loadRewardedVideo();
                } else {
                    Toast.makeText(MainActivity.this, "يتم تحميل الإعلان، حاول بعد ثوانٍ...", Toast.LENGTH_SHORT).show();
                    loadRewardedVideo();
                }
            }
        });

        btnInterstitial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartAppAd.showAd(MainActivity.this);
            }
        });
    }

    private void loadRewardedVideo() {
        rewardedVideo.loadAd(StartAppAd.AdMode.REWARDED_VIDEO, new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {}
            @Override
            public void onFailedToReceiveAd(Ad ad) {
                // If it fails to load (e.g. NO FILL), log it or handle it.
                // It will retry when the user clicks the button again.
            }
        });
    }

    private void updateCoins() {
        tvCoins.setText("العملات: " + coins);
    }

    @Override
    public void onBackPressed() {
        StartAppAd.onBackPressed(this);
        super.onBackPressed();
    }
}
