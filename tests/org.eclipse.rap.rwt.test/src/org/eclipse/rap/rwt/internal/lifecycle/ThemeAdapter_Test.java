/*******************************************************************************
 * Copyright (c) 2009, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.lifecycle;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.internal.theme.IThemeAdapter;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.widgets.controlkit.ControlThemeAdapterImpl;
import org.eclipse.swt.internal.widgets.shellkit.ShellThemeAdapter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class ThemeAdapter_Test extends TestCase {

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    Fixture.fakeResponseWriter();
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testAdapterForShell() {
    Display display = new Display();
    Composite shell = new Shell( display, SWT.NONE );
    Object adapter = shell.getAdapter( IThemeAdapter.class );
    assertNotNull( adapter );
    assertTrue( adapter instanceof ShellThemeAdapter );
  }

  public void testAdapterForComposite() {
    Display display = new Display();
    Composite shell = new Shell( display, SWT.NONE );
    Composite composite = new Composite( shell, SWT.NONE );
    Object adapter = composite.getAdapter( IThemeAdapter.class );
    assertNotNull( adapter );
    assertTrue( adapter instanceof ControlThemeAdapterImpl );
  }

  public void testAdapterForCustomWidget() {
    Display display = new Display();
    Composite shell = new Shell( display, SWT.NONE );
    CustomWidget customWidget = new CustomWidget( shell, SWT.NONE );
    Object adapter = customWidget.getAdapter( IThemeAdapter.class );
    assertNotNull( adapter );
    assertTrue( adapter instanceof ControlThemeAdapterImpl );
  }

  static class CustomWidget extends Composite {
    private static final long serialVersionUID = 1L;
    public CustomWidget( Composite parent, int style ) {
      super( parent, style );
    }
  }
}