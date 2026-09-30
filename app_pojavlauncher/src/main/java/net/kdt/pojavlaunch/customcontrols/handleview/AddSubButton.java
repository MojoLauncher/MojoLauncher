package net.kdt.pojavlaunch.customcontrols.handleview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.annotation.Nullable;

import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlDrawer;
import net.kdt.pojavlaunch.customcontrols.buttons.ControlInterface;

import git.artdeell.mojo.R;

@SuppressLint("AppCompatCustomView")
public class AddSubButton extends Button implements ActionButtonInterface {
    private ControlInterface mCurrentlySelectedButton = null;

    public AddSubButton(Context context) {
        super(context);
        init();
    }

    public AddSubButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setText(R.string.customctrl_addsubbutton);
        setOnClickListener(this);
    }

    @Override
    public boolean shouldBeVisible() {
        return mCurrentlySelectedButton != null && mCurrentlySelectedButton instanceof ControlDrawer;
    }

    @Override
    public void setFollowedView(ControlInterface view) {
        mCurrentlySelectedButton = view;
    }

    @Override
    public void onClick() {
        if (mCurrentlySelectedButton instanceof ControlDrawer) {
            mCurrentlySelectedButton.getControlLayoutParent().addSubButton(
                    (ControlDrawer) mCurrentlySelectedButton,
                    new ControlData()
            );
        }
    }


}
