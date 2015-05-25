/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.venkman.gui.log;

import org.janelia.it.venkman.config.rules.PositionalVariableFunction;
import org.janelia.it.venkman.data.LarvaFrameData;
import org.janelia.it.venkman.data.LarvaSkeleton;
import org.janelia.it.venkman.data.TrackerPoint;
import org.janelia.it.venkman.rules.DefinedEnvironment;
import org.janelia.it.venkman.rules.LarvaStimulusRules;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A panel displaying the larva's movement within the tracker "arena".
 *
 * @author Eric Trautman
 */
public class ArenaPanel {
    private JPanel contentPanel;
    private JScrollPane skeletonScrollPane;
    private JPanel skeletonPanel;
    private JSpinner scaleSpinner;
    private JSpinner traceHistorySpinner;
    private JButton findLarvaButton;
    private JCheckBox showActualSkeletonCheckBox;

    private LogModel logModel;
    private double xPixelsPerMillimeter;
    private double yPixelsPerMillimeter;
    private double arenaScale;
    private long traceHistoryTime;
    private ArenaRuler horizontalRuler;
    private ArenaRuler verticalRuler;

    private DefinedEnvironment definedEnvironmentRule;

    private void createUIComponents() {
        skeletonPanel = new SkeletonPanel();
    }

    public ArenaPanel(LogModel logModel) {

        this.logModel = logModel;

        this.logModel.addPropertyChangeListener(
                LogModel.LOG_FILE_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateLogFile();
                    }
                });

        this.logModel.addPropertyChangeListener(
                LogModel.CURRENT_FRAME_PROPERTY,
                new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateCurrentFrame(false);
                    }
                });

        setPixelsPerMillimeter();

        this.arenaScale = 0.5;

        final int mmPerTick = 20;
        this.horizontalRuler = new ArenaRuler(true,
                                              xPixelsPerMillimeter,
                                              mmPerTick,
                                              arenaScale);
        this.verticalRuler = new ArenaRuler(false,
                                            yPixelsPerMillimeter,
                                            mmPerTick,
                                            arenaScale);

        SpinnerNumberModel spinnerModel =
                new SpinnerNumberModel(arenaScale, 0.5, 20.0, 0.5);
        this.scaleSpinner.setModel(spinnerModel);
        this.scaleSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Double value = (Double) scaleSpinner.getValue();
                scaleArena(value);
            }
        });
        this.scaleSpinner.setEnabled(false);

        this.traceHistoryTime = 0;
        spinnerModel = new SpinnerNumberModel(0, 0, 100, 25);
        this.traceHistorySpinner.setModel(spinnerModel);
        this.traceHistorySpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Integer value = (Integer) traceHistorySpinner.getValue();
                setTraceHistory(value);
            }
        });
        this.traceHistorySpinner.setEnabled(false);

        this.showActualSkeletonCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCurrentFrame(false);
            }
        });

        this.findLarvaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateCurrentFrame(true);
            }
        });
        this.findLarvaButton.setEnabled(false);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    private void updateLogFile() {

        LarvaStimulusRules rule = logModel.getCurrentRules();
        if (rule instanceof DefinedEnvironment) {
            definedEnvironmentRule = (DefinedEnvironment) rule;
            showActualSkeletonCheckBox.setEnabled(definedEnvironmentRule.isEnableOrientationLogic());
        } else {
            definedEnvironmentRule = null;
            showActualSkeletonCheckBox.setSelected(false);
            showActualSkeletonCheckBox.setEnabled(false);
        }
        ((SkeletonPanel) skeletonPanel).setArenaBackground();

        scaleArena(arenaScale);
        scaleSpinner.setEnabled(true);
        traceHistorySpinner.setEnabled(true);
        findLarvaButton.setEnabled(true);
    }

    private void updateCurrentFrame(boolean forceCenter) {
        final LarvaFrameData currentFrameData =
                logModel.getCurrentFrameData();
        if (currentFrameData != null) {
            updateSkeleton(currentFrameData.getSkeleton(), forceCenter);
        }
    }

    private void updateSkeleton(LarvaSkeleton currentSkeleton,
                                boolean forceCenter) {

        SkeletonPanel panel = (SkeletonPanel) skeletonPanel;
        panel.setSkeleton(currentSkeleton);

        final Rectangle skeletonBounds = panel.getSkeletonBounds();
        double minX = skeletonBounds.getX();
        double minY = skeletonBounds.getY();
        double maxX = minX + skeletonBounds.getWidth();
        double maxY = minY + skeletonBounds.getHeight();

        double x;
        double y;
        final int margin = 10;
        final Rectangle visibleRectangle = panel.getVisibleRect();
        if (forceCenter ||
            (visibleRectangle.getMinX() > (minX - margin)) ||
            (visibleRectangle.getMinY() > (minY - margin)) ||
            (visibleRectangle.getMaxX() < (maxX + margin)) ||
            (visibleRectangle.getMaxY() < (maxY + margin))) {

            final int viewPortWidth = visibleRectangle.width - (2 * margin);
            final int viewPortHeight = visibleRectangle.height - (2 * margin);
            final double larvaWidth = maxX - minX;
            final double larvaHeight = maxY - minY;
            final double widthOffset = (viewPortWidth - larvaWidth) / 2;
            final double heightOffset = (viewPortHeight - larvaHeight) / 2;
            final double upperLeftX = minX - widthOffset;
            if (upperLeftX > 0) {
                x = upperLeftX;
            } else {
                x = 0;
            }
            final double upperLeftY = minY - heightOffset;
            if (upperLeftY > 0) {
                y = upperLeftY;
            } else {
                y = 0;
            }

            final Rectangle rectangle =
                    new Rectangle((int) x, (int) y,
                                  viewPortWidth, viewPortHeight);

            panel.scrollRectToVisible(rectangle);
        }
    }

    private void setTraceHistory(int percentage) {
        if (percentage == 0) {
            traceHistoryTime = 0;
        } else {
            final long lastFrameTime = logModel.getLastFrameTime();
            final double factor = (double) percentage / 100.0;
            traceHistoryTime = (long) (lastFrameTime * factor) + 1;
        }
        skeletonPanel.repaint();
    }

    /**
     * Scales arena coordinates to display/view coordinates.
     */
    private void scaleArena(double scale) {

        arenaScale = scale;

        final TrackerPoint maximumPosition =
                logModel.getMaximumPosition();

        if (maximumPosition != null) {

            final int margin = 50;

            final int width = (int) scaleX(maximumPosition.getX()) + margin;
            horizontalRuler.setScale(scale);
            horizontalRuler.setPreferredWidth(width);

            final int height = (int) scaleY(maximumPosition.getY()) + margin;
            verticalRuler.setScale(scale);
            verticalRuler.setPreferredHeight(height);

            SkeletonPanel panel = (SkeletonPanel) skeletonPanel;
            panel.setPreferredSize(new Dimension(width, height));

            JPanel corner = new JPanel();
            corner.setBackground(ArenaRuler.BACKGROUND_COLOR);
            skeletonScrollPane.setCorner(
                    JScrollPane.UPPER_LEFT_CORNER,
                    corner);
            skeletonScrollPane.setColumnHeaderView(horizontalRuler);
            skeletonScrollPane.setRowHeaderView(verticalRuler);
            panel.scrollRectToVisible(new Rectangle(0, 0, 1, 1));

            // queue this event to make sure frame is visible before
            // skeleton is displayed (makes scrollToVisible work)
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateCurrentFrame(true);
                }
            });
        }
    }

    /**
     * Derives X and Y pixels per millimeter values for the current display
     * resolution.  Unfortunately, we can't simply use
     * {@link java.awt.Toolkit#getScreenResolution()} because it returns
     * 72 pixels per inch even for high resolution displays like Mac Retina.
     *
     * This method of accurately calculating the resolution was stolen from:
     * <a href="http://www.mailinglistarchive.com/html/java-dev@lists.apple.com/2012-06/msg00115.html">
     *     http://www.mailinglistarchive.com/html/java-dev@lists.apple.com/2012-06/msg00115.html
     * </a>
     */
    private void setPixelsPerMillimeter() {
        final GraphicsEnvironment localGraphicsEnvironment =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice defaultScreenDevice =
                localGraphicsEnvironment.getDefaultScreenDevice();
        final GraphicsConfiguration gc =
                defaultScreenDevice.getDefaultConfiguration();

        // get the Graphics2D of a compatible image for this configuration
        final BufferedImage compatibleImage = gc.createCompatibleImage(1, 1);
        final Graphics2D g2d = (Graphics2D) compatibleImage.getGraphics();

        // after these transforms, 72 units in either direction == 1 inch
        g2d.setTransform(gc.getDefaultTransform() );
        g2d.transform(gc.getNormalizingTransform() );
        final AffineTransform oneInch = g2d.getTransform();
        g2d.dispose();

        xPixelsPerMillimeter = oneInch.getScaleX() * 72 / 25.4;
        yPixelsPerMillimeter = oneInch.getScaleY() * 72 / 25.4;
    }

    private double scaleX(double value) {
        return arenaScale * xPixelsPerMillimeter * value;
    }

    private double scaleY(double value) {
        return arenaScale * yPixelsPerMillimeter * value;
    }

    private double actualX(double scaledValue) {
        return scaledValue / arenaScale / xPixelsPerMillimeter;
    }

    private double actualY(double scaledValue) {
        return scaledValue / arenaScale / yPixelsPerMillimeter;
    }

    public class SkeletonPanel
            extends JPanel {

        private LarvaSkeleton actualCurrentSkeleton;
        private LarvaSkeleton currentSkeleton; // possibly rotated
        private double currentScale;
        private Rectangle skeletonBounds;
        private ArenaIntensityBackground arenaIntensityBackground;
        private boolean rotateSkeleton;

        public SkeletonPanel() {
            actualCurrentSkeleton = null;
            currentSkeleton = null;
            skeletonBounds = null;
            arenaIntensityBackground = null;
            rotateSkeleton = true; // always true for now, could support rotated background later
            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(600, 600));
        }

        public void setArenaBackground() {
            if (definedEnvironmentRule == null) {
                arenaIntensityBackground = null;
            } else {
                arenaIntensityBackground = new ArenaIntensityBackground();
            }
        }

        public void setSkeleton(LarvaSkeleton currentSkeleton) {

            final LarvaSkeleton previousSkeleton = this.actualCurrentSkeleton;
            final double previousScale = this.currentScale;

            this.actualCurrentSkeleton = currentSkeleton;
            this.currentScale = arenaScale;

//            boolean fullRepaint = true;
//            final Rectangle previousSkeletonBounds = skeletonBounds;

            if (previousSkeleton != currentSkeleton) {

                this.currentSkeleton = currentSkeleton;
                if ((definedEnvironmentRule != null) && rotateSkeleton) {
                    final Long rotateTime = definedEnvironmentRule.getRotateTime();
                    if ((rotateTime != null) &&
                        (rotateTime <= currentSkeleton.getCaptureTime())) {
                        this.currentSkeleton =
                                definedEnvironmentRule.getRotatedAndTransformedSkeleton(
                                        currentSkeleton);
                    }
                }

            }

            if (previousScale != this.currentScale) {
                skeletonBounds = deriveSkeletonBounds(this.currentSkeleton);
            }

// Had to abandon optimized painting because of background problems.
// Leaving this commented-out here in case it can be used in the future.

//            // if scale is same and trace history is not being drawn,
//            // shrink repaint clip area to improve performance
//            if ((previousScale == this.currentScale) && (traceHistoryTime == 0)) {
//                fullRepaint = false;
//                final Rectangle repaintBounds =
//                        previousSkeletonBounds.union(skeletonBounds);
//                repaint(repaintBounds);
//            }
//
//            if (fullRepaint) {
                repaint();
//            }

        }

        public Rectangle getSkeletonBounds() {
            return skeletonBounds;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();

            if (arenaIntensityBackground != null) {

                final Long rotateTime = definedEnvironmentRule.getRotateTime();

                if ((rotateTime == null) || (rotateTime <= currentSkeleton.getCaptureTime())) {

                    // We should be able to use g2d.getClipBounds() here
                    // BUT paint problems surface when zoomed-in (arenaScale > 3).
                    // This covers the whole view port and seems to hide the problems.
                    final Rectangle clipBounds =
                            skeletonScrollPane.getViewport().getViewRect(); //

                    final double dx1 = clipBounds.getX();
                    final double dy1 = clipBounds.getY();
                    final double dx2 = dx1 + clipBounds.getWidth();
                    final double dy2 = dy1 + clipBounds.getHeight();

                    final int sx1 = (int) actualX(dx1);
                    final int sy1 = (int) actualY(dy1);
                    final int sx2 = (int) actualX(dx2);
                    final int sy2 = (int) actualY(dy2);

                    if (rotateSkeleton) {
                        g2d.drawImage(arenaIntensityBackground.getImage(),
                                      (int)dx1, (int)dy1, (int)dx2, (int)dy2,
                                      sx1, sy1, sx2, sy2, null);

                    } else {
                        g2d.drawImage(arenaIntensityBackground.getRotatedImage(),
                                      (int)dx1, (int)dy1, (int)dx2, (int)dy2,
                                      sx1, sy1, sx2, sy2, null);
                    }
                }

            }

            if (currentSkeleton != null) {

                if (showActualSkeletonCheckBox.isSelected() &&
                    (actualCurrentSkeleton != currentSkeleton)) {
                    g2d.setPaint(Color.GRAY);
                    drawSkeleton(g2d, actualCurrentSkeleton);
                    drawMarker(g2d, actualCurrentSkeleton.getHead(), Color.ORANGE);
                    // do not draw mid-point for smallest scale
                    if (arenaScale > 0.5) {
                        drawMarker(g2d, actualCurrentSkeleton.getMidpoint(), Color.ORANGE);
                    }
                }

                if (traceHistoryTime > 0) {
                    drawTraceHistory(g2d);
                }

                g2d.setPaint(Color.WHITE);

                drawSkeleton(g2d, currentSkeleton);

                final TrackerPoint head = currentSkeleton.getHead();
                drawMarker(g2d, head, Color.RED);

                // do not draw mid-point for smallest scale
                if (arenaScale > 0.5) {
                    final TrackerPoint midpoint = currentSkeleton.getMidpoint();
                    drawMarker(g2d, midpoint, Color.RED);
                }

            }

        }

        private Rectangle deriveSkeletonBounds(LarvaSkeleton skeleton) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = -1;
            int maxY = -1;
            int x;
            int y;
            for (TrackerPoint p : skeleton.getPoints()) {
                x = (int) scaleX(p.getX());
                y = (int) scaleY(p.getY());
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }

            // pad actual bounds so that mid-point and head are covered
            final int margin = 20;
            minX = minX - margin;
            minY = minY - margin;
            maxX = maxX + margin;
            maxY = maxY + margin;
            if (minX < 0) {
                minX = 0;
            }
            if (minY < 0) {
                minY = 0;
            }

            return new Rectangle(minX, minY, (maxX - minX), (maxY - minY));
        }

        private void drawSkeleton(Graphics2D g2d,
                                  LarvaSkeleton skeleton) {
            TrackerPoint previousPoint = null;
            for (TrackerPoint point : skeleton.getPoints()) {
                if (previousPoint != null) {
                    g2d.drawLine(
                            (int) scaleX(previousPoint.getX()),
                            (int) scaleY(previousPoint.getY()),
                            (int) scaleX(point.getX()),
                            (int) scaleY(point.getY()));
                }
                previousPoint = point;
            }
        }

        private void drawMarker(Graphics2D g2d,
                                TrackerPoint point,
                                Color color) {
            int heightAndWidth = 5;
            if (arenaScale < 2.0) {
                // use smaller marker if zoomed-out
                heightAndWidth = 3;
            }
            final double centerX = scaleX(point.getX());
            final double centerY = scaleY(point.getY());
            final double upperLeftX = centerX - heightAndWidth / 2.0;
            final double upperLeftY = centerY - heightAndWidth / 2.0;

            final Ellipse2D.Double circle =
                    new Ellipse2D.Double(upperLeftX,
                                         upperLeftY,
                                         heightAndWidth,
                                         heightAndWidth);
            g2d.setPaint(color);
            g2d.fill(circle);
        }

        private void drawTraceHistory(Graphics2D g2d) {

            final int currentFrameIndex = logModel.getCurrentFrame();
            final java.util.List<LarvaFrameData> list =
                    logModel.getFrameDataList();
            final int initialCapacity = (currentFrameIndex / 9) + 1;

            final TrackerPoint centroid = currentSkeleton.getCentroid();

            GeneralPath polyline =
                    new GeneralPath(GeneralPath.WIND_EVEN_ODD,
                                    initialCapacity);

            polyline.moveTo(scaleX(centroid.getX()),
                            scaleY(centroid.getY()));

            final boolean transformCentroid = (definedEnvironmentRule != null) && rotateSkeleton;
            final LarvaFrameData currentFrameData = list.get(currentFrameIndex);
            long stopTime = currentFrameData.getTime() - traceHistoryTime;
            LarvaFrameData frameData;
            TrackerPoint point = null;
            for (int i = currentFrameIndex - 15; i > 0; i = i - 15) {
                frameData = list.get(i);
                if (frameData.getTime() > stopTime) {

                    point = frameData.getSkeleton().getCentroid();

                    if (transformCentroid) {
                        point = definedEnvironmentRule.getRotatedPoint(point);
                        point = definedEnvironmentRule.getTransformedPoint(point);
                    }

                    polyline.lineTo(scaleX(point.getX()),
                                    scaleY(point.getY()));
                } else {
                    break;
                }
            }

            if (point != null) {
                g2d.setPaint(Color.GREEN);
                g2d.draw(polyline);
            }

        }

    }

    public class ArenaIntensityBackground {

        private BufferedImage image;
        private BufferedImage rotatedImage;

        public ArenaIntensityBackground() {
            final PositionalVariableFunction function =
                    definedEnvironmentRule.getIntensityFunction();
            final int width = (int) function.getMaximumVariableX();
            final int height = (int) function.getMaximumVariableY();

            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            int blue;
            double intensity;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    intensity = function.getValue(x, y);
                    blue = (int) ((intensity / 100) * 255);
                    image.setRGB(x, y, blue);
                }
            }

            final int white = Color.WHITE.getRGB();
            final int darkGray = Color.DARK_GRAY.getRGB();

            for (int x = 0; x < width; x++) {
                image.setRGB(x, 0, white);
                image.setRGB(x, height - 1, darkGray);
            }

            for (int y = 0; y < height; y++) {
                image.setRGB(0, y, darkGray);
                image.setRGB(width - 1, y, darkGray);
            }

            // note: rotated background is not currently used, but could be later
            if (definedEnvironmentRule.isEnableOrientationLogic()) {

                final double skeletonRotationInRadians =
                        definedEnvironmentRule.getRotationAngleInRadians();
                final double arenaRotationInRadians =
                        -1 * skeletonRotationInRadians;
                final double arenaCenterX = (double) width / 2.0;
                final double arenaCenterY = (double) height / 2.0;

                AffineTransform tx = new AffineTransform();
                tx.rotate(arenaRotationInRadians,
                          arenaCenterX,
                          arenaCenterY);

                final double skeletonXOffset = definedEnvironmentRule.getxOffset();
                final double skeletonYOffset = definedEnvironmentRule.getyOffset();

                final double arenaXOffset = -skeletonXOffset;
                final double arenaYOffset = -skeletonYOffset;

                tx.translate(arenaXOffset, arenaYOffset);

                rotatedImage =
                        new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = rotatedImage.createGraphics();

                // not sure this hint is really needed
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(image, tx, null);
            }

        }

        public BufferedImage getImage() {
            return image;
        }

        public BufferedImage getRotatedImage() {
            return rotatedImage;
        }

    }

}
