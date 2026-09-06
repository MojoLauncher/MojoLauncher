package net.kdt.pojavlaunch.modloaders.modmanager.dumbzip;

import java.io.File;
import java.io.IOException;

import me.andreasmelone.abstractzip.IZipFile;
import me.andreasmelone.abstractzip.IZipFileFactory;

public class DumbZipFactory implements IZipFileFactory {
    public static void use() {
        IZipFileFactory.Provider.setFactoryImpl(new DumbZipFactory());
    }
    @Override
    public IZipFile create(File file) throws IOException {
        return new DumbZipFile(file);
    }
}
