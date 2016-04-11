package com.daskott.flashlight;


import android.media.MediaPlayer;
import android.widget.ImageView;


/**
 * Created by Edmon_000 on 9/8/2015.
 */
public class Util
{
    //Play media file parsed
    public static void playSound(MediaPlayer mp)
    {
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                // TODO Auto-generated method stub
                mp.release();
            }
        });

        mp.start();
    }


    //Toggle the between the two images parsed
    public static void toggleButtonImage(int trueImage, int falseImage, boolean buttonState, ImageView button)
    {
        if(buttonState)
            button.setImageResource(trueImage);
        else
            button.setImageResource(falseImage);

    }


}
