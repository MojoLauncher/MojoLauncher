package git.artdeell.mojo.modding.modplatforms.imagecache;

import android.graphics.Bitmap;

/**
 * ModIconCache will call your view back when the image becomes available with this interface
 */
public interface ImageReceiver {
    void onImageAvailable(Bitmap image);
}
