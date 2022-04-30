package main.utils.pagination;

import lombok.Getter;
import main.utils.component.interactions.selectionmenu.SelectMenuOption;

import java.util.ArrayList;
import java.util.List;

public class MenuPage {
    @Getter
    private final List<SelectMenuOption> options = new ArrayList<>();

    public void addOption(SelectMenuOption option) {
        if (options.size() >= 25)
            throw new IllegalArgumentException("You cannot add any more options to the page!");

        options.add(option);
    }

    public void removeOption(int i) {
        options.remove(i);
    }

    public SelectMenuOption getOption(int i) {
        return options.get(i);
    }

    public List<String> toStringList() {
        final List<String> ret = new ArrayList<>();
        for (SelectMenuOption option : options) {
            if (option.toString().contains("Next Page") ||
                    option.toString().contains("Previous Page")
            ) continue;

            ret.add(option.toString());
        }

        return ret;
    }
}
