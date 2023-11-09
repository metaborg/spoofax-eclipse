package org.metaborg.spoofax.eclipse.editor;

import org.metaborg.spoofax.eclipse.resource.IEclipseResourceService;
import org.spoofax.interpreter.terms.IStrategoTerm;



/**
 * Typedef class for {@link EditorRegistry} with Spoofax interfaces.
 */
public class SpoofaxEditorRegistry extends EditorRegistry<IStrategoTerm> {
    @jakarta.inject.Inject @javax.inject.Inject public SpoofaxEditorRegistry(IEclipseResourceService resourceService) {
        super(resourceService);
    }
}
