/*******************************************************************************
 * Copyright (c) 2007, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.rwt.internal.theme;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.internal.theme.css.StyleRule;
import org.eclipse.rwt.internal.theme.css.StyleSheet;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Widget;


public class ThemeManager_Test extends TestCase {

  private ThemeManager manager;

  protected void setUp() {
    manager = new ThemeManager();
    Fixture.setUp();
  }

  protected void tearDown() {
    Fixture.tearDown();
  }

  public void testDefaultThemeableWidgetsBeforeActivation() {
    ThemeableWidget[] allThemeableWidgets = manager.getAllThemeableWidgets();

    assertTrue( allThemeableWidgets.length > 1 );
    assertEquals( Widget.class, allThemeableWidgets[ 0 ].widget );
  }

  public void testFallbackThemeBeforeActivation() {
    Theme fallbackTheme = manager.getTheme( ThemeManager.FALLBACK_THEME_ID );

    assertNotNull( fallbackTheme );
    assertEquals( "RAP Fallback Theme", fallbackTheme.getName() );
  }

  public void testRegisterTheme() {
    StyleSheet emptyStyleSheet = new StyleSheet( new StyleRule[ 0 ] );
    Theme customTheme = new Theme( "custom.id", "foo", emptyStyleSheet );
    manager.registerTheme( customTheme );
    manager.activate();
    assertTrue( manager.hasTheme( "custom.id" ) );
    List regThemeIds = Arrays.asList( manager.getRegisteredThemeIds() );
    assertTrue( regThemeIds.contains( "custom.id" ) );
    assertSame( customTheme, manager.getTheme( "custom.id" ) );
  }

  public void testRegisterThemeTwice() {
    StyleSheet emptyStyleSheet = new StyleSheet( new StyleRule[ 0 ] );
    Theme theme = new Theme( "id1", "foo", emptyStyleSheet );
    manager.registerTheme( theme );
    try {
      manager.registerTheme( theme );
      fail();
    } catch( IllegalArgumentException expected ) {
    }
  }

  public void testGetThemeableWidget() {
    manager.activate();
    ThemeableWidget themeableWidget = manager.getThemeableWidget( Button.class );
    assertNotNull( themeableWidget );
    assertNotNull( themeableWidget.loader );
    assertEquals( Button.class, themeableWidget.widget );
    assertNotNull( themeableWidget.elements );
    assertTrue( themeableWidget.elements.length > 0 );
  }

  public void testDefaultThemeInitialized() {
    manager.initialize();
    manager.activate();
    Theme defaultTheme = manager.getTheme( RWT.DEFAULT_THEME_ID );
    assertNotNull( defaultTheme.getValuesMap() );
    assertTrue( defaultTheme.getValuesMap().getAllValues().length > 0 );
  }

  public void testCustomAndDefaultThemeInitialized() throws Exception {
    StyleSheet styleSheet = ThemeTestUtil.getStyleSheet( "TestExample.css" );
    Theme customTheme = new Theme( "custom.id", "Custom Theme", styleSheet );
    manager.registerTheme( customTheme );
    manager.initialize();
    manager.activate();
    Theme defaultTheme = manager.getTheme( RWT.DEFAULT_THEME_ID );
    assertNotNull( defaultTheme.getValuesMap() );
    assertTrue( defaultTheme.getValuesMap().getAllValues().length > 0 );
    assertNotNull( customTheme.getValuesMap() );
    assertTrue( customTheme.getValuesMap().getAllValues().length > 0 );
  }

  public void testActivateAndDeactivate() {
    int beforeActivate = getFallbackTheme().getStyleSheet().getStyleRules().length;
    manager.activate();
    int afterActivate = getFallbackTheme().getValuesMap().getAllValues().length;
    manager.deactivate();
    int afterDeactivate = getFallbackTheme().getStyleSheet().getStyleRules().length;

    assertEquals( 0, beforeActivate );
    assertTrue( 0 < afterActivate );
    assertEquals( 0, afterDeactivate );
  }

  public void testGetAppearances() {
    manager.activate();

    List<String> appearances = manager.getAppearances();

    String joinedAppearances = join( appearances );
    assertTrue( joinedAppearances.contains( "\"tree-row\" : {" ) );
    assertTrue( joinedAppearances.contains( "\"sash-handle\" : {" ) );
  }

  private Theme getFallbackTheme() {
    return manager.getTheme( ThemeManager.FALLBACK_THEME_ID );
  }

  private static String join( List<String> appearances ) {
    StringBuilder buffer = new StringBuilder();
    for( String string : appearances ) {
      buffer.append( string );
    }
    return buffer.toString();
  }
}