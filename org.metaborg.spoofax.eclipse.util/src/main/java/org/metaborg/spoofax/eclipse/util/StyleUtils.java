package org.metaborg.spoofax.eclipse.util;

import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.apache.commons.lang3.math.NumberUtils.min;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.style.IRegionStyle;
import org.metaborg.core.style.IStyle;

import com.google.common.collect.Lists;

/**
 * Utility functions for creating Eclipse text styles.
 */
public final class StyleUtils {
    /**
     * Stores whether the current theme is a dark theme or not.
     */
    private static boolean isDarkTheme = false;

    static {
        calculateDarkTheme();
        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(e -> calculateDarkTheme());
    }

    private static void calculateDarkTheme() {
        ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
        // Get text color of selected tabs, or ...
        Color foregroundColor = colorRegistry.get("org.eclipse.ui.workbench.ACTIVE_TAB_UNSELECTED_TEXT_COLOR");
        if(foregroundColor == null) // ... the text color of the editors
            foregroundColor = colorRegistry.get("org.eclipse.ui.editors.foregroundColor");
        // If the text color is lighter than average (gray), then it's probably a light theme
        isDarkTheme = foregroundColor.getRed() + foregroundColor.getGreen() + foregroundColor.getBlue() > 384;
    }

    /**
     * Creates an Eclipse text presentation that colors the entire range as one color.
     * 
     * @param color
     *            Text foreground color to use.
     * @param length
     *            Length of the text.
     * @param display
     *            Display to create the Eclipse text presentation on.
     * @return Eclipse text presentation.
     */
    public static TextPresentation createTextPresentation(java.awt.Color color, int length, Display display) {
        final TextPresentation presentation = new TextPresentation();
        final StyleRange styleRange = new StyleRange();
        styleRange.start = 0;
        styleRange.length = length;
        styleRange.foreground = createColor(color, display);
        presentation.addStyleRange(styleRange);
        return presentation;
    }

    /**
     * Creates an Eclipse text presentation from given Spoofax styles.
     * 
     * @param styles
     *            Stream of Spoofax styles.
     * @param display
     *            Display to create the Eclipse text presentation on.
     * @return Eclipse text presentation.
     */
    public static <T> TextPresentation createTextPresentation(Iterable<IRegionStyle<T>> styles, Display display) {
        final TextPresentation presentation = new TextPresentation();
        for(IRegionStyle<T> regionStyle : styles) {
            final StyleRange styleRange = createStyleRange(regionStyle, display);
            presentation.addStyleRange(styleRange);
        }
        IRegion extent = presentation.getExtent();
        if(extent == null) {
            extent = new Region(0, 0);
        }
        final StyleRange defaultStyleRange = new StyleRange();
        defaultStyleRange.start = extent.getOffset();
        defaultStyleRange.length = extent.getLength();
        defaultStyleRange.foreground = createColor(java.awt.Color.BLACK, display);
        presentation.setDefaultStyleRange(defaultStyleRange);

        return presentation;
    }

    /**
     * Creates an Eclipse style range from given Spoofax style region.
     * 
     * @param regionStyle
     *            Spoofax style region.
     * @param display
     *            Display to create the Eclipse style range on.
     * @return Eclipse style range.
     */
    public static StyleRange createStyleRange(IRegionStyle<?> regionStyle, Display display) {
        final IStyle style = regionStyle.style();
        final ISourceRegion region = regionStyle.region();

        final StyleRange styleRange = new StyleRange();
        final java.awt.Color foreground = style.color();
        if(foreground != null) {
            styleRange.foreground = createColor(foreground, display);
        }
        final java.awt.Color background = style.backgroundColor();
        if(background != null) {
            styleRange.background = createColor(background, display);
        }
        if(style.bold()) {
            styleRange.fontStyle |= SWT.BOLD;
        }
        if(style.italic()) {
            styleRange.fontStyle |= SWT.ITALIC;
        }
        if(style.underscore()) {
            styleRange.underline = true;
        }
        if(style.strikeout()) {
            styleRange.strikeout = true;
        }

        styleRange.start = region.startOffset();
        styleRange.length = region.endOffset() - region.startOffset() + 1;

        return styleRange;
    }

    /**
     * Creates an Eclipse color from given Java color.
     * 
     * @param color
     *            Java color.
     * @param display
     *            Display to create the color on.
     * @return Eclipse color.
     */
    public static Color createColor(java.awt.Color color, Display display) {
        if(isDarkTheme) {
            color = invertLightness(color);
        }
        // GTODO: this color object needs to be disposed manually!
        return new Color(display, color.getRed(), color.getGreen(), color.getBlue());
    }

    // Invert lightness of color (note: lightness != brightness/value! L=0 is black, L=1 is white, L=0.5 is color).
    private static java.awt.Color invertLightness(java.awt.Color color) {
        float[] hsl = rgb2hsl(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
        // Flip lightness using a fancy formula: newL = 0.5 + (1 - L⁴) / 4
        float t = hsl[2] * hsl[2], newL = 0.5f + (1 - t * t) / 4f;
        float[] rgb = hsl2rgb(hsl[0], hsl[1], newL);
        return new java.awt.Color(rgb[0], rgb[1], rgb[2]);
    }

    // https://stackoverflow.com/a/54071699
    // input: r,g,b in [0,1], output: h in [0,360) and s,l in [0,1]
    private static float[] rgb2hsl(float r, float g, float b) {
        float a = max(r, g, b), n = a - min(r, g, b), f = (1 - Math.abs(a + a - n - 1));
        float h = n == 0 ? 0 : ((a == r) ? (g - b) / n : ((a == g) ? 2 + (b - r) / n : 4 + (r - g) / n));
        return new float[] { 60 * (h < 0 ? h + 6 : h), f == 0 ? 0 : n / f, (a + a - n) / 2 };
    }

    // https://stackoverflow.com/a/54014428
    // input: h in [0,360] and s,l in [0,1] - output: r,g,b in [0,1]
    private static float[] hsl2rgb(float h, float s, float l) {
        float a = s * min(l, 1 - l);
        float[] c = new float[3];
        for(int i = 0; i < 3; i++) {
            int n = (12 - i * 4) % 12;
            float k = (n + h / 30) % 12;
            c[i] = l - a * max(min(k - 3, 9 - k, 1), -1);
        }
        return c;
    }

    /**
     * Creates a deep copy of given style range.
     * 
     * @param styleRangeRef
     *            Style range to copy.
     * @return Deep copy of given style range.
     */
    public static StyleRange deepCopy(StyleRange styleRangeRef) {
        final StyleRange styleRange = new StyleRange(styleRangeRef);
        styleRange.start = styleRangeRef.start;
        styleRange.length = styleRangeRef.length;
        styleRange.fontStyle = styleRangeRef.fontStyle;
        return styleRange;
    }

    /**
     * Creates deep copies of style ranges in given text presentation.
     * 
     * @param presentation
     *            Text presentation to copy style ranges of.
     * @return Collection of deep style range copies.
     */
    public static Collection<StyleRange> deepCopies(TextPresentation presentation) {
        final Collection<StyleRange> styleRanges = Lists.newLinkedList();
        for(Iterator<StyleRange> iter = presentation.getNonDefaultStyleRangeIterator(); iter.hasNext();) {
            final StyleRange styleRange = iter.next();
            styleRanges.add(deepCopy(styleRange));
        }
        return styleRanges;
    }

    /**
     * Converts given style range to a string.
     * 
     * @param range
     *            Style range to convert.
     * @return String representation of style range.
     */
    public static String styleRangeToString(StyleRange range) {
        final StringBuilder sb = new StringBuilder();
        sb.append("StyleRange[");
        sb.append("start = " + range.start);
        sb.append("length = " + range.length);
        sb.append("underline = " + range.underline);
        sb.append("underlineStyle  = " + range.underlineStyle);
        sb.append("foreground = " + range.foreground);
        sb.append("]");
        return sb.toString();
    }
}
