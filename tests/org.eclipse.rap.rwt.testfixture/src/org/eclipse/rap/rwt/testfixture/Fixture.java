/*******************************************************************************
 * Copyright (c) 2002, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing implementation
 *    Frank Appel - replaced singletons and static fields (Bug 337787)
 ******************************************************************************/
package org.eclipse.rap.rwt.testfixture;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.rap.rwt.testfixture.internal.engine.ThemeManagerHelper;
import org.eclipse.rwt.application.Application;
import org.eclipse.rwt.application.Application.OperationMode;
import org.eclipse.rwt.application.ApplicationConfiguration;
import org.eclipse.rwt.engine.RWTServletContextListener;
import org.eclipse.rwt.internal.application.ApplicationContextHelper;
import org.eclipse.rwt.internal.application.ApplicationContextUtil;
import org.eclipse.rwt.internal.application.RWTFactory;
import org.eclipse.rwt.internal.lifecycle.CurrentPhase;
import org.eclipse.rwt.internal.lifecycle.DisplayUtil;
import org.eclipse.rwt.internal.lifecycle.IDisplayLifeCycleAdapter;
import org.eclipse.rwt.internal.lifecycle.IUIThreadHolder;
import org.eclipse.rwt.internal.lifecycle.LifeCycleUtil;
import org.eclipse.rwt.internal.lifecycle.RWTLifeCycle;
import org.eclipse.rwt.internal.protocol.ProtocolMessageWriter;
import org.eclipse.rwt.internal.resources.ResourceManagerImpl;
import org.eclipse.rwt.internal.resources.SystemProps;
import org.eclipse.rwt.internal.service.ContextProvider;
import org.eclipse.rwt.internal.service.RequestParams;
import org.eclipse.rwt.internal.service.ServiceContext;
import org.eclipse.rwt.internal.service.ServiceStore;
import org.eclipse.rwt.internal.util.HTTP;
import org.eclipse.rwt.lifecycle.AbstractWidgetLCA;
import org.eclipse.rwt.lifecycle.IWidgetAdapter;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.rwt.service.IServiceStore;
import org.eclipse.rwt.service.ISessionStore;
import org.eclipse.swt.internal.widgets.IDisplayAdapter;
import org.eclipse.swt.internal.widgets.WidgetAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;


/**
 * Test fixture for RWT.
 * <p>
 * <strong>IMPORTANT:</strong> This class is <em>not</em> part the public RAP
 * API. It may change or disappear without further notice. Use this class at
 * your own risk.
 * </p>
 */
public final class Fixture {

  public final static File TEMP_DIR = createTempDir();
  public static final File WEB_CONTEXT_DIR = new File( TEMP_DIR, "testapp" );
  public static final File WEB_CONTEXT_RWT_RESOURCES_DIR
    = new File( WEB_CONTEXT_DIR, ResourceManagerImpl.RESOURCES );
  public static final String IMAGE1 = "resources/images/image1.gif";
  public static final String IMAGE2 = "resources/images/image2.gif";
  public static final String IMAGE3 = "resources/images/image3.gif";
  public static final String IMAGE_100x50 = "resources/images/test-100x50.png";
  public static final String IMAGE_50x100 = "resources/images/test-50x100.png";

  private static final String SYS_PROP_USE_PERFORMANCE_OPTIMIZATIONS
    = "usePerformanceOptimizations";

  static {
    ThemeManagerHelper.replaceStandardResourceLoader();
    setSkipResourceRegistration( isPerformanceOptimizationsEnabled() );
    setSkipResourceDeletion( isPerformanceOptimizationsEnabled() );
  }

  private static ServletContext servletContext;
  private static RWTServletContextListener rwtServletContextListener;

  public static class FixtureApplicationConfigurator implements ApplicationConfiguration {
    public void configure( Application application ) {
      application.setOperationMode( OperationMode.SWT_COMPATIBILITY );
    }
  }

  ////////////////////////////////////////////
  // Methods to control global servlet context

  public static ServletContext createServletContext() {
    servletContext = new TestServletContext();
    Fixture.useTestResourceManager();
    return getServletContext();
  }

  public static ServletContext getServletContext() {
    return servletContext;
  }

  public static void disposeOfServletContext() {
    servletContext = null;
  }

  public static void setInitParameter( String name, String value ) {
    ensureServletContext();
    servletContext.setInitParameter( name, value );
  }

  public static void triggerServletContextInitialized() {
    ensureServletContext();
    registerConfigurer();
    rwtServletContextListener = new RWTServletContextListener();
    ServletContextEvent event = new ServletContextEvent( servletContext );
    rwtServletContextListener.contextInitialized( event );
  }

  public static void triggerServletContextDestroyed() {
    ServletContextEvent event = new ServletContextEvent( servletContext );
    if( rwtServletContextListener != null ) {
      rwtServletContextListener.contextDestroyed( event );
    }
    rwtServletContextListener = null;
  }


  ////////////////////////////////////////
  // Methods to control ApplicationContext

  public static void createApplicationContext() {
    ensureServletContext();
    createWebContextDirectory();
    triggerServletContextInitialized();
  }

  public static void disposeOfApplicationContext() {
    triggerServletContextDestroyed();
    disposeOfServletContext();
    // TODO [ApplicationContext]: At the time beeing this improves RWTAllTestSuite performance by
    //      50% on my machine without causing any test to fail. However this has a bad smell
    //      with it, so I introduced a flag that can be switch on for fast tests on local machines
    //      and switched of for the integration build tests. Think about a less intrusive solution.
    if( !isPerformanceOptimizationsEnabled() ) {
      deleteWebContextDirectory();
    }
  }


  ////////////////////////////////////
  // Methods to control ServiceContext

  public static void createServiceContext() {
    TestRequest request = new TestRequest();
    TestResponse response = new TestResponse();
    HttpSession session = createTestSession();
    request.setSession( session );
    createServiceContext( response, request );
  }

  public static void createServiceContext( HttpServletResponse response,
                                           HttpServletRequest request )
  {
    ServiceContext context = new ServiceContext( request, response );
    ServiceStore serviceStore = new ServiceStore();
    context.setServiceStore( serviceStore );
    ContextProvider.setContext( context );
  }

  private static TestSession createTestSession() {
    TestSession result = new TestSession();
    if( servletContext != null ) {
      result.setServletContext( servletContext );
    }
    return result;
  }

  public static void disposeOfServiceContext() {
    ThemeManagerHelper.resetThemeManagerIfNeeded();
    HttpSession session = ContextProvider.getRequest().getSession();
    ContextProvider.disposeContext();
    session.invalidate();
  }


  /////////////////////////////////////////////////////////////////////
  // Methods to control web context directories and resource management

  public static void createWebContextDirectory() {
    WEB_CONTEXT_DIR.mkdirs();
  }

  public static void deleteWebContextDirectory() {
    if( WEB_CONTEXT_DIR.exists() ) {
      delete( WEB_CONTEXT_DIR );
    }
  }


  //////////////////////////////
  // general setup and tear down

  public static void setUp() {
    useTestResourceManager();
    setSystemProperties();
    createApplicationContext();
    createServiceContext();
  }

  public static void useDefaultResourceManager() {
    ApplicationContextHelper.useDefaultResourceManager();
  }

  public static void useTestResourceManager() {
    ApplicationContextHelper.useTestResourceManager();
  }

  public static void tearDown() {
    disposeOfServiceContext();
    disposeOfApplicationContext();
    disposeOfServletContext();
    unsetSystemProperties();
    useDefaultResourceManager();
  }


  ////////////////////
  // LifeCycle helpers

  public static void readDataAndProcessAction( Display display ) {
    IDisplayLifeCycleAdapter displayLCA = DisplayUtil.getLCA( display );
    fakePhase( PhaseId.READ_DATA );
    displayLCA.readData( display );
    Fixture.preserveWidgets();
    fakePhase( PhaseId.PROCESS_ACTION );
    while( Display.getCurrent().readAndDispatch() ) {
    }
  }

  public static void readDataAndProcessAction( Widget widget ) {
    AbstractWidgetLCA widgetLCA = WidgetUtil.getLCA( widget );
    fakePhase( PhaseId.READ_DATA );
    widgetLCA.readData( widget );
    fakePhase( PhaseId.PROCESS_ACTION );
    while( Display.getCurrent().readAndDispatch() ) {
    }
  }

  public static void markInitialized( Widget widget ) {
    Object adapter = widget.getAdapter( IWidgetAdapter.class );
    WidgetAdapter widgetAdapter = ( WidgetAdapter )adapter;
    widgetAdapter.setInitialized( true );
  }

  public static void markInitialized( Display display ) {
    Object adapter = display.getAdapter( IWidgetAdapter.class );
    WidgetAdapter widgetAdapter = ( WidgetAdapter )adapter;
    widgetAdapter.setInitialized( true );
  }

  public static void preserveWidgets() {
    Display display = LifeCycleUtil.getSessionDisplay();
    IDisplayLifeCycleAdapter displayLCA = DisplayUtil.getLCA( display );
    PhaseId bufferedPhaseId = CurrentPhase.get();
    fakePhase( PhaseId.READ_DATA );
    displayLCA.preserveValues( display );
    fakePhase( bufferedPhaseId );
  }

  public static void clearPreserved() {
    Display display = LifeCycleUtil.getSessionDisplay();
    IDisplayLifeCycleAdapter displayLCA = DisplayUtil.getLCA( display );
    PhaseId bufferedPhaseId = CurrentPhase.get();
    fakePhase( PhaseId.RENDER );
    displayLCA.clearPreserved( display );
    fakePhase( bufferedPhaseId );
  }

  public static Message getProtocolMessage() {
    TestResponse response = ( TestResponse )ContextProvider.getResponse();
    finishResponse( response );
    return new Message( response.getContent() );
  }

  private static void finishResponse( TestResponse response ) {
    if( response.getContent().length() == 0 ) {
      ProtocolMessageWriter protocolWriter = ContextProvider.getProtocolWriter();
      try {
        response.getWriter().write( protocolWriter.createMessage() );
      } catch( IOException exception ) {
        throw new IllegalStateException( "Failed to get response writer", exception );
      }
    }
  }

  public static void fakeNewRequest( Display display ) {
    fakeNewRequest();
    fakeRequestParam( RequestParams.UIROOT, DisplayUtil.getId( display ) );
  }

  public static void fakeNewRequest() {
    fakeNewRequest( HTTP.METHOD_POST );
  }

  public static void fakeNewGetRequest() {
    fakeNewRequest( HTTP.METHOD_GET );
  }

  private static void fakeNewRequest( String method ) {
    HttpSession session = ContextProvider.getRequest().getSession();
    TestRequest request = new TestRequest();
    request.setSession( session );
    request.setMethod( method );
    TestResponse response = new TestResponse();
    ServiceContext serviceContext = new ServiceContext( request, response );
    serviceContext.setServiceStore( new ServiceStore() );
    ContextProvider.disposeContext();
    ContextProvider.setContext( serviceContext );
    fakeResponseWriter();
  }

  public static void fakeRequestParam( String key, String value ) {
    TestRequest request = ( TestRequest )ContextProvider.getRequest();
    request.setParameter( key, value );
  }

  public static void fakeResponseWriter() {
    TestResponse testResponse = ( TestResponse )ContextProvider.getResponse();
    testResponse.clearContent();
    ContextProvider.getContext().resetProtocolWriter();
  }

  public static void fakePhase( PhaseId phase ) {
    IServiceStore serviceStore = ContextProvider.getServiceStore();
    serviceStore.setAttribute( CurrentPhase.class.getName() + "#value", phase );
  }

  public static void executeLifeCycleFromServerThread() {
    IUIThreadHolder threadHolder = registerCurrentThreadAsUIThreadHolder();
    Thread serverThread = fakeRequestThread( threadHolder );
    simulateRequest( threadHolder, serverThread );
    RWTLifeCycle lifeCycle = ( RWTLifeCycle )RWTFactory.getLifeCycleFactory().getLifeCycle();
    while( LifeCycleUtil.getSessionDisplay().readAndDispatch() ) {
    }
    lifeCycle.sleep();
  }

  public static void replaceServiceStore( IServiceStore serviceStore ) {
    HttpServletRequest request = ContextProvider.getRequest();
    HttpServletResponse response = ContextProvider.getResponse();
    ServiceContext context = new ServiceContext( request, response );
    if( serviceStore != null ) {
      context.setServiceStore( serviceStore );
    }
    ContextProvider.disposeContext();
    ContextProvider.setContext( context );
  }

  ////////////////
  // general stuff

  public static void setSkipResourceRegistration( boolean skip ) {
    ApplicationContextHelper.setSkipResoureRegistration( skip );
  }

  public static void resetSkipResourceRegistration() {
    ApplicationContextHelper.setSkipResoureRegistration( isPerformanceOptimizationsEnabled() );
  }

  public static void setSkipResourceDeletion( boolean skip ) {
    ApplicationContextHelper.setSkipResoureDeletion( skip );
  }

  public static void resetSkipResourceDeletion() {
    ApplicationContextHelper.setSkipResoureDeletion( isPerformanceOptimizationsEnabled() );
  }

  public static void copyTestResource( String resourceName, File destination ) throws IOException {
    ClassLoader loader = Fixture.class.getClassLoader();
    InputStream is = loader.getResourceAsStream( resourceName );
    if( is == null ) {
      throw new IllegalArgumentException( "Resource could not be found: " + resourceName );
    }
    BufferedInputStream bis = new BufferedInputStream( is );
    try {
      OutputStream out = new FileOutputStream( destination );
      BufferedOutputStream bout = new BufferedOutputStream( out );
      try {
        int c = bis.read();
        while( c != -1 ) {
          bout.write( c );
          c = bis.read();
        }
      } finally {
        bout.close();
      }
    } finally {
      bis.close();
    }
  }

  public static void unsetSystemProperties() {
    System.getProperties().remove( SystemProps.USE_VERSIONED_JAVA_SCRIPT );
    System.getProperties().remove( SystemProps.CLIENT_LIBRARY_VARIANT );
  }

  public static void setSystemProperties() {
    // disable js-versioning by default to make comparison easier
    System.setProperty( SystemProps.USE_VERSIONED_JAVA_SCRIPT, "false" );
  }

  public static void runInThread( final Runnable runnable ) throws Throwable {
    final Object lock = new Object();
    final Throwable[] exception = { null };
    Runnable exceptionGuard = new Runnable() {
      public void run() {
        try {
          runnable.run();
        } catch( Throwable thr ) {
          synchronized( lock ) {
            exception[ 0 ] = thr;
          }
        }
      }
    };
    Thread thread = new Thread( exceptionGuard );
    thread.setDaemon( true );
    thread.start();
    thread.join();
    synchronized( lock ) {
      if( exception[ 0 ] != null ) {
        throw exception[ 0 ];
      }
    }
  }

  public static Thread[] startThreads( int threadCount, Runnable runnable ) {
    List<Thread> threads = new ArrayList<Thread>();
    for( int i = 0; i < threadCount; i++ ) {
      Thread thread = new Thread( runnable );
      thread.setDaemon( true );
      thread.start();
      threads.add( thread );
      Thread.yield();
    }
    Thread[] result = new Thread[ threads.size() ];
    threads.toArray( result );
    return result;
  }

  public static void joinThreads( Thread[] threads ) throws InterruptedException {
    for( int i = 0; i < threads.length; i++ ) {
      Thread thread = threads[ i ];
      thread.join();
    }
  }

  public static void delete( File toDelete ) {
    ApplicationContextUtil.delete( toDelete );
  }

  public static byte[] serialize( Object object ) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream( outputStream );
    objectOutputStream.writeObject( object );
    return outputStream.toByteArray();
  }

  public static Object deserialize( byte[] bytes ) throws IOException, ClassNotFoundException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream( bytes );
    ObjectInputStream objectInputStream = new ObjectInputStream( inputStream );
    return objectInputStream.readObject();
  }

  @SuppressWarnings("unchecked")
  public static <T> T serializeAndDeserialize( T instance ) throws Exception {
    byte[] bytes = serialize( instance );
    return ( T )deserialize( bytes );
  }

  @SuppressWarnings("unchecked")
  public static <T extends Widget> T serializeAndDeserialize( T instance ) throws Exception {
    byte[] bytes = serialize( instance );
    T result = ( T )deserialize( bytes );
    Object adapter = result.getDisplay().getAdapter( IDisplayAdapter.class );
    IDisplayAdapter displayAdapter = ( IDisplayAdapter )adapter;
    displayAdapter.attachThread();
    return result;
  }

  private static void ensureServletContext() {
    if( servletContext == null ) {
      createServletContext();
    }
  }

  private static void registerConfigurer() {
    setInitParameter( ApplicationConfiguration.CONFIGURATION_PARAM,
                      FixtureApplicationConfigurator.class.getName() );
  }

  private static void simulateRequest( IUIThreadHolder threadHolder, Thread serverThread ) {
    RWTLifeCycle lifeCycle = ( RWTLifeCycle )RWTFactory.getLifeCycleFactory().getLifeCycle();
    synchronized( threadHolder.getLock() ) {
      serverThread.start();
      try {
        lifeCycle.sleep();
      } catch( ThreadDeath e ) {
        throw new RuntimeException( e );
      }
    }
  }

  private static Thread fakeRequestThread( final IUIThreadHolder threadHolder ) {
    final RWTLifeCycle lifeCycle = ( RWTLifeCycle )RWTFactory.getLifeCycleFactory().getLifeCycle();
    final ServiceContext context = ContextProvider.getContext();
    Thread result = new Thread( new Runnable() {
      public void run() {
        synchronized( threadHolder.getLock() ) {
          ContextProvider.setContext( context );
          try {
            try {
              lifeCycle.execute();
              lifeCycle.setPhaseOrder( null );
            } catch( IOException e ) {
              throw new RuntimeException( e );
            }
          } finally {
            ContextProvider.releaseContextHolder();
            threadHolder.notifyAll();
          }
        }
      }
    }, "ServerThread" );
    return result;
  }

  private static IUIThreadHolder registerCurrentThreadAsUIThreadHolder() {
    final IUIThreadHolder result = new IUIThreadHolder() {
      private final Thread thread = Thread.currentThread();

      public void setServiceContext( ServiceContext serviceContext ) {
      }
      public void switchThread() {
        synchronized( getLock() ) {
          notifyAll();
          try {
            wait();
          } catch( InterruptedException e ) {
            throw new RuntimeException( e );
          }
        }
      }
      public void updateServiceContext() {
      }
      public void terminateThread() {
      }
      public Thread getThread() {
        return thread;
      }
      public Object getLock() {
        return this;
      }
    };
    ISessionStore sessionStore = ContextProvider.getSessionStore();
    LifeCycleUtil.setUIThread( sessionStore, result );
    return result;
  }

  ////////////////
  // general stuff

  private static boolean isPerformanceOptimizationsEnabled() {
    return Boolean.getBoolean( SYS_PROP_USE_PERFORMANCE_OPTIMIZATIONS );
  }

  private static File createTempDir() {
    File globalTmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
    String subDirName = "rap-test-" + Long.toHexString( System.currentTimeMillis() );
    File tmpDir = new File( globalTmpDir, subDirName );
    if( !tmpDir.mkdir() ) {
      String message = "Failed to create temp directory: " + tmpDir.getAbsolutePath();
      throw new IllegalStateException( message );
    }
    return tmpDir;
  }

  private Fixture() {
    // prevent instantiation
  }
}
