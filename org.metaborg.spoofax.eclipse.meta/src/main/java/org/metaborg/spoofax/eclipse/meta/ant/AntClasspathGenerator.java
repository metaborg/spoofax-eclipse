package org.metaborg.spoofax.eclipse.meta.ant;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.eclipse.core.runtime.FileLocator;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.util.BundleUtils;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.osgi.framework.Bundle;

public class AntClasspathGenerator {
    private static final ILogger logger = LoggerUtils.logger(AntClasspathGenerator.class);

    /**
     * @return List of classpath entries generated from installed Eclipse plugins.
     */
    public static URL[] classpaths() {
        final Collection<URL> classpath = new LinkedList<>();
        final Map<String, Bundle> bundles = BundleUtils.bundlesBySymbolicName(SpoofaxMetaPlugin.context());

        final Bundle antBundle = bundles.get("org.apache.ant");
        if(antBundle == null) {
            logger.error("Could not find Ant bundle 'org.apache.ant', language build will probably fail");
        } else {
            try {
                final File file = FileLocator.getBundleFile(antBundle);
                final String path = file.getAbsolutePath();
                final File lib = Paths.get(path, "lib").toFile();
                final File[] jarFiles = lib.listFiles(new FilenameFilter() {
                    @Override public boolean accept(File dir, String name) {
                        return name.endsWith(".jar");
                    }
                });
                for(File jarFile : jarFiles) {
                    classpath.add(jarFile.toURI().toURL());
                }
            } catch(IOException e) {
                logger.error("Error while adding 'org.apache.ant' to classpath for Ant build, "
                    + "language build will probably fail", e);
            }
        }

        for(final Bundle bundle : bundles.values()) {
            final String name = bundle.getSymbolicName();
            if(!name.startsWith("org.metaborg") && !name.startsWith("org.spoofax")
                && !name.startsWith("org.strategoxt")) {
                continue;
            }
            logger.debug("Including bundle {} at {} in Ant classpath", bundle.getSymbolicName(), bundle.getLocation());

            try {
                final File file = FileLocator.getBundleFile(bundle);
                final String path = file.getAbsolutePath();
                if(path.endsWith(".jar")) {
                    // An installed JAR plugin.
                    classpath.add(file.toURI().toURL());
                    continue;
                }

                final File targetClasses = Paths.get(path, "target", "classes").toFile();
                final File bin = Paths.get(path, "bin").toFile();
                if(targetClasses.exists()) {
                    // A plugin under development with all its classes in the target/classes directory.
                    classpath.add(targetClasses.toURI().toURL());
                } else if(bin.exists()) {
                    // A plugin under development with all its classes in the bin directory.
                    classpath.add(bin.toURI().toURL());
                } else {
                    // An installed unpacked plugin. Class files are extracted in this directory.
                    classpath.add(file.toURI().toURL());
                }

                // Also include any nested jar files.
                final Iterable<File> jarFiles =
                    FileUtils.listFiles(file, new RegexFileFilter(".+\\.jar"), DirectoryFileFilter.DIRECTORY);
                for(File jarFile : jarFiles) {
                    classpath.add(jarFile.toURI().toURL());
                }
            } catch(IOException e) {
                logger.error("Error while creating classpath for Ant build", e);
            }
        }

        return classpath.toArray(new URL[classpath.size()]);
    }
}
