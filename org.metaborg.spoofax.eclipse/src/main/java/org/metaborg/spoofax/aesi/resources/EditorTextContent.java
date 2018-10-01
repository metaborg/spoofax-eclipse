package org.metaborg.spoofax.aesi.resources;

import org.metaborg.aesi.resources.ITextContent;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.spoofax.interpreter.terms.IStrategoTerm;

/**
 * Text content associated with an opened Eclipse editor.
 */
public final class EditorTextContent implements ISpoofaxTextContent {

    // This implementation is not correct. Changes to the document are reflected
    // in this Content object, but it should be static once returned.

    private final IEclipseEditor<IStrategoTerm> editor;

    /**
     * Initializes a new instance of the {@link EditorTextContent} class.
     *
     * @param editor The associated editor.
     */
    public EditorTextContent(IEclipseEditor<IStrategoTerm> editor) {
        if (editor == null) throw new IllegalArgumentException("editor must not be null.");

        this.editor = editor;
    }

    /**
     * Gets the associated editor.
     *
     * @return The associated editor.
     */
    public IEclipseEditor<IStrategoTerm> getEditor() { return this.editor; }

    public long getLength() { return this.editor.document().getLength(); }

    public int getLineCount() { return this.editor.document().getNumberOfLines(); }

    public String getText() { return this.editor.document().get(); }

    public long getVersionStamp() {
        // TODO: Fix version stamp
        // The IDocument may have a modification stamp that is a long.
        return 0;
    }

    public ILanguageImpl getLanguage() { return this.editor.language(); }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ITextContent
            && ((ITextContent)obj).getText().equals(this.getText());
    }

    @Override
    public int hashCode() {
        return getText().hashCode();
    }
}
