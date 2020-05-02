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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class Server implements Closeable {

    private static final Logger defaultLogger = LoggerFactory.getLogger(Server.class);

    private final AsynchronousServerSocketChannel channel;
    private final Logger logger;

    public Server(EventLoop loop) throws IOException {
        this(loop, defaultLogger);
    }

    public Server(EventLoop loop, Logger logger) throws IOException {
        this.channel = AsynchronousServerSocketChannel.open(loop.channelGroup());
        this.logger = logger;
    }

    public void bind(SocketAddress local) throws IOException {
        bind(local, 0);
    }

    public void bind(SocketAddress local, int backlog) throws IOException {
        this.channel.bind(local, backlog);
    }

    public CompletableFuture<Void> serve(Function<Connection, CompletionStage<?>> handler) {
        return serve(new Handler(handler));
    }

    public CompletableFuture<Void> serve(Consumer<Connection> handler) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel result, Object attachment) {
                if (future.isDone()) {
                    try {
                        result.close();
                    } catch (IOException exc) {
                        logger.error(exc.getMessage(), exc);
                    }
                } else {
                    Server.this.channel.accept(null, this);
                    final Connection connection = new Connection(result);
                    try {
                        handler.accept(connection);
                    } catch (Exception exc) {
                        logger.error(exc.getMessage(), exc);
                    }
                }
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                future.completeExceptionally(exc);
            }
        });
        return future;
    }

    @Override
    public void close() throws IOException {
        if (channel.isOpen()) {
            channel.close();
        }
    }

    public AsynchronousServerSocketChannel channel() {
        return channel;
    }
}
