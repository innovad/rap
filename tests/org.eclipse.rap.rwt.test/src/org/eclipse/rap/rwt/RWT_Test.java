/*******************************************************************************
 * Copyright (c) 2010, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.client.Client;
import org.eclipse.rap.rwt.internal.application.ApplicationContextImpl;
import org.eclipse.rap.rwt.internal.application.RWTFactory;
import org.eclipse.rap.rwt.internal.lifecycle.LifeCycle;
import org.eclipse.rap.rwt.internal.service.ContextProvider;
import org.eclipse.rap.rwt.lifecycle.PhaseId;
import org.eclipse.rap.rwt.lifecycle.PhaseListener;
import org.eclipse.rap.rwt.service.ApplicationContext;
import org.eclipse.rap.rwt.service.UISession;
import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.internal.NoOpRunnable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;


public class RWT_Test extends TestCase {

  @Override
  protected void setUp() throws Exception {
    Fixture.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testRequestThreadExecFromBackgroundThread() throws Throwable {
    Runnable runnable = new Runnable() {
      public void run() {
        RWT.requestThreadExec( new NoOpRunnable() );
      }
    };
    try {
      Fixture.runInThread( runnable );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testRequestThreadExec() {
    final Thread[] requestThread = { null };
    Display display = new Display();
    // use asyncExec to run code during executeLifeCycleFromServerThread
    display.asyncExec( new Runnable() {
      public void run() {
        RWT.requestThreadExec( new Runnable() {
          public void run() {
            requestThread[ 0 ] = Thread.currentThread();
          }
        } );
      }
    } );
    Fixture.fakeNewRequest();
    Fixture.executeLifeCycleFromServerThread();
    assertNotNull( requestThread[ 0 ] );
  }

  public void testRequestThreadExecWithoutDisplay() {
    Runnable runnable = new NoOpRunnable();
    try {
      RWT.requestThreadExec( runnable );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testRequestThreadExecWithDisposedDisplay() {
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    Display display = new Display();
    display.dispose();
    Runnable runnable = new NoOpRunnable();
    try {
      RWT.requestThreadExec( runnable );
      fail();
    } catch( SWTException expected ) {
      assertEquals( SWT.ERROR_DEVICE_DISPOSED, expected.code );
    }
  }

  public void testRequestThreadExecWithNullRunnable() {
    new Display();
    try {
      RWT.requestThreadExec( null );
      fail();
    } catch( NullPointerException expected ) {
    }
  }

  public void testRequestThreadExecDelegatesToLifeCycle() {
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    RWTFactory.getLifeCycleFactory().configure( TestLifeCycle.class );
    RWTFactory.getLifeCycleFactory().activate();
    new Display();

    RWT.requestThreadExec( new NoOpRunnable() );

    TestLifeCycle lifeCycle = ( TestLifeCycle )RWTFactory.getLifeCycleFactory().getLifeCycle();
    assertEquals( TestLifeCycle.REQUEST_THREAD_EXEC, lifeCycle.getInvocationLog() );
  }

  public void testGetRequestFromBackgroundThread() throws Throwable {
    Runnable runnable = new Runnable() {
      public void run() {
        RWT.getRequest();
      }
    };

    try {
      Fixture.runInThread( runnable );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testGetResponseFromBackgroundThread() throws Throwable {
    Runnable runnable = new Runnable() {
      public void run() {
        RWT.getResponse();
      }
    };

    try {
      Fixture.runInThread( runnable );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  @SuppressWarnings( "deprecation" )
  public void testGetServiceStore_failsFromBackgroundThread() throws Throwable {
    Runnable runnable = new Runnable() {
      public void run() {
        RWT.getServiceStore();
      }
    };

    try {
      Fixture.runInThread( runnable );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  @SuppressWarnings( "deprecation" )
  public void testGetServiceStoreFromSessionThread() throws Throwable {
    final Display display = new Display();
    final Runnable runnable = new Runnable() {
      public void run() {
        RWT.getServiceStore();
      }
    };

    try {
      Fixture.runInThread( new Runnable() {
        public void run() {
          RWT.getUISession( display ).exec( runnable );
        }
      } );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testGetApplicationContext() {
    ApplicationContext context = RWT.getApplicationContext();

    ApplicationContext result = RWTFactory.getApplicationContext();

    assertSame( context, result );
  }

  public void testGetApplicationContext_failsInBackgroundThread() throws Throwable {
    try {
      Fixture.runInThread( new Runnable() {
        public void run() {
          RWT.getApplicationContext();
        }
      } );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testGetApplicationContext_succeedsInBackgroundThreadWithContext() throws Throwable {
    final AtomicReference<ApplicationContext> result = new AtomicReference<ApplicationContext>();
    ApplicationContext applicationContext = RWT.getApplicationContext();
    final UISession currentUISession = RWT.getUISession();

    Fixture.runInThread( new Runnable() {
      public void run() {
        Fixture.createServiceContext();
        ContextProvider.getContext().setUISession( currentUISession );
        result.set( RWT.getApplicationContext() );
      }
    } );

    assertSame( applicationContext, result.get() );
  }

  public void testGetUISession() {
    UISession result = RWT.getUISession();

    assertSame( ContextProvider.getUISession(), result );
  }

  public void testGetUISession_failsInBackgroundThread() throws Throwable {
    try {
      Fixture.runInThread( new Runnable() {
        public void run() {
          RWT.getUISession();
        }
      } );
      fail();
    } catch( IllegalStateException exception ) {
      assertEquals( "Invalid thread access", exception.getMessage() );
    }
  }

  public void testGetUISession_succeedsInBackgroundThreadWithContext() throws Throwable {
    final AtomicReference<UISession> result = new AtomicReference<UISession>();
    final UISession currentUISession = RWT.getUISession();

    Fixture.runInThread( new Runnable() {
      public void run() {
        Fixture.createServiceContext();
        ContextProvider.getContext().setUISession( currentUISession );
        result.set( RWT.getUISession() );
      }
    } );

    assertSame( currentUISession, result.get() );
  }

  public void testGetUISessionForDisplay() {
    Display display = new Display();

    UISession result = RWT.getUISession( display );

    assertSame( RWT.getUISession(), result );
  }

  public void testGetUISessionForDisplay_failsWithNullArgument() {
    try {
      RWT.getUISession( null );
      fail();
    } catch( NullPointerException exception ) {
      assertTrue( exception.getMessage().contains( "display" ) );
    }
  }

  public void testGetUISessionForDisplay_fromBackgroundThread() throws Throwable {
    final AtomicReference<UISession> result = new AtomicReference<UISession>();
    final Display display = new Display();

    Fixture.runInThread( new Runnable() {
      public void run() {
        result.set( RWT.getUISession( display ) );
      }
    } );

    assertSame( RWT.getUISession(), result.get() );
  }

  public void testGetUISessionForDisplay_alsoWorksWhenDisplayIsDisposed() {
    final Display display = new Display();
    display.dispose();

    UISession result = RWT.getUISession( display );

    assertSame( RWT.getUISession(), result );
  }

  public void testGetClient() {
    Client client = RWT.getClient();

    assertNotNull( client );
  }

  public void testGetLocale_getsLocaleFromUISession() {
    ContextProvider.getUISession().setLocale( Locale.ITALY );

    Locale result = RWT.getLocale();

    assertSame( Locale.ITALY, result );
  }

  public void testSetLocale_setsLocaleOnUISession() {
    RWT.setLocale( Locale.ITALY );

    Locale result = ContextProvider.getUISession().getLocale();

    assertSame( Locale.ITALY, result );
  }

  private static class TestLifeCycle extends LifeCycle {
    static final String REQUEST_THREAD_EXEC = "requestThreadExec";

    private String invocationLog = "";

    public TestLifeCycle( ApplicationContextImpl applicationContext ) {
      super( applicationContext );
    }

    @Override
    public void execute() throws IOException {
    }

    @Override
    public void requestThreadExec( Runnable runnable ) {
      invocationLog += REQUEST_THREAD_EXEC;
    }

    @Override
    public void addPhaseListener( PhaseListener phaseListener ) {
    }

    @Override
    public void removePhaseListener( PhaseListener phaseListener ) {
    }

    @Override
    public void sleep() {
    }

    String getInvocationLog() {
      return invocationLog;
    }
  }
}