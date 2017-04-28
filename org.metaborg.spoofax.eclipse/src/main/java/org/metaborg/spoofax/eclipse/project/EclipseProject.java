package org.metaborg.spoofax.eclipse.project;

import org.apache.commons.vfs2.FileObject;
import org.metaborg.core.config.IProjectConfig;
import org.metaborg.core.project.Project;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class EclipseProject extends Project {
    public final org.eclipse.core.resources.IProject eclipseProject;

    public EclipseProject(FileObject location, @Nullable IProjectConfig config,
        org.eclipse.core.resources.IProject eclipseProject) {
        super(location, config);
        this.eclipseProject = eclipseProject;
    }

}
