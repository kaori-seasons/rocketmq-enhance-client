package org.apache.rocketmq.sdk.shade.common.acl.binary;

import org.apache.rocketmq.sdk.shade.common.acl.common.StringUtils;
import org.springframework.beans.PropertyAccessor;

public class Base32 extends BaseNCodec {
    private static final int BITS_PER_ENCODED_BYTE = 5;
    private static final int BYTES_PER_ENCODED_BLOCK = 8;
    private static final int BYTES_PER_UNENCODED_BLOCK = 5;
    private static final byte[] CHUNK_SEPARATOR = {13, 10};
    private static final byte[] DECODE_TABLE = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25};
    private static final byte[] ENCODE_TABLE = {65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 50, 51, 52, 53, 54, 55};
    private static final byte[] HEX_DECODE_TABLE = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1, -1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
    private static final byte[] HEX_ENCODE_TABLE = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86};
    private static final int MASK_5BITS = 31;
    private final int decodeSize;
    private final byte[] decodeTable;
    private final int encodeSize;
    private final byte[] encodeTable;
    private final byte[] lineSeparator;

    public Base32() {
        this(false);
    }

    public Base32(boolean useHex) {
        this(0, null, useHex);
    }

    public Base32(int lineLength) {
        this(lineLength, CHUNK_SEPARATOR);
    }

    public Base32(int lineLength, byte[] lineSeparator) {
        this(lineLength, lineSeparator, false);
    }

    public Base32(int lineLength, byte[] lineSeparator, boolean useHex) {
        super(5, 8, lineLength, lineSeparator == null ? 0 : lineSeparator.length);
        if (useHex) {
            this.encodeTable = HEX_ENCODE_TABLE;
            this.decodeTable = HEX_DECODE_TABLE;
        } else {
            this.encodeTable = ENCODE_TABLE;
            this.decodeTable = DECODE_TABLE;
        }
        if (lineLength <= 0) {
            this.encodeSize = 8;
            this.lineSeparator = null;
        } else if (lineSeparator == null) {
            throw new IllegalArgumentException("lineLength " + lineLength + " > 0, but lineSeparator is null");
        } else if (containsAlphabetOrPad(lineSeparator)) {
            throw new IllegalArgumentException("lineSeparator must not contain Base32 characters: [" + StringUtils.newStringUtf8(lineSeparator) + PropertyAccessor.PROPERTY_KEY_SUFFIX);
        } else {
            this.encodeSize = 8 + lineSeparator.length;
            this.lineSeparator = new byte[lineSeparator.length];
            System.arraycopy(lineSeparator, 0, this.lineSeparator, 0, lineSeparator.length);
        }
        this.decodeSize = this.encodeSize - 1;
    }

    @Override
    public void decode(byte[] in, int inPos, int inAvail, BaseNCodec.Context context) {
        byte b;
        if (!context.eof) {
            if (inAvail < 0) {
                context.eof = true;
            }
            int i = 0;
            while (true) {
                if (i >= inAvail) {
                    break;
                }
                inPos++;
                byte b2 = in[inPos];
                if (b2 == 61) {
                    context.eof = true;
                    break;
                }
                byte[] buffer = ensureBufferSize(this.decodeSize, context);
                if (b2 >= 0 && b2 < this.decodeTable.length && (b = this.decodeTable[b2]) >= 0) {
                    context.modulus = (context.modulus + 1) % 8;
                    context.lbitWorkArea = (context.lbitWorkArea << 5) + ((long) b);
                    if (context.modulus == 0) {
                        int i2 = context.pos;
                        context.pos = i2 + 1;
                        buffer[i2] = (byte) ((int) ((context.lbitWorkArea >> 32) & 255));
                        int i3 = context.pos;
                        context.pos = i3 + 1;
                        buffer[i3] = (byte) ((int) ((context.lbitWorkArea >> 24) & 255));
                        int i4 = context.pos;
                        context.pos = i4 + 1;
                        buffer[i4] = (byte) ((int) ((context.lbitWorkArea >> 16) & 255));
                        int i5 = context.pos;
                        context.pos = i5 + 1;
                        buffer[i5] = (byte) ((int) ((context.lbitWorkArea >> 8) & 255));
                        int i6 = context.pos;
                        context.pos = i6 + 1;
                        buffer[i6] = (byte) ((int) (context.lbitWorkArea & 255));
                    }
                }
                i++;
            }
            if (context.eof && context.modulus >= 2) {
                byte[] buffer2 = ensureBufferSize(this.decodeSize, context);
                switch (context.modulus) {
                    case 2:
                        int i7 = context.pos;
                        context.pos = i7 + 1;
                        buffer2[i7] = (byte) ((int) ((context.lbitWorkArea >> 2) & 255));
                        return;
                    case 3:
                        int i8 = context.pos;
                        context.pos = i8 + 1;
                        buffer2[i8] = (byte) ((int) ((context.lbitWorkArea >> 7) & 255));
                        return;
                    case 4:
                        context.lbitWorkArea >>= 4;
                        int i9 = context.pos;
                        context.pos = i9 + 1;
                        buffer2[i9] = (byte) ((int) ((context.lbitWorkArea >> 8) & 255));
                        int i10 = context.pos;
                        context.pos = i10 + 1;
                        buffer2[i10] = (byte) ((int) (context.lbitWorkArea & 255));
                        return;
                    case 5:
                        context.lbitWorkArea >>= 1;
                        int i11 = context.pos;
                        context.pos = i11 + 1;
                        buffer2[i11] = (byte) ((int) ((context.lbitWorkArea >> 16) & 255));
                        int i12 = context.pos;
                        context.pos = i12 + 1;
                        buffer2[i12] = (byte) ((int) ((context.lbitWorkArea >> 8) & 255));
                        int i13 = context.pos;
                        context.pos = i13 + 1;
                        buffer2[i13] = (byte) ((int) (context.lbitWorkArea & 255));
                        return;
                    case 6:
                        context.lbitWorkArea >>= 6;
                        int i14 = context.pos;
                        context.pos = i14 + 1;
                        buffer2[i14] = (byte) ((int) ((context.lbitWorkArea >> 16) & 255));
                        int i15 = context.pos;
                        context.pos = i15 + 1;
                        buffer2[i15] = (byte) ((int) ((context.lbitWorkArea >> 8) & 255));
                        int i16 = context.pos;
                        context.pos = i16 + 1;
                        buffer2[i16] = (byte) ((int) (context.lbitWorkArea & 255));
                        return;
                    case 7:
                        context.lbitWorkArea >>= 3;
                        int i17 = context.pos;
                        context.pos = i17 + 1;
                        buffer2[i17] = (byte) ((int) ((context.lbitWorkArea >> 24) & 255));
                        int i18 = context.pos;
                        context.pos = i18 + 1;
                        buffer2[i18] = (byte) ((int) ((context.lbitWorkArea >> 16) & 255));
                        int i19 = context.pos;
                        context.pos = i19 + 1;
                        buffer2[i19] = (byte) ((int) ((context.lbitWorkArea >> 8) & 255));
                        int i20 = context.pos;
                        context.pos = i20 + 1;
                        buffer2[i20] = (byte) ((int) (context.lbitWorkArea & 255));
                        return;
                    default:
                        throw new IllegalStateException("Impossible modulus " + context.modulus);
                }
            }
        }
    }

    @Override
    public void encode(byte[] in, int inPos, int inAvail, BaseNCodec.Context context) {
        if (!context.eof) {
            if (inAvail < 0) {
                context.eof = true;
                if (!(0 == context.modulus && this.lineLength == 0)) {
                    byte[] buffer = ensureBufferSize(this.encodeSize, context);
                    int savedPos = context.pos;
                    switch (context.modulus) {
                        case 0:
                            break;
                        case 1:
                            int i = context.pos;
                            context.pos = i + 1;
                            buffer[i] = this.encodeTable[((int) (context.lbitWorkArea >> 3)) & 31];
                            int i2 = context.pos;
                            context.pos = i2 + 1;
                            buffer[i2] = this.encodeTable[((int) (context.lbitWorkArea << 2)) & 31];
                            int i3 = context.pos;
                            context.pos = i3 + 1;
                            buffer[i3] = 61;
                            int i4 = context.pos;
                            context.pos = i4 + 1;
                            buffer[i4] = 61;
                            int i5 = context.pos;
                            context.pos = i5 + 1;
                            buffer[i5] = 61;
                            int i6 = context.pos;
                            context.pos = i6 + 1;
                            buffer[i6] = 61;
                            int i7 = context.pos;
                            context.pos = i7 + 1;
                            buffer[i7] = 61;
                            int i8 = context.pos;
                            context.pos = i8 + 1;
                            buffer[i8] = 61;
                            break;
                        case 2:
                            int i9 = context.pos;
                            context.pos = i9 + 1;
                            buffer[i9] = this.encodeTable[((int) (context.lbitWorkArea >> 11)) & 31];
                            int i10 = context.pos;
                            context.pos = i10 + 1;
                            buffer[i10] = this.encodeTable[((int) (context.lbitWorkArea >> 6)) & 31];
                            int i11 = context.pos;
                            context.pos = i11 + 1;
                            buffer[i11] = this.encodeTable[((int) (context.lbitWorkArea >> 1)) & 31];
                            int i12 = context.pos;
                            context.pos = i12 + 1;
                            buffer[i12] = this.encodeTable[((int) (context.lbitWorkArea << 4)) & 31];
                            int i13 = context.pos;
                            context.pos = i13 + 1;
                            buffer[i13] = 61;
                            int i14 = context.pos;
                            context.pos = i14 + 1;
                            buffer[i14] = 61;
                            int i15 = context.pos;
                            context.pos = i15 + 1;
                            buffer[i15] = 61;
                            int i16 = context.pos;
                            context.pos = i16 + 1;
                            buffer[i16] = 61;
                            break;
                        case 3:
                            int i17 = context.pos;
                            context.pos = i17 + 1;
                            buffer[i17] = this.encodeTable[((int) (context.lbitWorkArea >> 19)) & 31];
                            int i18 = context.pos;
                            context.pos = i18 + 1;
                            buffer[i18] = this.encodeTable[((int) (context.lbitWorkArea >> 14)) & 31];
                            int i19 = context.pos;
                            context.pos = i19 + 1;
                            buffer[i19] = this.encodeTable[((int) (context.lbitWorkArea >> 9)) & 31];
                            int i20 = context.pos;
                            context.pos = i20 + 1;
                            buffer[i20] = this.encodeTable[((int) (context.lbitWorkArea >> 4)) & 31];
                            int i21 = context.pos;
                            context.pos = i21 + 1;
                            buffer[i21] = this.encodeTable[((int) (context.lbitWorkArea << 1)) & 31];
                            int i22 = context.pos;
                            context.pos = i22 + 1;
                            buffer[i22] = 61;
                            int i23 = context.pos;
                            context.pos = i23 + 1;
                            buffer[i23] = 61;
                            int i24 = context.pos;
                            context.pos = i24 + 1;
                            buffer[i24] = 61;
                            break;
                        case 4:
                            int i25 = context.pos;
                            context.pos = i25 + 1;
                            buffer[i25] = this.encodeTable[((int) (context.lbitWorkArea >> 27)) & 31];
                            int i26 = context.pos;
                            context.pos = i26 + 1;
                            buffer[i26] = this.encodeTable[((int) (context.lbitWorkArea >> 22)) & 31];
                            int i27 = context.pos;
                            context.pos = i27 + 1;
                            buffer[i27] = this.encodeTable[((int) (context.lbitWorkArea >> 17)) & 31];
                            int i28 = context.pos;
                            context.pos = i28 + 1;
                            buffer[i28] = this.encodeTable[((int) (context.lbitWorkArea >> 12)) & 31];
                            int i29 = context.pos;
                            context.pos = i29 + 1;
                            buffer[i29] = this.encodeTable[((int) (context.lbitWorkArea >> 7)) & 31];
                            int i30 = context.pos;
                            context.pos = i30 + 1;
                            buffer[i30] = this.encodeTable[((int) (context.lbitWorkArea >> 2)) & 31];
                            int i31 = context.pos;
                            context.pos = i31 + 1;
                            buffer[i31] = this.encodeTable[((int) (context.lbitWorkArea << 3)) & 31];
                            int i32 = context.pos;
                            context.pos = i32 + 1;
                            buffer[i32] = 61;
                            break;
                        default:
                            throw new IllegalStateException("Impossible modulus " + context.modulus);
                    }
                    context.currentLinePos += context.pos - savedPos;
                    if (this.lineLength > 0 && context.currentLinePos > 0) {
                        System.arraycopy(this.lineSeparator, 0, buffer, context.pos, this.lineSeparator.length);
                        context.pos += this.lineSeparator.length;
                        return;
                    }
                    return;
                }
                return;
            }
            for (int i33 = 0; i33 < inAvail; i33++) {
                byte[] buffer2 = ensureBufferSize(this.encodeSize, context);
                context.modulus = (context.modulus + 1) % 5;
                inPos++;
                int b = in[inPos];
                if (b < 0) {
                    b += 256;
                }
                context.lbitWorkArea = (context.lbitWorkArea << 8) + ((long) b);
                if (0 == context.modulus) {
                    int i34 = context.pos;
                    context.pos = i34 + 1;
                    buffer2[i34] = this.encodeTable[((int) (context.lbitWorkArea >> 35)) & 31];
                    int i35 = context.pos;
                    context.pos = i35 + 1;
                    buffer2[i35] = this.encodeTable[((int) (context.lbitWorkArea >> 30)) & 31];
                    int i36 = context.pos;
                    context.pos = i36 + 1;
                    buffer2[i36] = this.encodeTable[((int) (context.lbitWorkArea >> 25)) & 31];
                    int i37 = context.pos;
                    context.pos = i37 + 1;
                    buffer2[i37] = this.encodeTable[((int) (context.lbitWorkArea >> 20)) & 31];
                    int i38 = context.pos;
                    context.pos = i38 + 1;
                    buffer2[i38] = this.encodeTable[((int) (context.lbitWorkArea >> 15)) & 31];
                    int i39 = context.pos;
                    context.pos = i39 + 1;
                    buffer2[i39] = this.encodeTable[((int) (context.lbitWorkArea >> 10)) & 31];
                    int i40 = context.pos;
                    context.pos = i40 + 1;
                    buffer2[i40] = this.encodeTable[((int) (context.lbitWorkArea >> 5)) & 31];
                    int i41 = context.pos;
                    context.pos = i41 + 1;
                    buffer2[i41] = this.encodeTable[((int) context.lbitWorkArea) & 31];
                    context.currentLinePos += 8;
                    if (this.lineLength > 0 && this.lineLength <= context.currentLinePos) {
                        System.arraycopy(this.lineSeparator, 0, buffer2, context.pos, this.lineSeparator.length);
                        context.pos += this.lineSeparator.length;
                        context.currentLinePos = 0;
                    }
                }
            }
        }
    }

    @Override
    public boolean isInAlphabet(byte octet) {
        return octet >= 0 && octet < this.decodeTable.length && this.decodeTable[octet] != -1;
    }
}
