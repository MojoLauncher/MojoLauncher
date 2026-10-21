package git.artdeell.mojo.utils.jre.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ClassVersionReader {
    public static int readClassMajorVersion(InputStream inputStream) throws IOException, ClassFormatException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int magic = dataInputStream.readInt();
        if(magic != 0xCAFEBABE) throw new ClassFormatException("Illegal class file magic");
        dataInputStream.skipBytes(2);
        return dataInputStream.readUnsignedShort();
    }

    public static int classMajorToVMMajor(int majorVersion) {
        if(majorVersion < 46) return 2; // there isn't even an arm64 port of jre 1.1 (or anything before 1.8 in fact)
        return majorVersion - 44;
    }
}
