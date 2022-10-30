package main.utils.resume;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumeUtils {
    private final Logger logger = LoggerFactory.getLogger(ResumeUtils.class);

    private final ResumeData data;
    private static ResumeUtils instance;

    private ResumeUtils() {
        data = new ResumeData();
    }

    public void saveInfo(Guild guild, AudioChannel channel) {
        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);

        data.addGuild(
                guild.getIdLong(),
                channel.getIdLong(),
                musicManager.getPlayer().getPlayingTrack(),
                musicManager.getScheduler().queue
        );
    }

    public void loadInfo(Guild guild) {
        if (!hasInfo(guild)) return;

        ResumeData.GuildResumeData info = data.getInfo(guild.getIdLong());
        if (info == null)
            return;

        try {
            RobertifyAudioManager.getInstance().loadAndPlay(
                    guild.getIdLong(),
                    info.getChannelId(),
                    info
            );
        } catch (Exception e) {
            logger.error("Unexpected error", e);
        }

        data.removeGuild(guild.getIdLong());
    }

    public void removeInfo(Guild guild) {
        if (!hasInfo(guild)) return;
        data.removeGuild(guild.getIdLong());
    }

    public boolean hasInfo(Guild guild) {
        return data.guildHasInfo(guild.getIdLong());
    }

    public static ResumeUtils getInstance() {
        if (instance == null)
            instance = new ResumeUtils();
        return instance;
    }
}
