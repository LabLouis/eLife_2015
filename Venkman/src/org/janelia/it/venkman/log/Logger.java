/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import java.io.File;

/**
 * <p>
 * Utility for writing calculated and derived data to a file.
 * JAXB libraries are used to translate logged objects to XML, so all
 * logged object classes must support (be annotated for) JAXB.
 * </p>
 *
 * <p>
 * All write operations are performed on a separate thread and clients can
 * control how often writes are attempted as well as how many objects must
 * be logged before performing a write.
 * </p>
 *
 * <p><br>
 * <b>NOTE:</b>
 * Upon completion of log processing, clients must call {@link #stopLogging()}
 * to flush/write any remaining logged data, close the log file, and stop
 * the log writer thread.
 * </p>
 *
 *
 * @author Eric Trautman
 */
public class Logger {

    /** The thread for writing to the log file. */
    private LogThread logThread;

    /** Indicates whether the logging has been stopped. */
    private Boolean loggingEnabled;

    /**
     * The default number of log objects to buffer
     * before attempting a write.
     */
    public static final int DEFAULT_ITEMS_TO_BUFFER = 0;

    /**
     * The default number of milliseconds that the write thread
     * should wait (sleep) between each write attempt.
     */
    public static final int DEFAULT_WRITE_PAUSE = 5000;

    /**
     * Constructs a logger with default parameters that saves the
     * log file in the current working directory.
     */
    public Logger() {
        this(new File("."), "x");
    }

    /**
     * Constructs a logger with default parameters that saves the
     * log file in the specified directory.
     *
     * @param  logDirectoryPath  path of log file parent directory.
     *
     * @param  sessionId         id of session being logged.
     */
    public Logger(File logDirectoryPath,
                  String sessionId) {
        this(DEFAULT_ITEMS_TO_BUFFER,
             DEFAULT_WRITE_PAUSE,
             logDirectoryPath,
             sessionId);
    }

    /**
     * Constructs a logger with the specified parameters.
     *
     * @param  numberOfItemsToBufferBeforeWrite  number of log objects to
     *                                           buffer before attempting
     *                                           a write.
     *
     * @param  millisecondsBetweenWriteAttempts  number of milliseconds that
     *                                           the write thread should wait
     *                                           between each write attempt.
     *
     * @param  logDirectoryPath  path of log file parent directory.
     *
     * @param  sessionId         id of session being logged.
     */
    public Logger(int numberOfItemsToBufferBeforeWrite,
                  int millisecondsBetweenWriteAttempts,
                  File logDirectoryPath,
                  String sessionId) {
        this.logThread = new LogThread(numberOfItemsToBufferBeforeWrite,
                                       millisecondsBetweenWriteAttempts,
                                       logDirectoryPath,
                                       sessionId);
        this.loggingEnabled = null;
    }

    /**
     * @return the full (absolute) path and file name for the log file.
     */
    public String getLogFilePathAndName() {
        return logThread.getLogFilePathAndName();
    }

    /**
     * Log the specified object.  Note that all logged object classes must
     * support (be annotated for) JAXB.
     *
     * @param  logObject  object to log.
     *
     * @throws IllegalStateException
     *   if this logger has not been started or has already been stopped.
     */
    public synchronized void log(Object logObject)
            throws IllegalStateException {

        // start up log thread if this is the first log request
        if (loggingEnabled == null) {
            logThread.start();
            loggingEnabled = true;
        }

        if (loggingEnabled) {
            logThread.offer(logObject);
        } else {
            throw new IllegalStateException("logging disabled, cannot log " +
                                            logObject);
        }
    }

    /**
     * Logs the specified message to the log file (with a timestamp).
     *
     * @param  message  message to log.
     */
    public void logMessage(String message) {
        log(new LogMessage(message));
    }

    /**
     * Stops logging by flushing/writing any remaining logged data and
     * closing the log file.
     */
    public synchronized void stopLogging() {

        loggingEnabled = false;
        if (logThread.isAlive()) {
            logThread.interrupt();
        }
    }

}
