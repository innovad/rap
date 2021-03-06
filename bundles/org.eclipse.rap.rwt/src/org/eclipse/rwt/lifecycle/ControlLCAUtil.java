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
import java.lang.reflect.Field;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rwt.internal.lifecycle.JSConst;
import org.eclipse.rwt.internal.protocol.ClientObjectFactory;
import org.eclipse.rwt.internal.protocol.IClientObject;
import org.eclipse.rwt.internal.service.ContextProvider;
import org.eclipse.rwt.internal.util.NumberFormatUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.events.ActivateEvent;
import org.eclipse.swt.internal.events.EventLCAUtil;
import org.eclipse.swt.internal.graphics.ImageFactory;
import org.eclipse.swt.internal.widgets.*;
import org.eclipse.rwt.internal.util.ActiveKeysUtil;
import org.eclipse.swt.widgets.*;


/**
 * Utility class that provides a number of useful static methods to support the
 * implementation of life cycle adapters (LCAs) for {@link Control}s.
 *
 * @see WidgetLCAUtil
 * @since 1.0
 */
public class ControlLCAUtil {

  private static final String JS_FUNC_ADD_ACTIVATE_LISTENER_WIDGET
    = "addActivateListenerWidget";
  private static final String JS_FUNC_REMOVE_ACTIVATE_LISTENER_WIDGET
    = "removeActivateListenerWidget";
  private static final String JS_FUNC_SET_HAS_LISTENER = "setHasListener";
  private static final String JS_EVENT_TYPE_FOCUS = "focus";
  private static final String JS_EVENT_TYPE_MOUSE = "mouse";
  private static final String JS_EVENT_TYPE_MENU_DETECT = "menuDetect";

  // Property names to preserve widget property values
  private static final String PROP_ACTIVATE_LISTENER = "activate";
  private static final String PROP_FOCUS_LISTENER = "focus";
  private static final String PROP_MOUSE_LISTENER = "mouse";
  private static final String PROP_KEY_LISTENER = "key";
  private static final String PROP_TRAVERSE_LISTENER = "traverse";
  private static final String PROP_MENU_DETECT_LISTENER = "menuDetect";
  private static final String PROP_TAB_INDEX = "tabIndex";
  private static final String PROP_CURSOR = "cursor";
  private static final String PROP_BACKGROUND_IMAGE = "backgroundImage";
  private static final String PROP_BACKGROUND_REPEAT = "backgroundRepeat";
  private static final String PROP_BACKGROUND_POSITION = "backgroundPosition";
  private static final String PROP_CHILDREN = "children";

  private static final String USER_DATA_KEY_LISTENER = "keyListener";
  private static final String USER_DATA_TRAVERSE_LISTENER = "traverseListener";
  private static final String USER_DATA_BACKGROUND_IMAGE_SIZE = "backgroundImageSize";
  static final int MAX_STATIC_ZORDER = 300;

  private static final String CURSOR_UPARROW = "widget/cursors/up_arrow.cur";

  private ControlLCAUtil() {
    // prevent instance creation
  }

  ///////////////////////////////////////////////////////////
  // Methods to read and process common properties and events

  /**
   * Reads the bounds of the specified control from the current request and
   * applies it to the control. If no bounds are not submitted for the control,
   * it remains unchanged.
   *
   * @param control the control whose bounds to read and set
   */
  // TODO [rst] Revise: This seems to unnecessarily call getter and setter even
  //            when no bounds are submitted.
  public static void readBounds( Control control ) {
    Rectangle current = control.getBounds();
    Rectangle newBounds = WidgetLCAUtil.readBounds( control, current );
    control.setBounds( newBounds );
  }

  /**
   * Process a <code>HelpEvent</code> if the current request specifies that
   * there occured a help event for the given <code>widget</code>.
   *
   * @param control the control to process
   * @since 1.3
   */
  public static void processMenuDetect( Control control ) {
    if( WidgetLCAUtil.wasEventSent( control, JSConst.EVENT_MENU_DETECT ) ) {
      MenuDetectEvent event = new MenuDetectEvent( control );
      Point point = readXYParams( control,
                                  JSConst.EVENT_MENU_DETECT_X,
                                  JSConst.EVENT_MENU_DETECT_Y );
      point = control.getDisplay().map( control, null, point );
      event.x = point.x;
      event.y = point.y;
      event.processEvent();
    }
  }

  public static void processMouseEvents( Control control ) {
    if( WidgetLCAUtil.wasEventSent( control, JSConst.EVENT_MOUSE_DOWN ) ) {
      MouseEvent event = new MouseEvent( control, MouseEvent.MOUSE_DOWN );
      event.button = readIntParam( JSConst.EVENT_MOUSE_DOWN_BUTTON );
      Point point = readXYParams( control, JSConst.EVENT_MOUSE_DOWN_X, JSConst.EVENT_MOUSE_DOWN_Y );
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( JSConst.EVENT_MOUSE_DOWN_TIME );
      event.stateMask =   EventLCAUtil.readStateMask( JSConst.EVENT_MOUSE_DOWN_MODIFIER )
                        | EventLCAUtil.translateButton( event.button );
      checkAndProcessMouseEvent( event );
    }
    String eventId = JSConst.EVENT_MOUSE_DOUBLE_CLICK;
    if( WidgetLCAUtil.wasEventSent( control, eventId ) ) {
      MouseEvent event = new MouseEvent( control, MouseEvent.MOUSE_DOUBLE_CLICK );
      event.button = readIntParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_BUTTON );
      Point point = readXYParams( control,
                                  JSConst.EVENT_MOUSE_DOUBLE_CLICK_X,
                                  JSConst.EVENT_MOUSE_DOUBLE_CLICK_Y );
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( JSConst.EVENT_MOUSE_DOUBLE_CLICK_TIME );
      String stateMaskParam = JSConst.EVENT_MOUSE_DOUBLE_CLICK_MODIFIER;
      event.stateMask =   EventLCAUtil.readStateMask( stateMaskParam )
                        | EventLCAUtil.translateButton( event.button );
      checkAndProcessMouseEvent( event );
    }
    if( WidgetLCAUtil.wasEventSent( control, JSConst.EVENT_MOUSE_UP ) ) {
      MouseEvent event = new MouseEvent( control, MouseEvent.MOUSE_UP );
      event.button = readIntParam( JSConst.EVENT_MOUSE_UP_BUTTON );
      Point point = readXYParams( control, JSConst.EVENT_MOUSE_UP_X, JSConst.EVENT_MOUSE_UP_Y );
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( JSConst.EVENT_MOUSE_UP_TIME );
      event.stateMask =   EventLCAUtil.readStateMask( JSConst.EVENT_MOUSE_UP_MODIFIER )
                        | EventLCAUtil.translateButton( event.button );
      checkAndProcessMouseEvent( event );
    }
  }

  public static void processKeyEvents( final Control control ) {
    if( WidgetLCAUtil.wasEventSent( control, JSConst.EVENT_KEY_DOWN ) ) {
      final int keyCode = readIntParam( JSConst.EVENT_KEY_DOWN_KEY_CODE );
      final int charCode = readIntParam( JSConst.EVENT_KEY_DOWN_CHAR_CODE );
      final int stateMask = EventLCAUtil.readStateMask( JSConst.EVENT_KEY_DOWN_MODIFIER );
      final int traverseKey = getTraverseKey( keyCode, stateMask );
      Event event;
      if( traverseKey != SWT.TRAVERSE_NONE ) {
        event = createEvent( control, TraverseEvent.KEY_TRAVERSED );
        initializeKeyEvent( event, keyCode, charCode, stateMask );
        event.detail = traverseKey;
        TraverseEvent traverseEvent = new TraverseEvent( event );
        traverseEvent.processEvent();
      }
      event = createEvent( control, KeyEvent.KEY_PRESSED );
      initializeKeyEvent( event, keyCode, charCode, stateMask );
      KeyEvent pressedEvent = new KeyEvent( event );
      pressedEvent.processEvent();
      event = createEvent( control, KeyEvent.KEY_RELEASED );
      initializeKeyEvent( event, keyCode, charCode, stateMask );
      KeyEvent releasedEvent = new KeyEvent( event );
      releasedEvent.processEvent();
    }
  }

  public static void processSelection( Widget widget, Item item, boolean readBounds ) {
    if( WidgetLCAUtil.wasEventSent( widget, JSConst.EVENT_WIDGET_SELECTED ) ) {
      SelectionEvent event
        = createSelectionEvent( widget, item, readBounds, SelectionEvent.WIDGET_SELECTED );
      event.processEvent();
    }
    if( WidgetLCAUtil.wasEventSent( widget, JSConst.EVENT_WIDGET_DEFAULT_SELECTED ) ) {
      SelectionEvent event
        = createSelectionEvent( widget, item, readBounds, SelectionEvent.WIDGET_DEFAULT_SELECTED );
      event.processEvent();
    }
  }

  /////////////////////////////////////////////
  // Methods to preserve common property values

  /**
   * Preserves the values of the following properties of the specified control:
   * <ul>
   * <li>bounds</li>
   * <li>z-index (except for Shells)</li>
   * <li>tab index</li>
   * <li>tool tip text</li>
   * <li>menu</li>
   * <li>visible</li>
   * <li>enabled</li>
   * <li>foreground</li>
   * <li>background</li>
   * <li>background image</li>
   * <li>font</li>
   * <li>cursor</li>
   * <li>whether ControlListeners are registered</li>
   * <li>whether ActivateListeners are registered</li>
   * <li>whether MouseListeners are registered</li>
   * <li>whether FocusListeners are registered</li>
   * <li>whether KeyListeners are registered</li>
   * <li>whether TraverseListeners are registered</li>
   * <li>whether HelpListeners are registered</li>
   * <li>whether MenuDetectListeners are registered</li>
   * </ul>
   *
   * @param control the control whose parameters to preserve
   * @see #writeChanges(Control)
   */
  public static void preserveValues( Control control ) {
    IWidgetAdapter adapter = WidgetUtil.getAdapter( control );
    WidgetLCAUtil.preserveBounds( control, control.getBounds() );
    // TODO [rh] revise this (see also writeZIndex)
    if( !( control instanceof Shell ) ) {
      adapter.preserve( Props.Z_INDEX, new Integer( getZIndex( control ) ) );
    }
    adapter.preserve( PROP_CHILDREN, getChildren( control ) );
    adapter.preserve( PROP_TAB_INDEX, new Integer( getTabIndex( control ) ) );
    WidgetLCAUtil.preserveToolTipText( control, control.getToolTipText() );
    adapter.preserve( Props.MENU, control.getMenu() );
    adapter.preserve( Props.VISIBLE, Boolean.valueOf( getVisible( control ) ) );
    WidgetLCAUtil.preserveEnabled( control, control.getEnabled() );
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    WidgetLCAUtil.preserveForeground( control, controlAdapter.getUserForeground() );
    WidgetLCAUtil.preserveBackground( control,
                                      controlAdapter.getUserBackground(),
                                      controlAdapter.getBackgroundTransparency() );
    preserveBackgroundImage( control );
    preserveBackgroundRepeat( control );
    preserveBackgroundPosition( control );
    WidgetLCAUtil.preserveFont( control, controlAdapter.getUserFont() );
    adapter.preserve( PROP_CURSOR, control.getCursor() );
    WidgetLCAUtil.preserveListener( control,
                                    PROP_ACTIVATE_LISTENER,
                                    ActivateEvent.hasListener( control ) );
    WidgetLCAUtil.preserveListener( control,
                                    PROP_MOUSE_LISTENER,
                                    MouseEvent.hasListener( control ) );
    if( ( control.getStyle() & SWT.NO_FOCUS ) == 0 ) {
      WidgetLCAUtil.preserveListener( control,
                                      PROP_FOCUS_LISTENER,
                                      FocusEvent.hasListener( control ) );
    }
    WidgetLCAUtil.preserveListener( control,
                                    PROP_KEY_LISTENER,
                                    KeyEvent.hasListener( control ) );
    WidgetLCAUtil.preserveListener( control,
                                    PROP_TRAVERSE_LISTENER,
                                    TraverseEvent.hasListener( control ) );
    WidgetLCAUtil.preserveListener( control,
                                    PROP_MENU_DETECT_LISTENER,
                                    MenuDetectEvent.hasListener( control ) );
    WidgetLCAUtil.preserveHelpListener( control );
    ActiveKeysUtil.preserveActiveKeys( control );
    ActiveKeysUtil.preserveCancelKeys( control );
  }

  /**
   * Preserves the value of the specified widget's background image.
   *
   * @param control the control whose background image property to preserve
   * @see #writeBackgroundImage(Control)
   */
  public static void preserveBackgroundImage( Control control ) {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Image image = controlAdapter.getUserBackgroundImage();
    IWidgetAdapter adapter = WidgetUtil.getAdapter( control );
    adapter.preserve( PROP_BACKGROUND_IMAGE, image );
  }

  public static void preserveBackgroundRepeat( Control control ) {
    IControlAdapter controlAdapter = control.getAdapter( IControlAdapter.class );
    String repeat = controlAdapter.getUserBackgroundRepeat();
    IWidgetAdapter adapter = WidgetUtil.getAdapter( control );
    adapter.preserve( PROP_BACKGROUND_REPEAT, repeat );
  }

  public static void preserveBackgroundPosition( Control control ) {
    IControlAdapter controlAdapter = control.getAdapter( IControlAdapter.class );
    String position = controlAdapter.getUserBackgroundPosition();
    IWidgetAdapter adapter = WidgetUtil.getAdapter( control );
    adapter.preserve( PROP_BACKGROUND_POSITION, position );
  }

  ///////////////////////////////////////////
  // Methods to render common property values

  /**
   * Determines for all of the following properties of the specified control
   * whether the property has changed during the processing of the current
   * request and if so, writes a protocol message to the response that updates the
   * corresponding client-side property.
   * <ul>
   * <li>bounds</li>
   * <li>z-index (except for Shells)</li>
   * <li>tab index</li>
   * <li>tool tip text</li>
   * <li>menu</li>
   * <li>visible</li>
   * <li>enabled</li>
   * <li>foreground</li>
   * <li>background</li>
   * <li>background image</li>
   * <li>font</li>
   * <li>cursor</li>
   * <!--li>whether ControlListeners are registered</li-->
   * <li>whether ActivateListeners are registered</li>
   * <li>whether MouseListeners are registered</li>
   * <li>whether FocusListeners are registered</li>
   * <li>whether KeyListeners are registered</li>
   * <li>whether TraverseListeners are registered</li>
   * <li>whether HelpListeners are registered</li>
   * </ul>
   *
   * @param control the control whose properties to set
   * @see #preserveValues(Control)
   * @since 1.5
   */
  public static void renderChanges( Control control ) {
    renderBounds( control );
    renderChildren( control );
    renderTabIndex( control );
    renderToolTip( control );
    renderMenu( control );
    renderVisible( control );
    renderEnabled( control );
    renderForeground( control );
    renderBackground( control );
    renderBackgroundImage( control );
    renderFont( control );
    renderCursor( control );
    ActiveKeysUtil.renderActiveKeys( control );
    ActiveKeysUtil.renderCancelKeys( control );
//    TODO [rst] missing: writeControlListener( control );
    renderListenActivate( control );
    renderListenFocus( control );
    renderListenMouse( control );
    renderListenKey( control );
    renderListenTraverse( control );
    renderListenMenuDetect( control );
    WidgetLCAUtil.renderListenHelp( control );
  }

  /**
   * Determines whether the bounds of the given control have changed during the
   * processing of the current request and if so, writes JavaScript code to the
   * response that updates the client-side bounds.
   *
   * @param control the control whose bounds to write
   * @since 1.5
   */
  public static void renderBounds( Control control ) {
    WidgetLCAUtil.renderBounds( control, control.getBounds() );
  }

  static void renderChildren( Control control ) {
    if( control instanceof Composite ) {
      String[] newValue = getChildren( control );
      WidgetLCAUtil.renderProperty( control, PROP_CHILDREN, newValue, null );
    }
  }

  static void renderTabIndex( Control control ) {
    if( control instanceof Shell ) {
      resetTabIndices( ( Shell )control );
      // tabIndex must be a positive value
      computeTabIndices( ( Shell )control, 1 );
    }
    Integer newValue = new Integer( getTabIndex( control ) );
    // there is no reliable default value for all controls
    if( WidgetLCAUtil.hasChanged( control, PROP_TAB_INDEX, newValue ) ) {
      IClientObject clientObject = ClientObjectFactory.getClientObject( control );
      clientObject.set( "tabIndex", newValue );
    }
  }

  /**
   * Determines whether the tool tip of the given control has changed during the
   * processing of the current request and if so, writes JavaScript code to the
   * response that updates the client-side tool tip.
   *
   * @param control the control whose tool tip to write
   * @since 1.5
   */
  public static void renderToolTip( Control control ) {
    WidgetLCAUtil.renderToolTip( control, control.getToolTipText() );
  }

  /**
   * Determines whether the property <code>menu</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * a protocol message to the response that updates the client-side menu
   * property.
   *
   * @param control the control whose menu property to write
   * @since 1.5
   */
  public static void renderMenu( Control control ) {
    WidgetLCAUtil.renderMenu( control, control.getMenu() );
  }

  /**
   * Determines whether the visibility of the given control has changed during
   * the processing of the current request and if so, writes JavaScript code to
   * the response that updates the client-side visibility.
   *
   * @param control the control whose visibility to write
   * @since 1.5
   */
  public static void renderVisible( Control control ) {
    Boolean newValue = Boolean.valueOf( getVisible( control ) );
    Boolean defValue = control instanceof Shell ? Boolean.FALSE : Boolean.TRUE;
    // TODO [tb] : Can we have a shorthand for this, like in JSWriter?
    if( WidgetLCAUtil.hasChanged( control, Props.VISIBLE, newValue, defValue ) ) {
      IClientObject clientObject = ClientObjectFactory.getClientObject( control );
      clientObject.set( "visibility", newValue );
    }
  }

  /**
   * Determines whether the property <code>enabled</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * a protocol message to the response that updates the client-side enabled
   * property.
   *
   * @param control the control whose enabled property to write
   * @since 1.5
   */
  public static void renderEnabled( Control control ) {
    // Using isEnabled() would result in unnecessarily updating child widgets of
    // enabled/disabled controls.
    WidgetLCAUtil.renderEnabled( control, control.getEnabled() );
  }

  /**
   * Determines whether the property <code>foreground</code> of the given
   * control has changed during the processing of the current request and if so,
   * writes a protocol message to the response that updates the client-side
   * foreground property.
   *
   * @param control the control whose foreground property to write
   * @since 1.5
   */
  public static void renderForeground( Control control ) {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    WidgetLCAUtil.renderForeground( control, controlAdapter.getUserForeground() );
  }

  /**
   * Determines whether the property <code>background</code> of the given
   * control has changed during the processing of the current request and if so,
   * writes a protocol message to the response that updates the client-side
   * background property.
   *
   * @param control the control whose background property to write
   * @since 1.5
   */
  public static void renderBackground( Control control ) {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    WidgetLCAUtil.renderBackground( control,
                                    controlAdapter.getUserBackground(),
                                    controlAdapter.getBackgroundTransparency() );
  }

  /**
   * Determines whether the background image of the given control has changed
   * during the processing of the current request and if so, writes a protocol
   * message to the response that updates the client-side background image
   * property.
   *
   * @param control the control whose background image property to write
   * @since 1.5
   */
  public static void renderBackgroundImage( Control control ) {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Image image = controlAdapter.getUserBackgroundImage();
    WidgetLCAUtil.renderProperty( control, PROP_BACKGROUND_IMAGE, image, null );
  }

  /**
   * Determines whether the property <code>font</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * a protocol message to the response that updates the client-side font property.
   *
   * @param control the control whose font property to write
   * @since 1.5
   */
  public static void renderFont( Control control ) {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Font newValue = controlAdapter.getUserFont();
    WidgetLCAUtil.renderFont( control, newValue );
  }

  static void renderCursor( Control control ) {
    Cursor newValue = control.getCursor();
    if( WidgetLCAUtil.hasChanged( control, PROP_CURSOR, newValue, null ) ) {
      IClientObject clientObject = ClientObjectFactory.getClientObject( control );
      clientObject.set( PROP_CURSOR, getQxCursor( newValue ) );
    }
  }

  //////////////////
  // render listener

  static void renderListenActivate( Control control ) {
    if( !control.isDisposed() ) {
      boolean newValue = ActivateEvent.hasListener( control );
      WidgetLCAUtil.renderListener( control, PROP_ACTIVATE_LISTENER, newValue, false );
    }
  }

  /**
   * Note that there is no corresponding readData method to fire the focus
   * events that are send by the client.
   * FocusEvents are thrown when the focus is changed programmatically and when
   * it is change by the user.
   * Therefore the methods in Display that maintain the current focusControl
   * also fire FocusEvents. The current client-side focusControl is read in
   * DisplayLCA#readData.
   */
  static void renderListenFocus( Control control ) {
    if( ( control.getStyle() & SWT.NO_FOCUS ) == 0 ) {
      boolean newValue = FocusEvent.hasListener( control );
      WidgetLCAUtil.renderListener( control, PROP_FOCUS_LISTENER, newValue, false );
    }
  }

  static void renderListenMouse( Control control ) {
    boolean newValue = MouseEvent.hasListener( control );
    WidgetLCAUtil.renderListener( control, PROP_MOUSE_LISTENER, newValue, false );
  }

  static void renderListenKey( Control control ) {
    boolean newValue = KeyEvent.hasListener( control );
    WidgetLCAUtil.renderListener( control, PROP_KEY_LISTENER, newValue, false );
  }

  static void renderListenTraverse( Control control ) {
    boolean newValue = TraverseEvent.hasListener( control );
    WidgetLCAUtil.renderListener( control, PROP_TRAVERSE_LISTENER, newValue, false );
  }

  static void renderListenMenuDetect( Control control ) {
    boolean newValue = MenuDetectEvent.hasListener( control );
    WidgetLCAUtil.renderListener( control, PROP_MENU_DETECT_LISTENER, newValue, false );
  }


  //////////////////////////
  // event processing helper

  private static Event createEvent( Widget widget, int type ) {
    Event result = new Event();
    result.type = type;
    result.display = widget.getDisplay();
    result.widget = widget;
    result.doit = true;
    return result;
  }

  private static SelectionEvent createSelectionEvent( Widget widget,
                                                      Item item,
                                                      boolean readBounds,
                                                      int type )
  {
    Rectangle bounds;
    if( widget instanceof Control && readBounds ) {
      Control control = ( Control )widget;
      bounds = WidgetLCAUtil.readBounds( control, control.getBounds() );
    } else {
      bounds = new Rectangle( 0, 0, 0, 0 );
    }
    int stateMask = EventLCAUtil.readStateMask( JSConst.EVENT_WIDGET_SELECTED_MODIFIER );
    return new SelectionEvent( widget, item, type, bounds, stateMask, null, true, SWT.NONE );
  }

  private static void initializeKeyEvent( Event evt, int keyCode, int charCode, int stateMask ) {
    evt.keyCode = translateKeyCode( keyCode );
    if( charCode == 0 ) {
      if( ( evt.keyCode & SWT.KEYCODE_BIT ) == 0 ) {
        evt.character = translateCharacter( evt.keyCode );
      }
    } else {
      evt.character = translateCharacter( charCode );
      if( Character.isLetter( charCode ) ) {
        // NOTE : keycodes from browser are the upper-case character, in SWT it is the lower-case
        evt.keyCode = Character.toLowerCase( charCode );
      }
    }
    evt.stateMask = stateMask;
  }

  private static void checkAndProcessMouseEvent( MouseEvent event ) {
    boolean pass = false;
    Control control = ( Control )event.widget;
    if( control instanceof Scrollable ) {
      Scrollable scrollable = ( Scrollable )control;
      Rectangle clientArea = scrollable.getClientArea();
      pass = clientArea.contains( event.x, event.y );
    } else {
      pass = event.x >= 0 && event.y >= 0;
    }
    if( pass ) {
      event.processEvent();
    }
  }

  //////////////
  // read helper

  private static Point readXYParams( Control control, String paramNameX, String paramNameY ) {
    int x = readIntParam( paramNameX );
    int y = readIntParam( paramNameY );
    return control.getDisplay().map( null, control, x, y );
  }

  private static int readIntParam( String paramName ) {
    String value = readStringParam( paramName );
    return NumberFormatUtil.parseInt( value );
  }

  private static String readStringParam( String paramName ) {
    HttpServletRequest request = ContextProvider.getRequest();
    String value = request.getParameter( paramName );
    return value;
  }

  //////////////////////
  // widget value getter

  private static String[] getChildren( Control control ) {
    String[] result = null;
    if( control instanceof Composite ) {
      Composite composite = ( Composite )control;
      IControlHolderAdapter controlHolder = composite.getAdapter( IControlHolderAdapter.class );
      Control[] children = controlHolder.getControls();
      result = new String[ children.length ];
      for( int i = 0; i < result.length; i++ ) {
        result[ i ] = WidgetUtil.getId( children[ i ] );
      }
    }
    return result;
  }

  // [if] Fix for bug 263025, 297466, 223873 and more
  // some qooxdoo widget with size (0,0) are not invisible
  private static boolean getVisible( Control control ) {
    Point size = control.getSize();
    return control.getVisible() && size.x > 0 && size.y > 0;
  }

  // TODO [rh] Eliminate instance checks. Let the respective classes always return NO_FOCUS
  private static boolean takesFocus( Control control ) {
    boolean result = true;
    result &= ( control.getStyle() & SWT.NO_FOCUS ) == 0;
    result &= control.getClass() != Composite.class;
    result &= control.getClass() != SashForm.class;
    return result;
  }

  private static int getTabIndex( Control control ) {
    int result = -1;
    if( takesFocus( control ) ) {
      result = ControlUtil.getControlAdapter( control ).getTabIndex();
    }
    return result;
  }

  private static void resetTabIndices( Composite composite ) {
    Control[] children = composite.getChildren();
    for( int i = 0; i < children.length; i++ ) {
      Control control = children[ i ];
      ControlUtil.getControlAdapter( control ).setTabIndex( -1 );
      if( control instanceof Composite ) {
        resetTabIndices( ( Composite )control );
      }
    }
  }

  /**
   * Recursively computes the tab indices for all child controls of a given
   * composite and stores the resulting values in the control adapters.
   */
  private static int computeTabIndices( Composite composite, int startIndex ) {
    Control[] tabList = composite.getTabList();
    int result = startIndex;
    for( int i = 0; i < tabList.length; i++ ) {
      Control control = tabList[ i ];
      IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
      controlAdapter.setTabIndex( result );
      // for Links, leave a range out to be assigned to hrefs on the client
      if( control instanceof Link ) {
        result += 300;
      } else {
        result += 1;
      }
      if( control instanceof Composite ) {
        result = computeTabIndices( ( Composite )control, result );
      }
    }
    return result;
  }

  static int getTraverseKey( int keyCode, int stateMask ) {
    int result = SWT.TRAVERSE_NONE;
    switch( keyCode ) {
      case 27:
        result = SWT.TRAVERSE_ESCAPE;
      break;
      case 13:
        result = SWT.TRAVERSE_RETURN;
      break;
      case 9:
        if( ( stateMask & SWT.MODIFIER_MASK ) == 0 ) {
          result = SWT.TRAVERSE_TAB_NEXT;
        } else if( stateMask == SWT.SHIFT ) {
          result = SWT.TRAVERSE_TAB_PREVIOUS;
        }
      break;
    }
    return result;
  }

  static int translateKeyCode( int keyCode ) {
    int result;
    switch( keyCode ) {
      case 16:
        result = SWT.SHIFT;
      break;
      case 17:
        result = SWT.CONTROL;
      break;
      case 18:
        result = SWT.ALT;
      break;
      case 20:
        result = SWT.CAPS_LOCK;
      break;
      case 38:
        result = SWT.ARROW_UP;
      break;
      case 37:
        result = SWT.ARROW_LEFT;
      break;
      case 39:
        result = SWT.ARROW_RIGHT;
      break;
      case 40:
        result = SWT.ARROW_DOWN;
      break;
      case 33:
        result = SWT.PAGE_UP;
      break;
      case 34:
        result = SWT.PAGE_DOWN;
      break;
      case 35:
        result = SWT.END;
      break;
      case 36:
        result = SWT.HOME;
      break;
      case 45:
        result = SWT.INSERT;
      break;
      case 46:
        result = SWT.DEL;
      break;
      case 112:
        result = SWT.F1;
      break;
      case 113:
        result = SWT.F2;
      break;
      case 114:
        result = SWT.F3;
      break;
      case 115:
        result = SWT.F4;
      break;
      case 116:
        result = SWT.F5;
      break;
      case 117:
        result = SWT.F6;
      break;
      case 118:
        result = SWT.F7;
      break;
      case 119:
        result = SWT.F8;
      break;
      case 120:
        result = SWT.F9;
      break;
      case 121:
        result = SWT.F10;
      break;
      case 122:
        result = SWT.F11;
      break;
      case 123:
        result = SWT.F12;
      break;
      case 144:
        result = SWT.NUM_LOCK;
      break;
      case 44:
        result = SWT.PRINT_SCREEN;
      break;
      case 145:
        result = SWT.SCROLL_LOCK;
      break;
      case 19:
        result = SWT.PAUSE;
      break;
      default:
        result = keyCode;
    }
    return result;
  }

  private static char translateCharacter( int keyCode ) {
    char result = ( char )0;
    if( Character.isDefined( ( char )keyCode ) ) {
      result = ( char )keyCode;
    }
    return result;
  }

  private static String getQxCursor( Cursor newValue ) {
    String result = null;
    if( newValue != null ) {
      // TODO [rst] Find a better way of obtaining the Cursor value
      // TODO [tb] adjust strings to match name of constants
      int value = 0;
      try {
        Class cursorClass = Cursor.class;
        Field field = cursorClass.getDeclaredField( "value" );
        field.setAccessible( true );
        value = field.getInt( newValue );
      } catch( Exception e ) {
        throw new RuntimeException( e );
      }
      switch( value ) {
        case SWT.CURSOR_ARROW:
          result = "default";
        break;
        case SWT.CURSOR_WAIT:
          result = "wait";
        break;
        case SWT.CURSOR_APPSTARTING:
          result = "progress";
          break;
        case SWT.CURSOR_CROSS:
          result = "crosshair";
        break;
        case SWT.CURSOR_HELP:
          result = "help";
        break;
        case SWT.CURSOR_SIZEALL:
          result = "move";
        break;
        case SWT.CURSOR_SIZENS:
          result = "row-resize";
        break;
        case SWT.CURSOR_SIZEWE:
          result = "col-resize";
        break;
        case SWT.CURSOR_SIZEN:
          result = "n-resize";
        break;
        case SWT.CURSOR_SIZES:
          result = "s-resize";
        break;
        case SWT.CURSOR_SIZEE:
          result = "e-resize";
        break;
        case SWT.CURSOR_SIZEW:
          result = "w-resize";
        break;
        case SWT.CURSOR_SIZENE:
        case SWT.CURSOR_SIZENESW:
          result = "ne-resize";
        break;
        case SWT.CURSOR_SIZESE:
          result = "se-resize";
        break;
        case SWT.CURSOR_SIZESW:
          result = "sw-resize";
        break;
        case SWT.CURSOR_SIZENW:
        case SWT.CURSOR_SIZENWSE:
          result = "nw-resize";
        break;
        case SWT.CURSOR_IBEAM:
          result = "text";
        break;
        case SWT.CURSOR_HAND:
          result = "pointer";
        break;
        case SWT.CURSOR_NO:
          result = "not-allowed";
        break;
        case SWT.CURSOR_UPARROW:
          result = CURSOR_UPARROW;
        break;
      }
    }
    return result;
  }

  /////////////////////
  // Deprecated methods

  /**
   * Determines for all of the following properties of the specified control
   * whether the property has changed during the processing of the current
   * request and if so, writes JavaScript code to the response that updates the
   * corresponding client-side property.
   * <ul>
   * <li>bounds</li>
   * <li>z-index (except for Shells)</li>
   * <li>tab index</li>
   * <li>tool tip text</li>
   * <li>menu</li>
   * <li>visible</li>
   * <li>enabled</li>
   * <li>foreground</li>
   * <li>background</li>
   * <li>background image</li>
   * <li>font</li>
   * <li>cursor</li>
   * <!--li>whether ControlListeners are registered</li-->
   * <li>whether ActivateListeners are registered</li>
   * <li>whether MouseListeners are registered</li>
   * <li>whether FocusListeners are registered</li>
   * <li>whether KeyListeners are registered</li>
   * <li>whether TraverseListeners are registered</li>
   * <li>whether HelpListeners are registered</li>
   * </ul>
   *
   * @param control the control whose properties to set
   * @throws IOException
   * @see #preserveValues(Control)
   * @deprecated Use {@link #renderChanges(Control)}
   */
  @Deprecated
  public static void writeChanges( Control control ) throws IOException {
    writeBounds( control );
    writeZIndex( control );
    writeTabIndex( control );
    writeToolTip( control );
    writeMenu( control );
    writeVisible( control );
    writeEnabled( control );
    writeForeground( control );
    writeBackground( control );
    writeBackgroundImage( control );
    writeFont( control );
    writeCursor( control );
//    TODO [rst] missing: writeControlListener( control );
    writeActivateListener( control );
    writeFocusListener( control );
    writeMouseListener( control );
    writeKeyListener( control );
    writeTraverseListener( control );
    writeMenuDetectListener( control );
    WidgetLCAUtil.writeHelpListener( control );
  }

  /**
   * Determines whether the bounds of the given control have changed during the
   * processing of the current request and if so, writes JavaScript code to the
   * response that updates the client-side bounds.
   *
   * @param control the control whose bounds to write
   * @throws IOException
   * @deprecated Use {@link #renderBounds(Control)} instead
   */
  @Deprecated
  public static void writeBounds( Control control ) throws IOException {
    Composite parent = control.getParent();
    WidgetLCAUtil.writeBounds( control, parent, control.getBounds() );
  }

  /**
   * Determines whether the z-index of the given control has changed during the
   * processing of the current request and if so, writes JavaScript code to the
   * response that updates the client-side z-index.
   *
   * @param control the control whose z-index to write
   * @throws IOException
   * @deprecated This method should not be used anymore as zIndex is not rendered directly anymore.
   */
  @Deprecated
  public static void writeZIndex( Control control ) throws IOException {
    // TODO [rst] remove surrounding if statement as soon as z-order on shells
    //      is completely implemented
    if( !( control instanceof Shell ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      Integer newValue = new Integer( getZIndex( control ) );
      writer.set( Props.Z_INDEX, JSConst.QX_FIELD_Z_INDEX, newValue, null );
    }
  }

  /**
   * Determines whether the tool tip of the given control has changed during the
   * processing of the current request and if so, writes JavaScript code to the
   * response that updates the client-side tool tip.
   *
   * @param control the control whose tool tip to write
   * @throws IOException
   * @deprecated Use {@link #renderToolTip(Control)} instead
   */
  @Deprecated
  public static void writeToolTip( Control control ) throws IOException {
    WidgetLCAUtil.writeToolTip( control, control.getToolTipText() );
  }

  /**
   * Determines whether the property <code>menu</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * JavaScript code to the response that updates the client-side menu
   * property.
   *
   * @param control the control whose menu property to write
   * @throws IOException
   * @deprecated Use {@link #renderMenu(Control)} instead
   */
  @Deprecated
  public static void writeMenu( Control control ) throws IOException {
    // [if] Write the shell context menu in Shell#writePopupMenu(),
    // otherwise the shell.setContextMenu is called before the actual creation
    // of the client menu widget. See bug 223879.
    if( !( control instanceof Shell ) ) {
      WidgetLCAUtil.writeMenu( control, control.getMenu() );
    }
  }

  /**
   * Determines whether the visibility of the given control has changed during
   * the processing of the current request and if so, writes JavaScript code to
   * the response that updates the client-side visibility.
   *
   * @param control the control whose visibility to write
   * @throws IOException
   * @deprecated Use {@link #renderVisible(Control)} instead
   */
  // TODO [rh] there seems to be a qooxdoo problem when trying to change the
  //      visibility of a newly created widget (no flushGlobalQueues was called)
  //      MSG: Modification of property "visibility" failed with exception:
  //           Error - Element must be created previously!
  @Deprecated
  public static void writeVisible( Control control ) throws IOException {
    // we only need getVisible here (not isVisible), as qooxdoo also hides/shows
    // contained controls
    Boolean newValue = Boolean.valueOf( getVisible( control ) );
    Boolean defValue = Boolean.TRUE;
    JSWriter writer = JSWriter.getWriterFor( control );
    writer.set( Props.VISIBLE, JSConst.QX_FIELD_VISIBLE, newValue, defValue );
  }

  /**
   * Determines whether the property <code>enabled</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * JavaScript code to the response that updates the client-side enabled
   * property.
   *
   * @param control the control whose enabled property to write
   * @throws IOException
   * @deprecated Use {@link #renderEnabled(Control)} instead
   */
  @Deprecated
  public static void writeEnabled( Control control ) throws IOException {
    // Using isEnabled() would result in unnecessarily updating child widgets of
    // enabled/disabled controls.
    WidgetLCAUtil.writeEnabled( control, control.getEnabled() );
  }

  /**
   * Determines whether the property <code>foreground</code> of the given
   * control has changed during the processing of the current request and if so,
   * writes JavaScript code to the response that updates the client-side
   * foreground property.
   *
   * @param control the control whose foreground property to write
   * @throws IOException
   * @deprecated Use {@link #renderForeground(Control)} instead
   */
  @Deprecated
  public static void writeForeground( Control control ) throws IOException {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    WidgetLCAUtil.writeForeground( control, controlAdapter.getUserForeground() );
  }

  /**
   * Determines whether the property <code>background</code> of the given
   * control has changed during the processing of the current request and if so,
   * writes JavaScript code to the response that updates the client-side
   * background property.
   *
   * @param control the control whose background property to write
   * @throws IOException
   * @deprecated Use {@link #renderBackground(Control)} instead
   */
  @Deprecated
  public static void writeBackground( Control control ) throws IOException {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    WidgetLCAUtil.writeBackground( control,
                                   controlAdapter.getUserBackground(),
                                   controlAdapter.getBackgroundTransparency() );
  }

  /**
   * Determines whether the background image of the given control has changed
   * during the processing of the current request and if so, writes JavaScript
   * code to the response that updates the client-side background image
   * property.
   *
   * @param control the control whose background image property to write
   * @throws IOException
   * @deprecated Use {@link #renderBackgroundImage(Control)} instead
   */
  @Deprecated
  public static void writeBackgroundImage( Control control ) throws IOException {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Image image = controlAdapter.getUserBackgroundImage();
    String repeat = controlAdapter.getUserBackgroundRepeat();
    String position = controlAdapter.getUserBackgroundPosition();
    if( WidgetLCAUtil.hasChanged( control, PROP_BACKGROUND_IMAGE, image, null )
        || WidgetLCAUtil.hasChanged( control, PROP_BACKGROUND_REPEAT, repeat, null )
        || WidgetLCAUtil.hasChanged( control, PROP_BACKGROUND_POSITION, position, null ))
    {
      JSWriter writer = JSWriter.getWriterFor( control );
      if( image != null ) {
        String imagePath = ImageFactory.getImagePath( image );
        Rectangle bounds = image.getBounds();
        Object[] args = new Object[] {
          USER_DATA_BACKGROUND_IMAGE_SIZE,
          new Integer[]{
            new Integer( bounds.width ),
            new Integer( bounds.height )
          }
        };
        writer.call( "setUserData", args );
        writer.set( "backgroundImage", imagePath );
      } else {
        Object[] args = new Object[]{ USER_DATA_BACKGROUND_IMAGE_SIZE, null };
        writer.call( "setUserData", args );
        writer.reset( "backgroundImage" );
      }
      writer.set( "backgroundRepeat", repeat );
      writer.set( "backgroundPosition", position );
    }
  }

  /**
   * Determines whether the property <code>font</code> of the given control
   * has changed during the processing of the current request and if so, writes
   * JavaScript code to the response that updates the client-side font property.
   *
   * @param control the control whose font property to write
   * @throws IOException
   * @deprecated Use {@link #renderFont(Control)} instead
   */
  @Deprecated
  public static void writeFont( Control control ) throws IOException {
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Font newValue = controlAdapter.getUserFont();
    WidgetLCAUtil.writeFont( control, newValue );
  }

  /**
   * @deprecated Use {@link #renderListenActivate(Control)} instead
   */
  @Deprecated
  public static void writeActivateListener( Control control ) throws IOException {
    if( !control.isDisposed() ) {
      Boolean newValue = Boolean.valueOf( ActivateEvent.hasListener( control ) );
      Boolean defValue = Boolean.FALSE;
      String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_ACTIVATE_LISTENER;
      Shell shell = control.getShell();
      if( !shell.isDisposed() && WidgetLCAUtil.hasChanged( control, prop, newValue, defValue ) ) {
        String function =   newValue.booleanValue()
                          ? JS_FUNC_ADD_ACTIVATE_LISTENER_WIDGET
                          : JS_FUNC_REMOVE_ACTIVATE_LISTENER_WIDGET;
        JSWriter writer = JSWriter.getWriterFor( control );
        writer.call( shell, function, new Object[]{ control } );
      }
    }
  }

  /**
   * Adds or removes client-side menu detect listeners for the the given
   * <code>control</code> as necessary.
   *
   * @param control
   * @since 1.3
   * @deprecated Use {@link #renderListenMenuDetect(Control)} instead
   */
  @Deprecated
  public static void writeMenuDetectListener( Control control ) throws IOException {
    String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_MENU_DETECT_LISTENER;
    Boolean hasLsnr = Boolean.valueOf( MenuDetectEvent.hasListener( control ) );
    if( WidgetLCAUtil.hasChanged( control, prop, hasLsnr, Boolean.FALSE ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      Object[] args = new Object[] { control, JS_EVENT_TYPE_MENU_DETECT, hasLsnr };
      writer.call( JSWriter.WIDGET_MANAGER_REF, JS_FUNC_SET_HAS_LISTENER, args );
    }
  }

  /**
   * Checks the given control for common SWT style flags (e.g.
   * <code>SWT.BORDER</code>) and if present, writes code to pass the according
   * states to the client.
   *
   * @param control
   * @throws IOException
   * @deprecated Use {@link WidgetLCAUtil#getStyles(Widget, String[])} to obtain
   *             the list of styles instead and set the result to the property
   *             <code>style</code>
   */
  @Deprecated
  public static void writeStyleFlags( Control control ) throws IOException {
    WidgetLCAUtil.writeStyleFlag( control, SWT.BORDER, "BORDER" );
  }

  @Deprecated
  private static void writeTabIndex( Control control ) throws IOException {
    if( control instanceof Shell ) {
      resetTabIndices( ( Shell )control );
      // tabIndex must be a positive value
      computeTabIndices( ( Shell )control, 1 );
    }
    int tabIndex = getTabIndex( control );
    Integer newValue = new Integer( tabIndex );
    JSWriter writer = JSWriter.getWriterFor( control );
    // there is no reliable default value for all controls
    writer.set( PROP_TAB_INDEX, JSConst.QX_FIELD_TAB_INDEX, newValue );
  }

  @Deprecated
  static void writeCursor( Control control ) throws IOException {
    Cursor newValue = control.getCursor();
    if( WidgetLCAUtil.hasChanged( control, PROP_CURSOR, newValue, null ) ) {
      String qxCursor = getQxCursor( newValue );
      JSWriter writer = JSWriter.getWriterFor( control );
      if( qxCursor == null ) {
        writer.reset( JSConst.QX_FIELD_CURSOR );
      } else {
        writer.set( JSConst.QX_FIELD_CURSOR, qxCursor );
      }
    }
  }

  @Deprecated
  static void resetActivateListener( Control control ) throws IOException {
    // TODO [tb] : no longer neeed for widgets that are disposed via protocol
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    Shell shell = controlAdapter.getShell();
    if( !shell.isDisposed() && ActivateEvent.hasListener( control ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      writer.call( shell, JS_FUNC_REMOVE_ACTIVATE_LISTENER_WIDGET, new Object[] { control } );
    }
  }

  @Deprecated
  private static void writeFocusListener( Control control ) throws IOException {
    if( ( control.getStyle() & SWT.NO_FOCUS ) == 0 ) {
      String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_FOCUS_LISTENER;
      Boolean hasListener = Boolean.valueOf( FocusEvent.hasListener( control ) );
      if( WidgetLCAUtil.hasChanged( control, prop, hasListener, Boolean.FALSE ) ) {
        JSWriter writer = JSWriter.getWriterFor( control );
        Object[] args = new Object[] { control, JS_EVENT_TYPE_FOCUS, hasListener };
        writer.call( JSWriter.WIDGET_MANAGER_REF, JS_FUNC_SET_HAS_LISTENER, args );
      }
    }
  }

  @Deprecated
  private static void writeMouseListener( Control control ) throws IOException {
    String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_MOUSE_LISTENER;
    Boolean hasListener = Boolean.valueOf( MouseEvent.hasListener( control ) );
    if( WidgetLCAUtil.hasChanged( control, prop, hasListener, Boolean.FALSE ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      Object[] args = new Object[] { control, JS_EVENT_TYPE_MOUSE, hasListener };
      writer.call( JSWriter.WIDGET_MANAGER_REF, JS_FUNC_SET_HAS_LISTENER, args );
    }
  }

  @Deprecated
  static void writeKeyListener( Control control ) throws IOException {
    String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_KEY_LISTENER;
    Boolean hasListener = Boolean.valueOf( KeyEvent.hasListener( control ) );
    Boolean defValue = Boolean.FALSE;
    if( WidgetLCAUtil.hasChanged( control, prop, hasListener, defValue ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      if( hasListener.booleanValue() ) {
        Object[] args = new Object[] { USER_DATA_KEY_LISTENER, hasListener };
        writer.call( "setUserData", args );
      } else {
        Object[] args = new Object[] { USER_DATA_KEY_LISTENER, null };
        writer.call( "setUserData", args );
      }
    }
  }

  @Deprecated
  static void writeTraverseListener( Control control ) throws IOException {
    String prop = WidgetLCAUtil.LISTENER_PREFIX + PROP_TRAVERSE_LISTENER;
    Boolean hasListener = Boolean.valueOf( TraverseEvent.hasListener( control ) );
    Boolean defValue = Boolean.FALSE;
    if( WidgetLCAUtil.hasChanged( control, prop, hasListener, defValue ) ) {
      JSWriter writer = JSWriter.getWriterFor( control );
      if( hasListener.booleanValue() ) {
        Object[] args = new Object[] { USER_DATA_TRAVERSE_LISTENER, hasListener };
        writer.call( "setUserData", args );
      } else {
        Object[] args = new Object[] { USER_DATA_TRAVERSE_LISTENER, null };
        writer.call( "setUserData", args );
      }
    }
  }

  /**
   * Preserves whether the given <code>widget</code> has one or more
   * <code>MenuDetect</code>s attached.
   *
   * @param control the widget to preserve
   * @since 1.3
   * @deprecated Use {@link WidgetLCAUtil#preserveListener(Widget, String, boolean)} instead
   */
  @Deprecated
  public static void preserveMenuDetectListener( Control control ) {
    WidgetLCAUtil.preserveListener( control,
                                    PROP_MENU_DETECT_LISTENER,
                                    MenuDetectEvent.hasListener( control ) );
  }

  /**
   * Determines the z-index to render for a given control.
   * @param control the control whose z-index is requested
   * @return the z-index
   * @deprecated This method should not be used anymore.
   */
  // TODO [rst] also document the meaning of the returned number
  @Deprecated
  public static int getZIndex( Control control ) {
    int max = MAX_STATIC_ZORDER;
    Composite parent = control.getParent();
    if( parent != null ) {
      max = Math.max( ControlHolder.size( parent ), max );
    }
    IControlAdapter controlAdapter = ControlUtil.getControlAdapter( control );
    return max - controlAdapter.getZIndex();
  }

}
