/**
 * **************************************************************************
 * AdvOptionsDialog.java
 * ****************************************************************************
 * Copyright © 2015 VLC authors and VideoLAN
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * ***************************************************************************
 */
package org.videolan.vlc.gui.dialogs;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.vlc.BuildConfig;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.MainActivity;
import org.videolan.vlc.gui.SecondaryActivity;
import org.videolan.vlc.gui.video.VideoPlayerActivity;
import org.videolan.vlc.interfaces.IDelayController;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.Util;
import org.videolan.vlc.util.VLCInstance;

import java.util.Calendar;

import static org.videolan.vlc.gui.dialogs.PickTimeFragment.ACTION_AUDIO_DELAY;
import static org.videolan.vlc.gui.dialogs.PickTimeFragment.ACTION_JUMP_TO_TIME;
import static org.videolan.vlc.gui.dialogs.PickTimeFragment.ACTION_SPU_DELAY;

public class AdvOptionsDialog extends DialogFragment implements View.OnClickListener {

    public final static String TAG = "VLC/AdvOptionsDialog";
    public static final String MODE_KEY = "mode";
    public static final int MODE_VIDEO = 0;
    public static final int MODE_AUDIO = 1;

    public static final int SPEED_TEXT = 0;
    public static final int SLEEP_TEXT = 1;
    public static final int TOGGLE_CANCEL = 2;
    public static final int DIALOG_LISTENER = 3;
    public static final int RESET_RETRY = 4;

    private int mMode = -1;
    private TextView mAudioMode;
    private TextView mEqualizer;

    private TextView mSpeedTv;
    private SeekBar mSeek;
    private Button mReset;

    private TextView mSleepTitle;
    private TextView mSleepTime;
    private TextView mSleepCancel;

    private TextView mJumpTitle;

    private TextView mAudioDelay;
    private TextView mSpuDelay;

    private Spinner mChapters;
    private TextView mChaptersTitle;
    private static AdvOptionsDialog sInstance;
    private int mTextColor;

    private IDelayController mDelayController;
    private LibVLC mLibVLC;
    public AdvOptionsDialog() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.attr.advanced_options_style);
        sInstance = this;
        if (VLCApplication.sPlayerSleepTime != null && VLCApplication.sPlayerSleepTime.before(Calendar.getInstance()))
            VLCApplication.sPlayerSleepTime = null;
        mLibVLC = VLCInstance.get();
        if (getArguments() != null && getArguments().containsKey(MODE_KEY))
            mMode = getArguments().getInt(MODE_KEY);
        else
            mMode = MODE_VIDEO;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mMode == MODE_VIDEO) {
            mDelayController = (IDelayController) activity;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_advanced_options, container, false);
        getDialog().setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);


        mSeek = (SeekBar) root.findViewById(R.id.playback_speed_seek);
        mSpeedTv = (TextView) root.findViewById(R.id.playback_speed_value);
        mReset = (Button) root.findViewById(R.id.playback_speed_reset);

        mSeek.setOnSeekBarChangeListener(mSeekBarListener);
        mReset.setOnClickListener(mResetListener);

        mSleepTitle = (TextView) root.findViewById(R.id.sleep_timer_title);
        mSleepTime = (TextView) root.findViewById(R.id.sleep_timer_value);
        mSleepCancel = (TextView) root.findViewById(R.id.sleep_timer_cancel);
        mJumpTitle = (TextView) root.findViewById(R.id.jump_title);

        mJumpTitle.setOnClickListener(this);

        /*if (BuildConfig.tv) {
            root.findViewById(R.id.sleep_timer_container).setVisibility(View.GONE);
        } else */{
            mSleepTitle.setOnClickListener(this);
            mSleepTime.setOnClickListener(this);
            mSleepCancel.setOnClickListener(this);
        }

        mReset.setOnFocusChangeListener(mFocusListener);
        mSleepTime.setOnFocusChangeListener(mFocusListener);
        mSleepCancel.setOnFocusChangeListener(mFocusListener);
        mJumpTitle.setOnFocusChangeListener(mFocusListener);

        if (mMode == MODE_VIDEO) {
            mAudioMode = (TextView) root.findViewById(R.id.playback_switch_audio);
            mAudioMode.setOnClickListener(this);
            mAudioMode.setOnFocusChangeListener(mFocusListener);

            mChapters = (Spinner) root.findViewById(R.id.jump_chapter);
            mChaptersTitle = (TextView) root.findViewById(R.id.jump_chapter_title);

            mAudioDelay = (TextView) root.findViewById(R.id.audio_delay);
            mSpuDelay = (TextView) root.findViewById(R.id.spu_delay);

            mSpuDelay.setOnClickListener(this);
            mSpuDelay.setOnFocusChangeListener(mFocusListener);
            mAudioDelay.setOnClickListener(this);
            mAudioDelay.setOnFocusChangeListener(mFocusListener);
            initChapterSpinner();
        } else {
            root.findViewById(R.id.audio_delay).setVisibility(View.GONE);
            root.findViewById(R.id.spu_delay).setVisibility(View.GONE);
            root.findViewById(R.id.jump_chapter).setVisibility(View.GONE);
            root.findViewById(R.id.jump_chapter_title).setVisibility(View.GONE);
            root.findViewById(R.id.playback_switch_audio).setVisibility(View.GONE);

        }

        if (mMode == MODE_AUDIO){
            mEqualizer = (TextView) root.findViewById(R.id.opt_equalizer);
            mEqualizer.setOnClickListener(this);
            mEqualizer.setOnFocusChangeListener(mFocusListener);
        } else
            root.findViewById(R.id.opt_equalizer).setVisibility(View.GONE);
        mHandler.sendEmptyMessage(TOGGLE_CANCEL);
        mTextColor = mSleepTitle.getCurrentTextColor();

        double speed = mLibVLC.getRate();
        if (speed != 1.0d) {
            speed = 100 * (1 + Math.log(speed) / Math.log(4));
            mSeek.setProgress((int) speed);
        }

        Window window = getDialog().getWindow();
        window.setBackgroundDrawableResource(Util.getResourceFromAttribute(getActivity(), R.attr.rounded_bg));
        window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
        return root;
    }

    private void initChapterSpinner() {
        int chaptersCount = mLibVLC.getChapterCount();
        if (chaptersCount <= 1){
            mChapters.setVisibility(View.GONE);
            mChaptersTitle.setVisibility(View.GONE);
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
        String chapterDescription;
        for (int i = 0 ; i < chaptersCount ; ++i) {
            chapterDescription = mLibVLC.getChapterDescription(i);
            adapter.insert(chapterDescription != null ? chapterDescription : Integer.toString(i), i);
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mChapters.setAdapter(adapter);
        mChapters.setSelection(mLibVLC.getChapter());
        mChapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != mLibVLC.getChapter())
                    mLibVLC.setChapter(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            float rate = (float) Math.pow(4, ((double) progress / (double) 100) - 1);
            mHandler.obtainMessage(SPEED_TEXT, Strings.formatRateString(rate)).sendToTarget();
            mLibVLC.setRate(rate);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {}
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    private View.OnClickListener mResetListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSeek.setProgress(100);
            mLibVLC.setRate(1);
        }
    };

    private void showTimePickerFragment(int action) {
        if (mDelayController == null && getActivity() instanceof IDelayController)
            mDelayController = (IDelayController) getActivity();
        DialogFragment newFragment = null;
        /*if (BuildConfig.tv) {
            switch (action){
                case PickTimeFragment.ACTION_AUDIO_DELAY:
                    newFragment = new AudioDelayDialog();
                    break;
                case PickTimeFragment.ACTION_SPU_DELAY:
                    newFragment = new SubsDelayDialog();
                    break;
                case PickTimeFragment.ACTION_JUMP_TO_TIME:
                    newFragment = new JumpToTimeDialog();
                    break;
                default:
                    return;
            }
        } else */{
            switch (action){
                case PickTimeFragment.ACTION_AUDIO_DELAY:
                    if (mDelayController != null)
                        mDelayController.showAudioDelaySetting();
                    break;
                case PickTimeFragment.ACTION_SPU_DELAY:
                    if (mDelayController != null)
                        mDelayController.showSubsDelaySetting();
                    break;
                case PickTimeFragment.ACTION_JUMP_TO_TIME:
                    newFragment = new JumpToTimeDialog();
                    break;
                default:
                    return;
            }
        }
        if (newFragment != null)
            newFragment.show(getActivity().getSupportFragmentManager(), "time");
        dismiss();
    }

    View.OnFocusChangeListener mFocusListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (v instanceof TextView)
                ((TextView) v).setTextColor(v.hasFocus() ?
                        sInstance.getResources().getColor(R.color.orange500) : mTextColor);
        }
    };

    private void showTimePicker(int action) {
        DialogFragment newFragment = new TimePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt("action", action);
        newFragment.setArguments(args);
        newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
        mHandler.sendEmptyMessage(RESET_RETRY);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(DIALOG_LISTENER, newFragment), 100);
        dismiss();
    }

    public static void setSleep(Context context, Calendar time) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(VLCApplication.SLEEP_INTENT);
        PendingIntent sleepPendingIntent = PendingIntent.getBroadcast(VLCApplication.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (time != null) {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), sleepPendingIntent);
        }
        else {
            alarmMgr.cancel(sleepPendingIntent);
        }
        VLCApplication.sPlayerSleepTime = time;
    }

    private final static Handler mHandler = new Handler(){

        public boolean retry = true;

        @Override
        public void handleMessage(Message msg) {
            String text = null;
            switch (msg.what) {
                case SPEED_TEXT:
                    text = (String) msg.obj;
                    sInstance.mSpeedTv.setText(text);
                    break;
                case TOGGLE_CANCEL:
                    sInstance.mSleepCancel.setVisibility(VLCApplication.sPlayerSleepTime == null ? View.GONE : View.VISIBLE);
                case SLEEP_TEXT:
                    if (VLCApplication.sPlayerSleepTime != null)
                        text = DateFormat.getTimeFormat(sInstance.mSleepTime.getContext()).format(VLCApplication.sPlayerSleepTime.getTime());
                    if (text == null)
                        text = "none set";
                    sInstance.mSleepTime.setText(text);
                    break;
                case DIALOG_LISTENER:
                    DialogFragment newFragment = (DialogFragment) msg.obj;
                    if (newFragment.getShowsDialog()) {
                        newFragment.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                mHandler.obtainMessage(TOGGLE_CANCEL).sendToTarget();
                            }
                        });
                    } else if (retry) {
                        retry = false;
                        sendMessageDelayed(msg, 300);
                    }
                    break;
                case RESET_RETRY:
                    retry = true;
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.audio_delay:
                showTimePickerFragment(ACTION_AUDIO_DELAY);
                break;
            case R.id.spu_delay:
                showTimePickerFragment(ACTION_SPU_DELAY);
                break;
            case R.id.jump_title:
                showTimePickerFragment(ACTION_JUMP_TO_TIME);
                break;
            case R.id.sleep_timer_title:
            case R.id.sleep_timer_value:
                showTimePicker(TimePickerDialogFragment.ACTION_SLEEP);
                break;
            case R.id.sleep_timer_cancel:
                setSleep(v.getContext(), null);
                mHandler.sendEmptyMessage(TOGGLE_CANCEL);
                break;
            case R.id.playback_switch_audio:
                ((VideoPlayerActivity)getActivity()).switchToAudioMode(true);
                break;
            case R.id.opt_equalizer:
                ((MainActivity)getActivity()).showSecondaryFragment(SecondaryActivity.EQUALIZER);
                dismiss();
                break;
        }
    }
}
