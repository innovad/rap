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
package org.eclipse.swt.internal.custom.ctabitemkit;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.Message;
import org.eclipse.rap.rwt.testfixture.Message.CreateOperation;
import org.eclipse.rap.rwt.testfixture.Message.DestroyOperation;
import org.eclipse.rap.rwt.testfixture.Message.Operation;
import org.eclipse.rwt.graphics.Graphics;
import org.eclipse.rwt.internal.protocol.ProtocolTestUtil;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import junit.framework.TestCase;


@SuppressWarnings("deprecation")
public class CTabItemLCA_Test extends TestCase {

  private Display display;
  private Shell shell;
  private CTabFolder folder;
  private CTabItemLCA lca;

  protected void setUp() throws Exception {
    Fixture.setUp();
    display = new Display();
    shell = new Shell( display );
    folder = new CTabFolder( shell, SWT.NONE );
    folder.setSize( 150, 150 );
    lca = new CTabItemLCA();
    Fixture.fakeNewRequest( display );
  }

  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testRenderCreate() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.renderInitialization( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertEquals( "rwt.widgets.CTabItem", operation.getType() );
    assertEquals( Integer.valueOf( 0 ), operation.getProperty( "index" ) );
  }

  public void testRenderCreateWithClose() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.CLOSE );

    lca.renderInitialization( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    Object[] styles = operation.getStyles();
    assertTrue( Arrays.asList( styles ).contains( "CLOSE" ) );
  }

  public void testRenderIndex() throws IOException {
    new CTabItem( folder, SWT.NONE );
    new CTabItem( folder, SWT.NONE );
    CTabItem item = new CTabItem( folder, SWT.NONE, 1 );

    lca.renderInitialization( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertEquals( Integer.valueOf( 1 ), operation.getProperty( "index" ) );
  }

  public void testRenderParent() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.renderInitialization( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertEquals( WidgetUtil.getId( item.getParent() ), operation.getParent() );
  }

  public void testRenderDispose() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.renderDispose( item );

    Message message = Fixture.getProtocolMessage();
    Operation operation = message.getOperation( 0 );
    assertTrue( operation instanceof DestroyOperation );
    assertEquals( WidgetUtil.getId( item ), operation.getTarget() );
  }

  public void testRenderInitialToolTip() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "toolTip" ) == -1 );
  }

  public void testRenderToolTip() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    item.setToolTipText( "foo" );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( "foo", message.findSetProperty( item, "toolTip" ) );
  }

  public void testRenderToolTipUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    item.setToolTipText( "foo" );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "toolTip" ) );
  }

  public void testRenderInitialCustomVariant() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "customVariant" ) == -1 );
  }

  public void testRenderCustomVariant() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    item.setData( WidgetUtil.CUSTOM_VARIANT, "blue" );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( "variant_blue", message.findSetProperty( item, "customVariant" ) );
  }

  public void testRenderCustomVariantUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    item.setData( WidgetUtil.CUSTOM_VARIANT, "blue" );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "customVariant" ) );
  }

  public void testRenderInitialBounds() throws IOException, JSONException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    JSONArray bounds = ( JSONArray )message.findCreateProperty( item, "bounds" );
    assertTrue( bounds.getInt( 2 ) > 0 );
    assertTrue( bounds.getInt( 3 ) > 0 );
  }

  public void testRenderBounds() throws IOException, JSONException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    JSONArray bounds = ( JSONArray )message.findSetProperty( item, "bounds" );
    assertTrue( bounds.getInt( 2 ) > 0 );
    assertTrue( bounds.getInt( 3 ) > 0 );
  }

  public void testRenderBoundsUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "bounds" ) );
  }

  public void testRenderInitialFont() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "font" ) == -1 );
  }

  public void testRenderFont() throws IOException, JSONException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    item.setFont( Graphics.getFont( "Arial", 20, SWT.BOLD ) );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    JSONArray actual = ( JSONArray )message.findSetProperty( item, "font" );
    assertTrue( ProtocolTestUtil.jsonEquals( "[\"Arial\"]", actual.getJSONArray( 0 ) ) );
    assertEquals( Integer.valueOf( 20 ), actual.get( 1 ) );
    assertEquals( Boolean.TRUE, actual.get( 2 ) );
    assertEquals( Boolean.FALSE, actual.get( 3 ) );
  }

  public void testRenderFontUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    item.setFont( Graphics.getFont( "Arial", 20, SWT.BOLD ) );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "font" ) );
  }

  public void testRenderInitialText() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "text" ) == -1 );
  }

  public void testRenderText() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    item.setText( "foo" );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( "foo", message.findSetProperty( item, "text" ) );
  }

  public void testRenderTextUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    item.setText( "foo" );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "text" ) );
  }

  public void testRenderInitialImage() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "image" ) );
  }

  public void testRenderImage() throws IOException, JSONException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Image image = Graphics.getImage( Fixture.IMAGE_100x50 );

    item.setImage( image );
    folder.setSelection( item );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    String imageLocation = ImageFactory.getImagePath( image );
    String expected = "[\"" + imageLocation + "\", 100, 50 ]";
    JSONArray actual = ( JSONArray )message.findSetProperty( item, "image" );
    assertTrue( ProtocolTestUtil.jsonEquals( expected, actual ) );
  }

  public void testRenderImageUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );
    Image image = Graphics.getImage( Fixture.IMAGE_100x50 );

    item.setImage( image );
    folder.setSelection( item );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "image" ) );
  }

  public void testRenderImageReset() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );
    Image image = Graphics.getImage( Fixture.IMAGE_100x50 );
    item.setImage( image );
    folder.setSelection( item );

    Fixture.preserveWidgets();
    item.setImage( null );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( JSONObject.NULL, message.findSetProperty( item, "image" ) );
  }

  public void testRenderInitialShowing() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "showing" ) == -1 );
  }

  public void testRenderShowing() throws IOException {
    CTabItem firstItem = new CTabItem( folder, SWT.NONE );
    CTabItem item = new CTabItem( folder, SWT.NONE );

    firstItem.setText( "foo bar foo bar" );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( Boolean.FALSE, message.findSetProperty( item, "showing" ) );
  }

  public void testRenderShowingUnchanged() throws IOException {
    CTabItem firstItem = new CTabItem( folder, SWT.NONE );
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    firstItem.setText( "foo bar foo bar" );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "showing" ) );
  }

  public void testRenderInitialShowClose() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    lca.render( item );

    Message message = Fixture.getProtocolMessage();
    CreateOperation operation = message.findCreateOperation( item );
    assertTrue( operation.getPropertyNames().indexOf( "showClose" ) == -1 );
  }

  public void testRenderShowClose() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );

    item.setShowClose( true );
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertEquals( Boolean.TRUE, message.findSetProperty( item, "showClose" ) );
  }

  public void testRenderShowCloseUnchanged() throws IOException {
    CTabItem item = new CTabItem( folder, SWT.NONE );
    Fixture.markInitialized( display );
    Fixture.markInitialized( item );

    item.setShowClose( true );
    Fixture.preserveWidgets();
    lca.renderChanges( item );

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( item, "showClose" ) );
  }

}
