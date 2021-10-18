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
package dev.triumphteam.cmds.core.flag.internal.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Returned when the flag contains an invalid argument.
 *
 * @param <S> The sender type.
 */
public final class InvalidFlagArgumentResult<S> implements ParseResult<S> {

    private final String typedArgument;
    private final String flag;
    private final Class<?> argumentType;

    public InvalidFlagArgumentResult(
            @NotNull final String typedArgument,
            @NotNull final String flag,
            @NotNull final Class<?> argumentType
    ) {
        this.typedArgument = typedArgument;
        this.flag = flag;
        this.argumentType = argumentType;
    }

    /**
     * Gets the argument the user typed.
     *
     * @return The typed argument.
     */
    @NotNull
    public String getTypedArgument() {
        return typedArgument;
    }

    /**
     * Gets the flag that had the issue.
     *
     * @return The flag.
     */
    @NotNull
    public String getFlag() {
        return flag;
    }

    /**
     * Gets the type the argument should have been.
     *
     * @return The argument type.
     */
    @NotNull
    public Class<?> getArgumentType() {
        return argumentType;
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final InvalidFlagArgumentResult<?> that = (InvalidFlagArgumentResult<?>) o;
        return typedArgument.equals(that.typedArgument) && flag.equals(that.flag) && argumentType.equals(that.argumentType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typedArgument, flag, argumentType);
    }

    @Override
    public String toString() {
        return "InvalidFlagArgumentResult{" +
                "typedArgument='" + typedArgument + '\'' +
                ", flag='" + flag + '\'' +
                ", argumentType=" + argumentType +
                '}';
    }
}