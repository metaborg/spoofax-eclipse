package org.strategoxt.imp.runtime.services;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.language.LanguageValidator;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.Descriptor;

/**
 * @author Lennart Kats <lennart add lclnet.nl>
 *
 */
public class MetaFileLanguageValidator extends LanguageValidator {
	
	private static boolean isLanguageRegistryPatchEnabled;
	
	private Descriptor descriptor;
	
	public MetaFileLanguageValidator(Descriptor descriptor) {
	    this.descriptor = descriptor;
	}

	@Override
	public boolean validate(IFile file) {
		isLanguageRegistryPatchEnabled = true;
		String metaFile = file.getFullPath().removeFileExtension().addFileExtension("meta").toOSString();
		String language = MetaFileReader.readSyntax(metaFile);
		
		return language == null || validateByLanguage(file, language);
	}
	
	public boolean validateByLanguage(IFile file, String languageName) {
		try {
			Language language = descriptor.getLanguage();
				
			if (languageName.equals(language.getName()))
				return true;
			
			if (isExactMatchAvailable(languageName))
				return false; // better alternative exists
			
			if (isExtensionOf(language, languageName))
				return true;
			
			if (!isExtensionOfAvailable(languageName) && validateByExtension(file))
				return true;

			return true;
		} catch (BadDescriptorException e) {
			Environment.logException(e);
			return false;
		}
	}
	
	private boolean validateByExtension(IFile file) {
		try {
			return descriptor.getLanguage().hasExtension(file.getFileExtension());
		} catch (BadDescriptorException e) {
			Environment.logException(e);
			return false;
		}
	}
	
	private boolean isExactMatchAvailable(String languageName) throws BadDescriptorException {
		Language myLanguage = descriptor.getLanguage(); 
		for (Language language : LanguageRegistry.getLanguages()) {
			if (language != myLanguage && languageName.equals(language.getName()))
				return true;
		}
		return false;
	}
	
	private boolean isExtensionOfAvailable(String languageName) throws BadDescriptorException {
		Language myLanguage = descriptor.getLanguage(); 
		for (Language language : LanguageRegistry.getLanguages()) {
			if (language != myLanguage && isExtensionOf(language, languageName))
				return true;
		}
		return false;
	}
	
	/**
	 * Tests if <code>language</code> is an extension of <code>languageName</code>.
	 */
	private static boolean isExtensionOf(Language language, String languageName) {
		Descriptor descriptor = Environment.getDescriptor(language);
		if (descriptor == null)
			return false;
		for (String extended : descriptor.getExtendedLanguages()) {
			if (languageName.equals(extended))
				return true;
		}
		return false;
	}

	@Override
	public boolean validate(String buffer) {
		if (!isLanguageRegistryPatchEnabled) {
			System.err.println("Warning: LanguageRegistry patch not enabled; cannot use .meta files");
			isLanguageRegistryPatchEnabled = true;
		}
			
		return true;
	}
}