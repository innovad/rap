/*******************************************************************************
 * Copyright (c) 2009, 2011 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.displaykit;

import java.util.ArrayList;
import java.util.Date;

import junit.framework.TestCase;

import org.eclipse.rap.rwt.testfixture.Fixture;
import org.eclipse.rap.rwt.testfixture.Message;
import org.eclipse.rap.rwt.testfixture.Message.CallOperation;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.widgets.*;
import org.json.JSONArray;
import org.json.JSONException;


public class DNDSupport_Test extends TestCase {

  private Display display;
  private Shell shell;
  private java.util.List<Object> events;

  protected void setUp() throws Exception {
    Fixture.setUp();
    Fixture.fakePhase( PhaseId.PROCESS_ACTION );
    display = new Display();
    shell = new Shell( display );
    events = new ArrayList<Object>();
  }

  protected void tearDown() throws Exception {
    Fixture.tearDown();
  }

  public void testCancelAfterDragDetectAndStartEvent() {
    shell.setLocation( 5, 5 );
    Control sourceControl = new Label( shell, SWT.NONE );
    sourceControl.setLocation( 10, 20 );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    Control targetControl = new Label( shell, SWT.NONE );
    new DropTarget( targetControl, DND.DROP_MOVE );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragStart( DragSourceEvent event ) {
        events.add( event );
        event.doit = false;
      }
    } );
    sourceControl.addDragDetectListener(  new DragDetectListener() {
      public void dragDetected( DragDetectEvent event ) {
        events.add(  event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDragSourceEvent( sourceControl, "dragStart", 1 );
    // run life cycle
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 2, events.size() );
    DragDetectEvent dragDetect = ( DragDetectEvent )events.get( 0 );
    assertEquals( DragDetectEvent.DRAG_DETECT, dragDetect.getID() );
    assertEquals( -16, dragDetect.x );
    assertEquals( -26, dragDetect.y );
    assertSame( sourceControl, dragDetect.widget );
    DragSourceEvent dragStart = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_START, dragStart.getID() );
    assertSame( dragSource, dragStart.widget );
    assertNotNull( Fixture.getProtocolMessage().findCallOperation( dragSource, "cancel" ) );
  }

  public void testLeaveBeforeEnter() {
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, DND.DROP_MOVE );
    dragSource.setTransfer( types );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, DND.DROP_MOVE );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new LogingDropTargetListener() );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragEnter", 1 );
    Fixture.executeLifeCycleFromServerThread();
    events.clear();
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragLeave", 2 );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragEnter", 3 );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragOver", 4 );
    // run life cycle
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 3, events.size() );
    DropTargetEvent dragLeave = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DRAG_LEAVE, dragLeave.getID() );
    assertSame( dropTarget, dragLeave.widget );
    DropTargetEvent dragEnter = ( DropTargetEvent )events.get( 1 );
    assertEquals( DropTargetEvent.DRAG_ENTER, dragEnter.getID() );
    assertSame( dropTarget, dragEnter.widget );
    DropTargetEvent dragOver = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DRAG_OVER, dragOver.getID() );
    assertSame( dropTarget, dragOver.widget );
  }

  public void testDataTransferOnDrop() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    });
    dragSource.addDragListener( new DragSourceAdapter(){
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
        event.data = "Hello World!";
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    int typeId = HTMLTransfer.getInstance().getSupportedTypes()[ 0 ].type;
    createDropTargetEvent( targetControl,
                           sourceControl,
                           "dropAccept",
                           1,
                           2,
                           "move",
                           typeId,
                           1 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 3, events.size() );
    // dropAccept expected
    DropTargetEvent dropAcceptEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropAcceptEvent.getID() );
    assertSame( dropTarget, dropAcceptEvent.widget );
    assertEquals( 1, dropAcceptEvent.x );
    assertEquals( 2, dropAcceptEvent.y );
    assertNull( dropAcceptEvent.data );
    // dragSetData expected
    DragSourceEvent dragSetDataEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSetDataEvent.getID() );
    assertSame( dragSource, dragSetDataEvent.widget );
    assertEquals( 1, dragSetDataEvent.x );
    assertEquals( 2, dragSetDataEvent.y );
    TransferData dataType = dragSetDataEvent.dataType;
    assertTrue( HTMLTransfer.getInstance().isSupportedType( dataType ) );
    // drop expected
    DropTargetEvent dropEvent = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DROP, dropEvent.getID() );
    assertSame( dropTarget, dropEvent.widget );
    assertEquals( dragSetDataEvent.dataType, dropEvent.currentDataType );
    assertEquals( 1, dropEvent.x );
    assertEquals( 2, dropEvent.y );
    assertEquals( "Hello World!", dropEvent.data );
  }

  public void testInvalidDataOnDragSetData() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[] { TextTransfer.getInstance() } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[] { TextTransfer.getInstance() } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    dragSource.addDragListener( new DragSourceAdapter(){
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
        event.data = new Date();
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 1 );
    // run life cycle
    try {
      Fixture.executeLifeCycleFromServerThread();
    } catch( SWTException e ) {
      events.add( e );
    }
    assertEquals( 3, events.size() );
    // dropAccept expected
    DropTargetEvent dropAcceptEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropAcceptEvent.getID() );
    // dragSetData expected
    DragSourceEvent dragSetDataEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSetDataEvent.getID() );
    // Exception expected
    SWTException exception = ( SWTException )events.get( 2 );
    assertEquals( DND.ERROR_INVALID_DATA, exception.code );
  }

  public void testChangeDataTypeOnDrop() {
    final TransferData[] originalDataType = new TransferData[ 1 ];
    Transfer[] transfer = new Transfer[]{
      HTMLTransfer.getInstance(),
      TextTransfer.getInstance()
    };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( transfer );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.setTransfer( transfer );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        originalDataType[ 0 ] = event.currentDataType;
        boolean isHTMLType = HTMLTransfer.getInstance().isSupportedType( event.currentDataType );
        TransferData newTransferData;
        if( isHTMLType ) {
          newTransferData = TextTransfer.getInstance().getSupportedTypes()[ 0 ];
        } else {
          newTransferData = HTMLTransfer.getInstance().getSupportedTypes()[ 0 ];
        }
        event.currentDataType = newTransferData;
        events.add( event );
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    dragSource.addDragListener( new DragSourceAdapter(){
      public void dragSetData( DragSourceEvent event ) {
        event.data = "data";
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 1 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 3, events.size() );
    // dropAccept expected
    DropTargetEvent dropAcceptEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropAcceptEvent.getID() );
    // dragSetData expected
    DragSourceEvent dragSetDataEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSetDataEvent.getID() );
    TransferData dataType = dragSetDataEvent.dataType;
    assertTrue( dataType != originalDataType[ 0 ] );
    // drop expected
    DropTargetEvent dropEvent = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DROP, dropEvent.getID() );
    assertTrue( dragSetDataEvent.dataType == dropEvent.currentDataType );
  }

  public void testChangeDataTypeInvalidOnDrop() {
    Transfer[] transfer = new Transfer[]{
      HTMLTransfer.getInstance(),
      TextTransfer.getInstance()
    };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( transfer );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.setTransfer( transfer );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        RTFTransfer rtfTransfer = RTFTransfer.getInstance();
        event.currentDataType = rtfTransfer.getSupportedTypes()[ 0 ];
        events.add( event );
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    dragSource.addDragListener( new DragSourceAdapter(){
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 1 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    // Invalid TransferData => no dropAccept
    assertEquals( 1, events.size() );
    DropTargetEvent dropAcceptEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropAcceptEvent.getID() );
  }

  public void testNoDropAfterDropAcceptEvent() {
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
        // prevent drop event
        event.detail = DND.DROP_NONE;
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    dragSource.addDragListener( new DragSourceListener() {
      public void dragStart( DragSourceEvent event ) {
        events.add( event );
      }
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
      }
      public void dragFinished( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dropAccept", 1 );
    createDragSourceEvent( dragSourceCont, "dragFinished", 2 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 2, events.size() );
    DropTargetEvent dropAcceptEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropAcceptEvent.getID() );
    DragSourceEvent event = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_END, event.getID() );
    assertSame( dragSource, event.widget );
    assertTrue( event.doit ); // Actual SWT behavior

  }

  public void testDropOverNonTarget() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragFinished( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.addDropListener( new LogingDropTargetListener() );
    shell.open();
    // Simulate request that sends a drop event 'somewhere', but outside a valid
    // drop target
    Fixture.fakeNewRequest( display );
    createDragSourceEvent( sourceControl, "dragFinished", 1 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 1, events.size() );
    assertTrue( events.get( 0 ) instanceof DragSourceEvent );
    DragSourceEvent event = ( DragSourceEvent )events.get( 0 );
    assertEquals( DragSourceEvent.DRAG_END, event.getID() );
    assertSame( dragSource, event.widget );
    assertTrue( event.doit ); // Actual SWT behavior
  }

  public void testDropOverTarget() {
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragSetData( DragSourceEvent event ) {
        event.data = "text";
        events.add( event );
      }
      public void dragFinished( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dropTarget.addDropListener( new LogingDropTargetListener() );
    shell.open();
    // Simulate request that sends a drop event over a valid drop target
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dropAccept", 1 );
    createDragSourceEvent( dragSourceCont, "dragFinished", 2 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 5, events.size() );
    // 1. expect dragLeave event
    assertTrue( events.get( 0 ) instanceof DropTargetEvent );
    DropTargetEvent dropTargetEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DRAG_LEAVE, dropTargetEvent.getID() );
    // 2. expect dropAccept event
    assertTrue( events.get( 1 ) instanceof DropTargetEvent );
    dropTargetEvent = ( DropTargetEvent )events.get( 1 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropTargetEvent.getID() );
    // 3. expect dragSetData event
    assertTrue( events.get( 2 ) instanceof DragSourceEvent );
    DragSourceEvent dragSourceEvent = ( DragSourceEvent )events.get( 2 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSourceEvent.getID() );
    // 4. expect drop event
    assertTrue( events.get( 3 ) instanceof DropTargetEvent );
    dropTargetEvent = ( DropTargetEvent )events.get( 3 );
    assertEquals( DropTargetEvent.DROP, dropTargetEvent.getID() );
    // 5. expect dragFinished event
    assertTrue( events.get( 4 ) instanceof DragSourceEvent );
    dragSourceEvent = ( DragSourceEvent )events.get( 4 );
    assertEquals( DragSourceEvent.DRAG_END, dragSourceEvent.getID() );
    assertTrue( dragSourceEvent.doit );
  }

  public void testChangeDetailInDropAccept() {
    int operations = DND.DROP_MOVE | DND.DROP_COPY;
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, operations );
    dragSource.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, operations );
    dropTarget.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragFinished( DragSourceEvent event ) {
        events.add(  event );
      }
      public void dragSetData( DragSourceEvent event ) {
        event.data = "text data";
        events.add(  event );
      }
    } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
        event.detail = DND.DROP_COPY;
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event over a valid drop target
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dropAccept", 1 );
    createDragSourceEvent( dragSourceCont, "dragFinished", 2 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 4, events.size() );
    // 1. expect dropAccept event
    DropTargetEvent dropTargetEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropTargetEvent.getID() );
    assertEquals( DND.DROP_COPY, dropTargetEvent.detail );
    // 2. expect dragSetData event
    DragSourceEvent dragSourceEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSourceEvent.getID() );
    assertEquals( DND.DROP_NONE, dragSourceEvent.detail );
    // 3. expect drop event
    dropTargetEvent = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DROP, dropTargetEvent.getID() );
    assertEquals( DND.DROP_COPY, dropTargetEvent.detail );
    // 4. expect dragFinished event
    dragSourceEvent = ( DragSourceEvent )events.get( 3 );
    assertEquals( DragSourceEvent.DRAG_END, dragSourceEvent.getID() );
    assertTrue( dragSourceEvent.doit );
    assertEquals( DND.DROP_COPY, dragSourceEvent.detail );
  }

  public void testChangeDetailInvalidInDropAccept() {
    int operations = DND.DROP_MOVE | DND.DROP_COPY;
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, operations );
    dragSource.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[] { HTMLTransfer.getInstance() } );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragFinished( DragSourceEvent event ) {
        events.add(  event );
      }
      public void dragSetData( DragSourceEvent event ) {
        events.add(  event );
      }
    } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
        event.detail = DND.DROP_COPY;
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event over a valid drop target
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dropAccept", 1 );
    createDragSourceEvent( dragSourceCont, "dragFinished", 2 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 2, events.size() );
    // 1. expect dropAccept event
    DropTargetEvent dropTargetEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropTargetEvent.getID() );
    assertEquals( DND.DROP_COPY, dropTargetEvent.detail );
    // 2. expect dragFinished event
    DragSourceEvent dragSourceEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_END, dragSourceEvent.getID() );
    assertTrue( dragSourceEvent.doit ); // This is still true in SWT/Win
    assertEquals( DND.DROP_NONE, dragSourceEvent.detail );
  }

  public void testDragSetDataDoitIsFalse() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
        event.data = "TestData";
        event.doit = false;
      }
      public void dragFinished( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dropAccept( DropTargetEvent event ) {
        events.add( event );
      }
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    // Simulate request that sends a drop event over a valid drop target
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 1 );
    createDragSourceEvent( sourceControl, "dragFinished", 2 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 4, events.size() );
    // 1. expect dropAccept event
    assertTrue( events.get( 0 ) instanceof DropTargetEvent );
    DropTargetEvent dropTargetEvent = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DROP_ACCEPT, dropTargetEvent.getID() );
    // 2. expect dragSetData event
    assertTrue( events.get( 1 ) instanceof DragSourceEvent );
    DragSourceEvent dragSourceEvent = ( DragSourceEvent )events.get( 1 );
    assertEquals( DragSourceEvent.DRAG_SET_DATA, dragSourceEvent.getID() );
    // NOTE: This is not the behavior documented for SWT,
    //       but how SWT behaves in Windows (bug?)
    // 3. expect drop event
    assertTrue( events.get( 2 ) instanceof DropTargetEvent );
    dropTargetEvent = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DROP, dropTargetEvent.getID() );
    assertNull( dropTargetEvent.data );
    // 4. expect dragFinished event
    assertTrue( events.get( 3 ) instanceof DragSourceEvent );
    dragSourceEvent = ( DragSourceEvent )events.get( 3 );
    assertEquals( DragSourceEvent.DRAG_END, dragSourceEvent.getID() );
    assertTrue( dragSourceEvent.doit );
  }

  public void testDragSetDataDataType() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    dragSource.setTransfer( new Transfer[] { TextTransfer.getInstance() } );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragSetData( DragSourceEvent event ) {
        events.add( event );
        event.data = "string";
      }
    } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void drop( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    dropTarget.setTransfer( new Transfer[] { TextTransfer.getInstance() } );
    shell.open();
    // Simulate request that sends a drop event over a valid drop target
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 0 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    // Ensure that dataType is set to something meaningful
    DragSourceEvent setDataEvent = ( DragSourceEvent )events.get( 0 );
    assertNotNull( setDataEvent.dataType );
    DropTargetEvent dropEvent = ( DropTargetEvent )events.get( 1 );
    assertSame( setDataEvent.data, dropEvent.data );
    assertNotNull( dropEvent.currentDataType );
    assertTrue( TransferData.sameType( setDataEvent.dataType, dropEvent.currentDataType ) );
  }

  public void testResponseNoDetailChange() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    int typeId = TextTransfer.getInstance().getSupportedTypes()[ 0 ].type;
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 10, 10, "copy", typeId, 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 10, 10, "copy", typeId, 2 );

    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    assertNull( message.findSetOperation( dragSource, "changeDetail" ) );
  }

  public void testResponseDetailChangedOnEnter() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragEnter( DropTargetEvent event ) {
        event.detail = DND.DROP_LINK;
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDetail" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    assertEquals( "DROP_LINK", call.getProperty( "detail" ) );
  }

  public void testResponseDetailChangedOnOver() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragOver( DropTargetEvent event ) {
        event.detail = DND.DROP_LINK;
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    int typeId = TextTransfer.getInstance().getSupportedTypes()[ 0 ].type;
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 10, 10, "move", typeId, 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 10, 10, "copy", typeId, 2 );
    Fixture.executeLifeCycleFromServerThread();
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDetail" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    assertEquals( "DROP_LINK", call.getProperty( "detail" ) );
  }

  public void testDropAcceptWithDetailChangedOnEnter() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragSetData( DragSourceEvent event ) {
        event.data = "some data";
      }
    } );
    dragSource.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( new Transfer[]{ TextTransfer.getInstance() } );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragEnter( DropTargetEvent event ) {
        event.detail = DND.DROP_COPY;
        events.add(  event );
      }
      public void dragOver( DropTargetEvent event ) {
        events.add(  event );
      }
      public void drop( DropTargetEvent event ) {
        events.add(  event );
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    createDropTargetEvent( targetControl, sourceControl, "dropAccept", 3 );
    createDragSourceEvent( sourceControl, "dragFinished", 3 );

    Fixture.executeLifeCycleFromServerThread();

    Message message = Fixture.getProtocolMessage();
    assertEquals( 0, message.getOperationCount() );
    assertEquals( 3, events.size() );
    DropTargetEvent dragEnter = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DRAG_ENTER, dragEnter.getID() );
    assertEquals( DND.DROP_COPY, dragEnter.detail );
    DropTargetEvent dragOver = ( DropTargetEvent )events.get( 1 );
    assertEquals( DropTargetEvent.DRAG_OVER, dragOver.getID() );
    assertEquals( DND.DROP_COPY, dragOver.detail );
    DropTargetEvent drop = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DROP, drop.getID() );
    assertEquals( DND.DROP_COPY, drop.detail );
  }

  public void testDetermineDataType() {
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, DND.DROP_MOVE );
    Transfer[] sourceTransfers = new Transfer[]{
      TextTransfer.getInstance(),
      HTMLTransfer.getInstance()
    };
    Transfer[] targetTransfers = new Transfer[]{
      RTFTransfer.getInstance(),
      HTMLTransfer.getInstance()
    };
    dragSource.setTransfer( sourceTransfers );
    dropTarget.setTransfer( targetTransfers );
    TransferData[] dataTypes = DNDSupport.determineDataTypes( dragSource, dropTarget );
    assertTrue( dataTypes.length > 0 );
    assertTrue( HTMLTransfer.getInstance().isSupportedType( dataTypes[ 0 ] ) );
  }

  public void testResponseFeedbackChangedOnEnter() throws JSONException {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragEnter( DropTargetEvent event ) {
        event.feedback = DND.FEEDBACK_SELECT;
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    Fixture.executeLifeCycleFromServerThread();
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeFeedback" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    assertEquals( new Integer( DND.FEEDBACK_SELECT ), call.getProperty( "flags" ) );
    JSONArray feedbackArr = ( JSONArray )call.getProperty( "feedback" );
    assertEquals( "\"FEEDBACK_SELECT\"", feedbackArr.join( "," ) );
  }

  public void testResponseFeedbackChangedOnOver() throws JSONException {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragOver( DropTargetEvent event ) {
        event.feedback = DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    Fixture.executeLifeCycleFromServerThread();
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeFeedback" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    Integer expectedFlags = new Integer( DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND );
    assertEquals( expectedFlags, call.getProperty( "flags" ) );
    JSONArray feedbackArr = ( JSONArray )call.getProperty( "feedback" );
    assertEquals( "\"FEEDBACK_EXPAND\",\"FEEDBACK_SCROLL\"", feedbackArr.join( "," ) );
  }

  public void testResponseInitDataType() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] {
      TextTransfer.getInstance(),
      RTFTransfer.getInstance()
    };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    Fixture.executeLifeCycleFromServerThread();
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDataType" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
  }

  public void testResponseChangeDataTypeOnOver() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] {
      TextTransfer.getInstance(),
      RTFTransfer.getInstance()
    };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragEnter( DropTargetEvent event ) {
        events.add( event );
      }
      public void dragOver( DropTargetEvent event ) {
        event.currentDataType = RTFTransfer.getInstance().getSupportedTypes()[ 0 ];
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    Fixture.executeLifeCycleFromServerThread();
    DropTargetEvent dragEnter = ( DropTargetEvent )events.get( 0 );
    TransferData typeOnEnter = dragEnter.currentDataType;
    assertTrue( TextTransfer.getInstance().isSupportedType( typeOnEnter ) );
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDataType" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    Integer expectedType = new Integer( RTFTransfer.getInstance().getSupportedTypes()[ 0 ].type );
    assertEquals( expectedType, call.getProperty( "dataType" ) );
  }

  public void testResponseChangeDataTypeOnEnter() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] {
      TextTransfer.getInstance(),
      RTFTransfer.getInstance()
    };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragEnter( DropTargetEvent event ) {
        event.currentDataType = RTFTransfer.getInstance().getSupportedTypes()[ 0 ];
      }
      public void dragOver( DropTargetEvent event ) {
        events.add( event );
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 1 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 2 );
    Fixture.executeLifeCycleFromServerThread();
    DropTargetEvent dragOver = ( DropTargetEvent )events.get( 0 );
    TransferData typeOnOver = dragOver.currentDataType;
    assertTrue( RTFTransfer.getInstance().isSupportedType( typeOnOver ) );
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDataType" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    Integer expectedType = new Integer( RTFTransfer.getInstance().getSupportedTypes()[ 0 ].type );
    assertEquals( expectedType, call.getProperty( "dataType" ) );
  }

  public void testResponseChangeDataTypeInvalid() {
    // NOTE : Setting an invalid value on currentDataType reverts the field
    //        back to the next-best valid value. This is NOT SWT-like behavior!
    //        SWT would set null and display the DROP_NONE cursor.
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new DropTargetAdapter() {
      public void dragOver( DropTargetEvent event ) {
        event.currentDataType = RTFTransfer.getInstance().getSupportedTypes()[ 0 ];
      }
    } );
    shell.open();
    Fixture.fakeNewRequest( display );
    Fixture.executeLifeCycleFromServerThread();
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 1 );
    Fixture.executeLifeCycleFromServerThread();
    Message message = Fixture.getProtocolMessage();
    CallOperation call = message.findCallOperation( dragSource, "changeDataType" );
    assertEquals( WidgetUtil.getId( targetControl ), call.getProperty( "control" ) );
    Integer expectedType = new Integer( TextTransfer.getInstance().getSupportedTypes()[ 0 ].type );
    assertEquals( expectedType, call.getProperty( "dataType" ) );

  }

  public void testOperationChangedEvent() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_COPY;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control sourceControl = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( sourceControl, operations );
    dragSource.setTransfer( types );
    Control targetControl = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( targetControl, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new LogingDropTargetListener() );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    int dataType = TextTransfer.getInstance().getSupportedTypes()[ 0 ].type;
    createDropTargetEvent( targetControl, sourceControl, "dragEnter", 2 );
    createDropTargetEvent( targetControl,
                           sourceControl,
                           "dragOperationChanged",
                           0,
                           0,
                           "copy",
                           dataType,
                           3 );
    createDropTargetEvent( targetControl, sourceControl, "dragOver", 5 );
    // run life cycle
    Fixture.executeLifeCycleFromServerThread();
    assertEquals( 3, events.size() );
    DropTargetEvent dragEnter = ( DropTargetEvent )events.get( 0 );
    assertEquals( DropTargetEvent.DRAG_ENTER, dragEnter.getID() );
    assertSame( dropTarget, dragEnter.widget );
    DropTargetEvent dragOperationChanged = ( DropTargetEvent )events.get( 1 );
    assertEquals( DropTargetEvent.DRAG_OPERATION_CHANGED, dragOperationChanged.getID() );
    assertTrue( ( dragOperationChanged.detail & DND.DROP_COPY ) != 0 );
    DropTargetEvent dragOver = ( DropTargetEvent )events.get( 2 );
    assertEquals( DropTargetEvent.DRAG_OVER, dragOver.getID() );
    assertSame( dropTarget, dragOver.widget );
  }

  public void testOperationsField() {
    int operations = DND.DROP_MOVE | DND.DROP_LINK;
    Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
    Control dragSourceCont = new Label( shell, SWT.NONE );
    DragSource dragSource = new DragSource( dragSourceCont, operations );
    dragSource.setTransfer( types );
    dragSource.addDragListener( new DragSourceAdapter(){
      public void dragSetData( DragSourceEvent event ) {
        event.data = "text";
      }
    } );
    Control dropTargetCont = new Label( shell, SWT.NONE );
    DropTarget dropTarget = new DropTarget( dropTargetCont, operations );
    dropTarget.setTransfer( types );
    dropTarget.addDropListener( new LogingDropTargetListener() );
    shell.open();
    // Simulate request that sends a drop event
    Fixture.fakeNewRequest( display );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragEnter", 2 );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragOver", 5 );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dragOperationChanged", 7 );
    createDropTargetEvent( dropTargetCont, dragSourceCont, "dropAccept", 8 );
    // run life cycle
    Fixture.readDataAndProcessAction( display );
    assertEquals( 6, events.size() );
    assertEquals( operations, ( ( DropTargetEvent )events.get( 0 ) ).operations );
    assertEquals( operations, ( ( DropTargetEvent )events.get( 1 ) ).operations );
    assertEquals( operations, ( ( DropTargetEvent )events.get( 2 ) ).operations );
    assertEquals( 0, ( ( DropTargetEvent )events.get( 3 ) ).operations );
    assertEquals( operations, ( ( DropTargetEvent )events.get( 4 ) ).operations );
    assertEquals( operations, ( ( DropTargetEvent )events.get( 5 ) ).operations );
  }

  public void testDragStartEventRelativeCoordinates() {
    shell.setLocation( 5, 5 );
    Control sourceControl = new Label( shell, SWT.NONE );
    sourceControl.setLocation( 10, 20 );
    DragSource dragSource = new DragSource( sourceControl, DND.DROP_MOVE );
    Control targetControl = new Label( shell, SWT.NONE );
    new DropTarget( targetControl, DND.DROP_MOVE );
    dragSource.addDragListener( new DragSourceAdapter() {
      public void dragStart( DragSourceEvent event ) {
        events.add( event );
      }
    } );
    shell.open();

    Fixture.fakeNewRequest( display );
    createDragSourceEvent( sourceControl, "dragStart", 20, 30, "move", 1 );
    Fixture.executeLifeCycleFromServerThread();

    assertEquals( 1, events.size() );
    assertEquals( 4, ( ( DragSourceEvent )events.get( 0 ) ).x );
    assertEquals( 4, ( ( DragSourceEvent )events.get( 0 ) ).y );
  }

  // Mirrors _sendDragSourceEvent in DNDSupport.js
  private static void createDragSourceEvent( Control control, String eventType, int time ) {
    createDragSourceEvent( control, eventType, 0, 0, "move", time );
  }

  private static void createDragSourceEvent( Control control,
                                             String eventType,
                                             int x,
                                             int y,
                                             String operation,
                                             int time )
  {
    String prefix = "org.eclipse.swt.dnd." + eventType;
    String controlId = WidgetUtil.getId( control );
    Fixture.fakeRequestParam( prefix, controlId );
    Fixture.fakeRequestParam( prefix + ".x", String.valueOf( x ) );
    Fixture.fakeRequestParam( prefix + ".y", String.valueOf( y ) );
    Fixture.fakeRequestParam( prefix + ".time", String.valueOf( time ) );
  }

  // Mirrors _sendDropTargetEvent in DNDSupport.js
  private static void createDropTargetEvent( Control control,
                                             Control source,
                                             String eventType,
                                             int time )
  {
    int dataType = TextTransfer.getInstance().getSupportedTypes()[ 0 ].type;
    createDropTargetEvent( control, source, eventType, 0, 0, "move", dataType, time  );
  }

  private static void createDropTargetEvent( Control control,
                                             Control source,
                                             String eventType,
                                             int x,
                                             int y,
                                             String operation,
                                             int dataType,
                                             int time )
  {
    String prefix = "org.eclipse.swt.dnd." + eventType;
    String controlId = WidgetUtil.getId( control );
    String sourceId = WidgetUtil.getId( source );
    Fixture.fakeRequestParam( prefix, controlId );
    Fixture.fakeRequestParam( prefix + ".x", String.valueOf( x ) );
    Fixture.fakeRequestParam( prefix + ".y", String.valueOf( y ) );
    Fixture.fakeRequestParam( prefix + ".operation", operation );
    Fixture.fakeRequestParam( prefix + ".feedback", "0" );
    Fixture.fakeRequestParam( prefix + ".source", sourceId );
    Fixture.fakeRequestParam( prefix + ".time", String.valueOf( time ) );
    Fixture.fakeRequestParam( prefix + ".dataType", String.valueOf( dataType ) );
  }

  private class LogingDropTargetListener implements DropTargetListener {

    public void dragEnter( DropTargetEvent event ) {
      events.add( event );
    }

    public void dragLeave( DropTargetEvent event ) {
      events.add( event );
    }

    public void dragOperationChanged( DropTargetEvent event ) {
      events.add( event );
    }

    public void dragOver( DropTargetEvent event ) {
      events.add( event );
    }

    public void drop( DropTargetEvent event ) {
      events.add( event );
    }

    public void dropAccept( DropTargetEvent event ) {
      events.add( event );
    }

  }
}
