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

package net.dv8tion.jda.internal.requests.restaction.pagination;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.api.requests.restaction.pagination.SnowflakePaginationAction;

import javax.annotation.Nonnull;

public abstract class SnowflakePaginationActionImpl<T, M extends SnowflakePaginationAction<T, M>>
    extends AbstractPaginationActionImpl<T, M>
    implements SnowflakePaginationAction<T, M>
{
    /**
     * Creates a new PaginationAction instance
     *
     * @param api
     *        The current JDA instance
     * @param route
     *        The base route
     * @param maxLimit
     *        The inclusive maximum limit that can be used in {@link #limit(int)}
     * @param minLimit
     *        The inclusive minimum limit that can be used in {@link #limit(int)}
     * @param initialLimit
     *        The initial limit to use on the pagination endpoint
     */
    public SnowflakePaginationActionImpl(JDA api, Route.CompiledRoute route, int minLimit, int maxLimit, int initialLimit)
    {
        super(api, route, minLimit, maxLimit, initialLimit);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public M skipTo(long id)
    {
        if (!cached.isEmpty())
        {
            int cmp = Long.compareUnsigned(this.lastKey, id);
            if (cmp < 0) // old - new < 0 => old < new
                throw new IllegalArgumentException("Cannot jump to that id, it is newer than the current oldest element.");
        }
        if (this.lastKey != id)
            this.last = null;
        this.iteratorIndex = id;
        this.lastKey = id;
        return (M) this;
    }

    @Override
    public long getLastKey()
    {
        return lastKey;
    }
}
