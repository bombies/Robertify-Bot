package main.utils.component.interactions.selectionmenu;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.component.InteractionBuilderException;
import main.utils.component.InvalidBuilderExceptionKt;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class StringSelectionMenuBuilder {

    private String name;
    private String placeholder;
    private Pair<Integer, Integer> range;
    private final List<StringSelectMenuOption> options;
    @Nullable
    private Predicate<StringSelectInteractionEvent> permissionCheck;
    @Getter
    private boolean limited;

    private StringSelectionMenuBuilder(@NotNull String name, @NotNull String placeholder, @NotNull Pair<Integer, Integer> range, @NotNull List<Triple<String,String, Emoji>> options, @Nullable Predicate<StringSelectInteractionEvent> permissionCheck, boolean limited) {
        this.name = name.toLowerCase();
        this.placeholder = placeholder;
        this.range = range;
        this.permissionCheck = permissionCheck;

        final List<StringSelectMenuOption> l = new ArrayList<>();
        for (var option : options)
            l.add(StringSelectMenuOption.of(option.getLeft(), option.getMiddle(), option.getRight()));

        this.options =  l;
        this.limited = limited;
    }

    public StringSelectionMenuBuilder() {
        this.name = null;
        this.placeholder = null;
        this.range = null;
        this.permissionCheck = null;
        this.options = new ArrayList<>();
    }

    public StringSelectionMenuBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public StringSelectionMenuBuilder setPlaceHolder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public StringSelectionMenuBuilder setRange(int min, int max) {
        this.range = Pair.of(min, max);
        return this;
    }

    public StringSelectionMenuBuilder addOption(String label, String value, Emoji emoji) {
        this.options.add(StringSelectMenuOption.of(label, value, emoji));
        return this;
    }

    public StringSelectionMenuBuilder addOptions(StringSelectMenuOption... options) {
        this.options.addAll(Arrays.asList(options));
        return this;
    }

    public StringSelectionMenuBuilder setPermissionCheck(Predicate<StringSelectInteractionEvent> predicate) {
        this.permissionCheck = predicate;
        return this;
    }

    @SneakyThrows
    public StringSelectionMenuBuilder limitToUser(long userID) {
        if (name == null)
            throw new InvalidBuilderException("This menu can't be limited since a name for the menu wasn't provided!");

        this.limited = true;
        this.name += ":" + userID;
        return this;
    }

    public boolean checkPermission(StringSelectInteractionEvent e) {
        if (permissionCheck == null)
            throw new NullPointerException("There is no permission to check!");
        return permissionCheck.test(e);
    }

    /**
     *
     * @param name Name of the menu to be used as an identifier
     * @param placeholder The text that is to show up on the menu when there is nothing selected
     * @param range The range of values that will be allowed to be selected. Pair(Min, Max)
     * @param options The list of selection menu options to use
     * @return A new fancy selection menu
     */
    @SneakyThrows
    public static StringSelectionMenuBuilder of(String name, String placeholder, Pair<Integer, Integer> range, List<StringSelectMenuOption> options) {
        final List<Triple<String, String, Emoji>> ret = new ArrayList<>();

        if (options.size() > 25)
            throw new InvalidBuilderException("There can only be 25 options maximum!");

        for (var option : options)
            ret.add(Triple.of(option.getLabel(), option.getValue(), option.getEmoji()));

        return new StringSelectionMenuBuilder(name, placeholder, range, ret, null, false);
    }

    /**
     *
     * @param name Name of the menu to be used as an identifier
     * @param placeholder The text that is to show up on the menu when there is nothing selected
     * @param range The range of values that will be allowed to be selected. Pair(Min, Max)
     * @param options This of pair of options to be presented List(Pair(Label, Value))
     * @param permissionCheck The check that can be performed when a user interacts with the selection menu
     * @return A new fancy selection menu
     */
    public static StringSelectionMenuBuilder of(String name, String placeholder, Pair<Integer, Integer> range, List<Triple<String, String, Emoji>> options, Predicate<StringSelectInteractionEvent> permissionCheck) {
        return new StringSelectionMenuBuilder(name, placeholder, range, options, permissionCheck, false);
    }

    @SneakyThrows
    public SelectMenu build() {
        if (name == null)
            throw new InteractionBuilderException("The name of the menu can't be null!");
        if (placeholder == null)
            throw new InteractionBuilderException("The placeholder for the menu can't be null!");
        if (range == null)
            throw new InteractionBuilderException("The range for the menu can't be null!");

        StringSelectMenu.Builder builder = StringSelectMenu.create(name)
                .setPlaceholder(placeholder)
                .setRequiredRange(range.getLeft(), range.getRight());

        for (StringSelectMenuOption val : options)
            if (val.getEmoji() == null)
                builder.addOption(val.getLabel(), val.getValue());
            else
                builder.addOption(val.getLabel(), val.getValue(), val.getEmoji());

        return builder.build();
    }
}
