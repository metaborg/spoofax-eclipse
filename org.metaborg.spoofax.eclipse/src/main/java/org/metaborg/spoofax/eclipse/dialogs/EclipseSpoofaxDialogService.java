package org.metaborg.spoofax.eclipse.dialogs;

import org.metaborg.spoofax.core.dialogs.ISpoofaxDialogService;

import java.util.List;
import java.util.Objects;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


/**
 * The Eclipse implementation of the dialog service.
 */
public final class EclipseSpoofaxDialogService implements ISpoofaxDialogService {

    @Override
    public DialogOption showDialog(String message, String caption, DialogKind kind, List<DialogOption> options, int defaultOption) {
    	int dialogKind = toMessageDialog(kind);
    	String[] buttonLabels = getButtonLabels(options);
    	int defaultOptionIndex = defaultOption >= 0 && defaultOption < buttonLabels.length ? defaultOption : 0;
        Shell shell = new Shell(Display.getDefault());
        MessageDialog dialog = new MessageDialog(shell, caption, null, message, dialogKind, buttonLabels, defaultOptionIndex);
        int result = dialog.open();
        return result >= 0 && result < options.size() ? options.get(result) : null;
    }

    /**
     * Converts a {@link DialogKind} value to a {@link MessageDialog} value.
     *
     * @param kind the {@link DialogKind} value to convert; or {@code null}
     * @return the corresponding {@link MessageDialog} value
     */
    private int toMessageDialog(DialogKind kind) {
        if (kind == null) return MessageDialog.NONE;
    	switch (kind) {
    	    // @formatter:off
            case None:     return MessageDialog.NONE;
            case Info:     return MessageDialog.INFORMATION;
            case Warning:  return MessageDialog.WARNING;
            case Error:    return MessageDialog.ERROR;
            case Question: return MessageDialog.QUESTION;
            // @formatter:on
            default: throw new IllegalStateException("Unsupported dialog kind.");
        }
    }

    /**
     * Gets the button label of the specified {@link DialogOption} option.
     *
     * @param option the option for which to get the button label
     * @return the button label; or {@code null} when it is not a valid option
     */
    private String getButtonLabel(DialogOption option) {
        if (option == null || option.getName().isEmpty()) return null;
        // NOTE: If the option is one of the options we recognize,
        // we replace the name by the (preferably localized) name of the option on this platform.
        // @formatter:off
        if (DialogOption.OK.equals(option))     return "OK";
        if (DialogOption.CANCEL.equals(option)) return "Cancel";
        if (DialogOption.YES.equals(option))    return "Yes";
        if (DialogOption.NO.equals(option))     return "No";
        if (DialogOption.RETRY.equals(option))  return "Retry";
        if (DialogOption.ABORT.equals(option))  return "Abort";
        if (DialogOption.IGNORE.equals(option)) return "Ignore";
        // @formatter:on

        return option.getName();
    }

    /**
     * Gets the button labels corresponding to the given list of {@link DialogOption} options.
     *
     * Invalid options are removed.
     *
     * @param options the options for which to get the button labels; or {@code null}
     * @return an array of button labels
     */
    private String[] getButtonLabels(List<DialogOption> options) {
        if (options == null) return new String[] { getButtonLabel(DialogOption.OK) };
        return options.stream().map(this::getButtonLabel).filter(Objects::nonNull).toArray(String[]::new);
    }

}
