package net.kdt.pojavlaunch.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.google.android.material.slider.Slider;

import net.ashmeet.hyperlauncher.R;

public class CustomSeekBarPreference extends SeekBarPreference {

    private String mSuffix = "";
    private int mMin;
    private TextView mTextView;
    private int mIncrement;
    private int mMax = 100;

    @SuppressLint("PrivateResource")
    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        try (TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.SeekBarPreference, defStyleAttr, defStyleRes)) {
            mMin = a.getInt(R.styleable.SeekBarPreference_min, 0);
            mIncrement = a.getInt(R.styleable.SeekBarPreference_seekBarIncrement, 1);
        }
        TypedArray sa = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.max});
        mMax = sa.getInt(0, 100);
        sa.recycle();
        
        setLayoutResource(R.layout.app_preference_seekbar_layout);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.preference.R.attr.seekBarPreferenceStyle);
    }

    public CustomSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    public void setMin(int min) {
        super.setMin(min);
        this.mMin = min;
    }

    public void setSliderMax(int max) {
        super.setMax(max);
        this.mMax = max;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        
        mTextView = (TextView) view.findViewById(R.id.seekbar_value);
        Slider slider = (Slider) view.findViewById(R.id.material_slider);

        if (slider != null) {
            float step = Math.max(1f, (float) mIncrement);
            slider.setValueFrom(mMin);
            
            // Definitively fix IllegalStateException by adjusting range
            int range = mMax - mMin;
            float adjustedMax = mMin + (float) (Math.floor(range / step) * step);
            if (adjustedMax <= mMin) adjustedMax = mMin + step;
            
            slider.setValueTo(adjustedMax);
            slider.setStepSize(step);
            
            int currentValue = getValue();
            if (currentValue < mMin) currentValue = mMin;
            if (currentValue > adjustedMax) currentValue = (int) adjustedMax;

            // Align currentValue with stepSize to avoid IllegalStateException
            float remainder = (currentValue - mMin) % step;
            if (remainder != 0) {
                currentValue = Math.round(mMin + Math.round((currentValue - mMin) / step) * step);
                // Ensure we don't exceed adjustedMax after rounding
                if (currentValue > adjustedMax) {
                    currentValue = (int) adjustedMax;
                }
            }

            slider.setValue(currentValue);

            slider.clearOnChangeListeners();
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    int intValue = (int) value;
                    if (mTextView != null) {
                        mTextView.setText(intValue + mSuffix);
                    }
                }
            });

            slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {}

                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {
                    int value = (int) slider.getValue();
                    if (callChangeListener(value)) {
                        setValue(value);
                    }
                }
            });
        }

        if (mTextView != null) {
            mTextView.setText(getValue() + mSuffix);
        }
    }

    public void setSuffix(String suffix) {
        this.mSuffix = suffix;
    }

    public void setRange(int min, int max){
        setMin(min);
        setSliderMax(max);
    }

    public void setMaxKeepIncrement(int max) {
        setSliderMax(max);
    }
}
