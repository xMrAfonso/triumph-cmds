/**
 * MIT License
 *
 * Copyright (c) 2019-2021 Matt
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.triumphteam.cmd.slash;

import dev.triumphteam.cmd.core.extention.CommandExtensions;
import dev.triumphteam.cmd.core.extention.CommandOptions;
import dev.triumphteam.cmd.core.extention.defaults.DefaultArgumentValidator;
import dev.triumphteam.cmd.core.extention.defaults.DefaultCommandExecutor;
import dev.triumphteam.cmd.core.extention.registry.RegistryContainer;
import dev.triumphteam.cmd.core.extention.sender.SenderExtension;
import dev.triumphteam.cmd.slash.sender.SlashSender;
import org.jetbrains.annotations.NotNull;

public final class SlashCommandOptions<S> extends CommandOptions<SlashSender, S> {

    public SlashCommandOptions(
            final @NotNull SenderExtension<SlashSender, S> senderExtension,
            final @NotNull CommandExtensions<SlashSender, S> commandExtensions
    ) {
        super(senderExtension, commandExtensions);
    }

    public static final class Setup<S> extends CommandOptions.Setup<SlashSender, S, Setup<S>> {
        public Setup(final @NotNull RegistryContainer<SlashSender, S> registryContainer) {
            super(registryContainer);
        }
    }

    public static final class Builder<S> extends CommandOptions.Builder<SlashSender, S, SlashCommandOptions<S>, Setup<S>, Builder<S>> {

        public Builder(final @NotNull RegistryContainer<SlashSender, S> registryContainer) {
            super(new Setup<>(registryContainer));

            // Setters have to be done first thing, so they can be overriden.
            extensions(extension -> {
                extension.setArgumentValidator(new DefaultArgumentValidator<>());
                extension.setCommandExecutor(new DefaultCommandExecutor());
            });
        }

        @Override
        public @NotNull SlashCommandOptions<S> build(final @NotNull SenderExtension<SlashSender, S> senderExtension) {
            return new SlashCommandOptions<>(senderExtension, getCommandExtensions());
        }
    }
}
