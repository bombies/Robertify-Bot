package main.utils.pagination;

import lombok.Getter;
import main.constants.MessageButton;
import main.constants.RobertifyEmoji;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class Paginator {
    @Getter
    final Emoji frontEmoji, previousEmoji, nextEmoji, endEmoji;

    public Paginator(Emoji frontEmoji, Emoji previousEmoji, Emoji nextEmoji, Emoji endEmoji) {
        this.frontEmoji = frontEmoji;
        this.previousEmoji = previousEmoji;
        this.nextEmoji = nextEmoji;
        this.endEmoji = endEmoji;
    }

    public static Paginator getDefaultPaginator() {
        return new Paginator(
                Emoji.fromMarkdown(RobertifyEmoji.PREVIOUS_EMOJI.toString()),
                Emoji.fromMarkdown(RobertifyEmoji.REWIND_EMOJI.toString()),
                Emoji.fromMarkdown(RobertifyEmoji.PLAY_EMOJI.toString()),
                Emoji.fromMarkdown(RobertifyEmoji.END_EMOJI.toString())
        );
    }

    public static ActionRow getButtons(User user, Paginator paginator, boolean frontEnabled, boolean previousEnabled, boolean nextEnabled, boolean endEnabled) {
        Button frontButton = Button.of(ButtonStyle.SECONDARY, MessageButton.FRONT + user.getId(), paginator.getFrontEmoji());
        Button previousButton = Button.of(ButtonStyle.SECONDARY, MessageButton.PREVIOUS + user.getId(), paginator.getPreviousEmoji());
        Button nextButton = Button.of(ButtonStyle.SECONDARY, MessageButton.NEXT + user.getId(), paginator.getNextEmoji());
        Button endButton = Button.of(ButtonStyle.SECONDARY, MessageButton.END + user.getId(), paginator.getEndEmoji());
        return ActionRow.of(
                frontEnabled ? frontButton : frontButton.asDisabled(),
                previousEnabled ? previousButton : previousButton.asDisabled(),
                nextEnabled ? nextButton : nextButton.asDisabled(),
                endEnabled ? endButton : endButton.asDisabled()
        );
    }

    public static ActionRow getButtons(User user, boolean frontEnabled, boolean previousEnabled, boolean nextEnabled, boolean endEnabled) {
        return getButtons(user, getDefaultPaginator(), frontEnabled, previousEnabled, nextEnabled, endEnabled);
    }

    public static ActionRow getButtons(User user, Paginator paginator){
        return getButtons(user, paginator, true, true, true, true);
    }

    public static ActionRow getButtons(User user) {
        return getButtons(user, getDefaultPaginator());
    }

}
