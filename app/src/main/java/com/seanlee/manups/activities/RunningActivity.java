/* 
 * Project:Man-Ups   1.0 10/4/2014
 * 
 * Copyright CityU,EE5415,Group5
 *  
 * This Android software is Group5 project.  
 * 
 * Please be careful of all the variables' names and format!  - Sean
 *  
 */
package com.seanlee.manups.activities;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.seanlee.manups.R;
import com.seanlee.manups.databases.DatabaseOperation;
import com.seanlee.manups.services.RunningService;
import com.seanlee.manups.utils.Settings;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author LI Xiao
 * @version 1.3
 */
public class RunningActivity extends BasicActivity {

    // Define a constant for sending defined command to service
    final int SERVICE_STOP = 0;
    final int SEND_TIMER = 1;
    final String SERVICE_INTENT_ACTION = "edu.cityu.man_ups.RUNNING_SERVICE";
    final String ACTIVITY_INTENT_ACTION = "edu.cityu.man_ups.RUNNING_ACTIVITY";

    // Digital images resource references
    public int digit[] = {R.drawable.running_digit0,
            R.drawable.running_digit1, R.drawable.running_digit2,
            R.drawable.running_digit3, R.drawable.running_digit4,
            R.drawable.running_digit5, R.drawable.running_digit6,
            R.drawable.running_digit7, R.drawable.running_digit8,
            R.drawable.running_digit9,};

    private float distance = 0, calorie = 0;
    private int steps = 0, meter = 0;
    private EditText setWeight, setHeight;

    private Button mPushupsButton, mSitupsButton, mRecordButton,
            mCompleteButton, mSettingButton;
    private LinearLayout mBottomButtonLayout;
    private ImageView mThousandImageView, mHundredImageView, mDecadeImageView,
            mUnitsImageView;
    private TextView mStepCounter, mCalorieCounter;
    private Chronometer mChronometer;
    private TextView mDateTextView;

    private ImageView mMaskingImageView;
    private TextView mPromptTextView;

    // Tap to run in background
    private TextView mRunBackground;

    // Default weight is 50kg
    // Step Length Reference: male = height x 0.415 female = height x 0.413
    private float mUserWeight = 50, mStepLength = 0.68475f, mUserHeight = 165f;

    // For formating the data
    private DecimalFormat format;

    private int mPreviousMeter;
    private static final String TABLE_NAME = "manups";

    BroadcastReceiver mBroadcastReceiver;

    Intent intent;

    boolean isServiceOn = false, isSetTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        mPushupsButton = (Button) findViewById(R.id.pushups_button);
        mSitupsButton = (Button) findViewById(R.id.situps_button);
        mRecordButton = (Button) findViewById(R.id.record_button);
        mCompleteButton = (Button) findViewById(R.id.complete_button);

        //recalculate bottom button height
        mBottomButtonLayout = (LinearLayout) findViewById(R.id.bottom_buttons_layout);
        mBottomButtonLayout.setLayoutParams(getBottomButtonLayoutParams(this));

        mSettingButton = (Button) findViewById(R.id.running_setting_button);
        mDateTextView = (TextView) findViewById(R.id.date_textview);

        mThousandImageView = (ImageView) findViewById(R.id.thousand);
        mHundredImageView = (ImageView) findViewById(R.id.hundred);
        mDecadeImageView = (ImageView) findViewById(R.id.decade);
        mUnitsImageView = (ImageView) findViewById(R.id.unit);

        mMaskingImageView = (ImageView) findViewById(R.id.masking_imageview);
        mPromptTextView = (TextView) findViewById(R.id.prompt_textview);

        mStepCounter = (TextView) findViewById(R.id.step_counter);
        mCalorieCounter = (TextView) findViewById(R.id.calorie_counter);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);

        mRunBackground = (TextView) findViewById(R.id.running_background_textview);
        mRunBackground.setVisibility(View.INVISIBLE);

        ItemsOnClickListener ItemOnClickListener = new ItemsOnClickListener();
        mPushupsButton.setOnClickListener(ItemOnClickListener);
        mSitupsButton.setOnClickListener(ItemOnClickListener);
        mRecordButton.setOnClickListener(ItemOnClickListener);
        mCompleteButton.setOnClickListener(ItemOnClickListener);

        // To get the day
        SimpleDateFormat mDate = new SimpleDateFormat("yyyy-MM-dd");
        mDateTextView.setText(mDate.format(new Date()));

        // Set the data format
        format = new DecimalFormat("0.00");

        // To disable the setting button
        mSettingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                setting();
            }
        });
        mSettingButton.setClickable(false);

        // Tap the screen
        mMaskingImageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                // Set the timer and start it
                mChronometer.setBase(SystemClock.elapsedRealtime());
                mChronometer.start();
                isSetTime = true;

                //After clicking the masking
                AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                alphaAnimation.setDuration(500);
                alphaAnimation.setFillAfter(true);
                mMaskingImageView.startAnimation(alphaAnimation);
                mMaskingImageView.setClickable(false);
                mPromptTextView.setAnimation(null);
                mPromptTextView.setVisibility(View.INVISIBLE);
                mSettingButton.setClickable(true);

                mCompleteButton.setClickable(true);
                mCompleteButton.setAlpha(1);

                // Disable the other button
                mPushupsButton.setClickable(false);
                mSitupsButton.setClickable(false);
                mRecordButton.setClickable(false);
                mPushupsButton.setAlpha(0.2f);
                mSitupsButton.setAlpha(0.2f);
                mRecordButton.setAlpha(0.2f);

                // Run in background TextView
                mRunBackground.setVisibility(View.VISIBLE);
                mRunBackground.startAnimation(setPromptTextViewAnimation());

                isServiceOn = true;
                // start the service for detecting motion
                intent = new Intent(RunningActivity.this, RunningService.class);
                startService(intent);

                Toast.makeText(RunningActivity.this,
                        getResources().getString(R.string.running_start_toast),
                        Toast.LENGTH_SHORT).show();
            }
        });

        mCompleteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // To Stop the chronometer
                mChronometer.stop();

                // To update database
                mPreviousMeter += meter;
                // Set all the counter to 0
                meter = 0;
                steps = 0;
                calorie = 0;

                DatabaseOperation databaseOperation = new DatabaseOperation(RunningActivity.this);
                databaseOperation.setCount(mPreviousMeter, "running", mDateTextView.getText().toString());

                // Reset the mask ImageView
                AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
                alphaAnimation.setDuration(500);
                alphaAnimation.setFillAfter(true);
                mMaskingImageView.startAnimation(alphaAnimation);
                mMaskingImageView.setClickable(true);

                // Breathing TextView for prompting user
                // "Tap the Screen to Start"
                mPromptTextView.startAnimation(setPromptTextViewAnimation());

                // Disable the setting button
                mSettingButton.setClickable(false);

                mCompleteButton.setClickable(false);
                mCompleteButton.setAlpha(0.2f);

                // Enable the other button
                mPushupsButton.setClickable(true);
                mSitupsButton.setClickable(true);
                mRecordButton.setClickable(true);
                mPushupsButton.setAlpha(1);
                mSitupsButton.setAlpha(1);
                mRecordButton.setAlpha(1);

                mRunBackground.setAnimation(null);
                mRunBackground.setVisibility(View.INVISIBLE);

                Intent intent = new Intent();
                intent.setAction(SERVICE_INTENT_ACTION);
                intent.putExtra("command", SERVICE_STOP);
                sendBroadcast(intent);

                Toast.makeText(
                        RunningActivity.this,
                        getResources().getString(
                                R.string.running_complete_toast),
                        Toast.LENGTH_SHORT).show();
            }
        });

        mRunBackground.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // click the text 'run in background'
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        DatabaseOperation databaseOperation = new DatabaseOperation(this);
        mPreviousMeter = databaseOperation.getPreviousCount(mDateTextView.getText().toString(), "running");

        // cannot click complete Button now
        mCompleteButton.setClickable(false);
        mCompleteButton.setAlpha(0.2f);

        // whether the running is going, if yes, set the CompleteButton to be clickable, if not restart the prompt TextView
        if (!mMaskingImageView.isClickable()) {
            mCompleteButton.setClickable(true);
            mCompleteButton.setAlpha(1);
        } else {
            // Reset the Breathing textview
            mPromptTextView.startAnimation(setPromptTextViewAnimation());
        }

        // Define a inside broadcast receiver
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // If the user click the notification and enter into the running activity again
                if (isServiceOn == false) {
                    isServiceOn = true;

                    Intent checkTimeIntent = new Intent();
                    checkTimeIntent.setAction(SERVICE_INTENT_ACTION);
                    checkTimeIntent.putExtra("command", SEND_TIMER);
                    sendBroadcast(checkTimeIntent);

                    AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                    alphaAnimation.setDuration(500);
                    alphaAnimation.setFillAfter(true);
                    mMaskingImageView.startAnimation(alphaAnimation);
                    mMaskingImageView.setClickable(false);
                    mPromptTextView.setAnimation(null);
                    mPromptTextView.setVisibility(View.INVISIBLE);
                    mSettingButton.setClickable(true);
                    mCompleteButton.setClickable(true);
                    mCompleteButton.setAlpha(1);

                    // Disable the other button
                    mPushupsButton.setClickable(false);
                    mSitupsButton.setClickable(false);
                    mRecordButton.setClickable(false);
                    mPushupsButton.setAlpha(0.2f);
                    mSitupsButton.setAlpha(0.2f);
                    mRecordButton.setAlpha(0.2f);

                    mRunBackground.setVisibility(View.VISIBLE);
                    mRunBackground.startAnimation(setPromptTextViewAnimation());
                }

                // After entering, it should update the chronometer
                if (isSetTime == false) {
                    Bundle bundle = intent.getExtras();
                    if (bundle.getLong("timer", -1) != -1) {
                        mChronometer.setBase(bundle.getLong("timer"));
                        mChronometer.start();
                        isSetTime = true;
                    }
                }

                Bundle bundle = intent.getExtras();
                steps = bundle.getInt("steps", -1);
                if (steps != -1) {

                    distance = steps * mStepLength;
                    meter = (int) distance;

                    //calculate the calorie
                    calorie = mUserWeight * distance * 1.036f / 1000;
                    displayDigital(meter);

                    mStepCounter.setText(steps + " "
                            + getResources().getString(R.string.step_unit));
                    mCalorieCounter.setText(format.format(calorie) + " "
                            + getResources().getString(R.string.calorie_unit));
                }
            }
        };

        //load the setting
        loadSharedPreference();

        // Register the BroadcastReceiver
        IntentFilter myIntentFilter = new IntentFilter();
        myIntentFilter.addAction(ACTIVITY_INTENT_ACTION);
        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPromptTextView.setAnimation(null);
        unregisterReceiver(mBroadcastReceiver);
    }

    private void displayDigital(int meter) {

        int thousand, hundred, decade, units;

        thousand = meter / 1000;
        hundred = (meter % 1000) / 100;
        decade = (meter % 100) / 10;
        units = meter % 10;

        mThousandImageView.setImageResource(digit[thousand]);
        mHundredImageView.setImageResource(digit[hundred]);
        mDecadeImageView.setImageResource(digit[decade]);
        mUnitsImageView.setImageResource(digit[units]);
    }

    public void setting() {

        final Dialog dialog = new Dialog(RunningActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.running_setting);
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams layoutParams = dialogWindow.getAttributes();
        layoutParams.x = 21;
        layoutParams.y = 15;
        dialog.show();
        setWeight = (EditText) dialog.findViewById(R.id.weightEditText);
        setHeight = (EditText) dialog.findViewById(R.id.heightEditText);

        setWeight.setText("" + mUserWeight);
        setHeight.setText("" + mUserHeight);

        Button confirm = (Button) dialog.findViewById(R.id.confirm);
        Button cancel = (Button) dialog.findViewById(R.id.cancel);

        // The setting dialog confirm button
        confirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (setWeight.getText().toString().equals("")) {
                    Toast.makeText(
                            RunningActivity.this,
                            getResources().getString(
                                    R.string.setting_weight_error),
                            Toast.LENGTH_SHORT).show();
                } else if (setHeight.getText().toString().equals("")) {
                    Toast.makeText(
                            RunningActivity.this,
                            getResources().getString(
                                    R.string.setting_height_error),
                            Toast.LENGTH_SHORT).show();
                } else {
                    mUserWeight = Float.parseFloat(setWeight.getText()
                            .toString());
                    mUserHeight = Float.parseFloat(setHeight.getText()
                            .toString());
                    mStepLength = mUserHeight / 100 * 0.414f;
                    Toast.makeText(RunningActivity.this,
                            getResources().getString(R.string.setting_success),
                            Toast.LENGTH_SHORT).show();

                    //Save into SharedPreference
                    saveSharedPreference();
                    dialog.dismiss();
                }
            }
        });

        // The setting dialog cancel button
        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }

    private AlphaAnimation setPromptTextViewAnimation() {
        AlphaAnimation alphaAnimationText = new AlphaAnimation(1, 0.1f);
        alphaAnimationText.setDuration(1300);
        alphaAnimationText.setRepeatMode(Animation.REVERSE);
        alphaAnimationText.setRepeatCount(Animation.INFINITE);
        return alphaAnimationText;
    }

    private void saveSharedPreference() {
        SharedPreferences sharedPreference = getSharedPreferences(Settings.SHARE_PREFERENCES_NAME, MODE_PRIVATE);
        sharedPreference.edit().putFloat("USER_HEIGHT", mUserHeight).apply();
        sharedPreference.edit().putFloat("BODY_WEIGHT", mUserWeight).apply();
        sharedPreference.edit().putFloat("STEP_LENGTH", mStepLength).apply();
        sharedPreference.edit().putBoolean("SET_USER_INFO", true).apply();
    }

    private void loadSharedPreference() {
        SharedPreferences sharedPreference = getSharedPreferences(Settings.SHARE_PREFERENCES_NAME, MODE_PRIVATE);
        mUserHeight = sharedPreference.getFloat("USER_HEIGHT", 165f);
        mUserWeight = sharedPreference.getFloat("BODY_WEIGHT", 50);
        mStepLength = sharedPreference.getFloat("STEP_LENGTH", 0.68475f);

        if (!sharedPreference.getBoolean("SET_USER_INFO", false)) {
            setting();
            Toast.makeText(RunningActivity.this,
                    getResources().getString(R.string.setting_require),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
