/*
 * Copyright (c) 2014 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.log;

import org.janelia.it.venkman.config.LarvaBehaviorParameters;
import org.janelia.it.venkman.config.rules.LEDFlashPattern;
import org.janelia.it.venkman.config.rules.LEDStimulus;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.rules.DefinedEnvironment;

import java.io.File;

/**
 * Tests the logger code by logging some data and reading it back in.
 *
 * @author Eric Trautman
 */
public class LoggerTest {

    public static void main(String[] args) {
        String logFileName = null;

        Logger logger = null;
        try {
            logger = new Logger();

            logFileName = logger.getLogFilePathAndName();

            logger.log(new LarvaBehaviorParameters());
            logger.log(new DefinedEnvironment(new LEDFlashPattern("0"),
                                              null,
                                              0.0));

            long logTime;
            long sleepTime;

            for (int i = 0; i < 200; i++) {

                logTime = System.currentTimeMillis() + 30;

                LarvaSkeleton skeleton =
                        new LarvaSkeleton(logTime,
                                          new TrackerPoint(i,0),    // tail
                                          new TrackerPoint(i,1),    // midpoint
                                          new TrackerPoint(i,2),    // head
                                          2,                        // length
                                          new TrackerPoint(i,1.5),  // centroid
                                          0,               // headToBodyAngle
                                          0);              // tailBearing
                LarvaFrameData frameData = new LarvaFrameData(skeleton);
                LEDStimulus stimulus = new LEDStimulus(i,
                                                       (i*100));
                logger.log(frameData);
                System.out.print("f ");
                logger.log(stimulus);
                System.out.print("s ");

                if (i % 20 == 0) {
                    logger.logMessage("hello world " + i);
                    System.out.println("m");
                }

                sleepTime = logTime - System.currentTimeMillis();
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (logger != null) {
                System.out.println("\ncalling stopLogging()");
                logger.stopLogging();
            }
        }


        if (logFileName != null) {
            LogReader reader = new LogReader(new File(logFileName));
            try {
                System.out.println("waiting for logger to finish ...");
                Thread.sleep(1000);
                reader.read();
            } catch (Exception e) {
                e.printStackTrace();
            }

            final LogSession logSession = reader.getSession();
            System.out.println(logSession.getLarvaBehaviorParameters());
            System.out.println(logSession.getLarvaStimulusRules());

            for (LarvaFrameData frameData : logSession.getFrameDataList()) {
                System.out.println(frameData);
            }

            for (LEDStimulus stimulus : logSession.getStimulusList()) {
                System.out.println(stimulus);
            }
        }
    }

}
