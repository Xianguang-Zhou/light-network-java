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

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author <a href="mailto:xianguang.zhou@outlook.com">Xianguang Zhou</a>
 */
public class Handler implements Consumer<Connection> {

    private static final Logger defaultLogger = LoggerFactory.getLogger(Handler.class);

    private final Function<Connection, CompletionStage<?>> asyncHandler;
    private final Logger logger;

    public Handler(Function<Connection, CompletionStage<?>> asyncHandler) {
        this(asyncHandler, defaultLogger);
    }

    public Handler(Function<Connection, CompletionStage<?>> asyncHandler, Logger logger) {
        this.asyncHandler = asyncHandler;
        this.logger = logger;
    }

    @Override
    public void accept(Connection connection) {
        asyncHandler.apply(connection).handle((result, exception) -> {
            if (exception != null) {
                logger.error(exception.getMessage(), exception);
            }
            try {
                connection.close();
            } catch (IOException exc) {
                logger.error(exc.getMessage(), exc);
            }
            return null;
        });
    }
}
