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
package org.eclipse.rap.rwt.cluster.testfixture.server;

import javax.servlet.http.HttpSession;

import org.eclipse.rwt.lifecycle.IEntryPoint;

public interface IServletEngine {
  String SERVLET_NAME= "rap";
  String SERVLET_PATH = "/" + SERVLET_NAME;

  void start( Class<? extends IEntryPoint> entryPointClass ) throws Exception;
  void stop( int timeout ) throws Exception;
  void stop() throws Exception;
  int getPort();
  
  HttpSession[] getSessions();
}
