package com.lambda_manager.core;

import com.lambda_manager.utils.Messages;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.exceptions.ExceptionHandler;
import io.micronaut.http.annotation.Produces;
import io.reactivex.exceptions.UndeliverableException;

import javax.inject.Singleton;
import java.util.logging.Level;
import java.util.logging.Logger;

@Produces
@Singleton
@Requires(classes = {ExceptionHandler.class})
public class ApplicationExceptionsHandler implements ExceptionHandler<io.reactivex.exceptions.UndeliverableException> {

    @Override
    public void handle(UndeliverableException exception) {
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, Messages.INTERNAL_ERROR);
    }
}