package app.controllers.impl;

import app.exceptions.ApiException;
import app.exceptions.Message;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionController.class);

    public void apiExceptionHandler(ApiException ex, Context ctx) {
        if (ex.getStatusCode() == 401) ctx.header("WWW-Authenticate", "Bearer");
        ctx.status(ex.getStatusCode()).json(new Message(ex.getStatusCode(), ex.getMessage()));
    }

    public void httpExceptionHandler(HttpResponseException ex, Context ctx) {
        ctx.status(ex.getStatus()).json(new Message(ex.getStatus(), ex.getMessage()));
    }

    public void exceptionHandler(Exception ex, Context ctx) {
        LOGGER.error("Request failed: {} {}", ctx.method(), ctx.path(), ex);
        if (ex instanceof jakarta.persistence.PersistenceException) {
            ctx.status(409).json(new Message(409, "Database constraint conflict"));
        } else {
            ctx.status(500).json(new Message(500, "Internal server error"));
        }
    }
}
