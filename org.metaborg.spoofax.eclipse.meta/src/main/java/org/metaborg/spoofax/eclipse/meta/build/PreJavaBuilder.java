package org.metaborg.spoofax.eclipse.meta.build;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.meta.core.SpoofaxLanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpec;
import org.metaborg.spoofax.meta.core.project.ISpoofaxLanguageSpecService;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import com.google.inject.Injector;

public class PreJavaBuilder extends Builder {
    public static final String id = SpoofaxMetaPlugin.id + ".builder.prejava";

    private static final ILogger logger = LoggerUtils.logger(PreJavaBuilder.class);

    private final SpoofaxMetaBuilder builder;


    public PreJavaBuilder() {
        super(SpoofaxMetaPlugin.injector().getInstance(IEclipseResourceService.class),
            SpoofaxMetaPlugin.injector().getInstance(IProjectService.class),
            SpoofaxMetaPlugin.injector().getInstance(ISpoofaxLanguageSpecService.class));
        final Injector injector = SpoofaxMetaPlugin.injector();
        this.builder = injector.getInstance(SpoofaxMetaBuilder.class);
    }


    @Override protected void build(final ISpoofaxLanguageSpec languageSpec, final IProgressMonitor monitor)
        throws CoreException, IOException {
        final SpoofaxLanguageSpecBuildInput input = createBuildInput(languageSpec);

        final IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            @Override public void run(IProgressMonitor workspaceMonitor) throws CoreException {
                try {
                    logger.info("Building language project {}", languageSpec);
                    builder.compilePreJava(input);
                } catch(MetaborgException e) {
                    workspaceMonitor.setCanceled(true);
                    monitor.setCanceled(true);
                    if(e.getCause() != null) {
                        logger.error("Exception thrown during build", e);
                        logger.error("BUILD FAILED");
                    } else {
                        final String message = e.getMessage();
                        if(message != null && !message.isEmpty()) {
                            logger.error(message);
                        }
                        logger.error("BUILD FAILED");
                    }
                } finally {
                    // Refresh project to force resource updates for files generated by the build.
                    getProject().refreshLocal(IResource.DEPTH_INFINITE, workspaceMonitor);
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, getProject(), IWorkspace.AVOID_UPDATE, monitor);
    }

    @Override protected void clean(ISpoofaxLanguageSpec languageSpec, IProgressMonitor monitor) throws CoreException {

    }

    @Override protected String description() {
        return "build";
    }
}
