/*******************************************************************************
 * Copyright (c) 2011, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rwt.internal;

import java.util.*;

import org.eclipse.rwt.internal.util.*;
import org.eclipse.rwt.internal.util.SharedInstanceBuffer.IInstanceCreator;
import org.eclipse.rwt.service.ISessionStore;
import org.eclipse.swt.internal.SerializableCompatibility;


public class SingletonManager implements SerializableCompatibility {

  private static final String ATTR_SINGLETON_MANAGER
    = SingletonManager.class.getName() + "#instance";

  public static void install( ISessionStore sessionStore ) {
    checkNotInstalled( sessionStore );
    sessionStore.setAttribute( ATTR_SINGLETON_MANAGER, new SingletonManager() );
  }

  public static SingletonManager getInstance( ISessionStore sessionStore ) {
    return ( SingletonManager )sessionStore.getAttribute( ATTR_SINGLETON_MANAGER );
  }

  private final Map<Class,Object> singletons;
  private transient SharedInstanceBuffer<Class,Object> typeLocks;

  private SingletonManager() {
    singletons = Collections.synchronizedMap( new HashMap<Class,Object>() );
    initialize();
  }

  private void initialize() {
    typeLocks = new SharedInstanceBuffer<Class,Object>();
  }

  @SuppressWarnings("unchecked")
  public <T> T getSingleton( Class<T> type ) {
    synchronized( getTypeLock( type ) ) {
      T result = ( T )singletons.get( type );
      if( result == null ) {
        result = ClassUtil.newInstance( type );
        singletons.put( type, result );
      }
      return result;
    }
  }

  private Object getTypeLock( Class type ) {
    Object result = typeLocks.get( type, new IInstanceCreator<Object>() {
      public Object createInstance() {
        return new Object();
      }
    } );
    return result;
  }

  private static void checkNotInstalled( ISessionStore sessionStore ) {
    if( getInstance( sessionStore ) != null ) {
      String msg = "SingletonManager already installed for session: " + sessionStore.getId();
      throw new IllegalStateException( msg );
    }
  }

  private Object readResolve() {
    initialize();
    return this;
  }
}
