package org.metaborg.spoofax.aesi.resources;

import com.google.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorInput;
import org.metaborg.aesi.resources.IResource;
import org.metaborg.aesi.resources.ITextContent;
import org.metaborg.aesi.resources.ResourceKind;
import org.metaborg.core.editor.IEditor;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageIdentifierService;
import org.metaborg.core.language.LanguageService;
import org.metaborg.spoofax.eclipse.editor.EditorRegistry;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.project.EclipseProjectService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.metaborg.spoofax.eclipse.util.Nullable;

import javax.naming.OperationNotSupportedException;

/**
 * Resource service for Eclipse.
 */
public class EclipseSpoofaxResourceService implements ISpoofaxResourceService {

    private final IEclipseResourceService spoofaxResourceService;
    private final IEclipseEditorRegistry<IStrategoTerm> editorRegistry;
    private final LanguageIdentifierService languageIdentifierService;

    /**
     * Initializes a new instance of the {@link EclipseSpoofaxResourceService} class.
     *
     * @param spoofaxResourceService The Spoofax resource service.
     * @param editorRegistry The Spoofax editor registry.
     * @param languageIdentifierService The language identifier service.
     */
    @Inject
    public EclipseSpoofaxResourceService(
            IEclipseResourceService spoofaxResourceService,
            IEclipseEditorRegistry<IStrategoTerm> editorRegistry,
            LanguageIdentifierService languageIdentifierService
    ) {
        assert spoofaxResourceService != null;
        assert editorRegistry != null;

        this.spoofaxResourceService = spoofaxResourceService;
        this.editorRegistry = editorRegistry;
        this.languageIdentifierService = languageIdentifierService;
    }

    @Override
    public ISpoofaxResource getWorkspace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ISpoofaxResource resolve(URI resourceUri) {
        FileObject file = this.spoofaxResourceService.resolve(resourceUri);
        return getResourceOfFile(file);
    }

    @Override
    public boolean exists(URI resourceUri) {
        ISpoofaxResource resource = resolve(resourceUri);
        if (resource == null) return false;
        try {
            return resource.getFile().exists();
        } catch (FileSystemException e) {
            return false;
        }
    }

    @Override
    public ISpoofaxResource getParentOf(URI resourceUri) {
        ISpoofaxResource resource = resolve(resourceUri);
        if (resource == null) return null;
        try {
            return getResourceOfFile(resource.getFile().getParent());
        } catch (FileSystemException e) {
            return null;
        }
    }

    @Override
    public Collection<? extends ISpoofaxResource> getChildrenOf(URI resourceUri) {
        ISpoofaxResource resource = resolve(resourceUri);
        if (resource == null) return null;
        try {
            return Arrays.stream(resource.getFile().getChildren())
                    .map(this::getResourceOfFile).collect(Collectors.toList());
        } catch (FileSystemException e) {
            return null;
        }
    }

    @Override
    public ISpoofaxTextContent getContentOf(URI resourceUri) {
        @Nullable IEclipseEditor<IStrategoTerm> editor = getEditorOf(resourceUri);
        if (editor != null) {
            // The file is currently opened.
            return new EditorTextContent(editor);
        } else {
            // The file is not currently opened.
            FileObject file = this.spoofaxResourceService.resolve(resourceUri);
            return new FileTextContent(file);
        }
    }

    @Override
    public ISpoofaxResource getResourceOf(IEditor editor) {
        @Nullable FileObject file = editor.resource();
        if (file == null) return null;
        @Nullable URI resourceUri = tryToUri(file.getName().getURI());
        if (resourceUri == null) return null;
        @Nullable ILanguageImpl language = editor.language();
        return new SpoofaxResource(resourceUri, file, ResourceKind.File, language);
    }

    /**
     * Gets the editor associated with the resource with the specified URI.
     *
     * @param resourceUri The URI of the resource.
     * @return The associated editor, if any; otherwise, null.
     */
    @Nullable
    private IEclipseEditor<IStrategoTerm> getEditorOf(URI resourceUri) {
        for (IEclipseEditor<IStrategoTerm> editor : this.editorRegistry.openEclipseEditors()) {
            @Nullable IEditorInput input = editor.input();
            if (input == null) continue;
            @Nullable FileObject file = this.spoofaxResourceService.resolve(input);
            if (file == null) continue;
            URI uri = tryToUri(file.getName().getURI());
            if (uri == null) continue;
            if (uri.equals(resourceUri))
                return editor;
        }
        return null;
    }

    /**
     * Attempts to convert a string to an URI.
     *
     * @param uriString The URI string to convert.
     * @return The resulting URI; or null when the URI parsing failed.
     */
    @Nullable
    private URI tryToUri(String uriString) {
        URI uri = null;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            // Ignored.
        }
        return uri;
    }

    /**
     * Returns the resource corresponding to the specified {@link FileObject}.
     *
     * @param file The {@link FileObject}.
     * @return The corresponding resource.
     */
    private SpoofaxResource getResourceOfFile(FileObject file) {
        ResourceKind kind = getResourceKind(file);
        ILanguageImpl language = this.languageIdentifierService.identify(file);
        return new SpoofaxResource(tryToUri(file.getName().getURI()), file, kind, language);
    }


    /**
     * Gets the kind of resource.
     * @param file The file object.
     * @return The kind of resource.
     */
    private ResourceKind getResourceKind(FileObject file) {
        ResourceKind kind;
        try {
            if (file.isFile()) {
                kind = ResourceKind.File;
            } else if (file.isFolder()) {
                kind = ResourceKind.Folder;
            } else {
                // TODO: Support Project and Workspace resources.
                throw new UnsupportedOperationException("Unknown kind.");
            }
        } catch (FileSystemException e) {
            throw new RuntimeException(e);
        }
        return kind;
    }

}
