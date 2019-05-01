package org.metaborg.spoofax.eclipse.meta.nature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.metaborg.spoofax.eclipse.build.SpoofaxProjectBuilder;
import org.metaborg.spoofax.eclipse.meta.SpoofaxMetaPlugin;
import org.metaborg.spoofax.eclipse.meta.build.CompileBuilder;
import org.metaborg.spoofax.eclipse.meta.build.GenerateSourcesBuilder;
import org.metaborg.spoofax.eclipse.meta.build.PackageBuilder;
import org.metaborg.spoofax.eclipse.nature.SpoofaxNature;
import org.metaborg.spoofax.eclipse.util.BuilderUtils;
import org.metaborg.spoofax.eclipse.util.CommonBuilders;
import org.metaborg.spoofax.eclipse.util.CommonNatures;
import org.metaborg.spoofax.eclipse.util.NatureUtils;
import org.metaborg.spoofax.eclipse.util.Nullable;

public class SpoofaxMetaNature implements IProjectNature {
	public static final String id = SpoofaxMetaPlugin.id + ".nature";

	private IProject project;

	@Override
	public void configure() throws CoreException {
		addDependencyBuilders(project, null);
		BuilderUtils.append(GenerateSourcesBuilder.id, project, null, IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.CLEAN_BUILD);
		BuilderUtils.append(CompileBuilder.id, project, null, IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.CLEAN_BUILD);
		BuilderUtils.append(PackageBuilder.id, project, null, IncrementalProjectBuilder.FULL_BUILD,
				IncrementalProjectBuilder.CLEAN_BUILD);
		sortBuilders(project, null);
	}

	@Override
	public void deconfigure() throws CoreException {
		BuilderUtils.removeFrom(GenerateSourcesBuilder.id, project, null);
		BuilderUtils.removeFrom(CompileBuilder.id, project, null);
		BuilderUtils.removeFrom(PackageBuilder.id, project, null);
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	public static void add(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
		// Cannot add dependency natures in configure method, as Eclipse checks
		// dependencies beforehand. Add them here.
		addDependencyNatures(project, monitor);
		NatureUtils.addTo(id, project, monitor);
	}

	private static void addDependencyNatures(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
		if(!NatureUtils.exists(CommonNatures.gradleNatureId, project)) {
			// Add Maven nature only if there is no Gradle nature.
			CommonNatures.addMavenNature(project, monitor);
		}
		CommonNatures.addJavaNature(project, monitor);
		SpoofaxNature.add(project, monitor);
	}

	private static void addDependencyBuilders(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
		CommonBuilders.appendJavaBuilder(project, monitor);
		if(!BuilderUtils.contains(CommonBuilders.gradleBuilderId, project)) {
			// Add Maven builder only if there is no Gradle builder.
			CommonBuilders.appendMavenBuilder(project, monitor);
		}
	}

	private static void sortBuilders(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
		final String[] buildOrder = new String[] { 
			CommonBuilders.mavenBuilderId, 
			CommonBuilders.gradleBuilderId,
			GenerateSourcesBuilder.id,
			SpoofaxProjectBuilder.id, 
			CompileBuilder.id, 
			CommonBuilders.javaBuilderId,
			PackageBuilder.id 
		};
		BuilderUtils.sort(project, monitor, buildOrder);
	}

	public static void remove(IProject project, @Nullable IProgressMonitor monitor) throws CoreException {
		NatureUtils.removeFrom(id, project, monitor);
	}
}
