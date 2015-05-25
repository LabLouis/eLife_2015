/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman;

import org.janelia.it.venkman.test.MockRulesServer;

import java.io.File;

/**
 * Launches the bio-rules server (or a mock server for testing).
 *
 * @author Eric Trautman
 */
public class Venkman {

    public static void main(String[] args) {
        try {

            if (args.length > 0) {
                final int port = Integer.parseInt(args[0]);

                RulesServer server = null;
                if (args.length == 1) {

                    server = new RulesServer(port, new File("."));

                } else if (args.length == 3) {

                    final File workDirectory = new File(args[2]);
                    server = new RulesServer(port, workDirectory);

                } else {

                    // use mock server ...

                    File file = new File(args[1]);
                    if (file.canRead()) {
                        server = new MockRulesServer(port, file);
                    } else {
                        System.out.println(
                                "\n\nERROR: cannot read response file " +
                                file.getAbsolutePath() + "\n\n");
                    }

                }

                if (server != null) {
                    server.run();
                }

            } else {
                System.out.println("\n\nUSAGE: java -jar rules-server.jar <port> [skeleton-response-file]");
                System.out.println("       java -jar rules-server.jar <port> -workDirectory <work directory>\n\n");
            }

        } catch (Exception e) {
            System.out.println("\n\nException caught, stopping server ...\n\n");
            e.printStackTrace();
        }
    }

}
