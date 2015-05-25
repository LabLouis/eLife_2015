/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Ruler to show physical location of larva within the arena.
 *
 * This implementation was adapted (simplified) from
 * <a href="http://java.sun.com/docs/books/tutorial/uiswing/examples/components/ScrollDemoProject/src/components/Rule.java">
 * Rule.java</a> in Sun's Scroll Demo.
 *
 * @author Eric Trautman
 */
public class ArenaRuler
        extends JComponent {

    private static final int SIZE = 35;
    public static final Color BACKGROUND_COLOR = Color.LIGHT_GRAY;

    private boolean horizontal;
    private double pixelsPerMillimeter;
    private int increment;
    private double scale;

    public ArenaRuler(boolean horizontal,
                      double pixelsPerMillimeter,
                      int millimetersBetweenTicks,
                      double scale) {
        this.horizontal = horizontal;
        this.pixelsPerMillimeter = pixelsPerMillimeter;
        this.increment = (int) (millimetersBetweenTicks * pixelsPerMillimeter);
        setScale(scale);
    }

    public void setScale(double scale) {
        if (scale > 0) {
            this.scale = scale;
        }
    }

    public void setPreferredHeight(int ph) {
        setPreferredSize(new Dimension(SIZE, ph));
    }

    public void setPreferredWidth(int pw) {
        setPreferredSize(new Dimension(pw, SIZE));
    }

    protected void paintComponent(Graphics g) {
        Rectangle drawHere = g.getClipBounds();

        // Fill clipping area with background color.
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(drawHere.x, drawHere.y, drawHere.width, drawHere.height);

        // Do the ruler labels in a small font that's black.
        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.setColor(Color.black);

        // Some vars we need.
        int end;
        int start;
        final int tickLength = 5;

        // Use clipping bounds to calculate first and last tick locations.
        if (horizontal) {
            start = (drawHere.x / increment) * increment;
            end = (((drawHere.x + drawHere.width) / increment) + 1)
                  * increment;
        } else {
            start = (drawHere.y / increment) * increment;
            end = (((drawHere.y + drawHere.height) / increment) + 1)
                  * increment;
        }

        String text = "0";
        // Make a special case of 0 to display the number
        // within the rule and draw a units label.
        if (start == 0) {
            if (horizontal) {
                g.drawLine(0, SIZE-1, 0, SIZE-tickLength-1);
                g.drawString(text, 2, 21);
            } else {
                g.drawLine(SIZE-1, 0, SIZE-tickLength-1, 0);
                g.drawString(text, 2, 8);
            }
            start = increment;
        }

        // ticks and labels
        final double tickScale = scale * pixelsPerMillimeter;
        for (int i = start; i < end; i += increment) {
            BigDecimal bd = new BigDecimal(((double) i / tickScale));
            bd = bd.setScale(0, BigDecimal.ROUND_UP);
            text = bd.toString();

            if (horizontal) {
                g.drawLine(i, SIZE-1, i, SIZE-tickLength-1);
                g.drawString(text, i-3, 21);
            } else {
                g.drawLine(SIZE-1, i, SIZE-tickLength-1, i);
                g.drawString(text, 2, i+3);
            }

        }
    }
}

