package main.utils.component.builders.selectionmenu;

import lombok.Getter;
import lombok.SneakyThrows;
import main.utils.component.InteractionBuilderException;
import main.utils.component.InvalidBuilderException;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SelectionMenuBuilder {

    @Getter
    @NotNull
    private final String name;
    @Getter @NotNull
    private final String placeholder;
    @Getter @NotNull
    private final Pair<Integer, Integer> range;
    @Getter @NotNull
    private final List<Triple<String, String, Emoji>> options;
    @Nullable
    private final Predicate<SelectionMenuEvent> permissionCheck;

    private SelectionMenuBuilder(@NotNull String name, @NotNull String placeholder, @NotNull Pair<Integer, Integer> range, @NotNull List<Triple<String,String, Emoji>> options, @Nullable Predicate<SelectionMenuEvent> permissionCheck) {
        this.name = name.toLowerCase();
        this.placeholder = placeholder;
        this.range = range;
        this.options = options;
        this.permissionCheck = permissionCheck;
    }

    public boolean checkPermission(SelectionMenuEvent e) {
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
    public static SelectionMenuBuilder of(String name, String placeholder, Pair<Integer, Integer> range, List<SelectionMenuOption> options) {
        final List<Triple<String, String, Emoji>> ret = new ArrayList<>();

        if (options.size() > 25)
            throw new InvalidBuilderException("There can only be 25 options maximum!");

        for (var option : options)
            ret.add(Triple.of(option.getLabel(), option.getValue(), option.getEmoji()));

        return new SelectionMenuBuilder(name, placeholder, range, ret, null);
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
    public static SelectionMenuBuilder of(String name, String placeholder, Pair<Integer, Integer> range, List<Triple<String, String, Emoji>> options, Predicate<SelectionMenuEvent> permissionCheck) {
        return new SelectionMenuBuilder(name, placeholder, range, options, permissionCheck);
    }

    public SelectionMenu build() throws InteractionBuilderException {
        SelectionMenu.Builder builder = SelectionMenu.create(name)
                .setPlaceholder(placeholder)
                .setRequiredRange(range.getLeft(), range.getLeft());

        for (Triple<String, String, Emoji> val : options)
            if (val.getRight() == null)
                builder.addOption(val.getLeft(), val.getMiddle());
            else
                builder.addOption(val.getLeft(), val.getMiddle(), val.getRight());

        return builder.build();
    }
}
