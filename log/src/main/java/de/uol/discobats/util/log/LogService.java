/*######################################################################################################
 # This file is part of the Distributed Component-Based Traffic Simulation (DisCo-BaTS) project.       #
 # Copyright (C) 2026 David Reiher <https://github.com/dvdrhr>                                         #
 #                                                                                                     #
 # This program is free software: you can redistribute it and/or modify it under the terms of the      #
 # GNU Lesser General Public License version 3 as published by the Free Software Foundation            #
 # This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;           #
 # without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           #
 # See the GNU Lesser General Public License version 3 for more details.                               #
 # You should have received a copy of the GNU Lesser General Public License along with this program.   #
 # If not, see <https://www.gnu.org/licenses/lgpl+gpl-3.0.txt/>.                                       #
 #                                                                                                     #
 # Module: log                                                                                         #
 # File: LogService.java                                                                               #
 # Last Updated: 2026-02-17 21:58:06                                                                   #
 ######################################################################################################*/

package de.uol.discobats.util.log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static de.uol.discobats.util.log.LogLevel.ERROR;
import static java.time.LocalTime.now;

/**
 * TODO javadoc
 *
 * @version 1
 * @author David Reiher (https://github.com/dvdrhr)
 */
public class LogService {

    private static final Logger ownLogger = LogManager.getLogger(LogService.class.getName());
    private static final HashMap<String, Logger> classLoggers = new HashMap<>();

    public static void log(String message, Object... messageArgs) {
        logWithArgs(message, messageArgs);
    }

    public static void logWithArgs(String message, Object... messageArgs) {
        log(getLogger(), LogLevel.INFO.get(), message, messageArgs);
    }

    public static void logWithArgs(LogLevel level, String message, Object... messageArgs) {
        log(getLogger(), level.get(), message, messageArgs);
    }

    public static void log(LogLevel level, String message, Object... messageArgs) {
        log(getLogger(), level.get(), message, messageArgs);
    }

    public static void log(LogLevel level, String name, String message, Object... messageArgs) {
        if (name == null || name.isBlank()) {
            log(ERROR, "'name' should not be null nor blank");
            return;
        }
        String prefix = getMessagePrefix(name);
        log(level, prefix + message, messageArgs);
    }

    private static void log(Logger logger, Level level, String message, Object... messageArgs) {
        if (logger == null) {
            log(ERROR, "'logger' should not be null");
            return;
        }
        if (level == null) {
            log(ERROR, "'level' should not be null");
            return;
        }
        if (message == null || message.isBlank()) {
            log(ERROR, "'message' should not be null");
            return;
        }
        logger.log(level, message, messageArgs);
    }

    public static void log(Exception e) {
        log(getLogger(), e);
    }

    public static void log(Exception e, String message, Object... messageArgs) {
        log(getLogger(), e, message, messageArgs);
    }

    public static void log(Logger logger, Exception e) {
        log(logger, e, "the following exception was thrown");
    }

    public static void log(Logger logger, Exception e, String message, Object... messageArgs) {
        if (e == null) {
            log(ERROR, "'exception e' should not be null");
            return;
        }

        String exName = e.getClass().getName();
        String exMessage = e.getMessage();
        String exStackTrace = Arrays.stream(e.getStackTrace())
                                    .sequential()
                                    .map(StackTraceElement::toString)
                                    .reduce("", (stackString, elemString) -> stackString + "\n" + " " + elemString);

        ArrayList<String> causeNames = new ArrayList<>();
        ArrayList<String> causeTraces = new ArrayList<>();
        ArrayList<String> causeMessages = new ArrayList<>();
        Throwable cause = e.getCause();
        while (cause != null) {
            causeNames.add(cause.getClass().getName());
            causeMessages.add(cause.getMessage());
            causeTraces.add(
                Arrays.stream(cause.getStackTrace())
                      .sequential()
                      .map(StackTraceElement::toString)
                      .reduce("", (stackString, elemString) -> stackString + "\n" + " " + elemString)
            );
            cause = cause.getCause();
        }

        StringBuilder exceptionMessagePart = new StringBuilder(" ==== EXCEPTION START ====\n" +
                                                               " Time: " + now() + "\n" +
                                                               " Class: " + exName + "\n" +
                                                               " Message: " + exMessage + "\n" +
                                                               " Stacktrace: " + exStackTrace + "\n");

        if (!causeNames.isEmpty()) {
            for (int i = 0; i < causeNames.size(); i++) {
                //exceptionMessagePart.append("\n");
                exceptionMessagePart.append(" ---- CAUSE ").append(i + 1).append(" START ----\n");
                exceptionMessagePart.append(" Class: ").append(causeNames.get(i)).append("\n");
                exceptionMessagePart.append(" Message: ").append(causeMessages.get(i)).append("\n");
                exceptionMessagePart.append(" Stacktrace: ").append(causeTraces.get(i)).append("\n");
                exceptionMessagePart.append(" ---- CAUSE ").append(i + 1).append(" END ----\n");
            }
        }
        exceptionMessagePart.append(" ==== EXCEPTION END ====");

        log(logger,
            ERROR.get(),
            ((message != null && !message.isBlank()) ? (message + "\n") : ("")) + (exceptionMessagePart),
            messageArgs);
    }

    public static void logOwn(Exception e) {
        log(ownLogger, e);
    }

    public static RuntimeException logAndReturn(RuntimeException e) {
        log(e);
        return e;
    }

    public static Exception logAndReturn(Exception e) {
        log(e);
        return e;
    }

    public static RuntimeException logAndReturn(RuntimeException e, String message, Object... messageArgs) {
        log(e, message, messageArgs);
        return e;
    }

    public static void logAndThrow(RuntimeException e) throws RuntimeException {
        log(e);
        throw e;
    }

    public static void logAndThrow(RuntimeException e, String message, Object... messageArgs) throws RuntimeException {
        log(e, message, messageArgs);
        throw e;
    }

    public static void logAndThrow(Class<? extends RuntimeException> clazz, String message) throws Exception {
        if (clazz == null) {
            logAndThrow(IllegalArgumentException.class, "no exception class provided");
            return;
        }
        if (message == null) {
            logAndThrow(IllegalArgumentException.class, "message should not be null");
            return;
        }
        RuntimeException e = clazz.getConstructor(String.class).newInstance(message);
        logAndThrow(e);
    }

    private static Logger getLogger() {

        StackTraceElement stackTraceElement = Arrays.stream(Thread.currentThread().getStackTrace())
                                                    .filter(
                                                        element ->
                                                            element.getClassLoaderName() != null &&
                                                            !element.getClassName().equals(LogService.class.getName())
                                                    ).findFirst()
                                                    .orElse(null);

        String clazzName = stackTraceElement != null ? stackTraceElement.getClassName() : UnknownLogCaller.class.getName();
        Class<?> clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            clazz = UnknownLogCaller.class;
        }

        Logger logger = classLoggers.get(clazz.getName());
        if (logger == null) {
            logger = LogManager.getLogger(clazz);
            classLoggers.put(clazz.getName(), logger);
        }
        return logger;
    }

    private static String getMessagePrefix(String prefixText) {
        if (prefixText == null || prefixText.isBlank()) {
            log(ERROR, "'prefixText' should not be null nor blank");
            return null;
        }
        return prefixText + " | ";
    }

}
