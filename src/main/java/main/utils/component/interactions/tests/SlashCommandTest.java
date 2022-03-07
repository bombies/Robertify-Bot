package main.utils.component.interactions.tests;

import lombok.SneakyThrows;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class SlashCommandTest extends AbstractSlashCommand {

    @Override @SneakyThrows
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("testcommand")
                        .setDescription("Test Description")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "testOption1",
                                        "Test Description 1",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "testOption2",
                                        "Test Description 2",
                                        true
                                )
                        )
                        .build()
        );
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event))
            return;

        event.reply("Test command works!").queue();
    }
}
