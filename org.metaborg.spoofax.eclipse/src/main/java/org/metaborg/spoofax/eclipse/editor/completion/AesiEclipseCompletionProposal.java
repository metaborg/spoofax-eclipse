package org.metaborg.spoofax.eclipse.editor.completion;

import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
//import org.eclipse.ui.ISharedImages;
//import org.eclipse.ui.PlatformUI;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.ISharedImages;
import org.metaborg.aesi.codecompletion.ICodeCompletionProposal;
import org.metaborg.spoofax.aesi.codecompletion.ISpoofaxCodeCompletionProposal;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;

/**
 * An AESI code completion proposal.
 */
public final class AesiEclipseCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension5 {

    private final IInformationControlCreator informationControlCreator;
    private final ISpoofaxCodeCompletionProposal aesiProposal;
    private final int startOffset;
    private final int endOffset;

    /**
     * Initializes a new instance of the {@link AesiEclipseCompletionProposal} class.
     *
     * @param informationControlCreator The information control creator.
     * @param aesiProposal The AESI proposal being wrapped.
     * @param startOffset The offset of the start of the text to be replaced.
     *                    Usually either the current caret position, or the start of the prefix being replaced.
     * @param endOffset The offset of the end of the text to be replaced.
     *                  Usually either the current caret position, or the end of the selection.
     */
    public AesiEclipseCompletionProposal(
            IInformationControlCreator informationControlCreator,
            ISpoofaxCodeCompletionProposal aesiProposal,
            int startOffset,
            int endOffset
    ) {
        assert informationControlCreator != null;
        assert aesiProposal != null;
        assert startOffset >= 0;
        assert endOffset >= 0;
        assert startOffset <= endOffset;

        this.informationControlCreator = informationControlCreator;
        this.aesiProposal = aesiProposal;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public void apply(IDocument document) {
        String replacementString = getReplacementString();
        try {
            document.replace(this.startOffset, this.endOffset - this.startOffset, replacementString);
        } catch (BadLocationException e) {
            // Ignored
        }
    }

    /**
     * Gets the replacement string.
     * @return The replacement string.
     */
    private String getReplacementString() {
        // TODO: Support snippets
        return this.aesiProposal.getContent();
    }

    @Override
    public Point getSelection(IDocument document) {
        // TODO: Support cursor position
        return new Point(this.startOffset + getReplacementString().length(), 0);
//        return new Point(this.offset + this.fCursorPosition, 0);
//        throw new UnsupportedOperationException();
//        if(data.placeholders.isEmpty()) {
//            return new Point(data.cursorPosition, 0);
//        }
//
//        // There are placeholders, let linked mode take care of moving the cursor to the first placeholder. Returning
//        // null signals that selection should not be changed by the completion proposal.
//        return null;
    }

    @Override
    public String getDisplayString() {
        if (aesiProposal.getLabel() != null) {
            return this.aesiProposal.getLabel();
        } else {
            return this.aesiProposal.getContent();
        }
    }

    @Override
    public Image getImage() {
        if (this.aesiProposal.getScopeNames().contains("expansion")) {
            return SpoofaxPlugin.imageRegistry().get("expansion-icon");
        } else if (this.aesiProposal.getScopeNames().contains("recovery")) {
            return SpoofaxPlugin.imageRegistry().get("recovery-icon");
        } else if (this.aesiProposal.getScopeNames().contains("expansion.editing")) {
            return SpoofaxPlugin.imageRegistry().get("expansion-editing-icon");
        } else if (this.aesiProposal.getScopeNames().contains("class")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_CLASS);
        } else if (this.aesiProposal.getScopeNames().contains("function")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
        } else if (this.aesiProposal.getScopeNames().contains("method")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PUBLIC);
        } else if (this.aesiProposal.getScopeNames().contains("package")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
        } else if (this.aesiProposal.getScopeNames().contains("variable")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
        } else if (this.aesiProposal.getScopeNames().contains("constant")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
        } else if (this.aesiProposal.getScopeNames().contains("property")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
        } else if (this.aesiProposal.getScopeNames().contains("field")) {
            return JavaUI.getSharedImages().getImage(ISharedImages.IMG_FIELD_PUBLIC);
        } else {
            return null;
        }
    }

    @Override
    public IContextInformation getContextInformation() { return null; }

    @Override
    public IInformationControlCreator getInformationControlCreator() { return this.informationControlCreator; }

    @Override
    public CharSequence getPrefixCompletionText(IDocument document, int offset) { return null; }

    @Override
    public int getPrefixCompletionStart(IDocument document, int offset) { return 0; }


    @Override
    public String getAdditionalProposalInfo() {
        return BaseEncoding.base64().encode(SerializationUtils.serialize(getAdditionalProposalInfo(null)));
    }

    @Override
    public ISpoofaxCodeCompletionProposal getAdditionalProposalInfo(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) return null;
        return this.aesiProposal;
    }
}
