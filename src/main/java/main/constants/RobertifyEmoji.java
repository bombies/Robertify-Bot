package main.constants;

import net.dv8tion.jda.api.entities.Emoji;

public enum RobertifyEmoji {
    PREVIOUS_EMOJI("<:previous:980202130755444766>"),
    REWIND_EMOJI("<:rewind:980201728815276113>"),
    PLAY_EMOJI("<:play:980201728622329867>"),
    END_EMOJI("<:skip:980202130914803772>"),
    PAUSE_EMOJI("<:pause:913595131058356256>"),
    PLAY_AND_PAUSE_EMOJI("<:playandpause:980201728458768424>"),
    LOOP_EMOJI("<:loop:980197155929214986>"),
    SHUFFLE_EMOJI("<:shuffle:980197231623823482>"),
    STOP_EMOJI("<:stop:980201728450375681>"),
    QUIT_EMOJI("<:disconnect:980201728987267072>"),
    STAR_EMOJI("<:favourite:980201729096286239>"),
    CHECK_EMOJI("<:check:914738311535407136>"),
    BAR_START_FULL("<:VolumeBarEmoteStartFULL:917851063384674394>"),
    BAR_START_EMPTY("<:VolumeBarEmoteStartEMPTY:917851063321776198>"),
    BAR_MIDDLE_FULL("<:VolumeBarEmoteMiddlefull:917851063065907241>"),
    BAR_MIDDLE_EMPTY("<:VolumeBarEmoteMiddleEMPTY:917851399830765589>"),
    BAR_END_FULL("<:VolumeBarEmoteEndFULL:917851063321788486>"),
    BAR_END_EMPTY("<:VolumeBarEmoteEndEMPTY:917851063376306226>"),
    FEATURE("<:feature:938567758172540928>"),
    BUG_FIX("<:bugfix:938568095985979412>");

    private final String str;

    RobertifyEmoji(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str.trim();
    }

    public Emoji getEmoji() {
        return Emoji.fromMarkdown(str);
    }
}
