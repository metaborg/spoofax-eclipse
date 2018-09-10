package org.metaborg.spoofax.eclipse.editor.tracing;

import java.io.File;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.texteditor.ITextEditor;
import org.metaborg.core.source.ISourceLocation;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.RegionUtils;

final class SpoofaxHyperlink implements IHyperlink {
    private final IEclipseResourceService resourceService;

    private final ISourceRegion highlight;
    private final ISourceLocation target;
    private final FileObject editorResource;
    private final ITextEditor editor;
    private final @Nullable String hyperlinkText;

    public SpoofaxHyperlink(IEclipseResourceService resourceService, ISourceRegion highlight, ISourceLocation target,
            FileObject editorResource, ITextEditor editor) {
        this(resourceService, highlight, target, editorResource, editor, null);
    }

    public SpoofaxHyperlink(IEclipseResourceService resourceService, ISourceRegion highlight, ISourceLocation target,
            FileObject editorResource, ITextEditor editor, @Nullable String hyperlinkText) {
        this.resourceService = resourceService;

        this.highlight = highlight;
        this.target = target;
        this.editorResource = editorResource;
        this.editor = editor;
        this.hyperlinkText = hyperlinkText;
    }


    @Override public void open() {
        final FileObject targetResource = target.resource();
        final int offset = target.region().startOffset();

        if(targetResource.getName().equals(editorResource.getName())) {
            EditorUtils.selectAndFocus(editor, offset);
        } else {
            final IResource eclipseResource = resourceService.unresolve(targetResource);
            if(eclipseResource != null && eclipseResource instanceof IFile) {
                final IFile file = (IFile) eclipseResource;
                EditorUtils.open(file, offset);
            } else {
                final File file = resourceService.localFile(targetResource);
                EditorUtils.open(file.toURI());
            }
        }
    }

    @Override public @Nullable String getTypeLabel() {
        return null;
    }

    @Override public @Nullable String getHyperlinkText() {
        return hyperlinkText;
    }

    @Override public IRegion getHyperlinkRegion() {
        return RegionUtils.fromCore(highlight);
    }
}
