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

qx.Class.define( "org.eclipse.rwt.test.tests.TabFolderTest", {

  extend : qx.core.Object,

  members : {

    testCreateTabFolderOnTopByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var processor = org.eclipse.rwt.protocol.Processor;
      processor.processOperation( {
        "target" : "w3",
        "action" : "create",
        "type" : "rwt.widgets.TabFolder",
        "properties" : {
          "style" : [ "TOP" ],
          "parent" : "w2"
        }
      } );
      var ObjectManager = org.eclipse.rwt.protocol.ObjectManager;
      var widget = ObjectManager.getObject( "w3" );
      assertTrue( widget instanceof qx.ui.pageview.tabview.TabView );
      assertIdentical( shell, widget.getParent() );
      assertTrue( widget.getUserData( "isControl") );
      assertTrue( widget.getHideFocus() );
      assertTrue( widget.getPlaceBarOnTop() );
      shell.destroy();
      widget.destroy();
    },

    testCreateTabFolderOnBottomByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var processor = org.eclipse.rwt.protocol.Processor;
      processor.processOperation( {
        "target" : "w3",
        "action" : "create",
        "type" : "rwt.widgets.TabFolder",
        "properties" : {
          "style" : [ "BOTTOM" ],
          "parent" : "w2"
        }
      } );
      var ObjectManager = org.eclipse.rwt.protocol.ObjectManager;
      var widget = ObjectManager.getObject( "w3" );
      assertTrue( widget instanceof qx.ui.pageview.tabview.TabView );
      assertIdentical( shell, widget.getParent() );
      assertTrue( widget.getUserData( "isControl") );
      assertTrue( widget.getHideFocus() );
      assertFalse( widget.getPlaceBarOnTop() );
      shell.destroy();
      widget.destroy();
    },

    testSetSelectionByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var processor = org.eclipse.rwt.protocol.Processor;
      processor.processOperation( {
        "target" : "w3",
        "action" : "create",
        "type" : "rwt.widgets.TabFolder",
        "properties" : {
          "style" : [ "TOP" ],
          "parent" : "w2"
        }
      } );
      var item1 = this._createTabItemByProtocol( "w4", "w3" );
      var item2 = this._createTabItemByProtocol( "w5", "w3" );
      var item3 = this._createTabItemByProtocol( "w6", "w3" );
      TestUtil.protocolSet( "w3", { "selection" : "w5" } );
      var ObjectManager = org.eclipse.rwt.protocol.ObjectManager;
      var widget = ObjectManager.getObject( "w3" );
      assertFalse( item1.getChecked() );
      assertTrue( item2.getChecked() );
      assertFalse( item3.getChecked() );
      shell.destroy();
      widget.destroy();
      item1.destroy();
      item2.destroy();
      item3.destroy();
    },

    testCreateTabItemByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      assertTrue( item instanceof qx.ui.pageview.tabview.Button );
      assertIdentical( folder.getBar(), item.getParent() );
      assertNull( item.getUserData( "isControl") );
      var ObjectManager = org.eclipse.rwt.protocol.ObjectManager;
      var page = ObjectManager.getObject( "w4pg" );
      assertTrue( page instanceof qx.ui.pageview.tabview.Page );
      shell.destroy();
      folder.destroy();
      item.destroy();
    },

    testDestroyTabItemByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      var ObjectManager = org.eclipse.rwt.protocol.ObjectManager;
      var page = ObjectManager.getObject( "w4pg" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "destroy"
      } );
      TestUtil.flush();
      assertTrue( item.isDisposed() );
      assertTrue( page.isDisposed() );
      assertEquals( undefined, ObjectManager.getObject( "w4" ) );
      assertEquals( undefined, ObjectManager.getObject( "w4pg" ) );
      shell.destroy();
      folder.destroy();
    },

    testSetTextByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "set",
        "properties" : {
          "text" : "foo<>\" bar"
        }
      } );
      assertEquals( "foo&lt;&gt;&quot; bar", item.getLabel().toString() );
      shell.destroy();
      folder.destroy();
      item.destroy();
    },

    testSetImageByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "set",
        "properties" : {
          "image" : [ "image.png", 10, 20 ]
        }
      } );
      assertEquals( "image.png", item.getIcon() );
      shell.destroy();
      folder.destroy();
      item.destroy();
    },

    testSetControlByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      var control =  new org.eclipse.rwt.widgets.Button( "push" );
      var widgetManager = org.eclipse.swt.WidgetManager.getInstance();
      widgetManager.add( control, "w5", true, "rwt.widgets.Button" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "set",
        "properties" : {
          "control" : "w5"
        }
      } );
      assertTrue( control.getParent() instanceof qx.ui.pageview.tabview.Page );
      assertIdentical( widgetManager.findWidgetById( "w4pg" ), control.getParent() );
      shell.destroy();
      folder.destroy();
      item.destroy();
      control.destroy();
    },

    testSetToolTipByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "set",
        "properties" : {
          "toolTip" : "hello blue world"
        }
      } );
      assertEquals( "hello blue world", item.getUserData( "toolTipText" ) );
      assertTrue( item.getToolTip() !== null );
      shell.destroy();
      folder.destroy();
      item.destroy();
    },

    testSetCustomVariantByProtocol : function() {
      var TestUtil = org.eclipse.rwt.test.fixture.TestUtil;
      var shell = TestUtil.createShellByProtocol( "w2" );
      var folder = this._createTabFolderByProtocol( "w3", "w2" );
      var item = this._createTabItemByProtocol( "w4", "w3" );
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : "w4",
        "action" : "set",
        "properties" : {
          "customVariant" : "variant_blue"
        }
      } );
      assertTrue( item.hasState( "variant_blue" ) );
      shell.destroy();
      folder.destroy();
      item.destroy();
    },

    //////////////////
    // Helping methods

    _createTabFolderByProtocol : function( id, parentId ) {
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : id,
        "action" : "create",
        "type" : "rwt.widgets.TabFolder",
        "properties" : {
          "style" : [ "TOP" ],
          "parent" : parentId
        }
      } );
      return org.eclipse.rwt.protocol.ObjectManager.getObject( id );
    },

    _createTabItemByProtocol : function( id, parentId ) {
      org.eclipse.rwt.protocol.Processor.processOperation( {
        "target" : id,
        "action" : "create",
        "type" : "rwt.widgets.TabItem",
        "properties" : {
          "style" : [],
          "id" : id,
          "parent" : parentId,
          "index" : 0
        }
      } );
      return org.eclipse.rwt.protocol.ObjectManager.getObject( id );
    }

  }
  
} );