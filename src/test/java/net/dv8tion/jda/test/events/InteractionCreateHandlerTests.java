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

package net.dv8tion.jda.test.events;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.InteractionType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.data.SerializableData;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.entities.SelfUserImpl;
import net.dv8tion.jda.internal.handle.InteractionCreateHandler;
import net.dv8tion.jda.internal.utils.cache.MemberCacheViewImpl;
import net.dv8tion.jda.internal.utils.cache.SnowflakeCacheViewImpl;
import net.dv8tion.jda.test.BaseTest;
import net.dv8tion.jda.test.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InteractionCreateHandlerTests extends AbstractSocketHandlerTest
{
    private static final long GUILD_IMPL_ID = 81384788765712384L;
    private static final String EVENT_TYPE = "INTERACTION_CREATE";
    private static final String INTERACTION_TOKEN = "this.is.a.token";

    private static final long BOT_DM_PERMISSIONS = Permission.getRaw(
            Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MENTION_EVERYONE, Permission.MESSAGE_EXT_EMOJI);
    private static final long GDM_PERMISSIONS = Permission.getRaw(
            Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_MENTION_EVERYONE);


    private InteractionCreateHandler handler;
    private SnowflakeCacheViewImpl<User> userCache;

    @BeforeEach
    protected void setup()
    {
        super.setup();
        handler = new InteractionCreateHandler(jda);
        userCache = new SnowflakeCacheViewImpl<>(User.class, User::getName);

        when(jda.getSelfUser()).thenReturn(new SelfUserImpl(Constants.BUTLER_USER_ID, jda));
        when(jda.getUsersView()).thenReturn(userCache);
        when(jda.getGatewayPool()).thenReturn(scheduledExecutorService);
    }

    @Nested
    class GuildCacheAttachedInteractionTest extends BaseTest
    {
        @Mock
        private GuildImpl guildImpl;
        @Mock
        private GuildChannel channel;

        private MemberCacheViewImpl memberCache;

        @BeforeEach
        protected void setup()
        {
            super.setup();
            memberCache = new MemberCacheViewImpl();

            when(jda.getGuildById(eq(GUILD_IMPL_ID))).thenReturn(guildImpl);
            when(guildImpl.getMembersView()).thenReturn(memberCache);
            when(guildImpl.getGuildChannelById(eq(Constants.CHANNEL_ID))).thenReturn(channel);
        }

        @Test
        void minimalSlashCommand()
        {
            InteractionEventBuilder builder = InteractionEventBuilder.slashCommand();

            assertThatEvent(SlashCommandInteractionEvent.class)
                .hasGetterWithValueEqualTo(SlashCommandInteractionEvent::hasFullGuild, true)
                .isFiredBy(() -> handler.handle(random.nextLong(), event(EVENT_TYPE, builder.toData())));

            verify(jda, atLeastOnce()).getUsersView();
            verify(guildImpl, atLeastOnce()).getMembersView();
        }
    }

    @Nested
    class CacheDetachedInteractionTest extends BaseTest
    {
        @Test
        void minimalSlashCommand()
        {
            InteractionEventBuilder builder = InteractionEventBuilder.slashCommand().asUserInstalled();

            assertThatEvent(SlashCommandInteractionEvent.class)
                .hasGetterWithValueEqualTo(SlashCommandInteractionEvent::hasFullGuild, false)
                .isFiredBy(() -> handler.handle(random.nextLong(), event(EVENT_TYPE, builder.toData())));

            verify(jda, atLeastOnce()).getUsersView();
        }
    }

    private static class InteractionEventBuilder implements SerializableData
    {
        private final DataObject event = DataObject.empty()
            .put("version", 1)
            .put("id", Constants.INTERACTION_ID)
            .put("application_id", Constants.BUTLER_USER_ID)
            .put("token", INTERACTION_TOKEN)
            .put("entitlements", DataArray.empty());

        static InteractionEventBuilder slashCommand()
        {
            return new InteractionEventBuilder()
                .setType(InteractionType.COMMAND)
                .setDefaultChannel()
                .setDefaultGuild()
                .setDefaultMember()
                .setInteractionData(InteractionDataBuilder.slashCommand())
                .asGuildInstalled();
        }

        InteractionEventBuilder setType(InteractionType type)
        {
            event.put("type", type.getKey());
            return this;
        }

        InteractionEventBuilder setDefaultMember()
        {
            event.put("member", DataObject.empty()
                .put("id", Constants.MINN_USER_ID)
                .put("roles", DataArray.empty())
                .put("user", DataObject.empty()
                    .put("id", Constants.MINN_USER_ID)
                    .put("username", "minn")
                    .put("discriminator", "0")
                    .put("avatar", null)));
            return this;
        }

        InteractionEventBuilder setDefaultUser()
        {
            event.put("user", DataObject.empty()
                .put("id", Constants.MINN_USER_ID)
                .put("username", "minn")
                .put("discriminator", "0")
                .put("avatar", null));
            return this;
        }

        InteractionEventBuilder setContext(InteractionContextType context)
        {
            event.put("context", context.getType());
            switch (context)
            {
            case GUILD:
                event.put("app_permissions", Permission.ADMINISTRATOR.getRawValue());
                break;
            case BOT_DM:
                event.put("app_permissions", BOT_DM_PERMISSIONS);
                break;
            case PRIVATE_CHANNEL:
                event.put("app_permissions", GDM_PERMISSIONS);
                break;
            }
            return this;
        }

        InteractionEventBuilder setGuild(DataObject guild)
        {
            setContext(InteractionContextType.GUILD);
            event.put("guild", guild);
            event.put("guild_id", guild.getString("id"));
            return this;
        }

        InteractionEventBuilder setDefaultGuild()
        {
            return setDefaultGuild(null);
        }

        InteractionEventBuilder setDefaultGuild(Consumer<DataObject> customizer)
        {
            DataObject guild = DataObject.empty().put("id", GUILD_IMPL_ID);
            if (customizer != null)
                customizer.accept(guild);
            return setGuild(guild);
        }

        InteractionEventBuilder setChannel(DataObject channel)
        {
            event.put("channel", channel);
            event.put("channel_id", channel.getString("id"));
            return this;
        }

        InteractionEventBuilder setDefaultChannel()
        {
            return setDefaultChannel(null);
        }

        InteractionEventBuilder setDefaultChannel(Consumer<DataObject> customizer)
        {
            DataObject channel = DataObject.empty()
                .put("type", ChannelType.TEXT.getId())
                .put("id", Constants.CHANNEL_ID)
                .put("name", "general")
                .put("position", 42)
                .put("permissions", 123);
            if (customizer != null)
                customizer.accept(channel);
            return setChannel(channel);
        }

        InteractionEventBuilder setInteractionData(InteractionDataBuilder builder)
        {
            event.put("data", builder.toData());
            return this;
        }

        InteractionEventBuilder asGuildInstalled()
        {
            event.put("authorizing_integration_owners", DataObject.empty()
                .put(IntegrationType.GUILD_INSTALL.getType(), event.getString("guild_id")));
            return this;
        }

        InteractionEventBuilder asUserInstalled()
        {
            String userId = event.optObject("member").orElse(event).getObject("user").getString("id");
            event.put("authorizing_integration_owners", DataObject.empty()
                .put(IntegrationType.USER_INSTALL.getType(), userId));
            return this;
        }

        @Nonnull
        @Override
        public DataObject toData()
        {
            return event;
        }
    }

    private static class InteractionDataBuilder implements SerializableData
    {
        private final DataObject data = DataObject.empty()
            .put("id", Constants.COMMAND_ID)
            .put("name", "command_name")
            .put("options", DataArray.empty());

        static InteractionDataBuilder slashCommand()
        {
            return new InteractionDataBuilder()
                .setCommandType(Command.Type.SLASH);
        }

        InteractionDataBuilder setCommandType(Command.Type type)
        {
            data.put("type", type.getId());
            return this;
        }

        @Nonnull
        @Override
        public DataObject toData()
        {
            return data;
        }
    }
}
