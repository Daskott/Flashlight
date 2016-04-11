package com.daskott.flashlight;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ScreenLightActivity extends AppCompatActivity {

    //globals
    public static final String PREFS_NAME = "daskott_flashLight_settings";
    private SharedPreferences settings;
    private MediaPlayer mp;
    private boolean turnOnFlashOnStart;
    private boolean isScreenLightMode;
    private boolean isSilent;
    private boolean isShakeEnabled;
    private boolean hasFlash;
    private ImageButton powerButton;
    private ImageView flashModeButton;
    private Toolbar mToolbar;
    private Vibrator vibrateEffect;
    private  AudioManager audioManager;
    private Intent intent;


    // The following are used for the shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_light);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        init();
        setActionListeners();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_screen_light, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //Initialization process
    private void init()
    {

        //init ui elements
        powerButton = (ImageButton)findViewById(R.id.toggle_flash_button);
        flashModeButton = (ImageView)findViewById(R.id.toggle_flashmode);

        // ShakeDetector initialization
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //init Animation effects
        vibrateEffect = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //check if the device has a camera
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        //if no flash, make flash mode button invisible
        if(!hasFlash)
            flashModeButton.setVisibility(View.GONE);


        //control volume of click
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0);


        //Restore preferences from settings file and set the UI elements based on those settings.
        settings = getSharedPreferences(PREFS_NAME, 0);
        isSilent = settings.getBoolean("silentMode", false);
        isShakeEnabled = settings.getBoolean("shakeEnabled", false);

        intent = new Intent(this, MainActivity.class);

        //increase screen brightness
        setBrightness(100);

        //stop screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


    }


    //Set action listeners
    private void setActionListeners()
    {
        //power button
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                clickButtonEffect(R.id.toggle_flash_button, "Vibrate");

                turnOffScreenLight();

            }
        });

        //flash mode button
        flashModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                clickButtonEffect(R.id.toggle_flashmode, "Vibrate");

                //screenLightMode is set to false because its in screenLightMode duh!! and set turnOffFlashlight
                isScreenLightMode = false;
                turnOnFlashOnStart = true;


                saveSettings();

                //go back to main activity
                finish();
            }
        });

        //shake event
        shakeDetector = new ShakeDetector(new ShakeDetector.OnShakeListener() {

            @Override
            public void onShake() {
                if(isShakeEnabled)
                    handleShakeEvent();
            }
        });
    }


    //turn off screen light
    private void  turnOffScreenLight()
    {
        //screenLightMode is set to true because its in screenLightMode duh!! and set turnOffFlashlight
        isScreenLightMode = true;
        turnOnFlashOnStart = false;

        saveSettings();

        //go back to main activity
        finish();

        //play sound effect if sound is not mute
        if(!isSilent)
        {
            mp = MediaPlayer.create(ScreenLightActivity.this, R.raw.off_switch_sound);
            Util.playSound(mp);
        }

    }


    //what happens when you shake the device
    private void handleShakeEvent()
    {
        vibrateEffect.vibrate(30);

        turnOffScreenLight();
    }


    //Save user Settings
    private  void saveSettings()
    {
        //passing data to the main activity
        intent.putExtra("isComingFromScreenActivity", true);
        intent.putExtra("turnOnFlashOnStart", turnOnFlashOnStart);
        intent.putExtra("isScreenLightMode", isScreenLightMode);

        startActivity(intent);
    }


    //Set brightness of screen
    private void setBrightness(float brightness)
    {

        //screen brightness
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.screenBrightness = brightness / 100.0f;
        getWindow().setAttributes(layoutParams);


    }


    //vibrate & animate button on click
    private void clickButtonEffect(int buttonID, String effectName)
    {
        //vibrate
        if(effectName.equals("Vibrate") || effectName.equals("All"))
            vibrateEffect.vibrate(20);

    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //adjust click volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0);

        // Add the following line to register the Sensor Manager Listener onResume
        if(isShakeEnabled)
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Add the following line to unregister the Sensor Manager onPause
        if(isShakeEnabled)
            sensorManager.unregisterListener(shakeDetector);

    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

}
