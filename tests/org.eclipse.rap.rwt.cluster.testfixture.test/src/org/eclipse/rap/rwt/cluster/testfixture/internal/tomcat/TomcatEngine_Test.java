/*******************************************************************************
 * Copyright (c) 2011 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.cluster.testfixture.internal.tomcat;

import org.eclipse.rap.rwt.cluster.testfixture.internal.server.ServletEngineTestBase;
import org.eclipse.rap.rwt.cluster.testfixture.server.IServletEngineFactory;
import org.eclipse.rap.rwt.cluster.testfixture.server.TomcatFactory;


public class TomcatEngine_Test extends ServletEngineTestBase {

  protected IServletEngineFactory getServletEngineFactory() {
    return new TomcatFactory();
  }
}
