package org.strategoxt.imp.metatooling.loading;

import static org.eclipse.core.resources.IMarker.*;
import static org.eclipse.core.resources.IResourceDelta.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.strategoxt.imp.metatooling.building.DynamicDescriptorBuilder;
import org.strategoxt.imp.runtime.Environment;
import org.strategoxt.imp.runtime.dynamicloading.BadDescriptorException;
import org.strategoxt.imp.runtime.dynamicloading.DescriptorFactory;
import org.strategoxt.imp.runtime.parser.ast.AstMessageHandler;

/**
 * This class updates any editors in the environment,
 * triggered by resource change events.
 * 
 * @author Lennart Kats <lennart add lclnet.nl>
 */
public class DynamicDescriptorUpdater implements IResourceChangeListener {
	
	private static final DynamicDescriptorUpdater instance = new DynamicDescriptorUpdater();
	
	private final Set<IResource> asyncIgnoreOnce =
		Collections.synchronizedSet(new HashSet<IResource>());
	
	private final AstMessageHandler asyncMessageHandler =
		new AstMessageHandler(AstMessageHandler.ANALYSIS_MARKER_TYPE);
	
	private DynamicDescriptorBuilder asyncBuilder;
	
	private DynamicDescriptorBuilder getBuilder() {
		Environment.assertLock();
		if (asyncBuilder == null)
			asyncBuilder = new DynamicDescriptorBuilder(this);
		return asyncBuilder;
	}
	
	private DynamicDescriptorUpdater() {
		// use getInstance() instead
	}
	
	public static DynamicDescriptorUpdater getInstance() {
		return instance;
	}
	
	/**
	 * Loads the editor for the specified descriptor,
	 * and ignore it the next resource event arrives.
	 */
	public void forceUpdate(IResource resource) {
		synchronized (Environment.getSyncRoot()) {
			assert resource.toString().endsWith(".packed.esv");
			asyncIgnoreOnce.add(resource);
			loadPackedDescriptor(resource);
		}
	}

	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
			Job job = new WorkspaceJob("Updating editor descriptor runtime") {
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					// TODO: Finer-grained locking?
					synchronized (Environment.getSyncRoot()) {
						monitor.beginTask("", IProgressMonitor.UNKNOWN);
						postResourceChanged(event.getDelta(), monitor);
						return Status.OK_STATUS;
					}
				}
			};
			job.setRule(event.getResource());
			job.schedule();
		}
	}
	
	public void postResourceChanged(IResourceDelta delta, IProgressMonitor monitor) {
		IResourceDelta[] children = delta.getAffectedChildren();
		
		if (children.length == 0) {		
			IResource resource = delta.getResource();
			if (isSignificantChange(delta))
				updateResource(resource, monitor, false);
		} else {
			// Recurse
			for (IResourceDelta child : children)
				postResourceChanged(child, monitor);
		}
	}

	private static boolean isSignificantChange(IResourceDelta delta) {
		int flags = delta.getFlags();
		return (flags & CONTENT) == CONTENT
			|| (flags & MOVED_TO) == MOVED_TO
			|| (flags & MOVED_FROM) == MOVED_FROM
			|| (flags & REPLACED) == REPLACED
			|| (flags == 0);
	}
	
	public void updateResource(IResource resource, IProgressMonitor monitor, boolean startup) {
		Environment.assertLock();
		
		if (resource.getName().endsWith(".packed.esv")) {
			IResource source = getSourceDescriptor(resource);
			
			if (asyncIgnoreOnce.contains(resource)) {
				asyncIgnoreOnce.remove(resource);
				return;
			}
			
			if (!source.equals(resource) && source.exists() && !startup) {
				// Try to build using the .main.esv instead;
				// the build.xml file may touch the .packed.esv file
				// to signal a rebuild is necessary
				// TODO: Prevent duplicate builds triggered this way...?
				getBuilder().updateResource(source, monitor);
			} else if (resource.exists()) {
				monitor.subTask("Loading " + resource.getName());
				loadPackedDescriptor(resource);
			}
		}
	}

	public void loadPackedDescriptor(IResource descriptor) {
		Environment.assertLock();
		
		// TODO2: Properly trace back descriptor errors to their original source
		IResource source = getSourceDescriptor(descriptor);
		try {
			asyncMessageHandler.clearMarkers(source);
			asyncMessageHandler.clearMarkers(descriptor);
			
			IFile file = descriptor.getProject().getFile(descriptor.getProjectRelativePath());
			DescriptorFactory.load(file);
			
		} catch (BadDescriptorException e) {
			Environment.logException("Error in descriptor " + descriptor, e);
			reportError(source, "Error in descriptor: " + e.getMessage());
			reportError(descriptor, e.getOffendingTerm(), "Error in descriptor: " + e.getMessage());
		} catch (IOException e) {
			Environment.logException("Error reading descriptor " + descriptor, e);
			reportError(source, "Internal error reading descriptor" + e.getMessage());
		} catch (CoreException e) {
			Environment.logException("Unable to load descriptor " + descriptor, e);
			reportError(source, "Internal error loading descriptor: " + e.getMessage());
		} catch (RuntimeException e) {
			Environment.logException("Unable to load descriptor " + descriptor, e);
			reportError(source, "Internal error loading descriptor: " + e.getMessage());
		} catch (Error e) { // workspace thread swallows this >:(
			Environment.logException("Unable to load descriptor " + descriptor, e);
			reportError(source, "Internal error loading descriptor: " + e.getMessage());
			throw e;
		}
	}
	
	public static IResource getSourceDescriptor(IResource packedDescriptor) {
		String name = packedDescriptor.getName();
		name = name.substring(0, name.length() - ".packed.esv".length());
		IResource result = packedDescriptor.getParent().getParent().findMember("editor/" + name + ".main.esv");
		return result != null ? result : packedDescriptor;
	}
	
	public static IResource getTargetDescriptor(IResource packedDescriptor) {
		String name = packedDescriptor.getName();
		name = name.substring(0, name.length() - ".main.esv".length());
		IResource result = packedDescriptor.getParent().getParent().findMember("include/" + name + ".packed.esv");
		return result != null ? result : packedDescriptor;
	}
	
	private void reportError(final IResource descriptor, final String message) {
		Environment.assertLock();
		
		if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
			Job job = new WorkspaceJob("Add error marker") {
				{ setSystem(true); } // don't show to user
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
					return Status.OK_STATUS;
				}
			};
			job.setRule(descriptor);
			job.schedule();
		} else {
			asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
		}
	}
	
	private void reportError(final IResource descriptor, final IStrategoTerm offendingTerm, final String message) {
		Environment.assertLock();

		if (ResourcesPlugin.getWorkspace().isTreeLocked()) {
			Job job = new WorkspaceJob("Add error marker") {
				{ setSystem(true); } // don't show to user
				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) {
					asyncMessageHandler.addMarker(descriptor, offendingTerm, message, SEVERITY_ERROR);
					return Status.OK_STATUS;
				}
			};
			job.setRule(descriptor);
			job.schedule();
		} else {
			asyncMessageHandler.addMarkerFirstLine(descriptor, message, SEVERITY_ERROR);
		}
	}
}
