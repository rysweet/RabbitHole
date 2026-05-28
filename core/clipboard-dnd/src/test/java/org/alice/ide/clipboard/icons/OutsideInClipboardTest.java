package org.alice.ide.clipboard.icons;

import org.alice.ide.clipboard.DragReceptorState;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.Assert.*;

/**
 * Outside-in tests: exercise ClipboardIcon exactly as Swing would call it.
 * Verifies the refactored rendering delegates produce visible, non-blank output
 * and that the public API contract is preserved across all DragReceptorState values.
 */
public class OutsideInClipboardTest {

    // Scenario 1: Basic rendering - icon paints non-blank pixels in default state
    @Test
    public void paintIcon_defaultState_producesVisiblePixels() {
        ClipboardIcon icon = new ClipboardIcon();
        assertEquals("Default width", 48, icon.getIconWidth());
        assertEquals("Default height", 43, icon.getIconHeight());
        
        BufferedImage img = new BufferedImage(48, 43, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Paint exactly as Swing JLabel would call it
        icon.paintIcon(null, g2d, 0, 0);
        g2d.dispose();
        
        // Verify non-blank output
        int nonTransparentPixels = 0;
        for (int y = 0; y < 43; y++) {
            for (int x = 0; x < 48; x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 0) nonTransparentPixels++;
            }
        }
        assertTrue("Should paint visible pixels, got " + nonTransparentPixels, nonTransparentPixels > 100);
        System.out.println("Scenario 1 PASS: " + nonTransparentPixels + " non-transparent pixels painted (default/empty state)");
    }
    
    // Scenario 2: Full clipboard + all drag states produce pixel-distinct renderings
    @Test
    public void paintIcon_allStates_produceDifferentPixelData() {
        ClipboardIcon icon = new ClipboardIcon();
        
        // Render in IDLE/empty state
        BufferedImage imgEmpty = renderIcon(icon, false, DragReceptorState.IDLE);
        int[] emptyPixels = imgEmpty.getRGB(0, 0, 48, 43, null, 0, 48);
        
        // Render in IDLE/full state (should add paper → different pixels)
        BufferedImage imgFull = renderIcon(icon, true, DragReceptorState.IDLE);
        int[] fullPixels = imgFull.getRGB(0, 0, 48, 43, null, 0, 48);
        
        // Render in ENTERED state (should add paper even when not full)
        BufferedImage imgEntered = renderIcon(icon, false, DragReceptorState.ENTERED);
        int[] enteredPixels = imgEntered.getRGB(0, 0, 48, 43, null, 0, 48);
        
        // Count pixel-level differences
        int diffFull = countDifferingPixels(emptyPixels, fullPixels);
        int diffEntered = countDifferingPixels(emptyPixels, enteredPixels);
        
        // Full state should differ from empty (paper is rendered over board)
        assertTrue("Full should differ from empty by >100 pixels, got " + diffFull, diffFull > 100);
        
        // ENTERED state should also differ (paper shown on drag hover)
        assertTrue("ENTERED should differ from IDLE/empty by >100 pixels, got " + diffEntered, diffEntered > 100);
        
        // Verify dimension setter works (API contract)
        icon.setDimension(new Dimension(96, 86));
        assertEquals(96, icon.getIconWidth());
        assertEquals(86, icon.getIconHeight());
        BufferedImage imgScaled = new BufferedImage(96, 86, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imgScaled.createGraphics();
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        int scaledNonTransparent = countNonTransparent(imgScaled);
        assertTrue("Scaled rendering should produce visible pixels: " + scaledNonTransparent, scaledNonTransparent > 100);
        
        System.out.println("Scenario 2 PASS: diffFull=" + diffFull + " diffEntered=" + diffEntered 
                + " scaledPixels=" + scaledNonTransparent);
    }
    
    private BufferedImage renderIcon(ClipboardIcon icon, boolean isFull, DragReceptorState state) {
        icon.setFull(isFull);
        icon.setDragReceptorState(state);
        icon.setDimension(new Dimension(48, 43));
        
        BufferedImage img = new BufferedImage(48, 43, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        icon.paintIcon(null, g2d, 0, 0);
        g2d.dispose();
        return img;
    }
    
    private int countDifferingPixels(int[] a, int[] b) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) count++;
        }
        return count;
    }
    
    private int countNonTransparent(BufferedImage img) {
        int count = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >> 24) & 0xFF) > 0) count++;
            }
        }
        return count;
    }
}
