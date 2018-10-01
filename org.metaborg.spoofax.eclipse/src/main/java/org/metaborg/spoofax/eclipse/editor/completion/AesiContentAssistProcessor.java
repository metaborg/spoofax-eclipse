package org.metaborg.spoofax.eclipse.editor.completion;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.vfs2.FileObject;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import org.metaborg.aesi.ICancellationToken;
import org.metaborg.aesi.SourceOffset;
import org.metaborg.aesi.SubscriberCancellationToken;
import org.metaborg.aesi.codecompletion.ICodeCompletionProposal;
import org.metaborg.aesi.codecompletion.ICodeCompletionResult;
import org.metaborg.aesi.codecompletion.ICodeCompletionService;
import org.metaborg.core.editor.IEditor;
import org.metaborg.spoofax.aesi.codecompletion.ISpoofaxCodeCompletionProposal;
import org.metaborg.spoofax.aesi.codecompletion.ISpoofaxCodeCompletionResult;
import org.metaborg.spoofax.aesi.codecompletion.ISpoofaxCodeCompletionService;
import org.metaborg.spoofax.aesi.resources.EclipseSpoofaxResourceService;
import org.metaborg.spoofax.aesi.resources.ISpoofaxResource;
import org.metaborg.spoofax.eclipse.editor.IEclipseEditor;
import org.metaborg.spoofax.eclipse.util.Nullable;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.net.URI;

/**
 * An AESI compatible code completion (content assist) processor for Eclipse.
 */
public class AesiContentAssistProcessor implements IContentAssistProcessor {

    private static final ILogger logger = LoggerUtils.logger(SpoofaxContentAssistProcessor.class);
    private Subscription completionSubscription;
    private volatile ICompletionProposal[] cachedProposals;
    private final ISpoofaxCodeCompletionService codeCompletionService;
    private final EclipseSpoofaxResourceService resourceService;
    private final IInformationControlCreator informationControlCreator;
    private final IEclipseEditor<?> editor;

    /**
     * Initializes a new instance of the {@link AesiContentAssistProcessor} class.
     *
     * @param codeCompletionService The code completion service.
     * @param resourceService The AESI resource service.
     * @param informationControlCreator The information control creator.
     * @param editor The editor.
     */
    @Inject
    public AesiContentAssistProcessor(
            ISpoofaxCodeCompletionService codeCompletionService,
            EclipseSpoofaxResourceService resourceService,
            @Assisted IInformationControlCreator informationControlCreator,
            @Assisted IEclipseEditor<?> editor) {
        assert codeCompletionService != null;
        assert resourceService != null;
        assert informationControlCreator != null;
        assert editor != null;

        this.codeCompletionService = codeCompletionService;
        this.resourceService = resourceService;
        this.informationControlCreator = informationControlCreator;
        this.editor = editor;
    }

    /**
     * Factory class.
     */
    public interface Factory {
        /**
         * Creates a new instance of the {@link AesiContentAssistProcessor} class.
         *
         * @param informationControlCreator The information control creator.
         * @param editor The editor.
         * @return The created instance.
         */
        AesiContentAssistProcessor create(
                IInformationControlCreator informationControlCreator,
                IEclipseEditor<?> editor
        );
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        // When we have a result from the code completion subscription, return those.
        if (this.cachedProposals != null) {
            final ICompletionProposal[] proposals = this.cachedProposals;
            this.cachedProposals = null;
            return proposals;
        }

        // Unsubscribe from any previous subscription.
        if (this.completionSubscription != null) {
            this.completionSubscription.unsubscribe();
            this.completionSubscription = null;
        }

        assert this.completionSubscription == null;

        // Create an asynchronous computation for code completion.
        this.completionSubscription = Observable.create((Observable.OnSubscribe<Void>) subscriber -> {

            ICancellationToken cancellationToken = new SubscriberCancellationToken(subscriber);

            if (subscriber.isUnsubscribed()) return;

            ISpoofaxResource resource = this.resourceService.getResourceOf(this.editor);

            this.cachedProposals = computeProposals(resource, offset, cancellationToken);

            if (this.cachedProposals == null) return;

            // Re-trigger code completion once the list of proposals has been computed.
            // This time, code completion will return the results from the cache.
            Display.getDefault().syncExec(() -> {
                if(subscriber.isUnsubscribed()) return;
                final ITextOperationTarget target = (ITextOperationTarget) viewer;
                target.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS);
            });
        }).observeOn(Schedulers.computation()).subscribeOn(Schedulers.computation()).subscribe();

        return null;
    }

    /**
     * Determines the caret position.
     *
     * @param viewer The text viewer.
     * @return The zero-based caret offset; or null when it could not be determined.
     */
    @Nullable
    private SourceOffset getCaretPosition(ITextViewer viewer) {
        ISelectionProvider selectionProvider = viewer.getSelectionProvider();
        ISelection selection = selectionProvider.getSelection();
        if (selection instanceof ITextSelection) {
            ITextSelection textSelection = (ITextSelection)selection;
            return new SourceOffset(textSelection.getOffset());
        }
        return null;
    }

    /**
     * Computes the code completion proposals.
     *
     * @param resource The resource.
     * @param caretOffset The zero-based offset of the caret from the start of the source text.
     * @param cancellationToken The cancellation token; or null.
     * @return The code completion proposals.
     */
    private ICompletionProposal[] computeProposals(ISpoofaxResource resource, int caretOffset, @Nullable ICancellationToken cancellationToken) {
        assert resource != null;

        URI resourceUri = resource.getUri();
        SourceOffset caret = new SourceOffset(caretOffset);
        ISpoofaxCodeCompletionResult result = this.codeCompletionService.getCompletions(resourceUri, caret, cancellationToken);
        return result.getProposals().stream()
                .map(p -> toEclipseCompletionProposal(p, caretOffset))
                .toArray(ICompletionProposal[]::new);
    }

    /**
     * Converts an AESI code completion proposal into an Eclipse code completion proposal.
     *
     * @param proposal The AESI code completion proposal to convert.
     * @return The Eclipse code completion proposal.
     */
    private ICompletionProposal toEclipseCompletionProposal(ISpoofaxCodeCompletionProposal proposal, int caretOffset) {
        assert proposal != null;

        // TODO: Take prefix into account.
        return new AesiEclipseCompletionProposal(this.informationControlCreator, proposal, caretOffset, caretOffset);
    }

    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) { return null; }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() { return null; }

    @Override
    public char[] getContextInformationAutoActivationCharacters() { return null; }

    @Override
    public String getErrorMessage() { return null; }

    @Override
    public IContextInformationValidator getContextInformationValidator() { return null; }
}
