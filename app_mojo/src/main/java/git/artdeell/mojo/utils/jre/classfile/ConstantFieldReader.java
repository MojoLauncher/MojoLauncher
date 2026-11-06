package git.artdeell.mojo.utils.jre.classfile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ConstantFieldReader {

    private static final byte CONSTANT_Class = 	7;
    private static final byte CONSTANT_Fieldref = 	9;
    private static final byte CONSTANT_Methodref = 	10;
    private static final byte CONSTANT_InterfaceMethodref = 	11;
    private static final byte CONSTANT_String = 	8;
    private static final byte CONSTANT_Integer = 	3;
    private static final byte CONSTANT_Float = 	4;
    private static final byte CONSTANT_Long = 	5;
    private static final byte CONSTANT_Double = 	6;
    private static final byte CONSTANT_NameAndType = 	12;
    private static final byte CONSTANT_Utf8 = 	1;
    private static final byte CONSTANT_MethodHandle = 	15;
    private static final byte CONSTANT_MethodType = 	16;
    private static final byte CONSTANT_Dynamic = 	17;
    private static final byte CONSTANT_InvokeDynamic = 	18;
    private static final byte CONSTANT_Module = 	19;
    private static final byte CONSTANT_Package = 	20;

    private final HashMap<Integer, Object> mConstants = new HashMap<>();
    private final HashMap<String, Object> mConstantFields = new HashMap<>();

    /**
     * Read all field attributes and return the field's constant value, if any
     */
    private Object readFieldAttributes(DataInputStream dataInputStream, int fieldId) throws IOException, ClassFormatException {
        int attribCount = dataInputStream.readUnsignedShort();
        Object constantValue = null;
        for(int i = 0; i < attribCount; i++) {
            int name_index = dataInputStream.readUnsignedShort();
            int length = dataInputStream.readInt();
            Object attrName = mConstants.get(name_index);
            if(!(attrName instanceof String)) throw new ClassFormatException("Field "+fieldId+" attr "+i+" illegal name "+name_index);

            if("ConstantValue".equals(attrName) && length == 2) {
                constantValue = mConstants.get(dataInputStream.readUnsignedShort());
            }else {
                dataInputStream.skipBytes(length);
            }
        }
        return constantValue;
    }

    private void readFields(DataInputStream dataInputStream) throws IOException, ClassFormatException {
        int fieldCount = dataInputStream.readUnsignedShort();
        for(int i = 0; i < fieldCount; i++) {
            dataInputStream.skipBytes(2);
            int nameIdx = dataInputStream.readUnsignedShort();
            dataInputStream.skipBytes(2);
            Object fieldName = mConstants.get(nameIdx);
            Object constantValue = readFieldAttributes(dataInputStream, i);
            if(!(fieldName instanceof String)) throw new ClassFormatException("Field "+i+" illegal name "+nameIdx);
            mConstantFields.put((String) fieldName, constantValue);
        }
    }

    private void readConstantPool(DataInputStream dataInputStream) throws IOException, ClassFormatException {
        int poolCount = dataInputStream.readUnsignedShort();
        for(int i = 1; i < poolCount; i++) {
            int type = dataInputStream.readUnsignedByte();
            switch (type) {
                // Collect all constant numeric values
                case CONSTANT_Integer:
                    mConstants.put(i, dataInputStream.readInt());
                    break;
                case CONSTANT_Float:
                    mConstants.put(i, dataInputStream.readFloat());
                    break;
                case CONSTANT_Long:
                    mConstants.put(i, dataInputStream.readLong());
                    i++;
                    break;
                case CONSTANT_Double:
                    mConstants.put(i, dataInputStream.readDouble());
                    i++;
                    break;
                case CONSTANT_Utf8:
                    mConstants.put(i, dataInputStream.readUTF());
                    break;

                // As for the rest - don't care
                case CONSTANT_Class:
                case CONSTANT_String:
                case CONSTANT_MethodType:
                case CONSTANT_Module:
                case CONSTANT_Package:
                    dataInputStream.skipBytes(2);
                    break;
                case CONSTANT_MethodHandle:
                    dataInputStream.skipBytes(3);
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                case CONSTANT_NameAndType:
                case CONSTANT_Dynamic:
                case CONSTANT_InvokeDynamic:
                    dataInputStream.skipBytes(4);
                    break;
                default:
                    throw new ClassFormatException("Unknown type" + type);
            }
        }
    }

    public Object getFieldValue(String fieldName) {
        return mConstantFields.get(fieldName);
    }

    public ConstantFieldReader read(InputStream classSteam) throws IOException, ClassFormatException {
        mConstantFields.clear();
        DataInputStream dataInputStream = new DataInputStream(classSteam);
        int magic = dataInputStream.readInt();
        if(magic != 0xCAFEBABE) throw new ClassFormatException("Invalid class file magic");
        dataInputStream.skipBytes(2); // minor
        int major = dataInputStream.readUnsignedShort();

        if(major < 45 || major > 65) throw new ClassFormatException("Unsupported class file major version "+major);

        readConstantPool(dataInputStream);

        dataInputStream.skipBytes(6); // access_flags, this_class, super_class

        int interfacesCount = dataInputStream.readUnsignedShort();
        if(interfacesCount != 0) dataInputStream.skipBytes(2 * interfacesCount);

        readFields(dataInputStream);
        mConstants.clear();
        return this;
    }

}
