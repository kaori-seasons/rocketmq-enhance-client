package org.apache.rocketmq.sdk.shade.common.acl.binary;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseNCodecInputStream extends FilterInputStream {
    private final BaseNCodec baseNCodec;
    private final boolean doEncode;
    private final byte[] singleByte = new byte[1];
    private final BaseNCodec.Context context = new BaseNCodec.Context();
    
    public BaseNCodecInputStream(InputStream in, BaseNCodec baseNCodec, boolean doEncode) {
        super(in);
        this.doEncode = doEncode;
        this.baseNCodec = baseNCodec;
    }

    @Override 
    public int available() throws IOException {
        return this.context.eof ? 0 : 1;
    }

    @Override 
    public synchronized void mark(int readLimit) {
    }

    @Override 
    public boolean markSupported() {
        return false;
    }

    @Override 
    public int read() throws IOException {
        int r = read(this.singleByte, 0, 1);
        while (r == 0) {
            r = read(this.singleByte, 0, 1);
        }
        if (r <= 0) {
            return -1;
        }
        byte b = this.singleByte[0];
        return b < 0 ? 256 + b : b;
    }

    @Override 
    public int read(byte[] b, int offset, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (offset < 0 || len < 0) {
            throw new IndexOutOfBoundsException();
        } else if (offset > b.length || offset + len > b.length) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        } else {
            int readLen = 0;
            while (readLen == 0) {
                if (!this.baseNCodec.hasData(this.context)) {
                    byte[] buf = new byte[this.doEncode ? 4096 : 8192];
                    int c = this.in.read(buf);
                    if (this.doEncode) {
                        this.baseNCodec.encode(buf, 0, c, this.context);
                    } else {
                        this.baseNCodec.decode(buf, 0, c, this.context);
                    }
                }
                readLen = this.baseNCodec.readResults(b, offset, len, this.context);
            }
            return readLen;
        }
    }

    @Override 
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("Negative skip length: " + n);
        } else {
            byte[] b = new byte[512];

            long todo;
            int len;
            for(todo = n; todo > 0L; todo -= (long)len) {
                len = (int)Math.min((long)b.length, todo);
                len = this.read(b, 0, len);
                if (len == -1) {
                    break;
                }
            }

            return n - todo;
        }
    }
}
