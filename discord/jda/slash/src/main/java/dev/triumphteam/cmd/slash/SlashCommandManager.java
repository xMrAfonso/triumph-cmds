/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2021 Matt
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.triumphteam.cmd.slash;

import dev.triumphteam.cmd.core.CommandManager;
import dev.triumphteam.cmd.core.command.RootCommand;
import dev.triumphteam.cmd.core.extention.registry.MessageRegistry;
import dev.triumphteam.cmd.core.extention.sender.SenderExtension;
import dev.triumphteam.cmd.core.processor.RootCommandProcessor;
import dev.triumphteam.cmd.slash.choices.ChoiceKey;
import dev.triumphteam.cmd.slash.sender.SlashSender;
import dev.triumphteam.cmd.slash.util.JdaMappingUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Command Manager for Slash Commands.
 * Allows for registering of global and guild specific commands.
 * As well the implementation of custom command senders.
 *
 * @param <S> The sender type.
 */
public final class SlashCommandManager<S> extends CommandManager<SlashSender, S> {

    private final JDA jda;

    private final SlashRegistryContainer<S> registryContainer;

    private final Map<String, RootCommand<SlashSender, S>> globalCommands = new HashMap<>();
    private final Map<Long, Map<String, RootCommand<SlashSender, S>>> guildCommands = new HashMap<>();

    public SlashCommandManager(
            final @NotNull JDA jda,
            final @NotNull SlashCommandOptions<S> commandOptions,
            final @NotNull SlashRegistryContainer<S> registryContainer
    ) {
        super(commandOptions);
        this.jda = jda;
        this.registryContainer = registryContainer;

        registerArgument(User.class, (sender, arg) -> "");
        // jda.addEventListener(new SlashCommandListener<>(this, senderMapper));
    }

    @Contract("_, _, _ -> new")
    public static <S> @NotNull SlashCommandManager<S> create(
            final @NotNull JDA jda,
            final @NotNull SenderExtension<SlashSender, S> senderExtension,
            final @NotNull Consumer<SlashCommandOptions.Builder<S>> builder
    ) {
        final SlashRegistryContainer<S> registryContainer = new SlashRegistryContainer<>();
        final SlashCommandOptions.Builder<S> extensionBuilder = new SlashCommandOptions.Builder<>(registryContainer);
        builder.accept(extensionBuilder);
        return new SlashCommandManager<>(jda, extensionBuilder.build(senderExtension), registryContainer);
    }

    @Contract("_, _ -> new")
    public static @NotNull SlashCommandManager<SlashSender> create(
            final @NotNull JDA jda,
            final @NotNull Consumer<SlashCommandOptions.Builder<SlashSender>> builder
    ) {
        final SlashRegistryContainer<SlashSender> registryContainer = new SlashRegistryContainer<>();
        final SlashCommandOptions.Builder<SlashSender> extensionBuilder = new SlashCommandOptions.Builder<>(registryContainer);

        // Setup defaults for Bukkit
        final MessageRegistry<SlashSender> messageRegistry = registryContainer.getMessageRegistry();
        // setUpDefaults(messageRegistry);

        // Then accept configured values
        builder.accept(extensionBuilder);
        return new SlashCommandManager<>(jda, extensionBuilder.build(new SlashSenderExtension()), registryContainer);
    }

    @Contract("_ -> new")
    public static @NotNull SlashCommandManager<SlashSender> create(final @NotNull JDA jda) {
        return create(jda, builder -> {});
    }

    public void registerChoices(final @NotNull ChoiceKey key, final @NotNull Supplier<List<String>> choiceSupplier) {
        registryContainer.getChoiceRegistry().register(key, choiceSupplier);
    }

    public void execute(final @NotNull SlashCommandInteractionEvent event) {
        final Guild guild = event.getGuild();
        if (guild == null) return;

        final String name = event.getName();
        final RootCommand<SlashSender, S> command = guildCommands
                .getOrDefault(guild.getIdLong(), Collections.emptyMap())
                .get(name);

        final SenderExtension<SlashSender, S> senderExtension = getCommandOptions().getSenderExtension();
        final S sender = senderExtension.map(new SlashCommandSender(event));


        // TODO continue
    }

    @Override
    public void registerCommand(final @NotNull Object command) {

    }

    public void registerCommand(final Guild guild, final @NotNull Object command) {
        registerCommand(guild.getIdLong(), command);
    }

    public void registerCommand(final Long guildId, final @NotNull Object command) {
        final RootCommandProcessor<SlashSender, S> processor = new RootCommandProcessor<>(
                command,
                getRegistryContainer(),
                getCommandOptions()
        );

        final String name = processor.getName();

        // Get or add command, then add its sub commands
        final RootCommand<SlashSender, S> rootCommand = guildCommands
                .computeIfAbsent(guildId, it -> new HashMap<>())
                .computeIfAbsent(name, it -> new RootCommand<>(processor));

        rootCommand.addCommands(command, processor.commands(rootCommand));
    }

    public void pushCommands() {
        guildCommands.forEach((key, value) -> {
            final Guild guild = jda.getGuildById(key);
            if (guild == null) return;
            guild.updateCommands()
                    .addCommands(value.values().stream().map(JdaMappingUtil::mapCommand).collect(Collectors.toList()))
                    .queue();
        });
    }

    @Override
    public void unregisterCommand(final @NotNull Object command) {

    }

    @Override
    protected @NotNull SlashRegistryContainer<S> getRegistryContainer() {
        return registryContainer;
    }
}
