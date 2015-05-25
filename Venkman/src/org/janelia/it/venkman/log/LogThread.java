/*
 * Copyright (c) 2011 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import org.janelia.it.venkman.jaxb.MarshallerCache;

import javax.xml.bind.JAXBException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This thread handles the writing of log objects to a log file without
 * blocking other more important tracker processing threads.
 * The thread's {@link #interrupt} method should be called
 *
 * @author Eric Trautman
 */
public class LogThread extends Thread {

    /**
     * The queue of objects to be written to the log file.
     * Items are placed on the queue by client threads via the
     * {@link #offer(Object)} method. This thread then pulls items off
     * the queue and into a local (hidden) buffer for simplified
     * processing.
     */
    private ConcurrentLinkedQueue<Object> queue;

    /** The number log objects to queue before attempting a write. */
    private int numberOfItemsToBufferBeforeWrite;

    /**
     * The milliseconds that the write thread should wait (sleep)
     * between each write attempt.
     */
    private int millisecondsBetweenWriteAttempts;

    /** The time this log session started. */
    private long sessionStartTime;

    /** The absolute path and name for the log file. */
    private String logFilePathAndName;

    /**
     * The local buffer of log objects pulled off the queue for
     * logging by this thread.
     */
    private List<Object> bufferedLogObjects;

    /** The log file output stream. */
    private OutputStream out;

    /** Cache of JAXB marshaller instances. */
    private MarshallerCache marshallerCache;

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
    public LogThread(int numberOfItemsToBufferBeforeWrite,
                     int millisecondsBetweenWriteAttempts,
                     File logDirectoryPath,
                     String sessionId) {
        this.numberOfItemsToBufferBeforeWrite =
                numberOfItemsToBufferBeforeWrite;
        this.millisecondsBetweenWriteAttempts =
                millisecondsBetweenWriteAttempts;
        this.sessionStartTime = System.currentTimeMillis();

        final String fileName =
                "venkman-log-" +
                LogDateFormats.FILE_NAME.format(this.sessionStartTime) +
                "-" + sessionId + ".xml";
        final File file = new File(logDirectoryPath, fileName);
        this.logFilePathAndName = file.getAbsolutePath();

        this.queue = new ConcurrentLinkedQueue<Object>();
        this.bufferedLogObjects = new ArrayList<Object>();
        this.marshallerCache = new MarshallerCache();
    }

    public String getLogFilePathAndName() {
        return logFilePathAndName;
    }

    public void offer(Object logObject) {
        queue.offer(logObject);
    }

    /**
     * Monitors the log object queue, removing entries and writing them
     * to the log file as required.
     */
    @Override
    public void run() {

        try {

            // open the log file output stream for logging
            open();

            // poll for log items as they get added to the queue by clients
            int numberOfItems;
            Object logObject;
            while (! isInterrupted()) {

                // limit number of items processed in each loop
                // (call to size() is slow because of traversal but that's ok)
                numberOfItems = queue.size();

                // move items from queue to buffer so that we don't
                // have to worry about synchronization
                for (int i = 0; i < numberOfItems; i++) {
                    logObject = queue.poll();
                    if (logObject == null) {
                        break;
                    } else {
                        bufferedLogObjects.add(logObject);
                    }
                }

                if (bufferedLogObjects.size() >
                    numberOfItemsToBufferBeforeWrite) {
                    writeBufferedEntries();
                }

                if (millisecondsBetweenWriteAttempts > 0) {
                    sleep(millisecondsBetweenWriteAttempts);
                }
            }

        } catch (InterruptedException e) {

            System.out.println("Rules Plugin Log Thread: stopped logging to " +
                               logFilePathAndName);

        } catch (Throwable t) {

            System.err.println(
                    "Rules Plugin Log Thread: exception caught, " +
                    "stopped logging to " + logFilePathAndName);
            t.printStackTrace(System.err);

        } finally {

            // flush the queue, write all remaining entries to the log file,
            // and close the output stream
            close();
        }

    }

    /**
     * Open the log file output stream for logging.
     *
     * @throws java.io.IOException
     *   if any errors occur while writing to the log file.
     */
    private void open() throws IOException {
        out = new BufferedOutputStream(
                new FileOutputStream(logFilePathAndName, true));

        StringBuilder sb = new StringBuilder();
        sb.append("<logSession startTime='");
        sb.append(LogDateFormats.DEFAULT.format(sessionStartTime));
        sb.append("'>\n");
        out.write(sb.toString().getBytes());
    }

    /**
     * Writes all buffered log objects to the log file.
     *
     * @throws JAXBException
     *   if JAXB cannot marshall an entry.
     *
     * @throws IOException
     *   if data cannot be written to the log file.
     */
    private void writeBufferedEntries()
            throws JAXBException, IOException {

        for (Object logObject : bufferedLogObjects) {
            marshallerCache.marshal(logObject, out);
        }

        if (bufferedLogObjects.size() > 0) {
            out.flush();
            bufferedLogObjects.clear();
        }
    }

    /**
     * Removes all remaining entries from the log queue and
     * writes them to the log file.
     *
     * @throws javax.xml.bind.JAXBException
     *   if JAXB cannot marshall an entry.
     *
     * @throws java.io.IOException
     *   if data cannot be written to the log file.
     */
    private void flushQueue()
            throws JAXBException, IOException {
        Object logObject;
        while (true) {
            logObject = queue.poll();
            if (logObject == null) {
                break;
            } else {
                bufferedLogObjects.add(logObject);
            }
        }
        writeBufferedEntries();
    }

    /**
     * Flush the queue, write all remaining entries to the log file,
     * and close the output stream.
     */
    private void close() {

        try {
            flushQueue();
        } catch (Throwable t) {
            System.err.println("Rules Plugin Log Thread: " +
                               "failed to flush items from queue to " +
                               logFilePathAndName + ", queue size is " +
                               queue.size() + ", buffer size is " +
                               bufferedLogObjects.size());
            t.printStackTrace(System.err);
        }

        if (out != null) {
            try {
                out.write("\n</logSession>\n".getBytes());
                out.close();
            } catch (Throwable t) {
                System.err.println("Rules Plugin Log Thread: could not close " +
                                   logFilePathAndName);
                t.printStackTrace(System.err);
            }
        }
    }

}