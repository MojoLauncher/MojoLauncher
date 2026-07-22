package net.kdt.pojavlaunch.modloaders.modmanager.dumbzip;

import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipFile;

import me.andreasmelone.abstractzip.IZipEntry;
import me.andreasmelone.abstractzip.IZipFile;

public class DumbZipFile implements IZipFile {

    private final HashMap<String,DumbZipEntry> mEntries;
    private final String mComment;
    private final File mZipFile;

    public DumbZipFile(File file) throws IOException {
        mZipFile = file;
        try(RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            ZipCentralDirectory zipCentralDirectory = findEOCD(randomAccessFile);
            mComment = zipCentralDirectory.mComment;
            mEntries = new HashMap<>(zipCentralDirectory.mNumEntries);
            for(int i = 0; i < zipCentralDirectory.mNumEntries; i++) {
                processEntry(zipCentralDirectory.mCentralDirectroy);
            }
        }
    }

    private ByteBuffer readHeader(RandomAccessFile randomAccessFile, int size) throws IOException {
        byte[] header = new byte[size];
        randomAccessFile.read(header);
        return ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
    }

    private ByteBuffer readHeader(InputStream inputStream, int size) throws IOException {
        byte[] header = new byte[size];
        if(inputStream.read(header) != size) throw new IOException("Failed to fully read header");
        return ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
    }

    private String readString(RandomAccessFile randomAccessFile, int size) throws IOException{
        byte[] bytes = new byte[size];
        randomAccessFile.read(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String readString(ByteBuffer byteBuffer, int size) {
        byte[] bytes = new byte[size];
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void processEntry(ByteBuffer directoryBuffer) throws IOException {
        // Header buffer to read zip fields explicitly
        ByteBuffer headerBuffer = directoryBuffer.slice().order(ByteOrder.LITTLE_ENDIAN);
        if(headerBuffer.getInt(0) != 0x02014b50) throw new IOException("Invalid CDFH magic");

        int nameLength = Short.toUnsignedInt(headerBuffer.getShort(28));
        int extraLength = Short.toUnsignedInt(headerBuffer.getShort(30));
        int commentLength = Short.toUnsignedInt(headerBuffer.getShort(32));

        // Advance directory buffer past the header
        directoryBuffer.position(directoryBuffer.position() + 46);
        String fileName = readString(directoryBuffer, nameLength);
        if(fileName.endsWith(".class")) {
            // Reject entry (since we dgaf) by skipping the rest of it
            int skipAmount = extraLength + commentLength;
            // Why the duplicate position call for extra here and below?
            // ByteBuffer.position takes a non-insignificant amount of time, surprisingly
            if(skipAmount != 0) directoryBuffer.position(extraLength + skipAmount);
            return;
        }
        // Skip extra field
        if(extraLength != 0) directoryBuffer.position(directoryBuffer.position() + extraLength);
        String comment = "";
        if(commentLength != 0) comment = readString(directoryBuffer, commentLength);

        short compressionMethod = headerBuffer.getShort(10);
        long crc32 = Integer.toUnsignedLong(headerBuffer.getInt(16));
        long compressedSize = Integer.toUnsignedLong(headerBuffer.getInt(20));
        long uncompressedSize = Integer.toUnsignedLong(headerBuffer.getInt(24));
        long offset = Integer.toUnsignedLong(headerBuffer.getInt(42));

        mEntries.put(fileName, new DumbZipEntry(fileName, 0, uncompressedSize, compressedSize, crc32, comment, offset, compressionMethod));
    }

    private ZipCentralDirectory findEOCD(RandomAccessFile randomAccessFile) throws IOException{
        long offset = randomAccessFile.length() - 22;
        ZipCentralDirectory zipCentralDirectory;
        while ((zipCentralDirectory = checkEOCD(randomAccessFile, offset)) == null && offset > 0) {
            offset--;
        }
        if(zipCentralDirectory == null) throw new IOException("ZIP central directory not found");
        return zipCentralDirectory;
    }

    private ZipCentralDirectory checkEOCD(RandomAccessFile randomAccessFile, long offset) throws IOException{
        randomAccessFile.seek(offset);
        ByteBuffer headerBuffer = readHeader(randomAccessFile, 22);
        if(headerBuffer.getInt(0) != 0x06054b50) {
            Log.i("DZF", "Detect failed: bad magic");
            return null;
        }

        int nDisk = headerBuffer.getShort(4);
        int cdDisk = headerBuffer.getShort(6);
        int nRecords = Short.toUnsignedInt(headerBuffer.getShort(8));
        int nRecordsOnDisk = Short.toUnsignedInt(headerBuffer.getShort(10));
        if(nDisk != cdDisk || nRecords != nRecordsOnDisk) {
            Log.i("DZF", "Detect failed: unexpected multipart");
            return null; // multi disk zips aren't supported
        }

        long centralDirectorySize = Integer.toUnsignedLong(headerBuffer.getInt(12));
        if(centralDirectorySize > offset) {
            Log.i("DZF", "Detect failed: bad offset");
            return null;
        }

        // Check if the CDFH is present
        randomAccessFile.seek(offset - centralDirectorySize);
        if(randomAccessFile.readInt() != 0x504B0102) {
            Log.i("DZF", "Detect failed: CDFH missing at CD start");
            return null;
        }

        randomAccessFile.seek(offset - centralDirectorySize);
        ByteBuffer centralDirectory = readHeader(randomAccessFile, (int) centralDirectorySize);

        String comment = "";
        int commentLength = Short.toUnsignedInt(headerBuffer.getShort(20));
        if(commentLength != 0) {
            randomAccessFile.seek(offset + 22);
            comment = readString(randomAccessFile, commentLength);
        }
        return new ZipCentralDirectory(nRecords, centralDirectory, comment);
    }

    @Override
    public @Nullable InputStream openEntry(@NotNull IZipEntry iZipEntry) throws IOException {
        if(!(iZipEntry instanceof DumbZipEntry)) throw new RuntimeException("Unexpected ZIP entry type");
        DumbZipEntry dumbZipEntry = (DumbZipEntry) iZipEntry;
        FileInputStream fileInputStream = new FileInputStream(mZipFile);
        try {
            if(fileInputStream.skip(dumbZipEntry.mDataOffset) != dumbZipEntry.mDataOffset) throw new IOException("Failed to seek to destination file");

            ByteBuffer localFileHeader = readHeader(fileInputStream, 30);
            if(localFileHeader.getInt(0) != 0x04034B50) throw new IOException("Bad local ZIP magic");
            int nameLength = Short.toUnsignedInt(localFileHeader.getShort(26));
            int fieldLength = Short.toUnsignedInt(localFileHeader.getShort(28));
            int skipAmount = nameLength + fieldLength;

            if(fileInputStream.skip(skipAmount) != skipAmount) throw new IOException("Failed to seek to compressed data");

            switch (dumbZipEntry.mCompressionMethod) {
                case 0:
                    return new LengthLimitInputStream(fileInputStream, dumbZipEntry.getSize());
                case 8:
                    return new InflaterInputStream(new LengthLimitInputStream(fileInputStream, dumbZipEntry.getCompressedSize()), new Inflater(true));
                default:
                    throw new IOException("Unknown compression method "+dumbZipEntry.mCompressionMethod);
            }
        }catch (IOException e) {
            fileInputStream.close();
            throw e;
        }
    }

    @Nullable
    @Override
    public IZipEntry findEntry(@NotNull String s) {
        return mEntries.get(s);
    }

    @Nullable
    @Override
    public String getComment() {
        return mComment;
    }

    @Override
    public void close() throws IOException {

    }

    private static class ZipCentralDirectory {
        public final int mNumEntries;
        public final ByteBuffer mCentralDirectroy;
        public final String mComment;

        private ZipCentralDirectory(int mNumEntries, ByteBuffer mCentralDirectroy, String mComment) {
            this.mNumEntries = mNumEntries;
            this.mCentralDirectroy = mCentralDirectroy;
            this.mComment = mComment;
        }
    }
}
