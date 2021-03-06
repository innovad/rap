/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.wizards;

import org.eclipse.rwt.SessionSingletonBase;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.registry.IWorkbenchRegistryConstants;

/**
 * Registry that contains wizards contributed via the <code>newWizards</code>
 * extension point.
 * 
 */
public final class NewWizardRegistry extends AbstractExtensionWizardRegistry {

// RAP [rst] session singleton
//	private static NewWizardRegistry singleton;
	
	/**
	 * Return the singleton instance of this class.
	 * 
	 * @return the singleton instance of this class
	 */
	public static synchronized NewWizardRegistry getInstance() {		
      // RAP [rst] session singleton
	  return ( NewWizardRegistry )SessionSingletonBase.getInstance( NewWizardRegistry.class );
//		if (singleton == null) {
//			singleton = new NewWizardRegistry();
//		}
//		return singleton;
	}
		
	/**
	 * Private constructor.
	 */
	private NewWizardRegistry() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.wizards.AbstractExtensionWizardRegistry#getExtensionPoint()
	 */
	protected String getExtensionPoint() {
		return IWorkbenchRegistryConstants.PL_NEW;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.internal.wizards.AbstractExtensionWizardRegistry#getPlugin()
	 */
	protected String getPlugin() {
		// RAP [bm]: namespace
//		return PlatformUI.PLUGIN_ID;
		return PlatformUI.PLUGIN_EXTENSION_NAME_SPACE;
		// RAPEND: [bm] 
	}
}
