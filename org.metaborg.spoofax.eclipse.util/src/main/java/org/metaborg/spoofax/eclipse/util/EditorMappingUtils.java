package org.metaborg.spoofax.eclipse.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorMapping;
import org.eclipse.ui.internal.registry.EditorDescriptor;
import org.eclipse.ui.internal.registry.EditorRegistry;
import org.eclipse.ui.internal.registry.FileEditorMapping;
import org.metaborg.util.iterators.Iterables2;

/**
 * Utility functions for changing editor mappings/associations.
 * 
 * Note: Eclipse API expects {@link EditorRegistry}, {@link EditorDescriptor}, and {@link FileEditorMapping} instead of
 * their interface counterparts when changing mappings. Need to cast and use non-API classes to programmatically change
 * mappings.
 */
@SuppressWarnings("restriction")
public class EditorMappingUtils {
    public static void set(IEditorRegistry iEditorRegistry, String editorId, Iterable<String> iterExtensions) {
        final EditorRegistry editorRegistry = (EditorRegistry) iEditorRegistry;
        final EditorDescriptor editorDescription = (EditorDescriptor) editorRegistry.findEditor(editorId);
        final String[] extensions = Iterables2.toArray(iterExtensions, String.class);
        if(extensions.length == 0) {
            return;
        }
        final FileEditorMapping[] additionalMappings = new FileEditorMapping[extensions.length];
        for(int i = 0; i < extensions.length; ++i) {
            additionalMappings[i] = mapping(extensions[i], editorDescription);
        }
        final IFileEditorMapping[] iMappings = editorRegistry.getFileEditorMappings();
        final FileEditorMapping[] mappings = Arrays.copyOf(iMappings, iMappings.length, FileEditorMapping[].class);
        final FileEditorMapping[] newMappings = ArrayUtils.addAll(mappings, additionalMappings);
        editorRegistry.setFileEditorMappings(newMappings);
    }

    public static void remove(IEditorRegistry iEditorRegistry, String editorId, Iterable<String> iterExtensions) {
        final EditorRegistry editorRegistry = (EditorRegistry) iEditorRegistry;
        final EditorDescriptor editorDescription = (EditorDescriptor) editorRegistry.findEditor(editorId);
        final String[] extensions = Iterables2.toArray(iterExtensions, String.class);
        if(extensions.length == 0) {
            return;
        }
        final IFileEditorMapping[] mappings = editorRegistry.getFileEditorMappings();
        for(int i = 0; i < mappings.length; ++i) {
            final FileEditorMapping mapping = (FileEditorMapping) mappings[i];
            if(ArrayUtils.contains(extensions, mapping.getExtension())) {
                mapping.removeEditor(editorDescription);
            }
        }
        final FileEditorMapping[] newMappings = Arrays.copyOf(mappings, mappings.length, FileEditorMapping[].class);
        editorRegistry.setFileEditorMappings(newMappings);
    }

    public static FileEditorMapping mapping(String extension, EditorDescriptor editorDescription) {
        final FileEditorMapping mapping = new FileEditorMapping(extension);
        mapping.addEditor(editorDescription);
        setDefaultEditor(mapping, editorDescription);
        return mapping;
    }

    /**
     * Sets the default editor for given mapping. Compatible with Eclipse Mars and previous versions via reflection.
     * 
     * @param mapping
     *            Mapping to set default editor for.
     * @param editorDescription
     *            Default editor to set.
     */
    public static void setDefaultEditor(FileEditorMapping mapping, EditorDescriptor editorDescription) {
        final Method method = getSetDefaultEditorMethod();
        try {
            method.invoke(mapping, editorDescription);
        } catch(IllegalAccessException e) {
            throw new RuntimeException("Could not set default editor", e);
        } catch(IllegalArgumentException e) {
            throw new RuntimeException("Could not set default editor", e);
        } catch(InvocationTargetException e) {
            throw new RuntimeException("Could not set default editor", e);
        }
    }

    /**
     * Gets the correct {@link FileEditorMapping#setDefaultEditor} method via reflection. Parameter changed from
     * EditorDescriptor to IEditorDescriptor in Eclipse Mars.
     */
    private static Method getSetDefaultEditorMethod() {
        final String methodName = "setDefaultEditor";
        try {
            return FileEditorMapping.class.getDeclaredMethod(methodName, EditorDescriptor.class);
        } catch(NoSuchMethodException e1) {
            try {
                return FileEditorMapping.class.getDeclaredMethod(methodName, IEditorDescriptor.class);
            } catch(NoSuchMethodException e2) {
                throw new RuntimeException("Cannot find setDefaultEditor method via reflection", e2);
            }
        }
    }
}
