package org.metaborg.spoofax.eclipse.util;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.source.SourceRegion;

/**
 * Utility functions for converting regions.
 */
public class RegionUtils {
    /**
     * Converts an Eclipse region to a Core source region.
     */
    public static ISourceRegion toCore(IRegion region) {
        final int offset = region.getOffset();
        return new SourceRegion(offset, -1, -1, offset + region.getLength(), -1, -1);
    }

    /**
     * Converts an Eclipse text selection to a Core source region.
     */
    public static ISourceRegion toCore(ITextSelection textSelection) {
        final int offset = textSelection.getOffset();
        return new SourceRegion(offset, -1, -1, offset + textSelection.getLength(), -1, -1);
    }

    /**
     * Converts a Core source region to an Eclipse region.
     */
    public static IRegion fromCore(ISourceRegion region) {
        final int offset = region.startOffset();
        return new Region(offset, region.endOffset() - offset + 1);
    }
}
