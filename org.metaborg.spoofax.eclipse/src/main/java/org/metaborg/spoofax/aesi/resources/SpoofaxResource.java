package org.metaborg.spoofax.aesi.resources;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.aesi.resources.IResource;
import org.metaborg.aesi.resources.ResourceKind;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.spoofax.core.Spoofax;

import org.metaborg.spoofax.eclipse.util.Nullable;
import java.net.URI;

/**
 * A Spoofax resource.
 */
public final class SpoofaxResource implements ISpoofaxResource {

    /**
     * Initializes a new instance of the {@link SpoofaxResource} class.
     * @param uri The resource URI.
     * @param file The file object.
     * @param kind The kind of resource.
     * @param language The language of the resource; or null.
     */
    public SpoofaxResource(
            URI uri,
            FileObject file,
            ResourceKind kind,
            @Nullable ILanguageImpl language
    ) {
        if (uri == null) throw new IllegalArgumentException("uri must not be null.");
        if (file == null) throw new IllegalArgumentException("file must not be null.");
        if (kind == null) throw new IllegalArgumentException("kind must not be null.");

        this.uri = uri;
        this.file = file;
        this.kind = kind;
        this.language = language;
    }

    private final URI uri;
    @Override
    public URI getUri() { return this.uri; }

    @Override
    public String getName() { return this.file.getName().getBaseName(); }

    private final FileObject file;
    /**
     * Gets the associated virtual file object.
     *
     * @return The associated virtual file object.
     */
    public FileObject getFile() { return this.file; }

    private final ResourceKind kind;
    @Override
    public ResourceKind getKind() { return this.kind; }

    @Override
    public Boolean hasChildren() {
        if (getKind() == ResourceKind.File)
            // Files have _no_ children.
            return false;
        else
            // Folders, projects, and workspaces _may_ have children.
            return null;
    }

    @Nullable private final ILanguageImpl language;
    @Override
    @Nullable public ILanguageImpl getLanguage() { return this.language; }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SpoofaxResource
            && equals((SpoofaxResource)obj);
    }

    public boolean equals(SpoofaxResource other) {
        return other != null
            && this.uri == other.uri;
    }

    @Override
    public int hashCode() {
        return this.uri.hashCode();
    }

    @Override
    public String toString() {
        return this.uri.toString();
    }
}
