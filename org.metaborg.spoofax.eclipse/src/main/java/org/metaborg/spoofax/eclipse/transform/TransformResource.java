package org.metaborg.spoofax.eclipse.transform;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class TransformResource {
    public final FileObject source;
    public final IProject project;
    public final String text;
    public final @Nullable ISourceRegion selection;


    public TransformResource(FileObject source, IProject project, String text, @Nullable ISourceRegion selection) {
        this.source = source;
        this.project = project;
        this.text = text;
        this.selection = selection;
    }


    @Override public String toString() {
        return source.toString();
    }
}
