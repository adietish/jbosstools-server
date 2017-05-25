/*******************************************************************************
 * Copyright (c) 2011-2013 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.ide.eclipse.as.classpath.core.runtime.internal;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.jboss.ide.eclipse.as.classpath.core.runtime.IRuntimePathProvider;
import org.jboss.ide.eclipse.as.classpath.core.runtime.RuntimeJarUtility;
import org.jboss.ide.eclipse.as.classpath.core.runtime.cache.internal.InternalRuntimeClasspathModel;
import org.jboss.ide.eclipse.as.classpath.core.runtime.jbossmodules.internal.JBossModulesDefaultClasspathModel;
import org.jboss.ide.eclipse.as.classpath.core.runtime.path.internal.LayeredProductPathProvider;
import org.jboss.ide.eclipse.as.classpath.core.runtime.path.internal.RuntimePathProviderFileset;
import org.jboss.ide.eclipse.as.core.server.internal.ExtendedServerPropertiesAdapterFactory;
import org.jboss.ide.eclipse.as.core.server.internal.extendedproperties.ServerExtendedProperties;
import org.jboss.ide.eclipse.as.core.util.IJBossRuntimeResourceConstants;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;

/**
 * This class is in charge of the default classpath entries when
 * the users have not overridden the settings on a per-runtime-type basis. 
 */
public class DefaultClasspathModelLoader implements IJBossToolingConstants, IJBossRuntimeResourceConstants {
	private static final String SEP = "/"; //$NON-NLS-1$
	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String CONFIG_DIR = RuntimeJarUtility.CONFIG_DIR_VAR_PATTERN;
	
	/**
	 * Get a runtime classpath model for the given server type.
	 * 
	 * @param type
	 * @return
	 */
	public InternalRuntimeClasspathModel getDefaultRuntimeClasspathModel(IRuntimeType type) {
		IRuntimePathProvider[] providers = null;
		String rtID = type.getId();
		switch(rtID) {
			case AS_32: 
				providers =  getDefaultAS3Entries(); break;
			case AS_40:
			case AS_42:
				providers =  getDefaultAS40Entries(); break;
			case AS_50:
			case AS_51:
			case EAP_50:
				providers =  getDefaultAS50Entries(); break;
			case EAP_43:
				providers =  getDefaultEAP43Entries(); break;
			case AS_60:
				providers =  getDefaultAS60Entries(); break;
		}


		if( providers != null ) {
			InternalRuntimeClasspathModel model = new InternalRuntimeClasspathModel();
			model.addProviders(providers);
			return model;
		}
		
		// Delegate the other defaults to a more customized
		// model for as7 and above
		if( jbossModulesStyle(type)) {
			return new JBossModulesDefaultClasspathModel(type);
		}
		
		// NEW_SERVER_ADAPTER add logic for new adapter here
		return new InternalRuntimeClasspathModel();
	}
	
	private boolean jbossModulesStyle(IRuntimeType rtt) {
		// NEW_SERVER_ADAPTER add logic for new adapter here
		// TODO this needs to be updated
		ServerExtendedProperties props = new ExtendedServerPropertiesAdapterFactory().getExtendedProperties(rtt);
		// deploy-only server causes invalid values here somehow? 
		return props != null && props.getFileStructure() == ServerExtendedProperties.FILE_STRUCTURE_CONFIG_DEPLOYMENTS;
	}
	
	private IRuntimePathProvider[] getDefaultAS3Entries() {
		ArrayList<RuntimePathProviderFileset> sets = new ArrayList<RuntimePathProviderFileset>();
		sets.add(new RuntimePathProviderFileset(LIB));
		sets.add(new RuntimePathProviderFileset(CONFIG_DIR + SEP + LIB));
		sets.add(new RuntimePathProviderFileset(CLIENT));
		return sets.toArray(new RuntimePathProviderFileset[sets.size()]);
	}
	
	private IRuntimePathProvider[] getDefaultAS40Entries() {
		ArrayList<RuntimePathProviderFileset> sets = new ArrayList<RuntimePathProviderFileset>();
		String deployPath = CONFIG_DIR + SEP + DEPLOY;
		sets.add(new RuntimePathProviderFileset(LIB));
		sets.add(new RuntimePathProviderFileset(CONFIG_DIR + SEP + LIB));
		sets.add(new RuntimePathProviderFileset(deployPath + SEP + JBOSS_WEB_DEPLOYER + SEP + JSF_LIB));
		sets.add(new RuntimePathProviderFileset(deployPath + SEP + AOP_JDK5_DEPLOYER));
		sets.add(new RuntimePathProviderFileset(deployPath + SEP + EJB3_DEPLOYER));
		sets.add(new RuntimePathProviderFileset(CLIENT));
		return sets.toArray(new RuntimePathProviderFileset[sets.size()]);
	}
	
	private IRuntimePathProvider[] getDefaultEAP43Entries() {
		return getDefaultAS40Entries();
	}
	
	private IRuntimePathProvider[] getDefaultAS50Entries() {
		ArrayList<RuntimePathProviderFileset> sets = new ArrayList<RuntimePathProviderFileset>();
		String deployerPath = CONFIG_DIR + SEP + DEPLOYERS;
		String deployPath = CONFIG_DIR + SEP + DEPLOY;
		sets.add(new RuntimePathProviderFileset(COMMON + SEP + LIB));
		sets.add(new RuntimePathProviderFileset(LIB));
		sets.add(new RuntimePathProviderFileset(CONFIG_DIR + SEP + LIB));
		
		sets.add(new RuntimePathProviderFileset(deployPath + SEP + JBOSSWEB_SAR + SEP + JSF_LIB));
		sets.add(new RuntimePathProviderFileset(EMPTY, deployPath + SEP + JBOSSWEB_SAR, JBOSS_WEB_SERVICE_JAR, EMPTY));
		sets.add(new RuntimePathProviderFileset(EMPTY, deployPath + SEP + JBOSSWEB_SAR, JSTL_JAR, EMPTY));
		sets.add(new RuntimePathProviderFileset(deployerPath + SEP + AS5_AOP_DEPLOYER));
		sets.add(new RuntimePathProviderFileset(deployerPath + SEP + EJB3_DEPLOYER));
		sets.add(new RuntimePathProviderFileset(EMPTY, deployerPath + SEP + WEBBEANS_DEPLOYER,JSR299_API_JAR, EMPTY));
		sets.add(new RuntimePathProviderFileset(CLIENT));
		return sets.toArray(new RuntimePathProviderFileset[sets.size()]);
	}
	
	private IRuntimePathProvider[] getDefaultAS60Entries() {
		ArrayList<IRuntimePathProvider> sets = new ArrayList<IRuntimePathProvider>();
		sets.addAll(Arrays.asList(getDefaultAS50Entries()));
		sets.add(new RuntimePathProviderFileset(CONFIG_DIR + SEP + DEPLOYERS + SEP + REST_EASY_DEPLOYER));
		sets.add(new RuntimePathProviderFileset(CONFIG_DIR + SEP + DEPLOYERS + SEP + JSF_DEPLOYER + SEP + MOJARRA_20 + SEP + JSF_LIB));
		return sets.toArray(new RuntimePathProviderFileset[sets.size()]);
	}
}
