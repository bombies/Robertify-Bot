package main.utils.component.interactions.selectionmenu;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import main.utils.component.InvalidBuilderExceptionKt;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class StringSelectMenuOption {
    @Getter
    private final String label;
    @Getter
    private final String value;
    @Getter
    private final Emoji emoji;
    @Getter
    private final Predicate<StringSelectInteractionEvent> predicate;

    private StringSelectMenuOption(@NotNull String label, @NotNull String value, @Nullable Emoji emoji, @Nullable Predicate<StringSelectInteractionEvent> predicate) {
        this.label = label;
        this.value = value;
        this.emoji = emoji;
        this.predicate = predicate;
    }

    public static OptionBuilder create() {
        return new OptionBuilder();
    }

    public static StringSelectMenuOption of(@NotNull String label, @NotNull String id, @Nullable Emoji emoji, @Nullable Predicate<StringSelectInteractionEvent> predicate) {
        return new StringSelectMenuOption(label, id, emoji, predicate);
    }

    public static StringSelectMenuOption of(String label, String id, Emoji emoji) {
        return of(label, id, emoji, null);
    }

    public static StringSelectMenuOption of(String label, String id) {
        return of(label, id, null);
    }

    public static class OptionBuilder {
        @Setter
        private String label;
        @Setter
        private String id;
        @Setter
        private Emoji emoji;
        @Setter
        private Predicate<StringSelectInteractionEvent> predicate;

        private OptionBuilder() {}

        @SneakyThrows
        public StringSelectMenuOption build() {
            if (label == null || label.isEmpty())
                throw new InvalidBuilderException("The label must not be null or empty!");

            if (id == null || id.isEmpty())
                throw new InvalidBuilderException("The ID must not be null or empty!");

            return new StringSelectMenuOption(label, id, emoji, predicate);
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
