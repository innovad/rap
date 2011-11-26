/*******************************************************************************
 * Copyright (c) 2002, 2011 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 *    Frank Appel - replaced singletons and static fields (Bug 337787)
 ******************************************************************************/
package org.eclipse.rwt.graphics;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.internal.lifecycle.LifeCycleUtil;
import org.eclipse.rwt.internal.resources.ResourceManagerImpl;
import org.eclipse.rwt.resources.IResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.graphics.FontUtil;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.widgets.Display;


@SuppressWarnings("deprecation")
public class Graphics_Test extends TestCase {

  private Display display;

  public void testGetColorWithNullArgument() {
    try {
      Graphics.getColor( null );
    } catch( IllegalArgumentException expected ) {
    }
  }
  
  public void testGetColorRGBReturnsEqualColorAsGetColor() {
    Color color1 = Graphics.getColor( new RGB( 1, 1, 1 ) );
    Color color2 = Graphics.getColor( 1, 1, 1 );
    assertEquals( color1, color2 );
  }
  
  public void testTextExtentNull() {
    Font font = Graphics.getFont( "Arial", 12, SWT.BOLD );
    try {
      Graphics.textExtent( font, null, 0 );
      fail( "Null string should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
    try {
      Graphics.textExtent( null, "", 0 );
      fail( "Null font should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
  }

  public void testStringExtentNull() {
    Font font = Graphics.getFont( "Arial", 12, SWT.BOLD );
    try {
      Graphics.stringExtent( font, null );
      fail( "Null string should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
    try {
      Graphics.stringExtent( null, "" );
      fail( "Null font should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
  }
  
  public void testGetCharHeightNull() {
    try {
      Graphics.getCharHeight( null );
      fail( "Null font should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
  }
  
  public void testGetAvgCharWidth() {
    float result = Graphics.getAvgCharWidth( display.getSystemFont() );
    assertTrue( result > 0 );
  }

  public void testGetAvgCharWidthNull() {
    try {
      Graphics.getAvgCharWidth( null );
      fail( "Null font should throw IAE" );
    } catch( IllegalArgumentException expected ) {
    }
  }
  
  public void testGetCursor() {
    Cursor cursor = Graphics.getCursor( SWT.CURSOR_ARROW );
    assertSame( display.getSystemCursor( SWT.CURSOR_ARROW ), cursor );
  }

  public void testGetImage() {
    Fixture.useDefaultResourceManager();
    IResourceManager resourceManager = RWT.getResourceManager();
    // only if you comment initial registration in
    // org.eclipse.swt.internal.widgets.displaykit.QooxdooResourcesUtil
    assertFalse( resourceManager.isRegistered( Fixture.IMAGE1 ) );
    Image image1 = Graphics.getImage( Fixture.IMAGE1 );
    String registerPath = getRegisterPath( image1 );
    assertTrue( resourceManager.isRegistered( registerPath ) );
    File contextDir = new File( Fixture.WEB_CONTEXT_DIR, ResourceManagerImpl.RESOURCES );
    assertTrue( new File( contextDir, registerPath ).exists() );
    Image image1a = Graphics.getImage( Fixture.IMAGE1 );
    assertSame( image1, image1a );
    // another picture
    Image image2 = Graphics.getImage( Fixture.IMAGE2 );
    String image2Path = getRegisterPath( image2 );
    assertTrue( resourceManager.isRegistered( image2Path ) );
    assertTrue( new File( contextDir, image2Path ).exists() );
    // ... and do it again...
    Graphics.getImage( Fixture.IMAGE1 );
    assertTrue( resourceManager.isRegistered( registerPath ) );
  }

  public void testGetImageWithClassLoader() throws IOException {
    String resourceName = "test.gif";
    File testGif = new File( Fixture.WEB_CONTEXT_DIR, resourceName );
    Fixture.copyTestResource( Fixture.IMAGE3, testGif );
    ClassLoader classLoader = classLoaderFromFile( Fixture.WEB_CONTEXT_DIR );

    Image image = Graphics.getImage( resourceName, classLoader );
    
    assertNotNull( image );
  }

  public void testGetImageFromClassLoaderWithNonExistingPath() {
    try {
      Graphics.getImage( "test.gif" );
      fail();
    } catch( SWTException expected ) {
    }
  }

  public void testGetImageWithInputStream() throws IOException {
    String imageName = "testIS.gif";
    File testGif = new File( Fixture.WEB_CONTEXT_DIR, imageName );
    Fixture.copyTestResource( Fixture.IMAGE3, testGif );
    URL[] urls = new URL[] { Fixture.WEB_CONTEXT_DIR.toURI().toURL() };
    URLClassLoader classLoader = new URLClassLoader( urls, null );
    assertFalse( RWT.getResourceManager().isRegistered( Fixture.IMAGE3 ) );
    try {
      Graphics.getImage( imageName );
      fail( "Image not available on the classpath." );
    } catch( SWTException expected ) {
    }
    InputStream is = classLoader.getResourceAsStream( imageName );
    Image image = Graphics.getImage( "test.gif", is );
    is.close();
    assertNotNull( image );
  }

  public void testGetImageWithIllegalArguments() {
    try {
      Graphics.getImage( null );
      fail( "Image#find must not allow null-argument" );
    } catch( IllegalArgumentException expected ) {
    }
    try {
      Graphics.getImage( "" );
      fail( "Image#find must not allow empty string argument" );
    } catch( IllegalArgumentException expected ) {
    }
    try {
      Graphics.getImage( "", new ByteArrayInputStream( new byte[ 1 ] ) );
      fail( "Image#find must not allow empty string argument" );
    } catch( IllegalArgumentException expected ) {
    }
  }

  public void testGetFont() {
    Font font = Graphics.getFont( "roman", 1, SWT.NORMAL );
    assertEquals( "roman", FontUtil.getData( font ).getName() );
    assertEquals( 1, FontUtil.getData( font ).getHeight() );
    assertEquals( SWT.NORMAL, FontUtil.getData( font ).getStyle() );
    Font sameFont = Graphics.getFont( "roman", 1, SWT.NORMAL );
    assertSame( font, sameFont );
    Font otherFont = Graphics.getFont( "arial", 2, SWT.NORMAL );
    assertNotSame( otherFont, font );
    Font boldFont = Graphics.getFont( "arial", 11, SWT.BOLD );
    assertEquals( SWT.BOLD, FontUtil.getData( boldFont ).getStyle() );
    Font italicFont = Graphics.getFont( "arial", 11, SWT.ITALIC );
    assertEquals( SWT.ITALIC, FontUtil.getData( italicFont ).getStyle() );
    sameFont = Graphics.getFont( new FontData( "roman", 1, SWT.NORMAL ) );
    assertSame( font, sameFont );
    Font arial13Normal = Graphics.getFont( "arial", 13, SWT.NORMAL );
    Font arial12Bold = Graphics.getFont( "arial", 12, SWT.BOLD );
    assertNotSame( arial13Normal, arial12Bold );
  }

  public void testGetFontReturnsCurrentDisplay() {
    Font font = Graphics.getFont( "roman", 1, SWT.NORMAL );
    assertSame( Display.getCurrent(), font.getDevice() );
  }

  public void testGetFontWithIllegalArguments() {
    try {
      Graphics.getFont( null, 1, SWT.NONE );
      fail( "The font name must not be null" );
    } catch( IllegalArgumentException expected ) {
    }
    try {
      Graphics.getFont( "abc", -1, SWT.NONE );
      fail( "The font size must not be negative" );
    } catch( IllegalArgumentException expected ) {
    }
    Font font = Graphics.getFont( "roman", 1, 1 << 3 );
    assertEquals( SWT.NORMAL, FontUtil.getData( font ).getStyle() );
  }

  public void testDisposeFactoryCreated() {
    Font font = Graphics.getFont( "roman", 1, SWT.NORMAL );
    try {
      font.dispose();
      fail( "It is not allowed to dispose of a factory-created color" );
    } catch( IllegalStateException e ) {
      assertFalse( font.isDisposed() );
    }
  }
  
  public void testCheckThreadFromUIThread() {
    try {
      Graphics.checkThread();
    } catch( Throwable notExpected ) {
      fail();
    }
  }

  public void testCheckThreadWithoutDisplay() {
    display.dispose();
    LifeCycleUtil.setSessionDisplay( null );
    try {
      Graphics.checkThread();
      fail();
    } catch( SWTException expected ) {
      assertEquals( SWT.ERROR_THREAD_INVALID_ACCESS, expected.code );
    }
  }
  
  public void testCheckThreadFromBackgroundThread() throws InterruptedException {
    final Throwable[] exception = { null };
    Thread thread = new Thread( new Runnable() {
      public void run() {
        try {
          Graphics.checkThread();
        } catch( Throwable expected ) {
          exception[ 0 ] = expected;
        }
      }
    } );
    thread.start();
    thread.join();
    assertTrue( exception[ 0 ] instanceof SWTException );
    SWTException swtException = ( SWTException )exception[ 0 ];
    assertEquals( SWT.ERROR_THREAD_INVALID_ACCESS, swtException.code );
  }

  protected void setUp() {
    Fixture.createApplicationContext();
    Fixture.createServiceContext();
    display = new Display();
  }

  protected void tearDown() {
    Fixture.disposeOfServiceContext();
    Fixture.disposeOfApplicationContext();
  }

  private static String getRegisterPath( Image image ) {
    String imagePath = ImageFactory.getImagePath( image );
    int prefixLength = ResourceManagerImpl.RESOURCES.length() + 1;
    return imagePath.substring( prefixLength );
  }

  private static ClassLoader classLoaderFromFile( File webContextDir ) throws IOException {
    URL[] urls = new URL[] { webContextDir.toURI().toURL() };
    return new URLClassLoader( urls, null );
  }
}
