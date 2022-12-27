package dev.triumphteam.cmd.core.extention;

import dev.triumphteam.cmd.core.extention.annotation.AnnotationProcessor;
import dev.triumphteam.cmd.core.extention.argument.ArgumentValidator;
import dev.triumphteam.cmd.core.extention.argument.CommandMethodProcessor;
import dev.triumphteam.cmd.core.extention.sender.SenderExtension;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public final class CommandExtensions<D, S> {

    private final Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation>> annotationProcessors;
    private final List<CommandMethodProcessor> commandMethodProcessors;

    private final SenderExtension<D, S> senderExtension;
    private final ArgumentValidator<S> argumentValidator;

    public CommandExtensions(
            final @NotNull Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation>> annotationProcessors,
            final @NotNull List<CommandMethodProcessor> commandMethodProcessors,
            final @NotNull SenderExtension<D, S> senderExtension,
            final @NotNull ArgumentValidator<S> argumentValidator
    ) {
        this.annotationProcessors = annotationProcessors;
        this.commandMethodProcessors = commandMethodProcessors;
        this.senderExtension = senderExtension;
        this.argumentValidator = argumentValidator;
    }

    public @NotNull Map<Class<? extends Annotation>, AnnotationProcessor<? extends Annotation>> getAnnotationProcessors() {
        return annotationProcessors;
    }

    public @NotNull List<CommandMethodProcessor> getCommandMethodProcessors() {
        return commandMethodProcessors;
    }

    public @NotNull SenderExtension<D, S> getSenderExtension() {
        return senderExtension;
    }

    public @NotNull ArgumentValidator<S> getArgumentValidator() {
        return argumentValidator;
    }
}
