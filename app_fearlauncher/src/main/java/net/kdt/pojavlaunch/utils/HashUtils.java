package net.kdt.pojavlaunch.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HashUtils {
    @RequiresApi(26)
    private static byte[] fileHashNio(MessageDigest messageDigest, Path p) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(65535);
        try(SeekableByteChannel channel = Files.newByteChannel(p, StandardOpenOption.READ)) {
            while(true) {
                buffer.rewind();
                if(channel.read(buffer) == -1) break;
                buffer.flip();
                messageDigest.update(buffer);
            }
        }
        return messageDigest.digest();
    }

    private static byte[] fileHashLegacy(MessageDigest messageDigest, File f) throws IOException {
        byte[] sha1Buffer = new byte[65535];
        try (FileInputStream stream = new FileInputStream(f)){
            int readLen;
            while((readLen = stream.read(sha1Buffer)) != -1) {
                messageDigest.update(sha1Buffer, 0, readLen);
            }
        }
        return messageDigest.digest();
    }

    public static byte[] fileHash(MessageDigest messageDigest, File f) throws IOException {
        if (SDK_INT >= Build.VERSION_CODES.O) return fileHashNio(messageDigest, f.toPath());
        else return fileHashLegacy(messageDigest, f);
    }

    public static boolean compareSHA1(File f, String sourceSHA) throws IOException{
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] wantedBytes = Hex.decodeHex(sourceSHA.toCharArray());
            byte[] localFileBytes = fileHash(messageDigest, f);
            return Arrays.equals(localFileBytes, wantedBytes);
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("WTF? SHA-1 digest missing!", e);
        }catch (DecoderException e) {
            throw new IOException("Bad SHA-1 hash: "+sourceSHA+" for file "+f.getName());
        }
    }
}
