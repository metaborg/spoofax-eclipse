package org.metaborg.spoofax.eclipse.transform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageIdentifierService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.processing.analyze.ISpoofaxAnalysisResultRequester;
import org.metaborg.spoofax.core.processing.parse.ISpoofaxParseResultRequester;
import org.metaborg.spoofax.core.transform.ISpoofaxTransformService;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditorRegistry;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.RegionUtils;
import org.metaborg.spoofax.eclipse.util.handler.AbstractHandlerUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.spoofax.interpreter.terms.IStrategoTerm;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class TransformHandler extends AbstractHandler {
    private static final ILogger logger = LoggerUtils.logger(TransformHandler.class);

    private final IEclipseResourceService resourceService;
    private final ILanguageService languageService;
    private final ILanguageIdentifierService languageIdentifierService;
    private final ISourceTextService sourceTextService;
    private final IContextService contextService;
    private final IProjectService projectService;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxTransformService transformService;
    private final ISpoofaxParseResultRequester parseResultRequester;
    private final ISpoofaxAnalysisService analysisService;
    private final ISpoofaxAnalysisResultRequester analysisResultRequester;
    private final IEclipseEditorRegistry<?> editorRegistry;


    public TransformHandler() {
        final Injector injector = SpoofaxPlugin.injector();

        this.resourceService = injector.getInstance(IEclipseResourceService.class);
        this.languageService = injector.getInstance(ILanguageService.class);
        this.languageIdentifierService = injector.getInstance(ILanguageIdentifierService.class);
        this.sourceTextService = injector.getInstance(ISourceTextService.class);
        this.contextService = injector.getInstance(IContextService.class);
        this.projectService = injector.getInstance(IProjectService.class);
        this.unitService = injector.getInstance(ISpoofaxUnitService.class);
        this.transformService = injector.getInstance(ISpoofaxTransformService.class);
        this.parseResultRequester = injector.getInstance(ISpoofaxParseResultRequester.class);
        this.analysisService = injector.getInstance(ISpoofaxAnalysisService.class);
        this.analysisResultRequester = injector.getInstance(ISpoofaxAnalysisResultRequester.class);
        this.editorRegistry =
            injector.getInstance(Key.get(new TypeLiteral<IEclipseEditorRegistry<IStrategoTerm>>() {}));
    }


    @Override public Object execute(ExecutionEvent event) throws ExecutionException {
        final LanguageIdentifier languageIdentifier = MenuContribution.toLanguageId(event);
        final ILanguageImpl language = languageService.getImpl(languageIdentifier);
        if(language == null) {
            final String message =
                logger.format("Cannot transform resource of language {}; language does not exist", languageIdentifier);
            throw new ExecutionException(message);
        }

        final ITransformGoal goal = MenuContribution.toGoal(event);
        final boolean hasOpenEditor = MenuContribution.toHasOpenEditor(event);

        final Collection<TransformResource> resources;
        if(hasOpenEditor) {
            final IEclipseEditor<?> editor = editorRegistry.previousEditor();
            if(editor == null) {
                final String message =
                    logger.format("Cannot transform resource of {}; no active Spoofax editor", language);
                throw new ExecutionException(message);
            }
            if(editor.resource() == null || editor.document() == null) {
                final String message =
                    logger.format("Cannot transform resource of {}; editor has no resource or document", language);
                throw new ExecutionException(message);
            }
            final ISourceRegion selectedRegion = selectedRegion(editor);
            resources = Collections.singletonList(createTransformResource(editor.resource(), editor.document().get(), selectedRegion));
        } else {
            final Iterable<IResource> eclipseResources = AbstractHandlerUtils.toResources(event);
            if(eclipseResources == null) {
                final String message = logger
                    .format("Cannot transform resource of {}; selection is null or not a structed selection", language);
                throw new ExecutionException(message);
            }
            final Collection<TransformResource> transformResources = new ArrayList<>();
            for(IResource eclipseResource : eclipseResources) {
                final FileObject resource = resourceService.resolve(eclipseResource);
                if(!languageIdentifierService.identify(resource, language)) {
                    continue;
                }

                try {
                    final String text = sourceTextService.text(resource);
                    transformResources.add(createTransformResource(resource, text, null));
                } catch(IOException e) {
                    logger.error("Cannot transform {}; exception while retrieving text, skipping", e, resource);
                }
            }
            resources = transformResources;
        }

        final Job transformJob = new TransformJob(contextService, unitService, transformService, parseResultRequester,
            analysisService, analysisResultRequester, language, resources, goal);
        transformJob.schedule();

        return null;
    }

    private @Nullable ISourceRegion selectedRegion(IEclipseEditor<?> editor) {
        final ISelection selection = editor.selectionProvider().getSelection();
        if(selection == null || !(selection instanceof ITextSelection)) {
            return null;
        }
        final ITextSelection textSelection = (ITextSelection) selection;
        if(textSelection.getLength() == 0) {
            return null;
        }
        return RegionUtils.toCore(textSelection);
    }
    
    private TransformResource createTransformResource(FileObject resource, String text, @Nullable ISourceRegion selectedRegion) {
        final IProject project = projectService.get(resource);
        return new TransformResource(resource, project, text, selectedRegion);
    }
}
