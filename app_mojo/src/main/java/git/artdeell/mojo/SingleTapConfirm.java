package git.artdeell.mojo;

import android.view.*;
import android.view.GestureDetector.*;

public class SingleTapConfirm extends SimpleOnGestureListener {
	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return true;
	}
}
