package dev.triumphteam.cmd.core.argument.keyed.internal;

import dev.triumphteam.cmd.core.suggestion.EmptySuggestion;
import dev.triumphteam.cmd.core.suggestion.SuggestionKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Flag {

    /**
     * Creates a {@link Flag} builder.
     *
     * @param flag The flag value to start with.
     * @return A {@link Argument.Builder} to create a new {@link Flag}.
     */
    @Contract("_ -> new")
    static @NotNull Builder flag(final @NotNull String flag) {
        return new Builder().flag(flag);
    }

    /**
     * Creates a {@link Flag} builder.
     *
     * @param longFlag The long flag value to start with.
     * @return A {@link Argument.Builder} to create a new {@link Flag}.
     */
    @Contract("_ -> new")
    static @NotNull Builder longFlag(final @NotNull String longFlag) {
        return new Builder().longFlag(longFlag);
    }

    /**
     * @return The flag identifier.
     */
    @Nullable String getFlag();

    /**
     * @return The long flag identifier.
     */
    @Nullable String getLongFlag();

    /**
     * @return Either be the {@link Flag#getFlag()} or the {@link Flag#getLongFlag()}..
     */
    @NotNull String getKey();

    /**
     * @return The description of the flag.
     */
    @NotNull String getDescription();

    /**
     * @return The {@link SuggestionKey} to be used.
     */
    @Nullable SuggestionKey getSuggestion();

    /**
     * @return Whether the flag contains arguments.
     */
    boolean hasArgument();

    /**
     * Simple builder for creating new {@link Flag}s.
     */
    final class Builder {

        private String flag;
        private String longFlag;
        private String description;
        private Class<?> argument;
        private SuggestionKey suggestionKey;

        /**
         * Sets the flag name.
         *
         * @param flag The flag to be used.
         * @return This builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder flag(final @NotNull String flag) {
            this.flag = flag;
            return this;
        }

        /**
         * Sets the long flag name.
         *
         * @param longFlag The long flag to be used.
         * @return This builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder longFlag(final @NotNull String longFlag) {
            this.longFlag = longFlag;
            return this;
        }

        /**
         * Sets the description of the Flag.
         *
         * @param description The description of the Flag.
         * @return This builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder description(final @NotNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the argument type of the Flag.
         *
         * @param argumentType The argument type of the Flag.
         * @return This builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder argument(final @NotNull Class<?> argumentType) {
            this.argument = argumentType;
            return this;
        }

        /**
         * Sets the suggestion key to be used by the Flag.
         * If not are supplied the Flag will use {@link EmptySuggestion} instead.
         *
         * @param suggestionKey The registered suggestion key.
         * @return This builder.
         */
        @Contract("_ -> this")
        public @NotNull Builder suggestion(final @NotNull SuggestionKey suggestionKey) {
            this.suggestionKey = suggestionKey;
            return this;
        }

        /**
         * Builds the flag.
         *
         * @return A new {@link Flag} with the data from this builder.
         */
        @Contract(" -> new")
        public @NotNull Flag build() {
            return new FlagOptions(this);
        }

        @NotNull Class<?> getArgument() {
            return argument;
        }

        @NotNull String getFlag() {
            return flag;
        }

        @NotNull String getLongFlag() {
            return longFlag;
        }

        @NotNull String getDescription() {
            return description;
        }

        @Nullable SuggestionKey getSuggestionKey() {
            return suggestionKey;
        }
    }
}