package main.utils.pagination;

import lombok.Getter;
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption;

import java.util.ArrayList;
import java.util.List;

public class MenuPage {
    @Getter
    private final List<StringSelectMenuOption> options = new ArrayList<>();

    public void addOption(StringSelectMenuOption option) {
        if (options.size() >= 25)
            throw new IllegalArgumentException("You cannot add any more options to the page!");

        options.add(option);
    }

    public void removeOption(int i) {
        options.remove(i);
    }

    public StringSelectMenuOption getOption(int i) {
        return options.get(i);
    }

    public List<String> toStringList() {
        final List<String> ret = new ArrayList<>();
        for (StringSelectMenuOption option : options) {
            if (option.toString().contains("Next Page") ||
                    option.toString().contains("Previous Page")
            ) continue;

            ret.add(option.toString());
        }

        return ret;
    }
}
