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
package dev.triumphteam.cmd.core.command;

import dev.triumphteam.cmd.core.annotations.Syntax;
import dev.triumphteam.cmd.core.argument.InternalArgument;
import dev.triumphteam.cmd.core.argument.LimitlessInternalArgument;
import dev.triumphteam.cmd.core.argument.StringInternalArgument;
import dev.triumphteam.cmd.core.exceptions.CommandExecutionException;
import dev.triumphteam.cmd.core.extention.CommandOptions;
import dev.triumphteam.cmd.core.extention.Result;
import dev.triumphteam.cmd.core.extention.ValidationResult;
import dev.triumphteam.cmd.core.extention.meta.CommandMeta;
import dev.triumphteam.cmd.core.extention.registry.MessageRegistry;
import dev.triumphteam.cmd.core.extention.sender.SenderExtension;
import dev.triumphteam.cmd.core.message.MessageKey;
import dev.triumphteam.cmd.core.message.context.BasicMessageContext;
import dev.triumphteam.cmd.core.message.context.InvalidArgumentContext;
import dev.triumphteam.cmd.core.message.context.MessageContext;
import dev.triumphteam.cmd.core.processor.CommandProcessor;
import dev.triumphteam.cmd.core.processor.SubCommandProcessor;
import dev.triumphteam.cmd.core.requirement.Requirement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SubCommand<D, S> implements ExecutableCommand<S> {

    private final Class<? extends S> senderType;
    private final List<InternalArgument<S, ?>> arguments;
    private final List<Requirement<D, S, ?>> requirements;

    private final String name;
    private final String syntax;
    private final CommandMeta meta;
    private final boolean containsLimitless;

    private final Object invocationInstance;
    private final Method method;
    private final CommandExecutor commandExecutor;

    private final SenderExtension<D, S> senderExtension;
    private final MessageRegistry<S> messageRegistry;

    public SubCommand(
            final @NotNull Object invocationInstance,
            final @NotNull Method method,
            final @NotNull SubCommandProcessor<D, S> processor,
            final @NotNull Command parentCommand
    ) {
        this.invocationInstance = invocationInstance;
        this.method = method;
        this.name = processor.getName();
        this.meta = processor.createMeta();
        this.senderType = processor.senderType();
        this.arguments = processor.arguments(meta);
        this.requirements = processor.requirements();

        this.containsLimitless = arguments.stream().anyMatch(LimitlessInternalArgument.class::isInstance);

        final CommandOptions<D, S> commandOptions = processor.getCommandOptions();

        this.messageRegistry = processor.getRegistryContainer().getMessageRegistry();
        this.senderExtension = commandOptions.getSenderExtension();
        this.commandExecutor = commandOptions.getCommandExtensions().getCommandExecutor();

        this.syntax = createSyntax(parentCommand, processor);
    }

    @Override
    public void execute(
            final @NotNull S sender,
            final @Nullable Supplier<Object> instanceSupplier,
            final @NotNull Deque<String> arguments,
            final @NotNull Map<String, Object> extra
    ) throws Throwable {
        final ValidationResult<MessageKey<MessageContext>> validationResult = senderExtension.validate(meta, senderType, sender);

        // If the result is invalid for a reason given by the validator, we stop the execution and use its key to send
        // a message to the sender
        if (validationResult instanceof ValidationResult.Invalid) {
            messageRegistry.sendMessage(
                    ((ValidationResult.Invalid<MessageKey<MessageContext>>) validationResult).getMessage(),
                    sender,
                    new BasicMessageContext(meta, syntax)
            );
            return;
        }

        if (!meetRequirements(sender)) return;

        // Creates the invoking arguments list
        final List<java.lang.Object> invokeArguments = new ArrayList<>();
        invokeArguments.add(sender);

        if (!validateAndCollectArguments(sender, invokeArguments, arguments)) {
            return;
        }

        if ((!containsLimitless) && arguments.size() >= invokeArguments.size()) {
            messageRegistry.sendMessage(MessageKey.TOO_MANY_ARGUMENTS, sender, new BasicMessageContext(meta, syntax));
            return;
        }

        commandExecutor.execute(
                meta,
                instanceSupplier == null ? invocationInstance : instanceSupplier.get(),
                method,
                invokeArguments
        );
    }

    @Override
    public @NotNull CommandMeta getMeta() {
        return meta;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getSyntax() {
        return syntax;
    }

    @Override
    public @NotNull Object getInvocationInstance() {
        return invocationInstance;
    }

    @Override
    public boolean isDefault() {
        return name.equals(dev.triumphteam.cmd.core.annotations.Command.DEFAULT_CMD_NAME);
    }

    @Override
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * Used for checking if the arguments are valid and adding them to the `invokeArguments`.
     *
     * @param sender          The sender of the command.
     * @param invokeArguments A list with the arguments that'll be used on the `invoke` of the command method.
     * @param commandArgs     The command arguments type.
     * @return False if any internalArgument fails to pass.
     */
    @SuppressWarnings("unchecked")
    private boolean validateAndCollectArguments(
            final @NotNull S sender,
            final @NotNull List<Object> invokeArguments,
            final @NotNull Deque<String> commandArgs
    ) {
        for (final InternalArgument<S, ?> internalArgument : arguments) {
            final @NotNull Result<@Nullable Object, BiFunction<@NotNull CommandMeta, @NotNull String, @NotNull InvalidArgumentContext>> result;
            if (internalArgument instanceof LimitlessInternalArgument) {
                final LimitlessInternalArgument<S> limitlessArgument = (LimitlessInternalArgument<S>) internalArgument;

                // From this point on [commandArgs] is treated as a simple Collection instead of Deque
                result = limitlessArgument.resolve(sender, commandArgs);
            } else if (internalArgument instanceof StringInternalArgument) {
                final StringInternalArgument<S> stringArgument = (StringInternalArgument<S>) internalArgument;
                final String arg = commandArgs.peek();

                if (arg == null || arg.isEmpty()) {
                    if (internalArgument.isOptional()) {
                        invokeArguments.add(null);
                        continue;
                    }

                    messageRegistry.sendMessage(MessageKey.NOT_ENOUGH_ARGUMENTS, sender, new BasicMessageContext(meta, syntax));
                    return false;
                }

                // Pop the command out
                commandArgs.pop();
                result = stringArgument.resolve(sender, arg);
            } else {
                // Should never happen, this should be a sealed type ... but hey, it's Java 8
                throw new CommandExecutionException("Found unsupported argument", "", name);
            }

            // In case of failure we send the Sender a message
            if (result instanceof Result.Failure) {
                messageRegistry.sendMessage(
                        MessageKey.INVALID_ARGUMENT,
                        sender,
                        ((Result.Failure<@Nullable Object, BiFunction<@NotNull CommandMeta, @NotNull String, @NotNull InvalidArgumentContext>>) result)
                                .getFail()
                                .apply(meta, syntax)
                );
                return false;
            }

            // In case of success we add the results
            if (result instanceof Result.Success) {
                invokeArguments.add(((Result.Success<@Nullable Object, BiFunction<@NotNull CommandMeta, @NotNull String, @NotNull InvalidArgumentContext>>) result).getValue());
            }
        }

        return true;
    }

    /**
     * Checks if the requirements to run the command are met.
     *
     * @param sender The sender of the command.
     * @return Whether all requirements are met.
     */
    private boolean meetRequirements(final @NotNull S sender) {
        for (final Requirement<D, S, ?> requirement : requirements) {
            if (!requirement.isMet(sender, meta, senderExtension)) {
                requirement.sendMessage(messageRegistry, sender, meta, syntax);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets an internalArgument value or null.
     *
     * @param list  The list to check from.
     * @param index The current index of the internalArgument.
     * @return The internalArgument name or null.
     */
    private @Nullable String valueOrNull(final @NotNull List<String> list, final int index) {
        if (index >= list.size()) return null;
        return list.get(index);
    }

    /**
     * Gets the left over of the arguments.
     *
     * @param list The list with all the arguments.
     * @param from The index from which should start removing.
     * @return A list with the leftover arguments.
     */
    private @NotNull List<String> leftOvers(final @NotNull List<String> list, final int from) {
        if (from > list.size()) return Collections.emptyList();
        return list.subList(from, list.size());
    }

    private @NotNull String createSyntax(final @NotNull Command parentCommand, final @NotNull CommandProcessor processor) {
        final Syntax syntaxAnnotation = processor.getSyntaxAnnotation();
        if (syntaxAnnotation != null) return syntaxAnnotation.value();

        final StringBuilder builder = new StringBuilder(parentCommand.getSyntax());

        if (!dev.triumphteam.cmd.core.annotations.Command.DEFAULT_CMD_NAME.equals(name)) {
            builder.append(" ").append(name);
        }

        arguments.forEach(argument -> builder.append(" ").append("<").append(argument.getName()).append(">"));

        return builder.toString();
    }
}
