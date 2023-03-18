package org.apache.rocketmq.sdk.shade.common.utils;

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class IOTinyUtils {
    public static String toString(InputStream input, String encoding) throws IOException {
        return null == encoding ? toString(new InputStreamReader(input, "UTF-8")) : toString(new InputStreamReader(input, encoding));
    }

    public static String toString(Reader reader) throws IOException {
        CharArrayWriter sw = new CharArrayWriter();
        copy(reader, sw);
        return sw.toString();
    }

    public static long copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[4096];
        char c = 0;
        while (true) {
            int n = input.read(buffer);
            if (n < 0) {
                return c;
            }
            output.write(buffer, 0, n);
            c += (long) n;
        }
    }

    public static List<String> readLines(Reader input) throws IOException {
        BufferedReader reader = toBufferedReader(input);
        List<String> list = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (null == line) {
                return list;
            }
            list.add(line);
        }
    }

    private static BufferedReader toBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
    }

    public static void copyFile(String source, String target) throws IOException {
        File sf = new File(source);
        if (!sf.exists()) {
            throw new IllegalArgumentException("source file does not exist.");
        }
        File tf = new File(target);
        tf.getParentFile().mkdirs();
        if (tf.exists() || tf.createNewFile()) {
            FileChannel sc = null;
            FileChannel tc = null;
            try {
                tc = new FileOutputStream(tf).getChannel();
                sc = new FileInputStream(sf).getChannel();
                sc.transferTo(0, sc.size(), tc);
                if (null != sc) {
                    sc.close();
                }
                if (null != tc) {
                    tc.close();
                }
            } catch (Throwable th) {
                if (null != sc) {
                    sc.close();
                }
                if (null != tc) {
                    tc.close();
                }
                throw th;
            }
        } else {
            throw new RuntimeException("failed to create target file.");
        }
    }

    public static void delete(File fileOrDir) throws IOException {
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                cleanDirectory(fileOrDir);
            }
            fileOrDir.delete();
        }
    }

    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IllegalArgumentException(directory + " does not exist");
        } else if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        } else {
            File[] files = directory.listFiles();
            if (files == null) {
                throw new IOException("Failed to list contents of " + directory);
            }
            IOException exception = null;
            for (File file : files) {
                try {
                    delete(file);
                } catch (IOException ioe) {
                    exception = ioe;
                }
            }
            if (null != exception) {
                throw exception;
            }
        }
    }

    public static void writeStringToFile(File file, String data, String encoding) throws IOException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(data.getBytes(encoding));
            if (null != os) {
                os.close();
            }
        } catch (Throwable th) {
            if (null != os) {
                os.close();
            }
            throw th;
        }
    }
}
