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
package org.eclipse.rwt.internal.protocol;

import junit.framework.TestCase;

import org.json.*;


public class ProtocolTestUtil_Test extends TestCase {
  
  public void testJsonEqualsEmptyArray() throws JSONException {
    assertTrue( ProtocolTestUtil.jsonEquals( "[]", new JSONArray()) );
  }
  
  public void testJsonEqualsNullArray() throws JSONException {
    JSONArray nullJsonArr = new JSONArray();
    nullJsonArr.put( JSONObject.NULL );
    JSONArray jsonArr = new JSONArray();
    jsonArr.put( new Integer( 2 ) );
    
    assertFalse( ProtocolTestUtil.jsonEquals( "[]", nullJsonArr ) );
    assertTrue( ProtocolTestUtil.jsonEquals( "[ null ]", nullJsonArr ) );
    assertFalse( ProtocolTestUtil.jsonEquals( "[ null ]", jsonArr ) );
  }
  
  public void testJsonEqualsArrayWithValue() throws JSONException {
    JSONArray jsonArr = new JSONArray();
    jsonArr.put( new Integer( 2 ) );
    
    assertTrue( ProtocolTestUtil.jsonEquals( "[ 2 ]", jsonArr ) );
    assertFalse( ProtocolTestUtil.jsonEquals( "[ 3 ]", jsonArr ) );
  }
  
}
