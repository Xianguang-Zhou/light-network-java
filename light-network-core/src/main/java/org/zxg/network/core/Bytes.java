/*
 * Copyright (c) 2020, Xianguang Zhou <xianguang.zhou@outlook.com>. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.zxg.network.core;

import java.nio.ByteBuffer;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class Bytes implements Cloneable {

    private final ByteBuffer buffer;

    public static void copy(Bytes src, int srcIndex, Bytes dest, int destIndex, int length) {
        final ByteBuffer srcBuffer = src.buffer;
        final ByteBuffer destBuffer = dest.buffer;
        final int srcLimit = srcBuffer.limit();
        final int srcPosition = srcBuffer.position();
        final int destPosition = destBuffer.position();
        try {
            final int newSrcPosition = srcPosition + srcIndex;
            srcBuffer.limit(newSrcPosition + length);
            srcBuffer.position(newSrcPosition);
            destBuffer.position(destPosition + destIndex);
            destBuffer.put(srcBuffer);
        } finally {
            srcBuffer.limit(srcLimit);
            srcBuffer.position(srcPosition);
            destBuffer.position(destPosition);
        }
    }

    public static Bytes copyOfRange(Bytes original, int from, int to, boolean direct) {
        final int newLength = to - from;
        if (newLength < 0) {
            throw new IllegalArgumentException(from + " > " + to);
        }
        final Bytes newBytes = new Bytes(newLength, direct);
        try {
            copy(original, from, newBytes, 0, Math.min(original.length() - from, newLength));
        } catch (Exception exc) {
            newBytes.free();
            throw exc;
        }
        return newBytes;
    }

    public Bytes(int length, boolean direct) {
        if (direct) {
            this.buffer = ByteBuffer.allocateDirect(length);
        } else {
            this.buffer = ByteBuffer.allocate(length);
        }
    }

    public Bytes(byte[] elements, boolean direct) {
        this(elements.length, direct);
        this.buffer.put(elements);
        this.buffer.flip();
    }

    public Bytes(ByteBuffer buffer, boolean direct) {
        this(buffer.remaining(), direct);
        final int originalPosition = buffer.position();
        try {
            this.buffer.put(buffer);
            this.buffer.flip();
        } finally {
            buffer.position(originalPosition);
        }
    }

    public Bytes(Bytes other, boolean direct) {
        this(other.buffer, direct);
    }

    protected int bufferIndex(int index) {
        return buffer.position() + index;
    }

    public byte get(int index) {
        return buffer.get(bufferIndex(index));
    }

    public void set(int index, byte value) {
        buffer.put(bufferIndex(index), value);
    }

    public void get(int begin, byte[] elements, int offset, int length) {
        final int originalPosition = buffer.position();
        try {
            buffer.position(bufferIndex(begin));
            buffer.get(elements, offset, length);
        } finally {
            buffer.position(originalPosition);
        }
    }

    public byte[] get(int begin, int end) {
        final byte[] elements = new byte[end - begin];
        get(begin, elements, 0, elements.length);
        return elements;
    }

    public byte[] get() {
        return get(0, length());
    }

    public void set(int begin, byte[] elements, int offset, int length) {
        final int originalPosition = buffer.position();
        try {
            buffer.position(bufferIndex(begin));
            buffer.put(elements, offset, length);
        } finally {
            buffer.position(originalPosition);
        }
    }

    public void set(int begin, byte[] elements, int offset) {
        set(begin, elements, offset, elements.length - offset);
    }

    public void set(byte[] elements, int offset, int length) {
        set(0, elements, offset, length);
    }

    public void set(byte[] elements, int offset) {
        set(elements, offset, elements.length - offset);
    }

    public void set(int begin, byte[] elements) {
        set(begin, elements, 0, elements.length);
    }

    public void set(byte[] elements) {
        set(0, elements);
    }

    public void removeLeft(int n) {
        buffer.position(buffer.position() + n);
    }

    public void removeRight(int n) {
        buffer.limit(buffer.limit() - n);
    }

    public void content(byte[] content) {
        buffer.clear();
        try {
            buffer.put(content);
        } finally {
            buffer.flip();
        }
    }

    public int length() {
        return buffer.remaining();
    }

    public int size() {
        return buffer.capacity();
    }

    @Override
    public Bytes clone() {
        final Bytes other = new Bytes(size(), isDirect());
        final int length = length();
        copy(this, 0, other, 0, length);
        other.buffer.limit(length);
        return other;
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Bytes)) {
            return false;
        }
        final Bytes other = (Bytes) obj;
        return this.buffer.equals(other.buffer);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (length() > 0) {
            final int last = buffer.limit() - 1;
            for (int index = buffer.position(); index < last; ++index) {
                builder.append(buffer.get(index));
                builder.append(", ");
            }
            builder.append(buffer.get(last));
        }
        builder.append(']');
        return builder.toString();
    }

    public boolean isDirect() {
        return buffer.isDirect();
    }

    public ByteBuffer buffer() {
        return buffer;
    }
}
