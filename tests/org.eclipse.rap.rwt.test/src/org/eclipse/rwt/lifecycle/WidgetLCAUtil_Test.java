/*******************************************************************************
 * Copyright (c) 2002, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.rwt.lifecycle;

import java.io.IOException;
import java.util.Date;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.Message;
import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.rwt.internal.lifecycle.JSConst;
import org.eclipse.rwt.internal.protocol.ProtocolTestUtil;
import org.eclipse.rwt.internal.protocol.ProtocolUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.internal.widgets.ControlUtil;
import org.eclipse.swt.internal.widgets.IControlAdapter;
import org.eclipse.swt.internal.widgets.IWidgetGraphicsAdapter;
import org.eclipse.swt.internal.widgets.Props;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


@SuppressWarnings("deprecation")
public class WidgetLCAUtil_Test extends TestCase {

  private Display display;
  private Shell shell;
  private Control widget;

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
    Fixture.fakeResponseWriter();
    display = new Display();
    shell = new Shell( display , SWT.NONE );
    widget = new Button( shell, SWT.PUSH );
  }

  @Override
  protected void tearDown() throws Exception {
    display.dispose();
    Fixture.tearDown();
  }

  public void testHasChanged() {
    Text text = new Text( shell, SWT.NONE );
    // test initial behaviour, text is same as default value -> no 'change'
    text.setText( "" );
    boolean hasChanged;
    hasChanged = WidgetLCAUtil.hasChanged( text, Props.TEXT, text.getText(), "" );
    assertEquals( false, hasChanged );
    // test initial behaviour, text is different as default value -> 'change'
    text.setText( "other value" );
    hasChanged = WidgetLCAUtil.hasChanged( text, Props.TEXT, text.getText(), "" );
    assertEquals( true, hasChanged );
    // test subsequent behaviour (when already initialized)
    Fixture.markInitialized( display );
    Fixture.markInitialized( text );
    Fixture.clearPreserved();
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( text, Props.TEXT, text.getText(), "" );
    assertEquals( false, hasChanged );
    Fixture.clearPreserved();
    Fixture.preserveWidgets();
    text.setText( "whatsoevervaluehasbeensetduringrequest" );
    hasChanged = WidgetLCAUtil.hasChanged( text, Props.TEXT, text.getText(), "" );
    assertEquals( true, hasChanged );
  }

  public void testHasChangedWidthArrays() {
    List list = new List( shell, SWT.MULTI );

    boolean hasChanged;
    hasChanged = WidgetLCAUtil.hasChanged( list, "items", new String[] { "a" } );
    assertEquals( true, hasChanged );

    list.setItems( new String[] { "a" } );
    Fixture.markInitialized( display );
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( list, "items", new String[] { "a" } );
    assertEquals( false, hasChanged );

    list.setItems( new String[] { "a" } );
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( list, "items", new String[] { "b" } );
    assertEquals( true, hasChanged );

    list.setItems( new String[] { "a" } );
    Fixture.preserveWidgets();
    hasChanged
      = WidgetLCAUtil.hasChanged( list, "items", new String[] { "a", "b" } );
    assertEquals( true, hasChanged );

    list.setItems( new String[] { "a" } );
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( list, "items", null );
    assertEquals( true, hasChanged );

    list.setItems( new String[] { "a", "b", "c" } );
    list.setSelection( new int[] { 0, 1, 2 } );
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( list, "selectionIndices", new int[] { 0, 1, 4 } );
    assertEquals( true, hasChanged );

    list.setItems( new String[] { "a", "b", "c" } );
    list.setSelection( new int[] { 0, 1, 2 } );
    Fixture.preserveWidgets();
    hasChanged = WidgetLCAUtil.hasChanged( list, "selectionIndices", new int[] { 0, 1, 2 } );
    assertEquals( false, hasChanged );
  }

  public void testEquals() {
    assertTrue( WidgetLCAUtil.equals( null, null ) );
    assertFalse( WidgetLCAUtil.equals( null, "1" ) );
    assertFalse( WidgetLCAUtil.equals( "1", null ) );
    assertFalse( WidgetLCAUtil.equals( "1", "2" ) );
    assertTrue( WidgetLCAUtil.equals( "1", "1" ) );
    assertTrue( WidgetLCAUtil.equals( new String[] { "1" },
                                   new String[] { "1" } ) );
    assertTrue( WidgetLCAUtil.equals( new int[] { 1 },
                                   new int[] { 1 } ) );
    assertTrue( WidgetLCAUtil.equals( new boolean[] { true },
                                   new boolean[] { true } ) );
    assertTrue( WidgetLCAUtil.equals( new long[] { 232 },
                                   new long[] { 232 } ) );
    assertTrue( WidgetLCAUtil.equals( new float[] { 232 },
                                   new float[] { 232 } ) );
    assertTrue( WidgetLCAUtil.equals( new double[] { 345 },
                                   new double[] { 345 } ) );
    assertTrue( WidgetLCAUtil.equals( new Date[] { new Date( 1 ) },
                                   new Date[] { new Date( 1 ) } ) );
    assertFalse( WidgetLCAUtil.equals( new double[] { 345 },
                                    new float[] { 345 } ) );
    assertFalse( WidgetLCAUtil.equals( new int[] { 345 },
                                    new float[] { 345 } ) );
    assertFalse( WidgetLCAUtil.equals( new int[] { 345 },
                                    new long[] { 345 } ) );
    assertFalse( WidgetLCAUtil.equals( new Date[] { new Date( 3 ) }, null ) );
  }

  public void testEscapeText() {
    // Empty Parameter
    try {
      WidgetLCAUtil.escapeText( null, true );
      fail( "NullPointerException expected" );
    } catch( NullPointerException e ) {
      // expected
    }
    // Text that goes unescaped
    assertEquals( "Test", WidgetLCAUtil.escapeText( "Test", false ) );
    assertEquals( "Test", WidgetLCAUtil.escapeText( "Test", true ) );
    assertEquals( "", WidgetLCAUtil.escapeText( "", false ) );
    assertEquals( "", WidgetLCAUtil.escapeText( "", true ) );
    // Brackets
    assertEquals( "&lt;", WidgetLCAUtil.escapeText( "<", false ) );
    assertEquals( "&gt;", WidgetLCAUtil.escapeText( ">", false ) );
    assertEquals( "&lt;&lt;&lt;", WidgetLCAUtil.escapeText( "<<<", false ) );
    String expected = "&lt;File &gt;";
    assertEquals( expected, WidgetLCAUtil.escapeText( "<File >", false ) );
    // Amp
    assertEquals( "&amp;&amp;&amp;File&quot;&gt;",
                  WidgetLCAUtil.escapeText( "&&&File\">", false ) );
    assertEquals( "Open &amp; Close",
                  WidgetLCAUtil.escapeText( "Open && Close", true ) );
    assertEquals( "E&lt;s&gt;ca'pe&quot; &amp; me",
                  WidgetLCAUtil.escapeText( "&E<s>ca'pe\" && me", true ) );
    // Quotes
    expected = "&quot;File&quot;";
    assertEquals( expected, WidgetLCAUtil.escapeText( "\"File\"", false ) );
    expected = "&quot;&quot;File";
    assertEquals( expected, WidgetLCAUtil.escapeText( "\"\"File", false ) );
    // Backslashes not modified
    expected = "Test\\";
    assertEquals( expected, WidgetLCAUtil.escapeText( "Test\\", false ) );
    // Escape unicode characters \u2028 and \u2029 - see bug 304364
    expected = "abc&#8232;abc&#8233;abc";
    assertEquals( expected,
                  WidgetLCAUtil.escapeText( "abc\u2028abc\u2029abc", false ) );
  }

  public void testTruncateZeros() {
    assertEquals( ( char )0, "\000".charAt( 0 ) );
    assertEquals( "foo ", WidgetLCAUtil.escapeText( "foo \000 bar", false ) );
    assertEquals( "foo", WidgetLCAUtil.escapeText( "foo\000", false ) );
    assertEquals( "", WidgetLCAUtil.escapeText( "\000foo", false ) );
    assertEquals( "&lt;foo", WidgetLCAUtil.escapeText( "<foo\000>", false ) );
    assertEquals( "&lt;foo", WidgetLCAUtil.escapeText( "<foo\000>", true ) );
  }

  public void testParseFontName() {
    // IE doesn't like quoted font names (or whatever qooxdoo makes out of them)
    String systemFontName
      = "\"Segoe UI\", Corbel, Calibri, Tahoma, \"Lucida Sans Unicode\", "
      + "sans-serif";
    String[] fontNames = ProtocolUtil.parseFontName( systemFontName );
    assertEquals( 6, fontNames.length );
    assertEquals( "Segoe UI", fontNames[ 0 ] );
    assertEquals( "Corbel", fontNames[ 1 ] );
    assertEquals( "Calibri", fontNames[ 2 ] );
    assertEquals( "Tahoma", fontNames[ 3 ] );
    assertEquals( "Lucida Sans Unicode", fontNames[ 4 ] );
    assertEquals( "sans-serif", fontNames[ 5 ] );

    // Empty font names don't cause trouble (at least for the browsers
    // currently tested - therefore don't make extra effort to eliminate them
    fontNames = ProtocolUtil.parseFontName( "a, , b" );
    assertEquals( 3, fontNames.length );
    assertEquals( "a", fontNames[ 0 ] );
    assertEquals( "", fontNames[ 1 ] );
    assertEquals( "b", fontNames[ 2 ] );
  }

  public void testFontBold() throws IOException {
    Label label = new Label( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeFont( label, label.getFont() );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( ", false, false );" ) );

    Font oldFont = label.getFont();
    FontData fontData = FontUtil.getData( oldFont );
    Font newFont = Graphics.getFont( fontData.getName(),
                                     fontData.getHeight(),
                                     SWT.BOLD );
    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeFont( label, newFont );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( ", true, false );" ) );
  }

  public void testFontItalic() throws IOException {
    Label label = new Label( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeFont( label, label.getFont() );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( ", false, false );" ) );

    Font oldFont = label.getFont();
    FontData fontData = FontUtil.getData( oldFont );
    Font newFont = Graphics.getFont( fontData.getName(),
                                     fontData.getHeight(),
                                     SWT.ITALIC );
    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeFont( label, newFont );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( ", false, true );" ) );
  }

  public void testFontSize() throws IOException {
    Label label = new Label( shell, SWT.NONE );
    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    Font oldFont = label.getFont();
    FontData fontData = FontUtil.getData( oldFont );
    Font newFont = Graphics.getFont( fontData.getName(), 42, SWT.NORMAL );
    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeFont( label, newFont );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( ", 42, false, false );" ) );
  }

  public void testFontReset() throws IOException {
    Label label = new Label( shell, SWT.NONE );
    Fixture.fakeResponseWriter();
    Font font = Graphics.getFont( "Arial", 12, SWT.BOLD );
    Fixture.markInitialized( label );
    WidgetLCAUtil.preserveFont( label, font );
    WidgetLCAUtil.writeFont( label, null );

    String labelId = WidgetUtil.getId( label );
    String expected = "var w = wm.findWidgetById( \"" + labelId + "\" );w.resetFont();";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testForegroundReset() throws IOException {
    Label label = new Label( shell, SWT.NONE );
    Fixture.fakeResponseWriter();
    Color red = Graphics.getColor( 255, 0, 0 );
    Fixture.markInitialized( label );
    WidgetLCAUtil.preserveForeground( label, red );
    WidgetLCAUtil.writeForeground( label, null );

    String labelId = WidgetUtil.getId( label );
    String expected = "var w = wm.findWidgetById( \"" + labelId + "\" );w.resetTextColor();";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteImage() throws IOException {
    Label item = new Label( shell, SWT.NONE );

    // for an un-initialized control: no image -> no markup
    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeImage( item,
                              Props.IMAGE,
                              JSConst.QX_FIELD_ICON,
                              item.getImage() );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    // for an un-initialized control: render image, if any
    Fixture.fakeResponseWriter();
    item.setImage( Graphics.getImage( Fixture.IMAGE1 ) );
    WidgetLCAUtil.writeImage( item,
                              Props.IMAGE,
                              JSConst.QX_FIELD_ICON,
                              item.getImage() );
    String expected = "w.setIcon( \""
                    + ImageFactory.getImagePath( item.getImage() )
                    + "\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    // for an initialized control with change image: render it
    Fixture.markInitialized( item );
    Fixture.preserveWidgets();
    Fixture.fakeResponseWriter();
    item.setImage( null );
    WidgetLCAUtil.writeImage( item, Props.IMAGE, JSConst.QX_FIELD_ICON, item.getImage() );
    assertTrue( ProtocolTestUtil.getMessageScript().contains( "w.setIcon( null );" ) );
  }

  public void testWriteVariant() throws IOException {
    Label label = new Label( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeCustomVariant( label );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    label.setData( WidgetUtil.CUSTOM_VARIANT, "my_variant" );
    WidgetLCAUtil.writeCustomVariant( label );
    String expected = "w.addState( \"variant_my_variant\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteCustomVariant() throws IOException {
    Control control = new Label( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeCustomVariant( control );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    control.setData( WidgetUtil.CUSTOM_VARIANT, "my_variant" );
    WidgetLCAUtil.writeCustomVariant( control );
    String expected = "w.addState( \"variant_my_variant\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveCustomVariant( control );
    control.setData( WidgetUtil.CUSTOM_VARIANT, "new_variant" );
    WidgetLCAUtil.writeCustomVariant( control );
    expected
      =   "w.removeState( \"variant_my_variant\" );w.addState( "
        + "\"variant_new_variant\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveCustomVariant( control );
    Fixture.markInitialized( control );
    control.setData( WidgetUtil.CUSTOM_VARIANT, null );
    WidgetLCAUtil.writeCustomVariant( control );
    expected = "w.removeState( \"variant_new_variant\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteBackground() throws Exception {
    Control control = new Label( shell, SWT.NONE );
    Color red = display.getSystemColor( SWT.COLOR_RED );

    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeBackground( control, null, false );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );
    Fixture.markInitialized( control );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, null, false );
    WidgetLCAUtil.writeBackground( control, null, true );
    String expected = "w.setBackgroundColor( null );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, null, true );
    WidgetLCAUtil.writeBackground( control, red, true );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, red, true );
    WidgetLCAUtil.writeBackground( control, red, false );
    expected = "w.setBackgroundColor( \"#ff0000\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, red, false );
    WidgetLCAUtil.writeBackground( control, null, false );
    expected = "w.resetBackgroundColor();";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, red, false );
    WidgetLCAUtil.writeBackground( control, red, false );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, null );
    WidgetLCAUtil.writeBackground( control, red );
    expected = "w.setBackgroundColor( \"#ff0000\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, red );
    WidgetLCAUtil.writeBackground( control, red );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );
  }

  public void testWriteBackground_Transparency_RemoveBackgroundGradient() throws IOException {
    Control control = new Label( shell, SWT.NONE );
    Fixture.markInitialized( control );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, null, false );
    WidgetLCAUtil.writeBackground( control, null, true );

    String expected = "w.setBackgroundGradient( null );w.setBackgroundColor( null );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteBackground_ResetBackgroundGradient() throws IOException {
    Control control = new Label( shell, SWT.NONE );
    Fixture.markInitialized( control );
    Color red = display.getSystemColor( SWT.COLOR_RED );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, red, false );
    WidgetLCAUtil.writeBackground( control, null, false );

    String expected = "w.resetBackgroundGradient();w.resetBackgroundColor();";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteBackground_RemoveBackgroundGradient() throws IOException {
    Control control = new Label( shell, SWT.NONE );
    Fixture.markInitialized( control );
    Color red = display.getSystemColor( SWT.COLOR_RED );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackground( control, null, false );
    WidgetLCAUtil.writeBackground( control, red );

    String expected = "w.setBackgroundGradient( null );w.setBackgroundColor( \"#ff0000\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteMenu() throws IOException {
    Label label = new Label( shell, SWT.NONE );

    // for an un-initialized control: no menu -> no markup
    Fixture.fakeResponseWriter();
    Fixture.markInitialized( display );
    WidgetLCAUtil.writeMenu( label, label.getMenu() );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    // for an un-initialized control: render menu, if any
    Fixture.fakeResponseWriter();
    Menu menu = new Menu( label );
    label.setMenu( menu );
    WidgetLCAUtil.writeMenu( label, label.getMenu() );
    String labelId = WidgetUtil.getId( label );
    String menuId = WidgetUtil.getId( menu );
    String expected = "wm.setContextMenu( wm.findWidgetById( \""
                      + labelId
                      + "\" ), wm.findWidgetById( \""
                      + menuId
                      + "\" ) );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    // for an initialized control with change menu: render it
    Fixture.markInitialized( label );
    Fixture.preserveWidgets();
    Fixture.fakeResponseWriter();
    label.setMenu( null );
    WidgetLCAUtil.writeMenu( label, label.getMenu() );
    expected = "wm.setContextMenu( wm.findWidgetById( \"" + labelId + "\" ), null );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteStyleFlag() throws IOException {
    Control control = new Label( shell, SWT.NONE );
    Control borderControl = new Label( shell, SWT.BORDER );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeStyleFlag( control, SWT.BORDER, "BORDER" );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.writeStyleFlag( borderControl, SWT.BORDER, "BORDER" );
    String expected = "w.addState( \"rwt_BORDER\" );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteBackgroundGradient() throws IOException {
    Control control = new Composite( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackgroundGradient( control );
    Fixture.markInitialized( control );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };
    gfxAdapter.setBackgroundGradient( gradientColors, percents, true );
    WidgetLCAUtil.writeBackgroundGradient( control );
    String controlId = WidgetUtil.getId( control );
    String expected = "wm.setBackgroundGradient"
                      + "( wm.findWidgetById( \""
                      + controlId
                      + "\" ), [\"#00ff00\",\"#0000ff\" ], "
                      + "[0,100 ], true );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackgroundGradient( control );
    gradientColors = new Color[] {
      Graphics.getColor( 255, 0, 0 ),
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    percents = new int[] { 0, 50, 100 };
    gfxAdapter.setBackgroundGradient( gradientColors, percents, true );
    WidgetLCAUtil.writeBackgroundGradient( control );
    expected
      = "wm.setBackgroundGradient"
      + "( wm.findWidgetById( \"" + controlId + "\" ), [\"#ff0000\",\"#00ff00\",\"#0000ff\" ],"
      + " [0,50,100 ], true );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackgroundGradient( control );
    WidgetLCAUtil.writeBackgroundGradient( control );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackgroundGradient( control );
    gfxAdapter.setBackgroundGradient( null, null, true );
    WidgetLCAUtil.writeBackgroundGradient( control );
    expected = "wm.setBackgroundGradient( wm.findWidgetById( \""
               + controlId
               + "\" ), null, null, true );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteBackgroundGradient_Horizontal() throws IOException {
    Control control = new Composite( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveBackgroundGradient( control );
    Fixture.markInitialized( control );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };
    gfxAdapter.setBackgroundGradient( gradientColors, percents, false );
    WidgetLCAUtil.writeBackgroundGradient( control );
    String controlId = WidgetUtil.getId( control );
    String expected
      = "wm.setBackgroundGradient"
      + "( wm.findWidgetById( \"" + controlId + "\" ), [\"#00ff00\",\"#0000ff\" ], "
      + "[0,100 ], false );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteRoundedBorder() throws IOException {
    Widget widget = new Composite( shell, SWT.NONE );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveRoundedBorder( widget );
    Fixture.markInitialized( widget );
    Object adapter = widget.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter graphicsAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color color = Graphics.getColor( 0, 255, 0 );
    graphicsAdapter.setRoundedBorder( 2, color, 5, 6, 7, 8 );
    WidgetLCAUtil.writeRoundedBorder( widget );
    String widgetId = WidgetUtil.getId( widget );
    String expected = "wm.setRoundedBorder"
                      + "( wm.findWidgetById( \""
                      + widgetId
                      + "\" ), 2, \"#00ff00\", 5, 6, 7, 8 );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveRoundedBorder( widget );
    WidgetLCAUtil.writeRoundedBorder( widget );
    assertEquals( "", ProtocolTestUtil.getMessageScript() );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveRoundedBorder( widget );
    graphicsAdapter.setRoundedBorder( 4, color, 5, 6, 7, 8 );
    WidgetLCAUtil.writeRoundedBorder( widget );
    expected = "wm.setRoundedBorder"
               + "( wm.findWidgetById( \""
               + widgetId
               + "\" ), 4, \"#00ff00\", 5, 6, 7, 8 );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );

    Fixture.fakeResponseWriter();
    WidgetLCAUtil.preserveRoundedBorder( widget );
    graphicsAdapter.setRoundedBorder( 4, color, 5, 4, 7, 8 );
    WidgetLCAUtil.writeRoundedBorder( widget );
    expected = "wm.setRoundedBorder"
               + "( wm.findWidgetById( \""
               + widgetId
               + "\" ), 4, \"#00ff00\", 5, 4, 7, 8 );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  public void testWriteHelpListener() throws IOException {
    Composite widget = new Composite( shell, SWT.NONE );
    Fixture.fakeResponseWriter();

    widget.addHelpListener( new HelpListener() {
      public void helpRequested( HelpEvent e ) {
      }
    } );
    WidgetLCAUtil.writeHelpListener( widget );

    String widgetId = WidgetUtil.getId( widget );
    String expected = "wm.setHasListener( wm.findWidgetById( \"" + widgetId + "\" ), \"help\", true );";
    assertTrue( ProtocolTestUtil.getMessageScript().contains( expected ) );
  }

  //////////////////////////////////////////////
  // Tests for new render methods using protocol

  public void testRenderIntialBackgroundNull() {
    WidgetLCAUtil.renderBackground( widget, null );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "background" ) );
  }

  public void testRenderBackground() throws JSONException {
    WidgetLCAUtil.renderBackground( widget, new Color( display, 0, 16, 255 ) );

    Message message = Fixture.getProtocolMessage();
    JSONArray actual = ( JSONArray )message.findSetProperty( widget, "background" );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,16,255,255]", actual ) );
  }

  public void testRenderBackgroundNull() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.setBackground( new Color( display, 0, 16, 255 ) );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderBackground( widget, null, false );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( widget, "background" ) );
  }

  public void testRenderBackgroundUnchanged() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.setBackground( new Color( display, 0, 16, 255 ) );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderBackground( widget, new Color( display, 0, 16, 255 ) );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "background" ) );
  }

  public void testRenderIntialBackgroundTransparent() throws JSONException {
    WidgetLCAUtil.renderBackground( widget, null, true );

    Message message = Fixture.getProtocolMessage();

    JSONArray actual = ( JSONArray )message.findSetProperty( widget, "background" );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,0,0,0]", actual ) );
  }

  public void testRenderBackgroundTransparencyUnchanged() {
    widget = new Button( shell, SWT.CHECK );
    shell.setBackgroundMode( SWT.INHERIT_DEFAULT );
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( widget );
    assertTrue( controlAdapter.getBackgroundTransparency() );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderBackground( widget, null, true );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "background" ) );
  }

  public void testRenderBackgroundNoMoreTransparent() throws JSONException {
    widget = new Button( shell, SWT.CHECK );
    shell.setBackgroundMode( SWT.INHERIT_DEFAULT );
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( widget );
    assertTrue( controlAdapter.getBackgroundTransparency() );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderBackground( widget, new Color( display, 0, 16, 255 ), false );

    Message message = Fixture.getProtocolMessage();

    JSONArray actual = ( JSONArray )message.findSetProperty( widget, "background" );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,16,255,255]", actual ) );
  }

  public void testRenderBackgroundReset() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.setBackground( new Color( display, 0, 16, 255 ) );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderBackground( widget, null );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( widget, "background" ) );
  }

  public void testRenderIntialForeground() {
    ControlLCAUtil.renderForeground( widget );

    Message message = Fixture.getProtocolMessage();

    assertNull( message.findSetOperation( widget, "foreground" ) );
  }

  public void testRenderForeground() throws JSONException {
    widget.setForeground( new Color( display, 0, 16, 255 ) );
    ControlLCAUtil.renderForeground( widget );

    Message message = Fixture.getProtocolMessage();


    JSONArray actual = ( JSONArray )message.findSetProperty( widget, "foreground" );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,16,255,255]", actual ) );
  }

  public void testRenderForegroundUnchanged() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.setForeground( new Color( display, 0, 16, 255 ) );

    Fixture.preserveWidgets();
    ControlLCAUtil.renderForeground( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "foreground" ) );
  }

  public void testRenderInitialCustomVariant() {
    WidgetLCAUtil.renderCustomVariant( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "customVariant" ) );
  }

  public void testRenderCustomVariant() {
    widget.setData( WidgetUtil.CUSTOM_VARIANT, "my_variant" );
    WidgetLCAUtil.renderCustomVariant( widget );

    Message message = Fixture.getProtocolMessage();
    assertEquals( "variant_my_variant", message.findSetProperty( widget, "customVariant" ) );
  }

  public void testRenderCustomVariantUnchanged() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.setData( WidgetUtil.CUSTOM_VARIANT, "my_variant" );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderCustomVariant( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "customVariant" ) );
  }

  public void testRenderInitialListenHelp() {
    WidgetLCAUtil.renderListenHelp( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( widget, "help" ) );
  }

  public void testRenderListenHelp() {
    HelpListener listener = new HelpListener() {
      public void helpRequested( HelpEvent e ) {
      }
    };
    widget.addHelpListener( listener );
    WidgetLCAUtil.renderListenHelp( widget );

    Message message = Fixture.getProtocolMessage();
    assertEquals( Boolean.TRUE, message.findListenProperty( widget, "help" ) );
  }

  public void testRenderListenHelpUnchanged() {
    HelpListener listener = new HelpListener() {
      public void helpRequested( HelpEvent e ) {
      }
    };
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.addHelpListener( listener );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderListenHelp( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findListenOperation( widget, "help" ) );
  }

  public void testRenderListenHelpRemoved() {
    HelpListener listener = new HelpListener() {
      public void helpRequested( HelpEvent e ) {
      }
    };
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    widget.addHelpListener( listener );
    Fixture.preserveWidgets();

    widget.removeHelpListener( listener );
    WidgetLCAUtil.renderListenHelp( widget );

    Message message = Fixture.getProtocolMessage();
    assertEquals( Boolean.FALSE, message.findListenProperty( widget, "help" ) );
  }

  public void testRenderBackgroundGradient() throws JSONException {
    Control control = new Composite( shell, SWT.NONE );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };

    gfxAdapter.setBackgroundGradient( gradientColors, percents, true );
    WidgetLCAUtil.renderBackgroundGradient( control );

    Message message = Fixture.getProtocolMessage();
    JSONArray gradient = ( JSONArray )message.findSetProperty( control, "backgroundGradient" );
    JSONArray colors = ( JSONArray )gradient.get( 0 );
    JSONArray stops = ( JSONArray )gradient.get( 1 );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,255,0,255]", colors.getJSONArray( 0 ) ) );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,0,255,255]", colors.getJSONArray( 1 ) ) );
    assertEquals( new Integer( 0 ), stops.get( 0 ) );
    assertEquals( new Integer( 100 ), stops.get( 1 ) );
    assertEquals( Boolean.TRUE, gradient.get( 2 ) );
  }

  public void testRenderBackgroundGradientHorizontal() throws JSONException {
    Control control = new Composite( shell, SWT.NONE );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };

    gfxAdapter.setBackgroundGradient( gradientColors, percents, false );
    WidgetLCAUtil.renderBackgroundGradient( control );

    Message message = Fixture.getProtocolMessage();
    JSONArray gradient = ( JSONArray )message.findSetProperty( control, "backgroundGradient" );
    JSONArray colors = ( JSONArray )gradient.get( 0 );
    JSONArray stops = ( JSONArray )gradient.get( 1 );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,255,0,255]", colors.getJSONArray( 0 ) ) );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,0,255,255]", colors.getJSONArray( 1 ) ) );
    assertEquals( new Integer( 0 ), stops.get( 0 ) );
    assertEquals( new Integer( 100 ), stops.get( 1 ) );
    assertEquals( Boolean.FALSE, gradient.get( 2 ) );
  }

  public void testRenderBackgroundGradientUnchanged() {
    Control control = new Composite( shell, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( control );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };

    gfxAdapter.setBackgroundGradient( gradientColors, percents, true );
    WidgetLCAUtil.preserveBackgroundGradient( control );
    WidgetLCAUtil.renderBackgroundGradient( control );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( control, "backgroundGradient" ) );
  }

  public void testResetBackgroundGradient() {
    Control control = new Composite( shell, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( control );
    Object adapter = control.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter gfxAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color[] gradientColors = new Color[] {
      Graphics.getColor( 0, 255, 0 ),
      Graphics.getColor( 0, 0, 255 )
    };
    int[] percents = new int[] { 0, 100 };
    gfxAdapter.setBackgroundGradient( gradientColors, percents, true );
    WidgetLCAUtil.preserveBackgroundGradient( control );

    gfxAdapter.setBackgroundGradient( null, null, true );
    WidgetLCAUtil.renderBackgroundGradient( control );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( control, "backgroundGradient" ) );
  }

  public void testRenderRoundedBorder() throws JSONException {
    Widget widget = new Composite( shell, SWT.NONE );
    Object adapter = widget.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter graphicsAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color color = Graphics.getColor( 0, 255, 0 );

    graphicsAdapter.setRoundedBorder( 2, color, 5, 6, 7, 8 );
    WidgetLCAUtil.renderRoundedBorder( widget );

    Message message = Fixture.getProtocolMessage();
    JSONArray border = ( JSONArray )message.findSetProperty( widget, "roundedBorder" );
    assertEquals( 6, border.length() );
    assertEquals( 2, border.getInt( 0 ) );
    assertTrue( ProtocolTestUtil.jsonEquals( "[0,255,0,255]", border.getJSONArray( 1 ) ) );
    assertEquals( 5, border.getInt( 2 ) );
    assertEquals( 6, border.getInt( 3 ) );
    assertEquals( 7, border.getInt( 4 ) );
    assertEquals( 8, border.getInt( 5 ) );
  }

  public void testRenderRoundedBorderUnchanged() {
    Widget widget = new Composite( shell, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    Object adapter = widget.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter graphicsAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color color = Graphics.getColor( 0, 255, 0 );
    graphicsAdapter.setRoundedBorder( 2, color, 5, 6, 7, 8 );

    WidgetLCAUtil.preserveRoundedBorder( widget );
    WidgetLCAUtil.renderRoundedBorder( widget );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( widget, "roundedBorder" ) );
  }

  public void testResetRoundedBorder() {
    Widget widget = new Composite( shell, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    Object adapter = widget.getAdapter( IWidgetGraphicsAdapter.class );
    IWidgetGraphicsAdapter graphicsAdapter = ( IWidgetGraphicsAdapter )adapter;
    Color color = Graphics.getColor( 0, 255, 0 );
    graphicsAdapter.setRoundedBorder( 2, color, 5, 6, 7, 8 );
    WidgetLCAUtil.preserveRoundedBorder( widget );

    graphicsAdapter.setRoundedBorder( 0, null, 0, 0, 0, 0 );
    WidgetLCAUtil.renderRoundedBorder( widget );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( widget, "roundedBorder" ) );
  }

  public void testRenderInitialMenu() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );

    WidgetLCAUtil.renderMenu( widget, widget.getMenu() );

    Message message = Fixture.getProtocolMessage();
    assertEquals( 0, message.getOperationCount() );
  }

  public void testRenderMenu() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    Menu menu = new Menu( widget );
    widget.setMenu( menu );

    WidgetLCAUtil.renderMenu( widget, widget.getMenu() );

    Message message = Fixture.getProtocolMessage();
    assertEquals( WidgetUtil.getId( menu ), message.findSetProperty( widget, "menu" ) );
  }

  public void testRenderMenuReset() {
    Fixture.markInitialized( display );
    Fixture.markInitialized( widget );
    Menu menu = new Menu( widget );
    widget.setMenu( menu );

    Fixture.preserveWidgets();
    WidgetLCAUtil.renderMenu( widget, null );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( widget, "menu" ) );
  }

}
