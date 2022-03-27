package dev.triumphteam.cmd.core.argument.named;

import dev.triumphteam.cmd.core.exceptions.CommandRegistrationException;
import dev.triumphteam.cmd.core.suggestion.SuggestionKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
abstract class AbstractArgumentBuilder<T extends AbstractArgumentBuilder<T>> {

    private final Class<?> type;
    private String name;
    private String description = "Description!";
    private SuggestionKey suggestionKey;

    public AbstractArgumentBuilder(@NotNull final Class<?> type) {
        this.type = type;
    }

    /**
     * Sets the name of the argument.
     *
     * @param name The name of the argument.
     * @return This builder.
     */
    @NotNull
    @Contract("_ -> this")
    public T name(@NotNull final String name) {
        this.name = name;
        return (T) this;
    }

    /**
     * Sets the description of the argument.
     *
     * @param description The description of the argument.
     * @return This builder.
     */
    @NotNull
    @Contract("_ -> this")
    public T description(@NotNull final String description) {
        this.description = description;
        return (T) this;
    }

    @NotNull
    @Contract("_ -> this")
    public T suggestion(@NotNull final SuggestionKey suggestionKey) {
        this.suggestionKey = suggestionKey;
        return (T) this;
    }

    /**
     * Builds the argument.
     *
     * @return A new {@link Argument} with the data from this builder.
     */
    @NotNull
    @Contract(" -> new")
    public Argument build() {
        return new SimpleArgument(this);
    }

    @NotNull
    Class<?> getType() {
        return type;
    }

    @NotNull
    String getName() {
        if (name == null || name.isEmpty()) {
            throw new CommandRegistrationException("Argument is missing a name!");
        }
        return name;
    }

    @NotNull
    String getDescription() {
        return description;
    }

    @Nullable
    SuggestionKey getSuggestionKey() {
        return suggestionKey;
    }
}