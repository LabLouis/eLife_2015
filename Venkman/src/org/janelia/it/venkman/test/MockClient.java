/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.test;

import org.janelia.it.venkman.config.Configuration;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.log.LogReader;
import org.janelia.it.venkman.log.LogSession;
import org.janelia.it.venkman.message.Message;
import org.janelia.it.venkman.message.MessageType;
import org.janelia.it.venkman.message.ResponseMessage;

import javax.xml.bind.JAXBException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Mock rules client for testing.
 *
 * @author Eric Trautman
 */
public class MockClient {

    private static final String VERSION_1 = "1";

    private int port;
    private File commandFile;
    private String sid;

    public MockClient(int port,
                      File commandFile,
                      String sid) {
        this.port = port;
        this.commandFile = commandFile;
        if (sid == null) {
            this.sid = "sid-0";
        } else {
            this.sid = sid;
        }
    }

    public void run() throws IOException {
        Socket socket = null;
        try {
            socket = new Socket("localhost", port);
            PrintWriter socketOut =
                    new PrintWriter(socket.getOutputStream(), true);
            BufferedReader socketIn = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            Reader reader;
            BufferedReader systemIn;

            boolean importedCommandsFromLog = false;
            if (commandFile == null) {
                reader = new InputStreamReader(System.in);
            } else if (commandFile.getName().endsWith(".xml")) {
                importedCommandsFromLog = true;
                reader = getLogFileReader();
            } else {
                reader = new FileReader(commandFile);
            }
            systemIn = new BufferedReader(reader);

            System.out.println("connected to port " + port);

            final long startTime = System.currentTimeMillis();
            long requestCount = 0;

            final String successfulResponseStatusCode =
                    "," + ResponseMessage.STATUS_OK;

            final boolean enterCommandsManually = (commandFile == null);
            String message;
            String response;
            boolean continueProcessing = true;
            while (continueProcessing) {
                if (enterCommandsManually) {
                    System.out.print("enter message to send: ");
                } else if (! importedCommandsFromLog) {
                    Thread.sleep(30); // mimic frame capture delay
                }
                message = systemIn.readLine();
                if ((message != null) && (message.length() > 0)) {
                    message = replaceFrameTime(message);
                    socketOut.println(message);
                    response = socketIn.readLine();
                    if (! importedCommandsFromLog) {
                        System.out.println("received response: " + response);
                    } else if (!response.contains(successfulResponseStatusCode)) {
                        System.out.println("received failure response: " + response);
                        continueProcessing = false;
                    }
                    requestCount++;
                } else {
                    continueProcessing = false;
                }
            }

            final long elapsedTime = System.currentTimeMillis() - startTime;
            final double avgTime = (double) elapsedTime / (double) requestCount;
            final BigDecimal scaledAvgTime =
                    new BigDecimal(avgTime).setScale(2, RoundingMode.HALF_UP);
            System.out.println("issued " + requestCount + " requests in " +
                               elapsedTime + " ms, average request time is " +
                               scaledAvgTime + " ms/request");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("\n\nignoring socket close exception\n\n");
                e.printStackTrace();
            }

        }
    }

    private Reader getLogFileReader() {

        StringBuilder commands = new StringBuilder(1024 * 1024);

        LogReader logReader = new LogReader(commandFile);

        try {
            logReader.read();
        } catch (JAXBException e) {
            throw new IllegalArgumentException(
                    "failed to parse " + commandFile.getAbsolutePath(), e);
        }

        LogSession session = logReader.getSession();

        Configuration configuration = session.getConfiguration();

        final Message openMessage =
                new Message(MessageType.OPEN_SESSION_REQUEST,
                            VERSION_1,
                            Arrays.asList(VERSION_1,
                                          configuration.getId().getFullName()));
        commands.append(openMessage.toString());
        commands.append("\n");

        List<LarvaFrameData> frameDataList = session.getFrameDataList();

        String values[] = new String[13];
        values[0] = sid;
        List<String> valueList = Arrays.asList(values);
        Message skeletonMessage =
                new Message(MessageType.PROCESS_LARVA_SKELETON_REQUEST,
                            VERSION_1,
                            valueList);
        LarvaSkeleton skeleton;
        for (LarvaFrameData frameData : frameDataList) {
            skeleton = frameData.getSkeleton();

            values[1] = String.valueOf(skeleton.getCaptureTime());
            values[2] = String.valueOf(skeleton.getHead().getX());
            values[3] = String.valueOf(skeleton.getHead().getY());
            values[4] = String.valueOf(skeleton.getMidpoint().getX());
            values[5] = String.valueOf(skeleton.getMidpoint().getY());
            values[6] = String.valueOf(skeleton.getTail().getX());
            values[7] = String.valueOf(skeleton.getTail().getY());
            values[8] = String.valueOf(skeleton.getLength());
            values[9] = String.valueOf(skeleton.getCentroid().getX());
            values[10] = String.valueOf(skeleton.getCentroid().getY());
            values[11] = String.valueOf(skeleton.getHeadToBodyAngle());
            values[12] = String.valueOf(skeleton.getTailBearing());

            commands.append(skeletonMessage.toString());
            commands.append("\n");
        }

        return new StringReader(commands.toString());
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                final int port = Integer.parseInt(args[0]);
                File commandFile = null;
                String sid = null;
                if (args.length > 1) {
                    commandFile = new File(args[1]);
                    if (args.length > 2) {
                        sid = args[2];
                    }
                }
                MockClient client = new MockClient(port, commandFile, sid);
                client.run();
            } else {
                System.out.println("\n\nUSAGE: java -cp venkman.jar " +
                                   MockClient.class.getName() +
                                   " <port> [command file] [sid]\n\n");
            }
        } catch (Exception e) {
            System.out.println("\n\nException caught, stopping client ...\n\n");
            e.printStackTrace();
        }
    }

    private String replaceFrameTime(String message) {
        return FRAME_TIME.matcher(message).replaceFirst(
                String.valueOf(System.currentTimeMillis()));
    }

    private static final Pattern FRAME_TIME = Pattern.compile("frame-time");

}
