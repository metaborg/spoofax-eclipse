package org.metaborg.spoofax.eclipse.meta.language;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.metaborg.core.project.IProjectService;
import org.metaborg.meta.core.project.ILanguageSpecService;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.LanguageLoader;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;



public class MetaLanguageLoader implements IResourceChangeListener {
    private static final ILogger logger = LoggerUtils.logger(MetaLanguageLoader.class);

    private final IEclipseResourceService resourceService;
    private final IProjectService projectService;
    private final ILanguageSpecService languageSpecService;

    private final LanguageLoader languageLoader;
    private final GlobalSchedulingRules globalRules;
    private final IWorkspaceRoot workspaceRoot;


    @jakarta.inject.Inject @javax.inject.Inject public MetaLanguageLoader(IEclipseResourceService resourceService, IProjectService projectService,
        ILanguageSpecService languageSpecService, GlobalSchedulingRules globalRules, LanguageLoader languageLoader) {
        this.resourceService = resourceService;
        this.projectService = projectService;
        this.languageSpecService = languageSpecService;

        this.globalRules = globalRules;
        this.languageLoader = languageLoader;
        this.workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    }


    @Override public void resourceChanged(IResourceChangeEvent event) {
        final Collection<IProject> newProjects = new LinkedList<>();
        final Collection<IProject> openedProjects = new LinkedList<>();

        if(event.getType() == IResourceChangeEvent.PRE_CLOSE || event.getType() == IResourceChangeEvent.PRE_DELETE) {
            final IResource resource = event.getResource();
            final FileObject location = resourceService.resolve(resource);
            languageLoader.unloadJob(location).schedule();
        }

        final IResourceDelta delta = event.getDelta();
        if(delta == null) {
            return;
        }

        try {
            delta.accept(new IResourceDeltaVisitor() {
                public boolean visit(IResourceDelta delta) throws CoreException {
                    final IResource resource = delta.getResource();
                    if(resource instanceof IProject) {
                        final IProject project = (IProject) resource;
                        final int kind = delta.getKind();
                        final int flags = delta.getFlags();
                        if(flags == IResourceDelta.OPEN) {
                            openedProjects.add(project);
                        } else if(kind == IResourceDelta.ADDED && project.isAccessible()) {
                            newProjects.add(project);
                        }
                    }

                    // Only continue for the workspace root
                    return resource.getType() == IResource.ROOT;
                }
            });
        } catch(CoreException e) {
            logger.error("Error occurred during project opened notification", e);
        }

        for(IProject project : newProjects) {
            if(!isLanguageProject(project)) {
                return;
            }
            final FileObject location = resourceService.resolve(project);
            languageLoader.loadJob(location, true).schedule();
        }

        for(IProject project : openedProjects) {
            if(!isLanguageProject(project)) {
                return;
            }
            final FileObject location = resourceService.resolve(project);
            languageLoader.loadJob(location, true).schedule();
        }
    }

    /**
     * Loads all language components and dialects from language specifications of workspace projects.
     */
    public void loadFromProjects() {
        logger.debug("Loading languages and dialects from workspace projects");
        for(final IProject project : workspaceRoot.getProjects()) {
            if(project.isOpen() && isLanguageProject(project)) {
                languageLoader.load(project, true);
            }
        }
    }

    /**
     * Creates a job that loads all language components and dialects from language specifications of workspace projects.
     */
    public Job loadFromProjectsJob() {
        final Job job = new DiscoverLanguagesFromProjectsJob(this);
        job.setRule(new MultiRule(new ISchedulingRule[] { workspaceRoot, globalRules.startupWriteLock(),
            globalRules.languageServiceLock() }));
        job.schedule();
        return job;
    }

    /**
     * Checks if given Eclipse project is a language specification project.
     * 
     * @param eclipseProject
     *            Eclipse project to check
     * @return True if project is a language specification project, false if not.
     */
    public boolean isLanguageProject(IProject eclipseProject) {
        final FileObject resource = resourceService.resolve(eclipseProject);
        final org.metaborg.core.project.IProject project = projectService.get(resource);
        if(project == null) {
            return false;
        }
        return languageSpecService.available(project);
    }
}
