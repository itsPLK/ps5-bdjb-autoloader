package org.ps5jb.client.payloads.autoloader;

import org.ps5jb.sdk.io.FileInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class FileIO {
    private FileIO() {
    }

    public static byte[] readAllBytes(File file) throws IOException {
        FileInputStream in = new FileInputStream(file.getAbsolutePath());
        try {
            return readAllBytes(in);
        } finally {
            in.close();
        }
    }

    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = in.read(chunk)) >= 0) {
            if (read == 0) {
                continue;
            }
            out.write(chunk, 0, read);
        }
        return out.toByteArray();
    }
}
