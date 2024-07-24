package org.metaborg.spoofax.eclipse.processing;

import org.metaborg.core.build.BuildInput;
import org.metaborg.core.language.dialect.IDialectProcessor;
import org.metaborg.core.processing.ILanguageChangeProcessor;
import org.metaborg.core.processing.ITask;
import org.metaborg.spoofax.core.build.ISpoofaxBuildOutput;
import org.metaborg.spoofax.core.build.ISpoofaxBuilder;
import org.metaborg.spoofax.core.processing.ISpoofaxProcessor;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxAnalyzeUnitUpdate;
import org.metaborg.spoofax.core.unit.ISpoofaxParseUnit;
import org.metaborg.spoofax.core.unit.ISpoofaxTransformUnit;
import org.metaborg.spoofax.eclipse.job.GlobalSchedulingRules;
import org.metaborg.spoofax.eclipse.language.LanguageLoader;
import org.metaborg.spoofax.eclipse.project.IEclipseProjectService;
import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.task.ICancel;
import org.metaborg.util.task.IProgress;



/**
 * Typedef class for {@link Processor} with Spoofax interfaces.
 */
public class SpoofaxProcessor
    extends Processor<ISpoofaxParseUnit, ISpoofaxAnalyzeUnit, ISpoofaxAnalyzeUnitUpdate, ISpoofaxTransformUnit<?>>
    implements ISpoofaxProcessor {
    @jakarta.inject.Inject public SpoofaxProcessor(IEclipseResourceService resourceService, IEclipseProjectService projectService,
        IDialectProcessor dialectProcessor, ISpoofaxBuilder builder, ILanguageChangeProcessor processor,
        GlobalSchedulingRules globalRules, LanguageLoader languageLoader) {
        super(resourceService, projectService, dialectProcessor, builder, processor, globalRules, languageLoader);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked") public ITask<ISpoofaxBuildOutput> build(BuildInput input,
        @Nullable IProgress progress, @Nullable ICancel cancel) {
        return (ITask<ISpoofaxBuildOutput>) super.build(input, progress, cancel);
    }
}
