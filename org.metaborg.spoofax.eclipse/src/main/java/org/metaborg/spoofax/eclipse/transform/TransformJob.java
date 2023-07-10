package org.metaborg.spoofax.eclipse.transform;

import java.util.Collection;
import java.util.concurrent.CancellationException;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.metaborg.core.action.ITransformGoal;
import org.metaborg.core.context.ContextException;
import org.metaborg.core.context.IContext;
import org.metaborg.core.context.IContextService;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.source.ISourceRegion;
import org.metaborg.core.transform.ITransformConfig;
import org.metaborg.core.transform.TransformConfig;
import org.metaborg.core.transform.TransformException;
import org.metaborg.spoofax.core.analysis.ISpoofaxAnalysisService;
import org.metaborg.spoofax.core.processing.analyze.ISpoofaxAnalysisResultRequester;
import org.metaborg.spoofax.core.processing.parse.ISpoofaxParseResultRequester;
import org.metaborg.spoofax.core.transform.ISpoofaxTransformService;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxInputUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxUnitService;
import org.metaborg.spoofax.eclipse.job.ThreadKillerJob;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.spoofax.eclipse.util.StatusUtils;
import org.metaborg.util.Strings;
import org.metaborg.util.concurrent.IClosableLock;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

public class TransformJob extends Job {
    private static final ILogger logger = LoggerUtils.logger(TransformJob.class);
    private static final long interruptTimeMillis = 3000;
    private static final long killTimeMillis = 5000;

    private final IContextService contextService;
    private final ISpoofaxUnitService unitService;
    private final ISpoofaxTransformService transformService;
    private final ISpoofaxParseResultRequester parseResultRequester;
    private final ISpoofaxAnalysisService analysisService;
    private final ISpoofaxAnalysisResultRequester analysisResultRequester;

    private final ILanguageImpl langImpl;
    private final Collection<TransformResource> resources;
    private final ITransformGoal goal;

    private ThreadKillerJob threadKiller;


    public TransformJob(IContextService contextService, ISpoofaxUnitService unitService,
        ISpoofaxTransformService transformService, ISpoofaxParseResultRequester parseResultProcessor,
        ISpoofaxAnalysisService analysisService, ISpoofaxAnalysisResultRequester analysisResultProcessor,
        ILanguageImpl langImpl, Collection<TransformResource> resources, ITransformGoal goal) {
        super("Transforming resources");

        this.contextService = contextService;
        this.unitService = unitService;
        this.transformService = transformService;
        this.parseResultRequester = parseResultProcessor;
        this.analysisService = analysisService;
        this.analysisResultRequester = analysisResultProcessor;

        this.langImpl = langImpl;
        this.resources = resources;

        this.goal = goal;
    }


    @Override protected IStatus run(IProgressMonitor monitor) {
        try {
            return transformAll(monitor);
        } catch(InterruptedException | CancellationException | ThreadDeath e) {
            return StatusUtils.cancel();
        } catch(OperationCanceledException e) {
            return StatusUtils.cancel();
        } finally {
            if(threadKiller != null) {
                threadKiller.cancel();
            }
            monitor.done();
        }
    }

    @Override protected void canceling() {
        final Thread thread = getThread();
        if(thread == null) {
            return;
        }

        logger.debug("Cancelling transform job for {}, interrupting in {}ms, killing in {}ms",
            Strings.tsJoin(resources, ", "), interruptTimeMillis, interruptTimeMillis + killTimeMillis);
        threadKiller = new ThreadKillerJob(thread, killTimeMillis);
        threadKiller.schedule(interruptTimeMillis);
    }

    private IStatus transformAll(IProgressMonitor progressMonitor) throws InterruptedException, ThreadDeath {
        final SubMonitor monitor = SubMonitor.convert(progressMonitor);

        if(monitor.isCanceled())
            return StatusUtils.cancel();

        final SubMonitor loopMonitor = monitor.split(1).setWorkRemaining(resources.size());
        for(TransformResource transformResource : resources) {
            if(loopMonitor.isCanceled())
                return StatusUtils.cancel();

            final FileObject source = transformResource.source;
            loopMonitor.setTaskName("Transforming " + source);
            try {
                final ISpoofaxInputUnit input = unitService.inputUnit(source, transformResource.text, langImpl, null);
                transform(input, transformResource.project, transformResource.selection, loopMonitor.split(1));
            } catch(ContextException | TransformException e) {
                final String message = logger.format("Transformation failed for {}", source);
                logger.error(message, e);
                return StatusUtils.error(message, e);
            }
        }

        return StatusUtils.success();
    }

    private void transform(ISpoofaxInputUnit input, IProject project, @Nullable ISourceRegion selection,
        SubMonitor monitor) throws ContextException, TransformException {
        final FileObject source = input.source();
        final IContext context = contextService.get(source, project, langImpl);
        final ITransformConfig config = new TransformConfig(selection);
        if(transformService.requiresAnalysis(langImpl, goal) && analysisService.available(langImpl)) {
            monitor.setWorkRemaining(3);
            monitor.setTaskName("Waiting for analysis result");
            final ISpoofaxAnalyzeUnit result = analysisResultRequester.request(input, context).blockingSingle();
            monitor.worked(1);
            monitor.setTaskName("Waiting for context read lock");
            try(IClosableLock lock = context.read()) {
                monitor.worked(1);
                monitor.setTaskName("Transforming " + source);
                transformService.transform(result, context, goal, config);
                monitor.worked(1);
            }
        } else {
            monitor.setWorkRemaining(2);
            monitor.setTaskName("Waiting for parse result");
            final ISpoofaxParseUnit result = parseResultRequester.request(input).blockingSingle();
            monitor.worked(1);
            monitor.setTaskName("Transforming " + source);
            transformService.transform(result, context, goal, config);
            monitor.worked(1);
        }
    }
}
