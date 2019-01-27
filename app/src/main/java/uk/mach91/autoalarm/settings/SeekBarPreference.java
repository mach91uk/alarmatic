package uk.mach91.autoalarm.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import uk.mach91.autoalarm.R;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
    private SeekBar mSeekBar;
    private int mProgress;
    private int mMax = 100;
    private int mIncrement = 1;
    private boolean mTouchDrag = false;

    public SeekBarPreference(Context context) {
        this(context, null, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_seekbar);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mSeekBar.setMax(mMax);
        mSeekBar.incrementProgressBy(mIncrement);
        mSeekBar.setProgress(mProgress);
        mSeekBar.setOnSeekBarChangeListener(this);
    }

    public void setMaxValue(int value) {
        mMax = value;
    }
    public void setIncrementSize(int value) {
        mIncrement = value;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser)
            return;

        if (!mTouchDrag) {
            setValue(progress);
        } else {
            seekBar.setProgress(progress);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTouchDrag = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
       if (mTouchDrag) {
           setValue(seekBar.getProgress());
       }
        mTouchDrag = false;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
    }

    public void setValue(int value) {
        if (shouldPersist()) {
            persistInt(value);
        }

        if (value != mProgress) {
            mProgress = value;
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }
}
