package org.metaborg.spoofax.eclipse.language;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.PlatformUI;
import org.metaborg.core.context.IContextProcessor;
import org.metaborg.core.language.ILanguageCache;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.LanguageFileSelector;
import org.metaborg.core.language.ResourceExtensionFacet;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.LanguageChangeProcessor;
import org.metaborg.core.processing.analyze.IAnalysisResultProcessor;
import org.metaborg.core.processing.parse.IParseResultProcessor;
import org.metaborg.spoofax.eclipse.editor.SpoofaxEditor;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.EditorMappingUtils;
import org.metaborg.spoofax.eclipse.util.MarkerUtils;
import org.metaborg.spoofax.eclipse.util.ResourceUtils;
import org.metaborg.util.Strings;
import org.metaborg.util.collection.Sets;
import org.metaborg.util.iterators.Iterables2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;



/**
 * Extends the {@code LanguageChangeProcessor} to include Eclipse-specific operations such as changing editor
 * associations and resource markers.
 */
public class EclipseLanguageChangeProcessor extends LanguageChangeProcessor {
    private static final ILogger logger = LoggerUtils.logger(EclipseLanguageChangeProcessor.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageIdentifierService languageIdentifier;

    private final IWorkspace workspace;
    private final IEditorRegistry eclipseEditorRegistry;
    private final Display display;


    @jakarta.inject.Inject @javax.inject.Inject public EclipseLanguageChangeProcessor(IEclipseResourceService resourceService,
        ILanguageIdentifierService languageIdentifier, IDialectProcessor dialectProcessor,
        IContextProcessor contextProcessor, IParseResultProcessor<?, ?> parseResultProcessor,
        IAnalysisResultProcessor<?, ?, ?> analysisResultProcessor, org.metaborg.core.editor.IEditorRegistry editorRegistry,
        Set<ILanguageCache> languageCaches) {
        super(dialectProcessor, contextProcessor, parseResultProcessor, analysisResultProcessor, editorRegistry, languageCaches);

        this.resourceService = resourceService;
        this.languageIdentifier = languageIdentifier;

        this.workspace = ResourcesPlugin.getWorkspace();
        this.eclipseEditorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
        this.display = Display.getDefault();
    }


    @Override public void addedComponent(ILanguageComponent component) {
        logger.debug("Running component added tasks for {}", component);

        final Set<String> extensions = getExtensions(component);
        if(!extensions.isEmpty()) {
            logger.debug("Associating extension(s) {} to Spoofax editor", Strings.join(extensions, ", "));
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.set(eclipseEditorRegistry, SpoofaxEditor.id, extensions);
                }
            });
        }

        super.addedComponent(component);
    }

    @Override public void reloadedComponent(ILanguageComponent oldComponent, ILanguageComponent newComponent) {
        logger.debug("Running component reloaded tasks for {}", newComponent);

        final Set<String> oldExtensions = getExtensions(oldComponent);
        final Set<String> newExtensions = getExtensions(newComponent);
        if(!oldExtensions.isEmpty() || !newExtensions.isEmpty()) {
            final Set<String> removeExtensions = new HashSet<>(Sets.difference(oldExtensions, newExtensions));
            final Set<String> addExtensions = new HashSet<>(Sets.difference(newExtensions, oldExtensions));
            if(!removeExtensions.isEmpty()) {
                logger.debug("Unassociating extension(s) {} from Spoofax editor", Strings.join(removeExtensions, ", "));
            }
            if(!addExtensions.isEmpty()) {
                logger.debug("Associating extension(s) {} to Spoofax editor", Strings.join(addExtensions, ", "));
            }
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.remove(eclipseEditorRegistry, SpoofaxEditor.id, removeExtensions);
                    EditorMappingUtils.set(eclipseEditorRegistry, SpoofaxEditor.id, addExtensions);
                }
            });
        }

        super.reloadedComponent(oldComponent, newComponent);
    }

    @Override protected void removedComponent(ILanguageComponent component) {
        logger.debug("Running component removed tasks for {}", component);

        final Set<String> extensions = getExtensions(component);
        if(!extensions.isEmpty()) {
            logger.debug("Unassociating extension(s) {} from Spoofax editor", Strings.join(extensions, ", "));
            display.asyncExec(new Runnable() {
                @Override public void run() {
                    EditorMappingUtils.remove(eclipseEditorRegistry, SpoofaxEditor.id, extensions);
                }
            });
        }

        super.removedComponent(component);
    }

    @Override public void removedImpl(ILanguageImpl language) {
        if(languageIdentifier.available(language)) {
            try {
                final Collection<FileObject> resources =
                    ResourceUtils.workspaceResources(resourceService, new LanguageFileSelector(languageIdentifier,
                        language), workspace.getRoot());
                final Collection<IResource> eclipseResources =
                    ResourceUtils.toEclipseResources(resourceService, resources);
                logger.debug("Removing markers from {} workspace resources", resources.size());
                for(IResource resource : eclipseResources) {
                    try {
                        MarkerUtils.clearAll(resource);
                    } catch(CoreException e) {
                        final String message = String.format("Cannot remove markers for resource %s", resource);
                        logger.error(message, e);
                    }
                }
            } catch(FileSystemException e) {
                final String message = String.format("Cannot retrieve all workspace resources for %s", language);
                logger.error(message, e);
            }
        }
        
        super.removedImpl(language);
    }


    private Set<String> getExtensions(ILanguageComponent component) {
        final Set<String> extensions = new HashSet<>();
        for(ResourceExtensionFacet facet : component.facets(ResourceExtensionFacet.class)) {
            Iterables2.addAll(extensions, facet.extensions());
        }
        return extensions;
    }
}
