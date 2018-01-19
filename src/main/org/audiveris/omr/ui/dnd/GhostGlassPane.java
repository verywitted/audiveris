//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G h o s t G l a s s P a n e                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.dnd;

import org.audiveris.omr.ui.symbol.SymbolImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Class {@code GhostGlassPane} is a special glasspane, meant for displaying a shape
 * being dragged and finally dropped.
 *
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostGlassPane
        extends JPanel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GhostGlassPane.class);

    /** Composite to be used over a droppable target */
    private static final AlphaComposite targetComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.5f);

    /** Composite to be used over a non-droppable target */
    private static final AlphaComposite nonTargetComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.2f);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The image to be dragged. */
    protected BufferedImage draggedImage = null;

    /** The previous display bounds. */
    protected Rectangle prevRectangle = null;

    /** The current location relative to this glasspane. */
    protected Point localPoint = null;

    /** Are we over a droppable target?. */
    protected boolean overTarget = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new GhostGlassPane object
     */
    public GhostGlassPane ()
    {
        setOpaque(false);
        setName("GhostGlassPane");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // paintComponent //
    //----------------//
    @Override
    public void paintComponent (Graphics g)
    {
        if ((draggedImage == null) || (localPoint == null)) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g;

        // Use composition with display underneath
        if (!overTarget) {
            g2.setComposite(nonTargetComposite);
        } else {
            g2.setComposite(targetComposite);
        }

        Rectangle rect = getImageBounds(localPoint);
        g2.drawImage(draggedImage, null, rect.x, rect.y);
    }

    //---------//
    // setImage //
    //---------//
    /**
     * Assign the image to be dragged
     *
     * @param draggedImage the image to drag
     */
    public void setImage (BufferedImage draggedImage)
    {
        this.draggedImage = draggedImage;
    }

    //---------------//
    // setOverTarget //
    //---------------//
    /**
     * Tell the glasspane whether we are currently over a droppable target
     *
     * @param overTarget true if over a target
     */
    public void setOverTarget (boolean overTarget)
    {
        this.overTarget = overTarget;
    }

    //----------//
    // setPoint //
    //----------//
    /**
     * Assign the current point, where the dragged image is to be displayed
     *
     * @param screenPoint the current location (screen-based)
     */
    public void setPoint (ScreenPoint screenPoint)
    {
        setLocalPoint(screenPoint.getLocalPoint(this));
    }

    //---------------//
    // setLocalPoint //
    //---------------//
    /**
     * Assign the current point, where the dragged image is to be displayed,
     * and repaint as few as possible of the glass pane.
     *
     * @param localPoint the current location (glasspane-based)
     */
    private void setLocalPoint (Point localPoint)
    {
        // Anything to repaint since last time the point was set?
        if (draggedImage != null) {
            Rectangle rect = getSceneBounds(localPoint);
            Rectangle dirty = new Rectangle(rect);

            if (prevRectangle != null) {
                dirty.add(prevRectangle);
            }

            dirty.grow(1, 1); // To cope with rounding errors

            // Set new values now, to avoid race condition with repaint
            this.localPoint = localPoint;
            prevRectangle = rect;

            repaint(dirty.x, dirty.y, dirty.width, dirty.height);
        } else {
            this.localPoint = localPoint;
            prevRectangle = null;
        }
    }

    //----------------//
    // getImageBounds //
    //----------------//
    /**
     * Report the bounds of just the image.
     *
     * @param center image center
     * @return image bounds
     */
    protected Rectangle getImageBounds (Point center)
    {
        Rectangle rect = new Rectangle(center);
        rect.grow(draggedImage.getWidth() / 2, draggedImage.getHeight() / 2);

        if (draggedImage instanceof SymbolImage) {
            SymbolImage symbolImage = (SymbolImage) draggedImage;
            Point refPoint = symbolImage.getRefPoint();

            if (refPoint != null) {
                rect.translate(-refPoint.x, -refPoint.y);
            }
        }

        return rect;
    }

    //----------------//
    // getSceneBounds //
    //----------------//
    /**
     * Report the bounds of whole scene (image + items).
     *
     * @param center location for image center (not necessarily scene center)
     * @return the bounds of whole scene
     */
    protected Rectangle getSceneBounds (Point center)
    {
        return getImageBounds(center); // By default
    }
}