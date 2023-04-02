import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import lavalink.client.io.Lavalink;
import lavalink.client.io.Link;
import lavalink.client.player.LavalinkPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.DefaultGuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.entities.sticker.StickerSnowflake;
import net.dv8tion.jda.api.entities.templates.Template;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.PrivilegeConfig;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.IntegrationPrivilege;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.managers.GuildManager;
import net.dv8tion.jda.api.managers.GuildStickerManager;
import net.dv8tion.jda.api.managers.GuildWelcomeScreenManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.*;
import net.dv8tion.jda.api.requests.restaction.order.CategoryOrderAction;
import net.dv8tion.jda.api.requests.restaction.order.ChannelOrderAction;
import net.dv8tion.jda.api.requests.restaction.order.RoleOrderAction;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.api.requests.restaction.pagination.BanPaginationAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.cache.MemberCacheView;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.cache.SortedSnowflakeCacheView;
import net.dv8tion.jda.api.utils.concurrent.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class UtilsTest {

    public static Guild getTestGuild() {
        return new Guild() {
            @NotNull
            @Override
            public RestAction<List<Command>> retrieveCommands(boolean b) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Command> retrieveCommandById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Command> upsertCommand(@NotNull CommandData commandData) {
                return null;
            }

            @NotNull
            @Override
            public CommandListUpdateAction updateCommands() {
                return null;
            }

            @NotNull
            @Override
            public CommandEditAction editCommandById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Void> deleteCommandById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<IntegrationPrivilege>> retrieveIntegrationPrivilegesById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<PrivilegeConfig> retrieveCommandPrivileges() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<EnumSet<Region>> retrieveRegions(boolean b) {
                return null;
            }

            @NotNull
            @Override
            public MemberAction addMember(@NotNull String s, @NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @Override
            public boolean isLoaded() {
                return false;
            }

            @Override
            public void pruneMemberCache() {

            }

            @Override
            public boolean unloadMember(long l) {
                return false;
            }

            @Override
            public int getMemberCount() {
                return 0;
            }

            @NotNull
            @Override
            public String getName() {
                return null;
            }

            @Nullable
            @Override
            public String getIconId() {
                return null;
            }

            @NotNull
            @Override
            public Set<String> getFeatures() {
                return null;
            }

            @Nullable
            @Override
            public String getSplashId() {
                return null;
            }

            @Nullable
            @Override
            public String getVanityCode() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<VanityInvite> retrieveVanityInvite() {
                return null;
            }

            @Nullable
            @Override
            public String getDescription() {
                return null;
            }

            @NotNull
            @Override
            public DiscordLocale getLocale() {
                return null;
            }

            @Nullable
            @Override
            public String getBannerId() {
                return null;
            }

            @NotNull
            @Override
            public BoostTier getBoostTier() {
                return null;
            }

            @Override
            public int getBoostCount() {
                return 0;
            }

            @NotNull
            @Override
            public List<Member> getBoosters() {
                return null;
            }

            @Override
            public int getMaxMembers() {
                return 0;
            }

            @Override
            public int getMaxPresences() {
                return 0;
            }

            @NotNull
            @Override
            public RestAction<MetaData> retrieveMetaData() {
                return null;
            }

            @Nullable
            @Override
            public VoiceChannel getAfkChannel() {
                return null;
            }

            @Nullable
            @Override
            public TextChannel getSystemChannel() {
                return null;
            }

            @Nullable
            @Override
            public TextChannel getRulesChannel() {
                return null;
            }

            @Nullable
            @Override
            public TextChannel getCommunityUpdatesChannel() {
                return null;
            }

            @Nullable
            @Override
            public Member getOwner() {
                return null;
            }

            @Override
            public long getOwnerIdLong() {
                return 0;
            }

            @NotNull
            @Override
            public Timeout getAfkTimeout() {
                return null;
            }

            @Override
            public boolean isMember(@NotNull UserSnowflake userSnowflake) {
                return false;
            }

            @NotNull
            @Override
            public Member getSelfMember() {
                return null;
            }

            @NotNull
            @Override
            public NSFWLevel getNSFWLevel() {
                return null;
            }

            @Nullable
            @Override
            public Member getMember(@NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public MemberCacheView getMemberCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<ScheduledEvent> getScheduledEventCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<StageChannel> getStageChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<ThreadChannel> getThreadChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<Category> getCategoryCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<TextChannel> getTextChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<NewsChannel> getNewsChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<VoiceChannel> getVoiceChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<ForumChannel> getForumChannelCache() {
                return null;
            }

            @NotNull
            @Override
            public List<GuildChannel> getChannels(boolean b) {
                return null;
            }

            @NotNull
            @Override
            public SortedSnowflakeCacheView<Role> getRoleCache() {
                return null;
            }

            @NotNull
            @Override
            public SnowflakeCacheView<RichCustomEmoji> getEmojiCache() {
                return null;
            }

            @NotNull
            @Override
            public SnowflakeCacheView<GuildSticker> getStickerCache() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<RichCustomEmoji>> retrieveEmojis() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<RichCustomEmoji> retrieveEmojiById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<GuildSticker>> retrieveStickers() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<GuildSticker> retrieveSticker(@NotNull StickerSnowflake stickerSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public GuildStickerManager editSticker(@NotNull StickerSnowflake stickerSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public BanPaginationAction retrieveBanList() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Ban> retrieveBan(@NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Integer> retrievePrunableMemberCount(int i) {
                return null;
            }

            @NotNull
            @Override
            public Role getPublicRole() {
                return null;
            }

            @Nullable
            @Override
            public DefaultGuildChannelUnion getDefaultChannel() {
                return null;
            }

            @NotNull
            @Override
            public GuildManager getManager() {
                return null;
            }

            @Override
            public boolean isBoostProgressBarEnabled() {
                return false;
            }

            @NotNull
            @Override
            public AuditLogPaginationAction retrieveAuditLogs() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Void> leave() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Void> delete() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Void> delete(@Nullable String s) {
                return null;
            }

            @NotNull
            @Override
            public AudioManager getAudioManager() {
                return null;
            }

            @NotNull
            @Override
            public Task<Void> requestToSpeak() {
                return null;
            }

            @NotNull
            @Override
            public Task<Void> cancelRequestToSpeak() {
                return null;
            }

            @NotNull
            @Override
            public JDA getJDA() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<Invite>> retrieveInvites() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<Template>> retrieveTemplates() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Template> createTemplate(@NotNull String s, @Nullable String s1) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<Webhook>> retrieveWebhooks() {
                return null;
            }

            @NotNull
            @Override
            public RestAction<GuildWelcomeScreen> retrieveWelcomeScreen() {
                return null;
            }

            @NotNull
            @Override
            public List<GuildVoiceState> getVoiceStates() {
                return null;
            }

            @NotNull
            @Override
            public VerificationLevel getVerificationLevel() {
                return null;
            }

            @NotNull
            @Override
            public NotificationLevel getDefaultNotificationLevel() {
                return null;
            }

            @NotNull
            @Override
            public MFALevel getRequiredMFALevel() {
                return null;
            }

            @NotNull
            @Override
            public ExplicitContentLevel getExplicitContentLevel() {
                return null;
            }

            @NotNull
            @Override
            public Task<Void> loadMembers(@NotNull Consumer<Member> consumer) {
                return null;
            }

            @NotNull
            @Override
            public CacheRestAction<Member> retrieveMemberById(long l) {
                return null;
            }

            @NotNull
            @Override
            public Task<List<Member>> retrieveMembersByIds(boolean b, @NotNull long... longs) {
                return null;
            }

            @NotNull
            @Override
            public Task<List<Member>> retrieveMembersByPrefix(@NotNull String s, int i) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<List<ThreadChannel>> retrieveActiveThreads() {
                return null;
            }

            @NotNull
            @Override
            public CacheRestAction<ScheduledEvent> retrieveScheduledEventById(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RestAction<Void> moveVoiceMember(@NotNull Member member, @Nullable AudioChannel audioChannel) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> modifyNickname(@NotNull Member member, @Nullable String s) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Integer> prune(int i, boolean b, @NotNull Role... roles) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> kick(@NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> ban(@NotNull UserSnowflake userSnowflake, int i, @NotNull TimeUnit timeUnit) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> unban(@NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> timeoutUntil(@NotNull UserSnowflake userSnowflake, @NotNull TemporalAccessor temporalAccessor) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> removeTimeout(@NotNull UserSnowflake userSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> deafen(@NotNull UserSnowflake userSnowflake, boolean b) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> mute(@NotNull UserSnowflake userSnowflake, boolean b) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> addRoleToMember(@NotNull UserSnowflake userSnowflake, @NotNull Role role) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> removeRoleFromMember(@NotNull UserSnowflake userSnowflake, @NotNull Role role) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> modifyMemberRoles(@NotNull Member member, @Nullable Collection<Role> collection, @Nullable Collection<Role> collection1) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> modifyMemberRoles(@NotNull Member member, @NotNull Collection<Role> collection) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> transferOwnership(@NotNull Member member) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<TextChannel> createTextChannel(@NotNull String s, @Nullable Category category) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<NewsChannel> createNewsChannel(@NotNull String s, @Nullable Category category) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<VoiceChannel> createVoiceChannel(@NotNull String s, @Nullable Category category) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<StageChannel> createStageChannel(@NotNull String s, @Nullable Category category) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<ForumChannel> createForumChannel(@NotNull String s, @Nullable Category category) {
                return null;
            }

            @NotNull
            @Override
            public ChannelAction<Category> createCategory(@NotNull String s) {
                return null;
            }

            @NotNull
            @Override
            public RoleAction createRole() {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<RichCustomEmoji> createEmoji(@NotNull String s, @NotNull Icon icon, @NotNull Role... roles) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<GuildSticker> createSticker(@NotNull String s, @NotNull String s1, @NotNull FileUpload fileUpload, @NotNull Collection<String> collection) {
                return null;
            }

            @NotNull
            @Override
            public AuditableRestAction<Void> deleteSticker(@NotNull StickerSnowflake stickerSnowflake) {
                return null;
            }

            @NotNull
            @Override
            public ScheduledEventAction createScheduledEvent(@NotNull String s, @NotNull String s1, @NotNull OffsetDateTime offsetDateTime, @NotNull OffsetDateTime offsetDateTime1) {
                return null;
            }

            @NotNull
            @Override
            public ScheduledEventAction createScheduledEvent(@NotNull String s, @NotNull GuildChannel guildChannel, @NotNull OffsetDateTime offsetDateTime) {
                return null;
            }

            @NotNull
            @Override
            public ChannelOrderAction modifyCategoryPositions() {
                return null;
            }

            @NotNull
            @Override
            public ChannelOrderAction modifyTextChannelPositions() {
                return null;
            }

            @NotNull
            @Override
            public ChannelOrderAction modifyVoiceChannelPositions() {
                return null;
            }

            @NotNull
            @Override
            public CategoryOrderAction modifyTextChannelPositions(@NotNull Category category) {
                return null;
            }

            @NotNull
            @Override
            public CategoryOrderAction modifyVoiceChannelPositions(@NotNull Category category) {
                return null;
            }

            @NotNull
            @Override
            public RoleOrderAction modifyRolePositions(boolean b) {
                return null;
            }

            @NotNull
            @Override
            public GuildWelcomeScreenManager modifyWelcomeScreen() {
                return null;
            }

            @Override
            public long getIdLong() {
                return 0;
            }
        };
    }

    public static AudioTrack getTestTrack(String id) {
        return new AudioTrack() {
            @Override
            public AudioTrackInfo getInfo() {
                return new AudioTrackInfo(
                        "test title",
                        "test author",
                        10000,
                        id,
                        false,
                        "http://testuri:80/"
                        );
            }

            @Override
            public String getIdentifier() {
                return getInfo().identifier;
            }

            @Override
            public AudioTrackState getState() {
                return AudioTrackState.PLAYING;
            }

            @Override
            public void stop() {

            }

            @Override
            public boolean isSeekable() {
                return false;
            }

            @Override
            public long getPosition() {
                return 0;
            }

            @Override
            public void setPosition(long l) {

            }

            @Override
            public void setMarker(TrackMarker trackMarker) {

            }

            @Override
            public long getDuration() {
                return getInfo().length;
            }

            @Override
            public AudioTrack makeClone() {
                return null;
            }

            @Override
            public AudioSourceManager getSourceManager() {
                return null;
            }

            @Override
            public void setUserData(Object o) {

            }

            @Override
            public Object getUserData() {
                return null;
            }

            @Override
            public <T> T getUserData(Class<T> aClass) {
                return null;
            }
        };
    }

    public static List<AudioTrack> getManyTracks(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> getTestTrack(String.valueOf(i)))
                .toList();
    }

    public static Link getTestLink() {
        final var testLavaLink = new Lavalink<>("testId", 1) {
            @Override
            protected Link buildNewLink(String guildId) {
                return null;
            }
        };
        return new Link(testLavaLink, "1234567891234556777") {
            @Override
            protected void removeConnection() {
            }

            @Override
            protected void queueAudioDisconnect() {
            }

            @Override
            protected void queueAudioConnect(long channelId) {
            }

            @Override
            public LavalinkPlayer getPlayer() {
                return new LavalinkPlayer(this);
            }
        };
    }
}
