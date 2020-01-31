// 
// Decompiled by Procyon v0.5.36
// 

package tech.bluemail.platform.utils;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.File;

public class Compressor
{
    private static final int BUFFER_SIZE = 4096;
    
    public static void unzip(final String zipFilePath, final String destDirectory) throws IOException {
        final File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
                final String filePath = destDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                }
                else {
                    final File dir = new File(filePath);
                    dir.mkdir();
                }
                zipIn.closeEntry();
            }
        }
    }
    
    private static void extractFile(final ZipInputStream zipIn, final String filePath) throws IOException {
        try (final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            final byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }
}
