/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.api.requests.restaction.pagination;

import javax.annotation.Nonnull;
import java.util.NoSuchElementException;

/**
 * Pagination using snowflake IDs.
 *
 * @param <T>
 *        The type of the paginated entities
 * @param <M>
 *        The type of the pagination action, used for chaining method calls
 */
public interface SnowflakePaginationAction<T, M extends SnowflakePaginationAction<T, M>> extends PaginationAction<T, M>
{
    /**
     * Skips past the specified ID for successive requests.
     * This will reset the {@link #getLast()} entity and cause a {@link NoSuchElementException} to be thrown
     * when attempting to get the last entity until a new retrieve action has been done.
     * <br>If cache is disabled this can be set to an arbitrary value irrelevant of the current last.
     * Set this to {@code 0} to start from the most recent message.
     *
     * <p>Fails if cache is enabled and the target id is newer than the current last id {@literal (id > last)}.
     *
     * <p><b>Example</b><br>
     * <pre>{@code
     * public MessagePaginationAction getOlderThan(MessageChannel channel, long time) {
     *     final long timestamp = TimeUtil.getDiscordTimestamp(time);
     *     final MessagePaginationAction paginator = channel.getIterableHistory();
     *     return paginator.skipTo(timestamp);
     * }
     *
     * getOlderThan(channel, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14))
     *     .forEachAsync((message) -> {
     *         boolean empty = message.getContentRaw().isEmpty();
     *         if (!empty)
     *             System.out.printf("%#s%n", message); // means: print display content
     *         return !empty; // means: continue if not empty
     *     });
     * }</pre>
     *
     * @param  id
     *         The snowflake ID to skip before, this is exclusive rather than inclusive
     *
     * @throws IllegalArgumentException
     *         If cache is enabled, and you are attempting to skip forward in time {@literal (id > last)}
     *
     * @return The current PaginationAction for chaining convenience
     *
     * @see    java.util.concurrent.TimeUnit
     * @see    net.dv8tion.jda.api.utils.TimeUtil
     */
    @Nonnull
    M skipTo(long id);

    /**
     * The current iteration anchor used for pagination.
     * <br>This is updated by each retrieve action.
     *
     * @return The current iteration anchor
     *
     * @see    #skipTo(long) Use skipTo(anchor) to change this
     */
    long getLastKey();
}
