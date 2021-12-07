package main.constants;

public enum RobertifyEmoji {
    PREVIOUS_EMOJI("<:rewind:913595299203805194>"),
    REWIND_EMOJI("<:back:913595555811307541>"),
    PLAY_EMOJI("<:play:913594529603547237>"),
    END_EMOJI("<:next:913594937298280528>"),
    PAUSE_EMOJI("<:pause:913595131058356256>"),
    PLAY_AND_PAUSE_EMOJI("<:playandpause:913594828338659368>"),
    LOOP_EMOJI("<:loop:913613388003823636>"),
    SHUFFLE_EMOJI("<:shuffle:913613387974471741>"),
    STOP_EMOJI("<:stop:913601140879425557>"),
    QUIT_EMOJI("<:close:913798787691073556>"),
    CHECK_EMOJI("<:check:914738311535407136>"),
    BAR_START_FULL("<:VolumeBarEmoteStartFULL:917851063384674394>"),
    BAR_START_EMPTY("<:VolumeBarEmoteStartEMPTY:917851063321776198>"),
    BAR_MIDDLE_FULL("<:VolumeBarEmoteMiddlefull:917851063065907241>"),
    BAR_MIDDLE_EMPTY("<:VolumeBarEmoteMiddleEMPTY:917851399830765589>"),
    BAR_END_FULL("<:VolumeBarEmoteEndFULL:917851063321788486>"),
    BAR_END_EMPTY("<:VolumeBarEmoteEndEMPTY:917851063376306226>");

    private final String str;

    RobertifyEmoji(String str) {
        this.str = str;
    }

    @Override
    public String toString() {
        return str;
    }
}
