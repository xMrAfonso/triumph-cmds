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
package dev.triumphteam.cmds.core.factory;

import dev.triumphteam.cmds.core.BaseCommand;
import dev.triumphteam.cmds.core.annotations.CommandFlags;
import dev.triumphteam.cmds.core.annotations.Default;
import dev.triumphteam.cmds.core.annotations.Flag;
import dev.triumphteam.cmds.core.annotations.Join;
import dev.triumphteam.cmds.core.annotations.Optional;
import dev.triumphteam.cmds.core.annotations.Optional;
import dev.triumphteam.cmds.core.annotations.Requirements;
import dev.triumphteam.cmds.core.annotations.SubCommand;
import dev.triumphteam.cmds.core.argument.ArgumentRegistry;
import dev.triumphteam.cmds.core.argument.ArgumentResolver;
import dev.triumphteam.cmds.core.argument.types.Argument;
import dev.triumphteam.cmds.core.argument.types.ArrayArgument;
import dev.triumphteam.cmds.core.argument.types.CollectionArgument;
import dev.triumphteam.cmds.core.argument.types.EnumArgument;
import dev.triumphteam.cmds.core.argument.types.FlagArgument;
import dev.triumphteam.cmds.core.argument.types.JoinedStringArgument;
import dev.triumphteam.cmds.core.argument.types.LimitlessArgument;
import dev.triumphteam.cmds.core.argument.types.ResolverArgument;
import dev.triumphteam.cmds.core.argument.types.StringArgument;
import dev.triumphteam.cmds.core.exceptions.SubCommandRegistrationException;
import dev.triumphteam.cmds.core.flag.Flags;
import dev.triumphteam.cmds.core.flag.internal.FlagGroup;
import dev.triumphteam.cmds.core.flag.internal.FlagOptions;
import dev.triumphteam.cmds.core.message.MessageKey;
import dev.triumphteam.cmds.core.message.MessageRegistry;
import dev.triumphteam.cmds.core.message.context.MessageContext;
import dev.triumphteam.cmds.core.requirement.Requirement;
import dev.triumphteam.cmds.core.requirement.RequirementKey;
import dev.triumphteam.cmds.core.requirement.RequirementRegistry;
import dev.triumphteam.cmds.core.requirement.RequirementResolver;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.triumphteam.cmds.core.factory.AnnotationUtil.getAnnotation;
import static dev.triumphteam.cmds.core.flag.internal.FlagValidator.validate;

/**
 * Abstracts most of the "extracting" from sub command annotations, allows for extending.
 * <br/>
 * I know this could be done better, but couldn't think of a better way.
 * If you do please PR or let me know on my discord!
 *
 * @param <S>  The sender type.
 * @param <SC> The sub command type.
 */
public abstract class AbstractSubCommandFactory<S, SC extends dev.triumphteam.cmds.core.SubCommand<S>> {

    private final BaseCommand baseCommand;
    private final Method method;
    // Name is nullable to detect if the method should or not be considered a sub command.
    private String name = null;
    private final List<String> alias = new ArrayList<>();
    private boolean isDefault = false;

    private final FlagGroup<S> flagGroup = new FlagGroup<>();
    private final List<Argument<S, ?>> arguments = new ArrayList<>();
    private final Set<Requirement<S>> requirements = new HashSet<>();

    private final ArgumentRegistry<S> argumentRegistry;
    private final RequirementRegistry<S> requirementRegistry;
    private final MessageRegistry<S> messageRegistry;

    protected AbstractSubCommandFactory(
            @NotNull final BaseCommand baseCommand,
            @NotNull final Method method,
            @NotNull final ArgumentRegistry<S> argumentRegistry,
            @NotNull final RequirementRegistry<S> requirementRegistry,
            @NotNull final MessageRegistry<S> messageRegistry
    ) {
        this.baseCommand = baseCommand;
        this.method = method;

        this.argumentRegistry = argumentRegistry;
        this.requirementRegistry = requirementRegistry;
        this.messageRegistry = messageRegistry;

        extractSubCommandNames();
        if (name == null) return;

        extractFlags();
        extractRequirements();
        extractArguments(method);
        validateArguments();
    }

    /**
     * Abstract method so children can handle the return of the new {@link dev.triumphteam.cmds.core.SubCommand}.
     * Nullable so the method can be ignored.
     *
     * @return A {@link dev.triumphteam.cmds.core.SubCommand} implementation.
     */
    @Nullable
    public abstract SC create(@NotNull final String parentName);

    /**
     * Allows for customizing the argument parsing, for example <code>@Value</code> and <code>@Completion</code> annotations.
     *
     * @param method The method to search from.
     */
    protected abstract void extractArguments(@NotNull final Method method);

    /**
     * Used for the child factories to get the sub command name.
     * It's nullable because a method might not have a {@link SubCommand} or {@link Default} annotation.
     *
     * @return The sub command name.
     */
    @Nullable
    protected String getName() {
        return name;
    }

    /**
     * Used for the child factories to get a {@link List<String>} with the sub command's alias.
     *
     * @return The sub command alias.
     */
    @NotNull
    protected List<String> getAlias() {
        return alias;
    }

    /**
     * Used for the child factories to get whether the sub command is default.
     *
     * @return Whether the command is default.
     */
    protected boolean isDefault() {
        return isDefault;
    }

    /**
     * Gets the {@link BaseCommand} instance, so it can be used later to invoke.
     *
     * @return The base command instance.
     */
    @NotNull
    protected BaseCommand getBaseCommand() {
        return baseCommand;
    }

    /**
     * Gets the method.
     *
     * @return The method.
     */
    @NotNull
    protected Method getMethod() {
        return method;
    }

    /**
     * Gets a set with the requirements.
     *
     * @return The requirements.
     */
    @NotNull
    protected Set<Requirement<S>> getRequirements() {
        return requirements;
    }

    /**
     * Gets the message registry.
     *
     * @return The message registry.
     */
    @NotNull
    protected MessageRegistry<S> getMessageRegistry() {
        return messageRegistry;
    }

    /**
     * Simple utility method for creating a new exception using the method and base command class.
     *
     * @param message The main message to pass to the exception.
     * @return A new {@link SubCommandRegistrationException}.
     */
    @NotNull
    @Contract("_ -> new")
    protected SubCommandRegistrationException createException(@NotNull final String message) {
        return new SubCommandRegistrationException(message, method, baseCommand.getClass());
    }

    /**
     * Gets the necessary arguments for the command.
     *
     * @return The arguments list.
     */
    @NotNull
    protected List<Argument<S, ?>> getArguments() {
        return arguments;
    }

    /**
     * Creates and adds the argument to the arguments list.
     *
     * @param parameter The current parameter to get data from.
     */
    protected void createArgument(@NotNull final Parameter parameter) {
        final Class<?> type = parameter.getType();
        final String parameterName = parameter.getName();
        final boolean optional = parameter.isAnnotationPresent(Optional.class);

        // Handler for using any Enum.
        if (Enum.class.isAssignableFrom(type)) {
            //noinspection unchecked
            addArgument(new EnumArgument<>(parameterName, (Class<? extends Enum<?>>) type, optional));
            return;
        }

        if (type == String[].class) {
            addArgument(new ArrayArgument<>(parameterName, optional));
            return;
        }

        // Handles collection argument.
        // TODO: Add more collection types.
        if (List.class.isAssignableFrom(type) || Set.class.isAssignableFrom(type)) {
            final ParameterizedType parameterizedType = (ParameterizedType) parameter.getParameterizedType();
            final Type[] types = parameterizedType.getActualTypeArguments();

            if (types.length != 1) {
                throw createException("Unsupported collection type \"" + type + "\"");
            }

            if (types[0] != String.class) {
                throw createException("Only String collections are allowed");
            }

            addArgument(new CollectionArgument<>(parameterName, type, optional));
            return;
        }

        // Handler for using String with `@Join`.
        if (type == String.class && parameter.isAnnotationPresent(Join.class)) {
            final Join joinAnnotation = parameter.getAnnotation(Join.class);
            addArgument(new JoinedStringArgument<>(parameterName, joinAnnotation.value(), optional));
            return;
        }

        // Handler for flags.
        if (type == Flags.class) {
            addArgument(new FlagArgument<>(flagGroup, parameterName, optional));
            return;
        }

        // All other types default to the resolver.
        final ArgumentResolver<S> resolver = argumentRegistry.getResolver(type);
        if (resolver == null) {
            throw createException("No argument of type \"" + type.getName() + "\" registered");
        }

        addArgument(new ResolverArgument<>(parameterName, type, resolver, optional));
    }

    /**
     * Utility to add the argument to the list.
     *
     * @param argument The created argument.
     */
    private void addArgument(@NotNull final Argument<S, ?> argument) {
        arguments.add(argument);
    }

    /**
     * Extracts the data from the method to retrieve the sub command name or the default name.
     */
    private void extractSubCommandNames() {
        final Default defaultAnnotation = getAnnotation(method, Default.class);
        final SubCommand subCommandAnnotation = getAnnotation(method, SubCommand.class);

        if (defaultAnnotation == null && subCommandAnnotation == null) {
            return;
        }

        if (defaultAnnotation != null) {
            name = Default.DEFAULT_CMD_NAME;
            alias.addAll(Arrays.stream(defaultAnnotation.alias()).map(String::toLowerCase).collect(Collectors.toList()));
            isDefault = true;
            return;
        }

        name = subCommandAnnotation.value().toLowerCase();
        alias.addAll(Arrays.stream(subCommandAnnotation.alias()).map(String::toLowerCase).collect(Collectors.toList()));

        if (this.name.isEmpty()) {
            throw createException("@" + SubCommand.class.getSimpleName() + " name must not be empty");
        }
    }

    /**
     * Extract all the flag data for the subcommand from the method.
     */
    private void extractFlags() {
        final CommandFlags commandFlags = getAnnotation(method, CommandFlags.class);
        if (commandFlags == null) return;

        final Flag[] flags = commandFlags.value();
        if (flags.length == 0) {
            throw createException("@" + CommandFlags.class.getSimpleName() + " must not be empty");
        }

        for (final Flag flagAnnotation : flags) {
            String flag = flagAnnotation.flag();
            if (flag.isEmpty()) flag = null;
            validate(flag, method, baseCommand);

            String longFlag = flagAnnotation.longFlag();
            if (longFlag.contains(" ")) {
                throw createException("@" + Flag.class.getSimpleName() + "'s identifiers must not contain spaces");
            }

            if (longFlag.isEmpty()) longFlag = null;

            final Class<?> argumentType = flagAnnotation.argument();
            StringArgument<S> argument = null;
            if (argumentType != void.class) {
                if (Enum.class.isAssignableFrom(argumentType)) {
                    //noinspection unchecked
                    argument = new EnumArgument<>(argumentType.getName(), (Class<? extends Enum<?>>) argumentType, false);
                } else {
                    final ArgumentResolver<S> resolver = argumentRegistry.getResolver(argumentType);
                    if (resolver == null) {
                        throw createException("@" + Flag.class.getSimpleName() + "'s argument contains unregistered type \"" + argumentType.getName() + "\"");
                    }

                    argument = new ResolverArgument<>(argumentType.getName(), argumentType, resolver, false);
                }
            }

            flagGroup.addFlag(
                    new FlagOptions<>(
                            flag,
                            longFlag,
                            argument,
                            flagAnnotation.optionalArg(),
                            flagAnnotation.required()
                    )
            );
        }
    }

    /**
     * Extract all the requirement data for the sub command from the method.
     */
    public void extractRequirements() {
        final Requirements requirementsAnnotation = getAnnotation(method, Requirements.class);
        if (requirementsAnnotation == null) {
            return;
        }

        for (final dev.triumphteam.cmds.core.annotations.Requirement requirementAnnotation : requirementsAnnotation.value()) {
            final RequirementKey requirementKey = RequirementKey.of(requirementAnnotation.value());
            final String messageKeyValue = requirementAnnotation.messageKey();

            final MessageKey<MessageContext> messageKey;
            if (messageKeyValue.isEmpty()) messageKey = null;
            else messageKey = MessageKey.of(messageKeyValue, MessageContext.class);

            final RequirementResolver<S> resolver = requirementRegistry.getRequirement(requirementKey);
            if (resolver == null) {
                throw createException("Could not find Requirement Key \"" + requirementKey.getKey() + "\"");
            }

            requirements.add(new Requirement<>(resolver, messageKey));
        }
    }

    /**
     * Argument validation makes sure some arguments are placed in the correct order.
     * For example a limitless argument and flags argument being one after the other, like:
     * `@Join final String text, final Flags flags`.
     * TODO: This can be improved.
     */
    private void validateArguments() {
        final int argSize = arguments.size();
        int limitlessPosition = -1;
        int flagsPosition = -1;

        // Collects validatable argument's position.
        for (int i = 0; i < argSize; i++) {
            final Argument<S, ?> argument = arguments.get(i);

            if (argument.isOptional() && i != argSize - 1) {
                throw createException("Optional argument is only allowed as the last argument");
            }

            if (argument instanceof FlagArgument) {
                if (flagGroup.isEmpty()) {
                    throw createException("\"Flags\" argument found but no \"CommandFlags\" annotation present");
                }

                if (flagsPosition != -1) {
                    throw createException("More than one \"Flags\" argument declared");
                }

                flagsPosition = i;
                continue;
            }

            if (argument instanceof LimitlessArgument) {
                if (limitlessPosition != -1) {
                    throw createException("More than one limitless argument declared");
                }

                limitlessPosition = i;
            }
        }

        // If flags argument is present check if it's the last one and if there is a limitless behind of it instead of after.
        if (flagsPosition != -1) {
            if (limitlessPosition != -1 && limitlessPosition != argSize - 2) {
                throw createException("\"Flags\" argument must always be after a limitless argument");
            }

            if (flagsPosition != argSize - 1) {
                throw createException("\"Flags\" argument must always be the last argument");
            }

            return;
        }

        // If it's a limitless argument checks if it's the last argument.
        if (limitlessPosition != -1 && limitlessPosition != argSize - 1) {
            throw createException("Limitless argument must be the last argument if \"Flags\" is not present");
        }
    }

    public RequirementRegistry<S> getRequirementRegistry() {
        return requirementRegistry;
    }
}