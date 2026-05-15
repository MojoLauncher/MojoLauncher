package com.kdt.mcgui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;

import net.ashmeet.hyperlauncher.R;

public class TextProgressBar extends ProgressBar {

    private int mTextPadding = 0;
    public TextProgressBar(Context context) {super(context, null, android.R.attr.progressBarStyleHorizontal); init();}

    public TextProgressBar(Context context, AttributeSet attrs) {super(context, attrs, android.R.attr.progressBarStyleHorizontal); init();}
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, android.R.attr.progressBarStyleHorizontal);
        init();
    }
    public TextProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, android.R.attr.progressBarStyleHorizontal, defStyleRes);
        init();
    }

    private Paint mTextPaint;
    private String mText = "";

    private void init(){
        setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.view_text_progressbar, null));
        setProgress(35);
        mTextPaint = new Paint();
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setFlags(Paint.FAKE_BOLD_TEXT_FLAG);
        mTextPaint.setAntiAlias(true);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float textSize = (float) ((getHeight() - getPaddingBottom() - getPaddingTop()) * 0.55);
        mTextPaint.setTextSize(textSize);

        float progressRatio = (float) getProgress() / getMax();
        int progressWidth = (int) (progressRatio * getWidth());

        float textWidth = mTextPaint.measureText(mText);
        int xPos = (int) Math.max(Math.min((progressWidth) + mTextPadding, getWidth() - textWidth - mTextPadding) , mTextPadding);
        int yPos = (int) ((getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;

        // Draw text on the empty part of the bar (Dark Gray/Black depending on contrast)
        canvas.save();
        canvas.clipRect(progressWidth, 0, getWidth(), getHeight());
        mTextPaint.setColor(Color.GRAY);
        canvas.drawText(mText, xPos, yPos, mTextPaint);
        canvas.restore();

        // Draw text on the filled part of the bar (White/Inverted)
        // Since minebutton_color is #FFFFFF, text on it should be black
        canvas.save();
        canvas.clipRect(0, 0, progressWidth, getHeight());
        mTextPaint.setColor(Color.BLACK);
        canvas.drawText(mText, xPos, yPos, mTextPaint);
        canvas.restore();
    }


    public final void setText(@StringRes int resid) {
        setText(getContext().getResources().getText(resid).toString());
    }

    public final void setText(String text){
        mText = text;
        invalidate();
    }

    public final void setTextPadding(int padding){
        mTextPadding = padding;
    }
}
