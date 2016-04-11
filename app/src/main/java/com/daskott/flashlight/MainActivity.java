package com.daskott.flashlight;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.content.DialogInterface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.daskott.flashlight.ShakeDetector.OnShakeListener;


public class MainActivity extends AppCompatActivity {

    //globals
    public static final String PREFS_NAME = "daskott_flashLight_settings";
    private SharedPreferences settings;
    private NotificationManager mNotificationManager;
    public static final int NOTIFICATION_ID = 1;
    private Camera camera;
    private boolean isFlashOn;
    private boolean isScreenLightMode;
    private boolean isSilent;
    private boolean hasFlash;
    private boolean isShakeEnabled;
    private boolean turnOnFlashOnStart;
    private boolean showNoCameraAlert;
    private boolean showShakeModeAlert;
    private boolean isComingFromScreenActivity = false;
    private Camera.Parameters params;
    private MediaPlayer mp;
    private ImageButton flashButton;
    private ImageView silentButton;
    private ImageView flashModeButton;
    private ImageView screenModeButton;
    private ImageView shakeModeButton;
    private AlertDialog dialog;
    private  AudioManager audioManager;


    // The following are used for the shake detection
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        init();
        setActionListeners();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    //initialization process
    private void init() {

        //init ui elements
        flashButton = (ImageButton) findViewById(R.id.toggle_flash_button);
        silentButton = (ImageView) findViewById(R.id.toggle_silent_button);
        flashModeButton = (ImageView) findViewById(R.id.toggle_flashmode);
        screenModeButton = (ImageView) findViewById(R.id.toggle_screenmode);
        shakeModeButton = (ImageView) findViewById(R.id.toggle_shakemode);

        //control volume of click
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0);

        // ShakeDetector initialization
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        //Restore preferences from settings file and set the UI elements based on those settings.
        settings = getSharedPreferences(PREFS_NAME, 0);
        getSettings("All");



        //check if the device has a camera
        hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (!hasFlash && showNoCameraAlert)
        {
            // device doesn't support flash
           showAlertDialog("FLASH MODE","Sorry, your device doesn't support flash light mode," +
                    " but you can still use the screen light mode :)");
            isScreenLightMode = true;

        } else
        {
            //stop screen from sleeping
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }


        updateIcons("All");
    }


    //Set action listeners
    private void setActionListeners() {
        //power button
        flashButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                clickButtonEffect(R.id.toggle_flash_button, "Vibrate");

                if (isFlashOn)
                    turnOffFlash();
                else
                    turnOnFlash();
            }
        });

        //silent button
        silentButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                toggleSilentMode();
                clickButtonEffect(R.id.toggle_silent_button, "Vibrate");
            }
        });

        //flash mode button
        flashModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                clickButtonEffect(R.id.toggle_flashmode, "Vibrate");

                //if the device has a flash, set flash mode
                if(hasFlash)
                {
                    //set screen mode to false only when necessary
                    if (isScreenLightMode)
                        isScreenLightMode = false;


                    //toggle image button
                    Util.toggleButtonImage(R.mipmap.off_flashmode, R.mipmap.on_flashmode, isScreenLightMode, flashModeButton);
                    screenModeButton.setImageResource(R.mipmap.off_screen_light);
                }
                else
                    Toast.makeText(getApplicationContext(), "Sorry, Flash Mode Not Supported :(", Toast.LENGTH_SHORT).show();

            }
        });

        //screen mode button
        screenModeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //click button effect
                clickButtonEffect(R.id.toggle_screenmode, "Vibrate");

                //set screen mode to true only when necessary
                if (!isScreenLightMode)
                    isScreenLightMode = true;

                //toggle image button
                Util.toggleButtonImage(R.mipmap.on_screen_light, R.mipmap.off_screen_light, isScreenLightMode, screenModeButton);
                flashModeButton.setImageResource(R.mipmap.off_flashmode);

                if (isFlashOn && isScreenLightMode)
                    turnOnFlash();


            }
        });

        //shake mode button
        shakeModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                isShakeEnabled = !isShakeEnabled;
                clickButtonEffect(R.id.toggle_shakemode, "Vibrate");


                if (isShakeEnabled) {
                    //alert for first time users about shake mode
                    if (showShakeModeAlert)
                    {
                        showAlertDialog("SHAKE MODE",
                                "Most devices do not support shake mode while the FlashLight is in the background. " +
                                        "So to use shake mode, the FlashLight has to be open. Enjoy :)");
                    }

                    // Add the following line to register the Sensor Manager Listener
                    sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);

                } else {
                    // Add the following line to unregister the Sensor Manager
                    sensorManager.unregisterListener(shakeDetector);
                }

                //toggle image button
                Util.toggleButtonImage(R.mipmap.on_shake, R.mipmap.off_shake, isShakeEnabled, shakeModeButton);
            }
        });


        //shake event
        shakeDetector = new ShakeDetector(new OnShakeListener() {

            @Override
            public void onShake() {
                if (isShakeEnabled)
                    handleShakeEvent();
            }
        });


    }


    // getting camera parameters
    private void getCamera() {
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();

            } catch (RuntimeException e) {

            }
        }
    }


    //Turning On Flash
    private void turnOnFlash() {
        //check which flash mode is in use, to turn on the right one
        if (isScreenLightMode)
        {
            turnOnScreenLight();

        }
        //if the device has a flash, turn on the flashlight
        else if(hasFlash)
        {

            turnOnCameraLight();
            // changing button/switch image
            flashButton.setImageResource(R.mipmap.on_button);

        }

        playSound("OnSound");
    }


    //Turning Off Flash
    private void turnOffFlash() {
        //check which flash mode is in use, to turn off the right one
        if (!isScreenLightMode)
        {
            turnOffCameraLight();
            //changing button/switch image
            flashButton.setImageResource(R.mipmap.off_button);
        }
        //do nothing for if isScreenLightMode == true

        playSound("OffSound");
    }


    //Turning On Camera Light
    private void turnOnCameraLight() {

        if (!isFlashOn) {

            if (camera == null || params == null) {
                return;
            }

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;

            //set notification
            sendNotification("Active");
        }


    }


    //Turning Off Camera Light
    private void turnOffCameraLight() {

        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;

            //clear notification
            mNotificationManager.cancelAll();
        }


    }


    //Turning On screen light
    private void turnOnScreenLight() {
        if (isFlashOn) {
            turnOffCameraLight();
        }
        saveSettings();

        //this takes us to the screen light activity to increase phone brightness
        Intent myIntent = new Intent(MainActivity.this, ScreenLightActivity.class);
        startActivity(myIntent);

    }


    //Toggle the silent state and its image indicator
    private void toggleSilentMode() {
        isSilent = !isSilent;
        Util.toggleButtonImage(R.mipmap.off_sound, R.mipmap.on_sound, isSilent, silentButton);
    }


    //Play button click sound
    private void playSound(String soundName) {
        if (!isSilent) {
            //set sound effect based on state of flash light
            if (soundName.equals("OnSound"))
                mp = MediaPlayer.create(MainActivity.this, R.raw.on_switch_sound);

            else if (soundName.equals("OffSound"))
                mp = MediaPlayer.create(MainActivity.this, R.raw.off_switch_sound);

            Util.playSound(mp);
        }
    }


    //vibrate button on click
    private void clickButtonEffect(int buttonID, String effectName) {
        Vibrator vibrateEffect = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //vibrate
        if (effectName.equals("Vibrate") || effectName.equals("All"))
            vibrateEffect.vibrate(20);

    }


    //what happens when you shake the device
    private void handleShakeEvent() {
        Vibrator vibrateEffect = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrateEffect.vibrate(30);

        //Toggle flashlight
        if (isFlashOn)
            turnOffFlash();
        else
            turnOnFlash();
    }


    //sets notification for the app
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notifyIntent = new Intent(getApplicationContext(), MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 0,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.notify_icon)
                        .setLargeIcon(((BitmapDrawable) getResources().getDrawable(R.mipmap.logo)).getBitmap())
                        .setContentTitle(getString(R.string.app_name))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setOngoing(true)
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }


    //Release the camera when ever its not in use
    private void releaseCamera() {
        //release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }

    }


    private void showAlertDialog(final String tittle, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(tittle).setMessage(message)
                .setPositiveButton("DON'T SHOW AGAIN", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // FIRE ZE MISSILES!
                        if(tittle.equals("FLASH MODE"))
                            showNoCameraAlert = false;
                        else if(tittle.equals("SHAKE MODE"))
                            showShakeModeAlert = false;
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        dialog.cancel();
                    }
                });


        // Create the AlertDialog object and return it
        dialog = builder.create();
        dialog.show();

    }


    //update UI icons
    private void updateIcons(String set)
    {

        //flash mode button
        Util.toggleButtonImage(R.mipmap.off_flashmode, R.mipmap.on_flashmode, isScreenLightMode,flashModeButton);

        //screen mode button
        Util.toggleButtonImage(R.mipmap.on_screen_light, R.mipmap.off_screen_light, isScreenLightMode, screenModeButton);

        if(set.equals("All"))
        {
            //silent mode button
            Util.toggleButtonImage(R.mipmap.off_sound, R.mipmap.on_sound, isSilent, silentButton);

            //shake mode button
            Util.toggleButtonImage(R.mipmap.on_shake, R.mipmap.off_shake, isShakeEnabled, shakeModeButton);
        }


    }



    //update power icon
    private void updatePowerIcon()
    {
        // changing button/switch image
        Util.toggleButtonImage(R.mipmap.on_button, R.mipmap.off_button, isFlashOn, flashButton);
    }


    //save user settings
    private void saveSettings()
    {
        // We need an Editor object to make preference changes.
        // Save App settings before app dies
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("silentMode", isSilent);
        editor.putBoolean("screenLightMode", isScreenLightMode);
        editor.putBoolean("turnOnFlashOnStart", turnOnFlashOnStart);
        editor.putBoolean("shakeEnabled", isShakeEnabled);
        editor.putBoolean("showNoCameraAlert", showNoCameraAlert);
        editor.putBoolean("showShakeModeAlert", showShakeModeAlert);

        // Commit the edits!
        editor.commit();
    }


    //get stored settings
    private void getSettings(String settingsName)
    {

        turnOnFlashOnStart = true;

        if(settingsName.equals("All"))
        {
            isSilent = settings.getBoolean("silentMode", false);
            isShakeEnabled = settings.getBoolean("shakeEnabled", false);
            showNoCameraAlert = settings.getBoolean("showNoCameraAlert", true);
            showShakeModeAlert = settings.getBoolean("showShakeModeAlert", true);
            isScreenLightMode = settings.getBoolean("screenLightMode", false);
        }
        else
        {
            turnOnFlashOnStart = getIntent().getBooleanExtra("turnOnFlashOnStart", true);
            isScreenLightMode= getIntent().getBooleanExtra("isScreenLightMode", isScreenLightMode);
        }

    }


    @Override
    public void onBackPressed()
    {
        //when back button is pressed, if flash is on do not kill the app
        if(isFlashOn)
            moveTaskToBack(true);
        else
            finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //clear all notifications when the app is destroyed
        if(mNotificationManager != null)
            mNotificationManager.cancelAll();

        //release a camera since its no longer in use
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Add the following line to unregister the Sensor Manager onPause
        if(isShakeEnabled)
            sensorManager.unregisterListener(shakeDetector);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        isComingFromScreenActivity = getIntent().getBooleanExtra("isComingFromScreenActivity", false);

        if(!isComingFromScreenActivity)
        {
            turnOnFlash();
        }
        else
        {
            getSettings("ScreenActivity");

            if(turnOnFlashOnStart)
                turnOnFlash();
            else
                isFlashOn = false;

            updatePowerIcon();
            updateIcons("ScreenActivity");

            //reset back to false
            getIntent().putExtra("isComingFromScreenActivity", false);

        }



        if(!hasFlash)
            isScreenLightMode = true;

        //adjust click volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0);

        // Add the following line to register the Sensor Manager Listener onResume
        if(isShakeEnabled)
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);


    }

    @Override
    protected void onStart() {
        super.onStart();

        getCamera();

    }

    @Override
    protected void onStop() {
        super.onStop();

        saveSettings();

        //if the flash is not 'On' release the camera, so other apps can use it
        if(!isFlashOn)
            releaseCamera();

    }
}
