/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.as.test.core.classpath.modules;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IFacetedProjectWorkingCopy;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.validation.ValidationFramework;
import org.jboss.ide.eclipse.as.classpath.core.runtime.CustomRuntimeClasspathModel;
import org.jboss.ide.eclipse.as.classpath.core.runtime.IRuntimePathProvider;
import org.jboss.ide.eclipse.as.classpath.core.runtime.cache.internal.ModuleSlotCache;
import org.jboss.ide.eclipse.as.classpath.core.runtime.internal.ProjectRuntimeClasspathProvider;
import org.jboss.ide.eclipse.as.classpath.core.runtime.path.internal.LayeredProductPathProvider;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.tools.as.test.core.ASMatrixTests;
import org.jboss.tools.as.test.core.internal.utils.IOUtil;
import org.jboss.tools.as.test.core.internal.utils.ResourceUtils;
import org.jboss.tools.as.test.core.internal.utils.wtp.CreateProjectOperationsUtility;
import org.jboss.tools.as.test.core.internal.utils.wtp.JavaEEFacetConstants;
import org.jboss.tools.as.test.core.internal.utils.wtp.OperationTestCase;
import org.jboss.tools.test.util.JobUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import junit.framework.TestCase;

public class JBossModulesClasspathTest  extends TestCase {

	@BeforeClass
	public static void setUpClass() throws Exception {
		ValidationFramework.getDefault().suspendAllValidation(true);
	}
	
	@AfterClass
	public static void tearDownClass() {
		ValidationFramework.getDefault().suspendAllValidation(false);
	}
	
	@Before
	public void setup() throws Exception {
		ASMatrixTests.getDefault().cleanup();
	}
	
	@After
	public void tearDown() throws Exception {
		ASMatrixTests.getDefault().cleanup();
	} 
	
	/**
	 * First test to make sure our testing structure is doing the right thing, making a mocked patch installation, etc
	 */
	public void testBasicModuleSlot() throws Exception {
		// create a server + runtime + project
		IServer s2 = MockJBossModulesUtil.createMockServerWithRuntime(IJBossToolingConstants.SERVER_EAP_61, "TestOne");
		IRuntime rt = s2.getRuntime();
		// Clear the list of modules to add 
		CustomRuntimeClasspathModel.getInstance().savePathProviders(rt.getRuntimeType(), new IRuntimePathProvider[]{});
		
		// Create project
		IDataModel dm = CreateProjectOperationsUtility.getWebDataModel("WebProject1", 
				null, null, null, null, JavaEEFacetConstants.WEB_24, false);
		IProject p = createSingleProject(dm, "WebProject1");
		
		// Ensure no results
		ProjectRuntimeClasspathProvider provider = new ProjectRuntimeClasspathProvider();
		IClasspathEntry[] entries = provider.resolveClasspathContainer(p, rt);
		displayEntries(entries);
		assertEquals(entries.length,0);
		
		// add a path provider, verify 1 result
		LayeredProductPathProvider pathPro = new LayeredProductPathProvider("org.jboss.as.server", null);
		CustomRuntimeClasspathModel.getInstance().savePathProviders(rt.getRuntimeType(), new IRuntimePathProvider[]{pathPro});
		entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);
		
		
		// Let's add a new module 
		IPath modules = rt.getLocation().append("modules");
		IPath base = modules.append("system").append("layers").append("base");
		MockJBossModulesUtil.cloneModule(base, "org.jboss.as.server", base, "org.max.wonka");
		
		// Make a global moduleslot path provider for this rt-type
		LayeredProductPathProvider wonkaProvider = new LayeredProductPathProvider("org.max.wonka", null);
		CustomRuntimeClasspathModel.getInstance().savePathProviders(rt.getRuntimeType(), new IRuntimePathProvider[]{pathPro, wonkaProvider});
		entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,2);
		
	}
	
	
	/**
	 * Test additions via manifest.mf
	 */
	public void testManifestAdditions() throws Exception {
		// create a server + runtime + project
		IServer s2 = MockJBossModulesUtil.createMockServerWithRuntime(IJBossToolingConstants.SERVER_EAP_61, "TestOne");
		IRuntime rt = s2.getRuntime();
		// Clear the list of modules to add 
		CustomRuntimeClasspathModel.getInstance().savePathProviders(rt.getRuntimeType(), new IRuntimePathProvider[]{});
		
		// Create project
		IDataModel dm = CreateProjectOperationsUtility.getWebDataModel("WebProject1", 
				null, null, null, null, JavaEEFacetConstants.WEB_24, false);
		IProject p = createSingleProject(dm, "WebProject1");
		
		// Make sure the project is targeted to a runtime
		IFacetedProject facetedProject = ProjectFacetsManager.create(p);
		IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
		workingCopy.addTargetedRuntime(RuntimeManager.getRuntime(rt.getName()));
		workingCopy.commitChanges(null);
		
		IFile manifest = p.getFile("MANIFEST.MF");
		String contents = "Dependencies: org.jboss.as.server\n";
		setContentsAndWaitForPropagation(manifest, contents);
		
		// Ensure 1 result
		ProjectRuntimeClasspathProvider provider = new ProjectRuntimeClasspathProvider();
		IClasspathEntry[] entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);
		
		
		// Let's add a new module 
		IPath modules = rt.getLocation().append("modules");
		IPath base = modules.append("system").append("layers").append("base");
		MockJBossModulesUtil.cloneModule(base, "org.jboss.as.server", base, "org.max.wonka");

		contents = "Dependencies: org.jboss.as.server, org.max.wonka\n";
		setContentsAndWaitForPropagation(manifest, contents);
		JobUtils.waitForIdle();
		System.out.println("Idle over");
		provider = new ProjectRuntimeClasspathProvider();
		entries = provider.resolveClasspathContainer(p, rt);
		System.out.println("Asserting");
		assertEquals(entries.length,2);

	
		// Remove it
		contents = "Dependencies: org.jboss.as.server\n";
		setContentsAndWaitForPropagation(manifest, contents);

		provider = new ProjectRuntimeClasspathProvider();
		entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);
	}
	
	private void setContentsAndWaitForPropagation(IFile file, String contents) throws Exception {
		final IProject p = file.getProject();
		File f = file.getLocation().toFile();
		
		System.out.println("Setting contents for file: " + file.getLocation().toOSString());
		System.out.println("Current time for file:  " + file.getLocation().toFile().lastModified());
		System.out.println(System.currentTimeMillis());
		System.out.println("Exists? " + file.exists());
		System.out.println("ToFile.exists? " + file.getLocation().toFile().exists());
		System.out.println("Is outdated pre-write? " + ModuleSlotCache.getInstance().isOutdated(file));
		
		IOUtil.setContents(f, contents);
		Job j = new WorkspaceJob("Refresh") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				p.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		j.join();
		System.out.println("Is outdated post-write? " + ModuleSlotCache.getInstance().isOutdated(file));
		JobUtils.waitForIdle(1500);
		System.out.println("Is outdated post-idle? " + ModuleSlotCache.getInstance().isOutdated(file));
	}
	
	/**
	 * Test additions via manifest.mf
	 */
	public void testManifestVersionAdditions() throws Exception {
		// create a server + runtime + project
		IServer s2 = MockJBossModulesUtil.createMockServerWithRuntime(IJBossToolingConstants.SERVER_EAP_61, "TestOne");
		IRuntime rt = s2.getRuntime();
		// Clear the list of modules to add 
		CustomRuntimeClasspathModel.getInstance().savePathProviders(rt.getRuntimeType(), new IRuntimePathProvider[]{});
		
		// Create project
		IDataModel dm = CreateProjectOperationsUtility.getWebDataModel("WebProject1", 
				null, null, null, null, JavaEEFacetConstants.WEB_24, false);
		IProject p = createSingleProject(dm, "WebProject1");
		
		// Make sure the project is targeted to a runtime
		IFacetedProject facetedProject = ProjectFacetsManager.create(p);
		IFacetedProjectWorkingCopy workingCopy = facetedProject.createWorkingCopy();
		workingCopy.addTargetedRuntime(RuntimeManager.getRuntime(rt.getName()));
		workingCopy.commitChanges(null);
		
		IFile manifest = p.getFile("MANIFEST.MF");
		String contents = "Dependencies: org.jboss.as.server\n";
		setContentsAndWaitForPropagation(manifest, contents);
		
		
		// Ensure 1 result
		ProjectRuntimeClasspathProvider provider = new ProjectRuntimeClasspathProvider();
		IClasspathEntry[] entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);
		
		// add main slot
		contents = "Dependencies: org.jboss.as.server:main\n";
		setContentsAndWaitForPropagation(manifest, contents);

		provider = new ProjectRuntimeClasspathProvider();
		entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);

	
		// Test on a slot that doesn't exist
		contents = "Dependencies: org.jboss.as.server:1.0\n";
		setContentsAndWaitForPropagation(manifest, contents);

		provider = new ProjectRuntimeClasspathProvider();
		entries = provider.resolveClasspathContainer(p, rt);
		displayEntries(entries);
		assertEquals(entries.length,0);

		
		// Lets add a new slot for our module and repeat it
		IPath modules = rt.getLocation().append("modules");
		IPath base = modules.append("system").append("layers").append("base");
		MockJBossModulesUtil.duplicateToSlot(base, "org.jboss.as.server", "1.0");
		
		// We have to re-set the contents or the cache won't know to update
		contents = "Dependencies:   org.jboss.as.server:1.0\n";
		setContentsAndWaitForPropagation(manifest, contents);

		provider = new ProjectRuntimeClasspathProvider();
		entries = provider.resolveClasspathContainer(p, rt);
		assertEquals(entries.length,1);
		IPath path = entries[0].getPath();
		assertTrue(path.toOSString().contains("1.0"));
	}
	

	private void displayEntries(IClasspathEntry[] entries) {
		System.out.println("\nDisplaying classpath entries");
		for( int i = 0; i < entries.length; i++ ) {
			System.out.println("   content kind: " + entries[i].getContentKind());
			System.out.println("   entry kind: " + entries[i].getEntryKind());
			System.out.println("   path: " + entries[i].getPath());
			System.out.println();
		}
	}
	
	protected IProject createSingleProject(IDataModel dm, String name) throws Exception {
		OperationTestCase.runAndVerify(dm);
		IProject p = ResourceUtils.findProject(name);
		if(!p.exists())
			fail();
		return p;
	}
}
