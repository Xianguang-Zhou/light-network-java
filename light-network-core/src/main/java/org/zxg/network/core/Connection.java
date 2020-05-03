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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class Connection implements Closeable {

    private final AsynchronousSocketChannel channel;

    public Connection(AsynchronousSocketChannel channel) {
        this.channel = channel;
    }

    public CompletableFuture<Bytes> read(Bytes bytes) {
        return read(bytes, 0L, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Bytes> read(Bytes bytes, long timeout, TimeUnit unit) {
        return readExactly(bytes, -1, timeout, unit);
    }

    public CompletableFuture<Bytes> readExactly(Bytes bytes, int n) {
        return readExactly(bytes, n, 0L, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Bytes> readExactly(Bytes bytes, int n, long timeout,
                                                TimeUnit unit) {
        final CompletableFuture<Bytes> future = new CompletableFuture<>();
        final ByteBuffer buffer = bytes.buffer();
        buffer.clear();
        if (n < 0) {
            channel.read(buffer, timeout, unit, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    buffer.flip();
                    future.complete(bytes);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    buffer.flip();
                    future.completeExceptionally(exc);
                }
            });
        } else {
            buffer.limit(n);
            channel.read(buffer, timeout, unit, null, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    if (buffer.hasRemaining()) {
                        if (-1 != result) {
                            channel.read(buffer, timeout, unit, null, this);
                        } else {
                            buffer.flip();
                            future.completeExceptionally(new IncompleteReadException());
                        }
                    } else {
                        buffer.flip();
                        future.complete(bytes);
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    buffer.flip();
                    future.completeExceptionally(exc);
                }
            });
        }
        return future;
    }

    public CompletableFuture<Bytes> readLine(Bytes bytes) {
        return readLine(bytes, 0L, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Bytes> readLine(Bytes bytes, long timeout,
                                             TimeUnit unit) {
        final CompletableFuture<Bytes> future = new CompletableFuture<>();
        final ByteBuffer buffer = bytes.buffer();
        buffer.clear();
        buffer.limit(1);
        channel.read(buffer, timeout, unit, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if (-1 == result) {
                    buffer.flip();
                    future.complete(bytes);
                    return;
                }
                if (buffer.hasRemaining()) {
                    channel.read(buffer, timeout, unit, null, this);
                } else {
                    if (10 == buffer.get(buffer.position() - 1)) {
                        buffer.flip();
                        future.complete(bytes);
                    } else {
                        buffer.limit(buffer.position() + 1);
                        channel.read(buffer, timeout, unit, null, this);
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                buffer.flip();
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    public CompletableFuture<Bytes> write(Bytes bytes) {
        return write(bytes, 0L, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Bytes> write(Bytes bytes, long timeout,
                                          TimeUnit unit) {
        final CompletableFuture<Bytes> future = new CompletableFuture<>();
        final ByteBuffer buffer = bytes.buffer();
        channel.write(buffer, timeout, unit, null, new CompletionHandler<Integer, Object>() {
            @Override
            public void completed(Integer result, Object attachment) {
                if (buffer.hasRemaining()) {
                    channel.write(buffer, timeout, unit, null, this);
                } else {
                    future.complete(bytes);
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    public CompletableFuture<Bytes> writeRange(Bytes bytes, int from, int to) {
        return writeRange(bytes, from, to, 0L, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Bytes> writeRange(Bytes bytes, int from, int to, long timeout,
                                               TimeUnit unit) {
        final CompletableFuture<Bytes> future = new CompletableFuture<>();
        final ByteBuffer buffer = bytes.buffer();
        final int limit = buffer.limit();
        final int position = buffer.position();
        buffer.limit(buffer.position() + to);
        buffer.position(buffer.position() + from);
        write(bytes, timeout, unit).handle((_bytes, exc) -> {
            buffer.limit(limit);
            buffer.position(position);
            if (exc != null) {
                future.completeExceptionally(exc);
            } else {
                future.complete(bytes);
            }
            return null;
        });
        return future;
    }

    @Override
    public void close() throws IOException {
        if (channel.isOpen()) {
            channel.close();
        }
    }

    public AsynchronousSocketChannel channel() {
        return channel;
    }
}
