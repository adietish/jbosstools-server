/*******************************************************************************
 * Copyright (c) 2007 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.as.ui.mbeans.project;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.common.internal.modulecore.AddClasspathFoldersParticipant;
import org.eclipse.jst.common.internal.modulecore.AddClasspathLibReferencesParticipant;
import org.eclipse.jst.common.internal.modulecore.AddMappedOutputFoldersParticipant;
import org.eclipse.jst.common.internal.modulecore.IgnoreJavaInSourceFolderParticipant;
import org.eclipse.jst.common.internal.modulecore.ReplaceManifestExportParticipant;
import org.eclipse.jst.common.internal.modulecore.SingleRootExportParticipant;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.classpathdep.ClasspathDependencyEnablement;
import org.eclipse.jst.j2ee.internal.common.exportmodel.JEEHeirarchyExportParticipant;
import org.eclipse.jst.j2ee.internal.common.exportmodel.JavaEESingleRootCallback;
import org.eclipse.wst.common.componentcore.internal.flat.IFlattenParticipant;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.web.internal.deployables.FlatComponentDeployable;
import org.jboss.ide.eclipse.as.wtp.core.modules.IJBTModule;

public class JBossSARModuleDelegate extends FlatComponentDeployable implements IJBTModule {

	public JBossSARModuleDelegate(IProject project){
		super(project);
	}

	public IModuleResource[] members() throws CoreException {
		return super.members();
	}
	
	@Override
	protected IFlattenParticipant[] getParticipants() {
		List<IFlattenParticipant> participants = new ArrayList<IFlattenParticipant>();
		participants.add(new JEEHeirarchyExportParticipant());
		participants.add(new AddMappedOutputFoldersParticipant());
		participants.add(new IgnoreJavaInSourceFolderParticipant());
		return participants.toArray(new IFlattenParticipant[participants.size()]);
	}
}
