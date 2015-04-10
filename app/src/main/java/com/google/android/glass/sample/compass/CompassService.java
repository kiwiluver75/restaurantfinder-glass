/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.glass.sample.compass;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;

import com.google.android.glass.sample.compass.model.Landmarks;
import com.google.android.glass.sample.compass.util.MathUtils;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;



/**
 * The main application service that manages the lifetime of the compass live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class CompassService extends Service {

    private static final String LIVE_CARD_TAG = "compass";

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class CompassBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readHeadingAloud()
        {
            float heading = mOrientationManager.getHeading();

            Location loc = mOrientationManager.getLocation();

            String longi = Double.toString(loc.getLongitude());

            System.out.println(longi);
            String lati = Double.toString(loc.getLatitude());

            String alti = Double.toString(loc.getAltitude());

            String error = Double.toString(loc.getAccuracy());

            try {

                String url = "http://labs.puneeth.org/techlab/getrestaurants";
                String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
// ...

                String query = String.format("param1=%s&param2=%s",
                        URLEncoder.encode(lati, charset),
                        URLEncoder.encode(longi, charset));


                URLConnection connection = new URL(url + "?" + query).openConnection();
                connection.setRequestProperty("Accept-Charset", charset);
                InputStream response = connection.getInputStream();


            }
            catch (MalformedURLException e) {
               return;
            }
            catch (IOException e) {
                // openConnection() failed
                // ...
                return;
            }





            Resources res = getResources();

            String[] spokenDirections = res.getStringArray(R.array.spoken_directions);
            String directionName = spokenDirections[MathUtils.getHalfWindIndex(heading)];

            int roundedHeading = Math.round(heading);
            int headingFormat;


            if (roundedHeading == 1)
            {
                headingFormat = R.string.spoken_heading_format_one;
            }

            else {
                headingFormat = R.string.spoken_heading_format;
            }



            //String headingText = res.getString(headingFormat, roundedHeading, directionName);
            String headingText = "Longitude is " + longi + " Latitude is " + lati + "Altitude is " + alti;


            mSpeech.speak(headingText, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private final CompassBinder mBinder = new CompassBinder();

    private OrientationManager mOrientationManager;
    private Landmarks mLandmarks;
    private TextToSpeech mSpeech;

    private LiveCard mLiveCard;
    private CompassRenderer mRenderer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Even though the text-to-speech engine is only used in response to a menu action, we
        // initialize it when the application starts so that we avoid delays that could occur
        // if we waited until it was needed to start it up.
        mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // Do nothing.
            }
        });

        SensorManager sensorManager =
                (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mOrientationManager = new OrientationManager(sensorManager, locationManager);
        mLandmarks = new Landmarks(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mLiveCard == null) {
            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new CompassRenderer(this, mOrientationManager, mLandmarks);

            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);
            mLiveCard.setVoiceActionEnabled(true);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, CompassMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);

            // Only reveal the card if the service was started explicitly by the user. If the
            // service dies in the background from some sort of error, we can recover when the
            // system restarts it automatically (because the compass is stateless), but we don't
            // want to disrupt the user by revealing the live card from out of nowhere. We detect
            // whether this was an automated restart by checking if the intent is null, and if so,
            // we publish silently instead.
            mLiveCard.publish((intent == null) ? PublishMode.SILENT : PublishMode.REVEAL);
        } else {
            mLiveCard.navigate();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

        mSpeech.shutdown();

        mSpeech = null;
        mOrientationManager = null;
        mLandmarks = null;

        super.onDestroy();
    }
}
