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

qx.Class.define( "org.eclipse.rwt.test.tests.ScrollBarTest", {

  extend : qx.core.Object,

  construct : function() {
    var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
    TestUtil.prepareTimerUse();
    qx.Class.__initializeClass( org.eclipse.rwt.widgets.ScrollBar );
    org.eclipse.rwt.widgets.ScrollBar.prototype._getMinThumbSize = function() { return 8; };
  },

  members : {

    testCreateDispose : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar();
      bar.setMergeEvents( true );
      assertTrue( bar.isSeeable() );
      var timer = bar._eventTimer;
      assertNotNull( timer );
      bar.destroy();
      TestUtil.flush();
      assertTrue( bar.isDisposed() );
      assertTrue( timer.isDisposed() );
      assertNull( bar._eventTimer );
    },

    testDimension : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( true, false );
      var hBar = this._createScrollBar( true, true );
      assertFalse( hBar.isSeeable() );
      assertFalse( vBar.isSeeable() );
      TestUtil.flush();
      var hBounds = TestUtil.getElementBounds( hBar.getElement() );
      var vBounds = TestUtil.getElementBounds( vBar.getElement() );
      assertEquals( 100, hBounds.width );
      assertEquals( 100, vBounds.height );
      assertEquals( 10, vBounds.width );
      assertEquals( 10, hBounds.height );
      var isMshtml = qx.core.Variant.isSet( "qx.client", "mshtml" );
      if( isMshtml ) {
        var hTargetBounds = TestUtil.getElementBounds( hBar._getTargetNode() );
        var vTargetBounds = TestUtil.getElementBounds( vBar._getTargetNode() );
        assertEquals( 10, vTargetBounds.width );
        assertEquals( 10, hTargetBounds.height );
      }
      hBar.destroy();
      vBar.destroy();
    },

    testProppagateKeys : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar();
      TestUtil.click( bar._thumb );
      var log = [];
      bar.addEventListener( "keypress", function( event ) {
        log.push( event );
      } );
      bar.addEventListener( "keydown", function( event ) {
        log.push( event );
      } );
      bar.addEventListener( "keyup", function( event ) {
        log.push( event );
      } );
      TestUtil.pressOnce( bar._thumb._getTargetNode(), "Enter" );
      TestUtil.pressOnce( bar._thumb._getTargetNode(), "Down" );
      assertEquals( 6, log.length );
      bar.destroy();
    },

    testSetValidValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar();
      var events = this._getChangeLogger( bar );
      bar.setValue( 50 );
      assertEquals( 50, bar.getValue() );
      bar.setValue( 200 );
      assertEquals( 200, bar.getValue() );
      bar.setValue( 0 );
      assertEquals( 0, bar.getValue() );
      assertEquals( 3, events.length );
      bar.destroy();
    },

    testFireEventOnSetMaximum : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar();
      var events = this._getChangeLogger( bar );
      bar.setValue( 250 );
      bar.setMaximum( 200 );
      assertEquals( 2, events.length );
      bar.destroy();
    },

    testSetInvalidValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar();
      var events = [];
      bar.setValue( 50 );
      assertEquals( 50, bar.getValue() );
      bar.setValue( -1 );
      assertEquals( 0, bar.getValue() );
      bar.setValue( 201 );
      assertEquals( 200, bar.getValue() );
      bar.destroy();
    },

    testRelativeKnobPosition : function() {
      // TODO [tb] : with new ScrollBar change to test absolute values
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      var hBar = this._createScrollBar( false, true );
      vBar.setValue( 0 );
      hBar.setValue( 0 );
      assertEquals( [ 0, 0 ], this._getRelativeKnobPosition( vBar ) );
      assertEquals( [ 0, 0 ], this._getRelativeKnobPosition( hBar ) );
      vBar.setValue( 100 );
      hBar.setValue( 100 );
      assertEquals( [ 0, 33 ], this._getRelativeKnobPosition( vBar ) );
      assertEquals( [ 33, 0 ], this._getRelativeKnobPosition( hBar ) );
      hBar.setMaximum( 400 );
      vBar.setMaximum( 400 );
      assertEquals( [ 0, 25 ], this._getRelativeKnobPosition( vBar ) );
      assertEquals( [ 25, 0 ], this._getRelativeKnobPosition( hBar ) );
      vBar.setHeight( 200 );
      hBar.setWidth( 200 );
      assertEquals( [ 0, 25 ], this._getRelativeKnobPosition( vBar ) );
      assertEquals( [ 25, 0 ], this._getRelativeKnobPosition( hBar ) );
      hBar.destroy();      
      vBar.destroy();      
    },

    testResizeReducesValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar.setValue( 150 );
      assertEquals( [ 0, 50 ], this._getRelativeKnobPosition( vBar ) );
      vBar.setHeight( 200 );
      TestUtil.flush();
      assertEquals( [ 0, 33 ], this._getRelativeKnobPosition( vBar ) );
      vBar.destroy();      
    },

    testResizeLargerThanMaximum : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar.setValue( 150 );
      assertEquals( [ 0, 50 ], this._getRelativeKnobPosition( vBar ) );
      vBar.setHeight( 301 );
      TestUtil.flush();
      assertEquals( [ 0, 0 ], this._getRelativeKnobPosition( vBar ) );
      vBar.destroy();      
    },

    testChangeValueWhileInvisible : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar.setValue( 0 );
      assertEquals( [ 0, 0 ], this._getRelativeKnobPosition( vBar ) );
      vBar.setVisibility( false );
      vBar.setValue( 100 );
      vBar.setVisibility( true );
      assertEquals( 100, vBar.getValue() );
      assertEquals( [ 0, 33 ], this._getRelativeKnobPosition( vBar ) );
      vBar.destroy();
    },

    testKnobSize : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      assertEquals( [ 0, 40 ], this._getRelativeThumbLength( vBar ) );
      vBar.setMaximum( 200 );
      assertEquals( [ 0, 59 ], this._getRelativeThumbLength( vBar ) );
      vBar.setHeight( 50 );
      assertEquals( [ 0, 44 ], this._getRelativeThumbLength( vBar ) );
      vBar.destroy();
    },  

    testKnobVisibility : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar._thumb.setImage( "bla.jpg", 10, 20 );
      assertEquals( 27, vBar._thumb.getHeight() );
      assertTrue( vBar._thumb.isCellVisible( 1 ) );
      vBar.setHeight( 95 );
      assertEquals( 24, vBar._thumb.getHeight() );
      assertFalse( vBar._thumb.isCellVisible( 1 ) );
      vBar.destroy();
    },

    testIdealValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar.setValue( 500 );
      assertEquals( 300 - 100, vBar.getValue() );
      vBar.setMaximum( 700 );
      assertEquals( 500, vBar.getValue() );
      vBar.setValue( 650 );
      assertEquals( 600, vBar.getValue() );
      vBar.setHeight( 50 );
      assertEquals( 650, vBar.getValue() );
      vBar.destroy();
    },

    testClearIdealValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      vBar.setValue( 500 );
      assertEquals( 200, vBar.getValue() );
      TestUtil.click( vBar._minButton );
      TestUtil.click( vBar._maxButton );
      assertEquals( 200, vBar.getValue() );
      vBar.setMaximum( 700 );
      assertEquals( 200, vBar.getValue() );
      vBar.destroy();
    },

    testStopPropagation : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var vBar = this._createScrollBar( false, false );
      var ok = true;
      var fail = function() {
        ok = false;
      };
      TestUtil.getDocument().addEventListener( "dblclick", fail );
      TestUtil.getDocument().addEventListener( "click", fail );
      TestUtil.doubleClick( vBar );
      assertTrue( ok );
      vBar.destroy();
    },

    testMergeEventsFastScroll : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      var events = this._getChangeLogger( bar );
      bar.setMergeEvents( true );
      bar.setValue( 100 );
      bar.setValue( 200 );
      bar.setValue( 199 );
      assertEquals( 0, events.length );
      assertTrue( bar._eventTimer.isEnabled() ); 
      TestUtil.forceInterval( bar._eventTimer );
      assertFalse( bar._eventTimer.isEnabled() );
      assertEquals( 1, events.length );
      bar.destroy();
    },

    testTurnMergeEventsOff : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      assertFalse( bar._mergeEvents );
      bar.setMergeEvents( true );
      assertTrue( bar._mergeEvents );
      var error = null;
      try {
        bar.setMergeEvents( false );
      } catch( ex ) {
        error = ex;
      }
      assertNotNull( error );
      assertTrue( bar._mergeEvents );
      bar.destroy();
    },

    testMergeEventsSlowScroll : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      var events = this._getChangeLogger( bar );
      bar.setMergeEvents( true );
      bar.setValue( 10 );
      bar.setValue( 20 );
      bar.setValue( 30 );
      bar.setValue( 130 );
      bar.setValue( 230 );
      assertEquals( 3, events.length );
      TestUtil.forceInterval( bar._eventTimer );
      assertEquals( 4, events.length );
      bar.destroy();
    },

    testMergeEventsBackAndForth : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      var events = this._getChangeLogger( bar );
      bar.setMergeEvents( true );
      bar.setValue( 130 );
      assertEquals( 0, events.length );
      assertTrue( bar._eventTimer.getEnabled() );
      bar.setValue( 20 );
      assertEquals( 1, events.length );
      assertFalse( bar._eventTimer.getEnabled() );
      bar.destroy();
    },

    testIncrement : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setIncrement( 30 );
      assertEquals( 30, bar._increment );
      bar.destroy();
    },
 
    testPageIncrement : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setIncrement( 30 );
      assertEquals( 70, bar._pageIncrement );
      bar.setHeight( 200 );
      assertEquals( 170, bar._pageIncrement );
      bar.destroy();      
    },

    testAutoEnableMergeEvents : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 350 );
      bar.autoEnableMerge( 350 );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 350 );
      assertTrue( bar.getMergeEvents() );
      bar.destroy();
    },

    testAutoEnableMergeEventsEarly : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 600 );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 1010 );
      assertTrue( bar.getMergeEvents() );
      bar.destroy();
    },

    testAutoEnableMergeEventsIgnoreZero : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 0 );
      bar.autoEnableMerge( 0 );
      bar.autoEnableMerge( 0 );
      bar.autoEnableMerge( 0 );
      bar.autoEnableMerge( 0 );
      assertFalse( bar.getMergeEvents() );
      bar.autoEnableMerge( 1010 );
      assertTrue( bar.getMergeEvents() );
      bar.destroy();
    },

    testMinThumbSizeByMaxValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 5000 );
      TestUtil.flush();
      var minSize = 8;
      assertEquals( minSize, bar._thumb.getHeight() );
      bar.setValue( 4900 );
      assertEquals( 4900, bar.getValue() );
      bar.setValue( 5900 );
      assertEquals( 4900, bar.getValue() );
      bar.destroy();
    },

    testMaxValueEqualsSize : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 21 );
      bar.setHeight( 21 );
      TestUtil.flush();
      bar.setValue( 0 ); // Order is relevant (flush first)
      var minSize = 8;
      assertEquals( 0, bar.getValue() );
      assertEquals( minSize, bar._thumb.getHeight() );
      
      bar.destroy();
    },

    testMinThumbSizeBySliderSize : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 1000 );
      TestUtil.flush();
      var minSize = 8;
      assertEquals( minSize, bar._thumb.getHeight() );
      bar.setValue( 1000 );
      assertEquals( 900, bar.getValue() );
      bar.setValue( 1900 );
      assertEquals( 900, bar.getValue() );
      bar.destroy();
    },

    testMinThumbSizeUndoBySetMaximum : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 5000 );
      TestUtil.flush();
      var minSize = 8;
      assertEquals( minSize, bar._thumb.getHeight() );
      bar.setMaximum( 400 );
      assertEquals( 100, bar._thumbLength );
      assertEquals( 20, bar._thumb.getHeight() );
      bar.setValue( 400 );
      assertEquals( 300, bar.getValue() );
      bar.destroy();
    },

    testMinThumbSizeUndoBySetSize : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 1000 );
      TestUtil.flush();
      var minSize = 8;
      assertEquals( minSize, bar._thumb.getHeight() );
      bar.setHeight( 500 );
      assertEquals( 500, bar._thumbLength );
      assertEquals( 240, bar._thumb.getHeight() );
      bar.setValue( 1000 );
      assertEquals( 500, bar.getValue() );
      bar.destroy();
    },

    testNegativeLineSize : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setHeight( 10 );
      bar.setMaximum( 400 );
      bar.setValue( 40 );
      bar.setHeight( 20 );
      assertEquals( 40, bar.getValue() );
      bar.setMaximum( 4000 );
      assertEquals( 40, bar.getValue() );
      bar.destroy();
    },

    testSizeZero : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setHeight( 0 );
      bar.setMaximum( 400 );
      bar.setValue( 40 );
      bar.setHeight( 20 );
      assertEquals( 40, bar.getValue() );
      bar.setMaximum( 4000 );
      assertEquals( 40, bar.getValue() );
      bar.destroy();
    },

    testMaximumZero : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setMaximum( 0 );
      bar.setValue( 40 );
      bar.setHeight( 20 );
      assertEquals( 0, bar.getValue() );
      bar.setValue( -20 );
      assertEquals( 0, bar.getValue() );
      bar.destroy();
    },

    testMouseWheel : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      assertEquals( 0, bar.getValue() );
      TestUtil.fakeWheel( bar, 1 );
      assertEquals( 0, bar.getValue() );
      TestUtil.fakeWheel( bar, -1 );
      assertEquals( 20, bar._selection );
      TestUtil.fakeWheel( bar, -1 );
      assertEquals( 40, bar._selection );
      bar.destroy();
    },

    testGetValue_NoFractionalValue : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = this._createScrollBar( false, false );
      bar.setValue( 5 );
      bar.setHeight( 60 );
      TestUtil.flush();
      assertEquals( 5, bar.getValue() );
    },

    // test div increment
    
    /////////
    // Helper

    _createScrollBar : function( noFlush, horizontal) {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var bar = new org.eclipse.rwt.widgets.ScrollBar( horizontal );
      bar.setLeft( 10 );
      bar.setTop( 10 );
      bar.setMaximum( 300 );
      if( horizontal === true ) {
        bar.setWidth( 100 );
      } else {
        bar.setHeight( 100 );
      }
      bar.addToDocument();
      if( noFlush !== true ) {
        TestUtil.flush();
      }
      return bar;
    },

    _getRelativeKnobPosition : function( bar ) {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var button = 10; 
      var result = [ 0, 0 ];
      if( bar._horizontal ) {
        var length = bar.getWidth() - button * 2;
        result[ 0 ] = Math.round( 100 * ( bar._thumb.getLeft() - button ) / length );
      } else {
        var length = bar.getHeight() - button * 2;
        result[ 1 ] = Math.round( 100 * ( bar._thumb.getTop() - button ) / length );
      }
      return result;
    },

    _getRelativeThumbLength : function( bar ) {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var button = 16; 
      var result = [ 0, 0 ];
      if( bar._horizontal ) {
        var length = bar.getWidth() - button * 2;
        result[ 0 ] = Math.round( 100 * ( bar._thumb.getWidth() ) / length );
      } else {
        var length = bar.getHeight() - button * 2;
        result[ 1 ] = Math.round( 100 * ( bar._thumb.getHeight() ) / length );
      }
      return result;
    },

    _getChangeLogger : function( bar ) {
      var events = [];
      bar.addEventListener( "changeValue", function( event ) {
        events.push( event );
      }, this );
      return events;
    }

  }

} );