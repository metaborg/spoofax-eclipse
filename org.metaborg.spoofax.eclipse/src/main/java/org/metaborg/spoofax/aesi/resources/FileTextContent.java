package org.metaborg.spoofax.aesi.resources;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.language.ILanguageImpl;

/**
 * A file backed by a {@link FileObject}.
 */
public class FileTextContent implements ISpoofaxTextContent {

    // TODO: This implementation is wrong.
    // It should be a snapshot of the file at the time its content was retrieved.
    // With this implementation, it can change.

    /**
     * Initializes a new instance of the {@link FileTextContent} class.
     *
     * @param file The associated file.
     */
    public FileTextContent(FileObject file) {
        if (file == null) throw new IllegalArgumentException("file must not be null.");

        this.file = file;
    }

    private final FileObject file;
    /**
     * Gets the associated file.
     * @return The associated file.
     */
    public FileObject getFile() { return this.file; }

    public ILanguageImpl getLanguage() {
        // TODO: Determine language
        throw new UnsupportedOperationException();
    }

    public long getLength() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public long getVersionStamp() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public int getLineCount() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getText() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
