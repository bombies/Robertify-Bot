package main.utils.pagination;

import lombok.Getter;
import main.utils.component.builders.selectionmenu.SelectionMenuOption;

import java.util.ArrayList;
import java.util.List;

public class MenuPage {
    @Getter
    private final List<SelectionMenuOption> options = new ArrayList<>();

    public void addOption(SelectionMenuOption option) {
        if (options.size() >= 25)
            throw new IllegalArgumentException("You cannot add any more options to the page!");

        options.add(option);
    }

    public void removeOption(int i) {
        options.remove(i);
    }

    public SelectionMenuOption getOption(int i) {
        return options.get(i);
    }

    public List<String> toStringList() {
        final List<String> ret = new ArrayList<>();
        for (SelectionMenuOption option : options) {
            if (option.toString().contains("Next Page") ||
                    option.toString().contains("Previous Page")
            ) continue;

            ret.add(option.toString());
        }

        return ret;
    }
}
