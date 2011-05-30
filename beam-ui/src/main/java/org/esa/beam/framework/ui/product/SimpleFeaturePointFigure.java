/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractPointFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Handle;
import com.bc.ceres.swing.figure.Symbol;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.PointHandle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class SimpleFeaturePointFigure extends AbstractPointFigure implements SimpleFeatureFigure {

    private static final Font labelFont = new Font("Helvetica", Font.BOLD, 14);
    private static final int[] labelOutlineAlphas = new int[]{64, 128, 192, 255};
    private static final Stroke[] labelOutlineStrokes = new Stroke[labelOutlineAlphas.length];
    private static final Color[] labelOutlineColors = new Color[labelOutlineAlphas.length];
    private static final Color labelFontColor = Color.WHITE;
    private static final Color labelOutlineColor = Color.BLACK;

    private final SimpleFeature simpleFeature;
    private Point geometry;
    private double radius;

    static {
        for (int i = 0; i < labelOutlineAlphas.length; i++) {
            labelOutlineStrokes[i] = new BasicStroke((labelOutlineAlphas.length - i));
            labelOutlineColors[i] = new Color(labelOutlineColor.getRed(),
                                              labelOutlineColor.getGreen(),
                                              labelOutlineColor.getBlue(),
                                              labelOutlineAlphas[i]);
        }
    }

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, FigureStyle style) {
        this(simpleFeature, style, style);
    }

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, FigureStyle normalStyle, FigureStyle selectedStyle) {
        super(normalStyle, selectedStyle);
        this.simpleFeature = simpleFeature;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Point)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        geometry = (Point) o;
        radius = 6.0;
        setSelectable(true);
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Point getGeometry() {
        return geometry;
    }

    @Override
    public void setGeometry(Geometry geometry) {
        this.geometry = (Point) geometry;
    }

    @Override
    public void forceRegeneration() {
    }

    @Override
    public double getX() {
        return geometry.getX();
    }

    @Override
    public double getY() {
        return geometry.getY();
    }

    @Override
    public void setLocation(double x, double y) {
        Coordinate coordinate = geometry.getCoordinate();
        coordinate.x = x;
        coordinate.y = y;
        geometry.geometryChanged();
        fireFigureChanged();
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public Rectangle2D getBounds() {
        final double eps = 1e-10;
        return new Rectangle2D.Double(getX() - eps, getY() - eps, 2 * eps, 2 * eps);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        final double dx = point.getX() - getX();
        final double dy = point.getY() - getY();
        final AffineTransform scaleInstance = AffineTransform.getScaleInstance(m2v.getScaleX(), m2v.getScaleY());
        final Point2D delta = scaleInstance.transform(new Point2D.Double(dx, -dy), null);
        final Symbol symbol = getSymbol();
        return symbol.containsPoint(delta.getX() + symbol.getRefX(),
                                    delta.getY() + symbol.getRefY());
/*
        final double dx = point.getX() - getX();
        final double dy = point.getY() - getY();
        final Object symbolAttribute = simpleFeature.getAttribute("symbol");
        AffineTransform scaleInstance = AffineTransform.getScaleInstance(m2v.getScaleX(), m2v.getScaleY());
        Point2D delta = scaleInstance.transform(new Point2D.Double(dx, -dy), null);
        if (symbolAttribute instanceof ShapeFigure) {
            final Rectangle2D bounds = ((ShapeFigure) symbolAttribute).getBounds();
            return bounds.contains(delta);
        } else {
            return delta.getX() * delta.getX() + delta.getY() * delta.getY() < radius * radius;
        }
*/
    }

    @Override
    protected void drawPointSymbol(Rendering rendering, Symbol symbol) {
        super.drawPointSymbol(rendering, symbol);
/*
        rendering.getGraphics().setPaint(Color.BLUE);
        rendering.getGraphics().setStroke(new BasicStroke(1.0f));
        final Object symbolAttribute = simpleFeature.getAttribute("symbol");
        if (symbolAttribute instanceof ShapeFigure) {
            ((ShapeFigure) symbolAttribute).draw(rendering.getGraphics());
        } else {
            drawCross(rendering);
        }
        if (isSelected()) {
            rendering.getGraphics().setPaint(new Color(255, 255, 0, 200));
            rendering.getGraphics().setStroke(new BasicStroke(0.5f));
            if (symbolAttribute instanceof PlacemarkSymbol) {
                ((PlacemarkSymbol) symbolAttribute).drawSelected(rendering.getGraphics());
            } else {
                drawCross(rendering);
            }
        }
*/
        final Object labelAttribute = simpleFeature.getAttribute("label");
        if (labelAttribute instanceof String) {
            drawLabel(rendering, (String) labelAttribute);
        }
    }

    private void drawLabel(Rendering rendering, String label) {
        final Graphics2D graphics = rendering.getGraphics();
        final Font oldFont = graphics.getFont();
        final Stroke oldStroke = graphics.getStroke();
        final Paint oldPaint = graphics.getPaint();

        try {
            graphics.setFont(labelFont);
            GlyphVector glyphVector = labelFont.createGlyphVector(graphics.getFontRenderContext(), label);
            Rectangle2D logicalBounds = glyphVector.getLogicalBounds();
            float tx = (float) (logicalBounds.getX() - 0.5 * logicalBounds.getWidth());
            float ty = (float) (1.0 + logicalBounds.getHeight());
            Shape labelOutline = glyphVector.getOutline(tx, ty);

            for (int i = 0; i < labelOutlineAlphas.length; i++) {
                graphics.setStroke(labelOutlineStrokes[i]);
                graphics.setPaint(labelOutlineColors[i]);
                graphics.draw(labelOutline);
            }

            graphics.setPaint(labelFontColor);
            graphics.fill(labelOutline);
        } finally {
            graphics.setPaint(oldPaint);
            graphics.setStroke(oldStroke);
            graphics.setFont(oldFont);
        }
    }

    @Override
    public int getMaxSelectionStage() {
        return 2;
    }

    @Override
    public Handle[] createHandles(int selectionStage) {
        if (selectionStage == 2) {
            DefaultFigureStyle handleStyle = new DefaultFigureStyle();
            handleStyle.setStrokeColor(Color.ORANGE);
            handleStyle.setStrokeOpacity(0.8);
            handleStyle.setStrokeWidth(1.0);
            handleStyle.setFillColor(Color.WHITE);
            handleStyle.setFillOpacity(0.5);
            return new Handle[]{new PointHandle(this, handleStyle)};
            // return new Handle[] {new PointHandle(this, handleStyle, new Rectangle(-20, -20, 40, 40))};
        }
        return super.createHandles(selectionStage);
    }
}