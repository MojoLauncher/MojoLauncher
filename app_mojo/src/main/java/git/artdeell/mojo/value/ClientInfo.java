package git.artdeell.mojo.value;

import androidx.annotation.Keep;

@Keep
public class ClientInfo {
	public String sha1;
	public int size = -1;
	public String url;
}
