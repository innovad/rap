/*******************************************************************************
 * Copyright (c) 2008, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/

/**
 * This class provides the client-side counterpart for 
 * org.eclipse.swt.widgets.Scale.
 */
qx.Class.define( "org.eclipse.swt.widgets.Scale", {
  extend : qx.ui.layout.CanvasLayout,

  construct : function( isHorizontal ) {
    this.base( arguments );
    this.setAppearance( "scale" );
    this._horizontal = isHorizontal;
    
    // Has selection listener
    this._hasSelectionListener = false;
    
    // Flag indicates that the next request can be sent
    this._readyToSendChanges = true;
    
    // Default values
    this._selection = 0;
    this._minimum = 0;
    this._maximum = 100;
    this._increment = 1;
    this._pageIncrement = 10;    
    this._pxStep = 1.34;

    // Base line
    this._line = new qx.ui.basic.Image();
    if( this._horizontal ) {
      this._line.addState( org.eclipse.swt.widgets.Scale.STATE_HORIZONTAL );
    }
    this._line.setAppearance( "scale-line" ); 
    this._line.setResizeToInner( true ); 
    this._line.addEventListener( "mousedown", this._onLineMouseDown, this );
    this.add( this._line );

    // Thumb
    this._thumb = new org.eclipse.rwt.widgets.BasicButton( "push", true );
    if( this._horizontal ) {
      this._thumb.addState( org.eclipse.swt.widgets.Scale.STATE_HORIZONTAL );
    }
    this._thumb.setAppearance( "scale-thumb" );
    this._thumb.addEventListener( "mousedown", this._onThumbMouseDown, this );
    this._thumb.addEventListener( "mousemove", this._onThumbMouseMove, this );
    this._thumb.addEventListener( "mouseup", this._onThumbMouseUp, this );
    // Fix IE Styling issues
    org.eclipse.swt.WidgetUtil.fixIEBoxHeight( this._thumb );
    this.add( this._thumb );    
    // Thumb offset
    this._thumbOffset = 0;

    // Add events listeners
    if( this._horizontal ) {
      this.addEventListener( "changeWidth", this._onChangeWidth, this );
    } else {  
      this.addEventListener( "changeHeight", this._onChangeHeight, this );
    }
    this.addEventListener( "contextmenu", this._onContextMenu, this );
    this.addEventListener( "keypress", this._onKeyPress, this );
    this.addEventListener( "mousewheel", this._onMouseWheel, this );
  },
  
  destruct : function() {
    this._line.removeEventListener( "mousedown", this._onLineMouseDown, this );
    this._thumb.removeEventListener( "mousedown", this._onThumbMouseDown, this );
    this._thumb.removeEventListener( "mousemove", this._onThumbMouseMove, this );
    this._thumb.removeEventListener( "mouseup", this._onThumbMouseUp, this );
    if( this._horizontal ) {
      this.removeEventListener( "changeWidth", this._onChangeWidth, this );
    } else {
      this.removeEventListener( "changeHeight", this._onChangeHeight, this );
    }
    this.removeEventListener( "contextmenu", this._onContextMenu, this );
    this.removeEventListener( "keypress", this._onKeyPress, this );
    this.removeEventListener( "mousewheel", this._onMouseWheel, this );
    this._disposeObjects( "_line", "_thumb" );
    this._thumb = null;
  },
  
  events : {
    "selectionChanged" : "qx.event.type.Event",
    "minimumChanged" : "qx.event.type.Event",
    "maximumChanged" : "qx.event.type.Event"
  },

  statics : {
    STATE_HORIZONTAL : "horizontal",
    PADDING : 8,
    SCALE_LINE_OFFSET : 9,
    THUMB_OFFSET : 9,
    HALF_THUMB : 5,
    
    _isNoModifierPressed : function( evt ) {
      return    !evt.isCtrlPressed() 
             && !evt.isShiftPressed() 
             && !evt.isAltPressed() 
             && !evt.isMetaPressed();      
    }
  },
  
  members : {
    _onChangeWidth : function( evt ) {
      this._line.setWidth( this.getWidth() - 2 * org.eclipse.swt.widgets.Scale.PADDING );
      this._updateStep();
      this._updateThumbPosition();
    },
    
    _onChangeHeight : function( evt ) {
      this._line.setHeight( this.getHeight() - 2 * org.eclipse.swt.widgets.Scale.PADDING );
      this._updateStep();
      this._updateThumbPosition();
    },
    
    _onContextMenu : function( evt ) {
      var menu = this.getContextMenu();      
      if( menu != null ) {
        menu.setLocation( evt.getPageX(), evt.getPageY() );
        menu.setOpener( this );
        menu.show();
        evt.stopPropagation();
      }
    },
    
    _onKeyPress : function( evt ) {
      var keyIdentifier = evt.getKeyIdentifier();
      var sel;
      if( org.eclipse.swt.widgets.Scale._isNoModifierPressed( evt ) ) {
        switch( keyIdentifier ) {
          case "Left":
            sel = this._selection - this._increment;
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "Down":
            if( this._horizontal ) {
              sel = this._selection - this._increment;  
            } else {
              sel = this._selection + this._increment;
            }
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "Right":
            sel = this._selection + this._increment;
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "Up": 
            if( this._horizontal ) {
              sel = this._selection + this._increment;
            } else {
              sel = this._selection - this._increment;    
            }
            evt.preventDefault();
            evt.stopPropagation();
            break; 
          case "Home":
            sel = this._minimum;
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "End":
            sel = this._maximum;
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "PageDown":
            if( this._horizontal ) {
              sel = this._selection - this._pageIncrement;
            } else {
              sel = this._selection + this._pageIncrement;
            }
            evt.preventDefault();
            evt.stopPropagation();
            break;
          case "PageUp":
            if( this._horizontal ) {
              sel = this._selection + this._pageIncrement;
            } else {
              sel = this._selection - this._pageIncrement;   
            }
            evt.preventDefault();
            evt.stopPropagation();
            break;
        }
        if( sel != undefined ) {
          this.setSelection( sel );
          this._scheduleSendChanges();
        }
      }
    },
    
    _onMouseWheel : function( evt ) {
      if( this.getFocused() ) {
        evt.preventDefault();
        evt.stopPropagation();
        var change = Math.round( evt.getWheelDelta() );
        this.setSelection( this._selection - change );
        this._scheduleSendChanges();
      }
    },
    
    _onLineMouseDown : function( evt ) {
      var pxSel;
      var mousePos;
      var sel;
      if( evt.isLeftButtonPressed() ){
        if( this._horizontal ) {
          pxSel = this._thumb.getLeft() + org.eclipse.swt.widgets.Scale.HALF_THUMB;
          mousePos = evt.getPageX() - qx.bom.element.Location.getLeft( this.getElement() );
        } else {
          pxSel = this._thumb.getTop() + org.eclipse.swt.widgets.Scale.HALF_THUMB;
          mousePos = evt.getPageY() - qx.bom.element.Location.getTop( this.getElement() );
        }
        if( mousePos > pxSel ) {
          sel = this._selection + this._pageIncrement;         
        } else {
          sel = this._selection - this._pageIncrement;        
        }
        this.setSelection( sel );
        this._scheduleSendChanges();
      }
    },
    
    _onThumbMouseDown : function( evt ) {
      var mousePos;
      if( evt.isLeftButtonPressed() ) {
        if( this._horizontal ) {        
          mousePos = evt.getPageX() - qx.bom.element.Location.getLeft( this.getElement() );
          this._thumbOffset = mousePos - this._thumb.getLeft();
        } else {        
          mousePos = evt.getPageY() - qx.bom.element.Location.getTop( this.getElement() );
          this._thumbOffset = mousePos - this._thumb.getTop();  
        }
        this._thumb.setCapture(true);
      }
    },
    
    _onThumbMouseMove : function( evt ) {
      var mousePos;
      if( this._thumb.getCapture() ) {
        if( this._horizontal ) {        
          mousePos = evt.getPageX() - qx.bom.element.Location.getLeft( this.getElement() );
        } else {        
          mousePos = evt.getPageY() - qx.bom.element.Location.getTop( this.getElement() );
        }
        var sel = this._getSelectionFromThumbPosition( mousePos - this._thumbOffset );
        if( this._selection != sel ) {
          this.setSelection( sel );
          this._scheduleSendChanges();
        }
      }
    },
    
    _onThumbMouseUp : function( evt ) {
      this._thumb.setCapture( false );
    },
    
    _updateStep : function() {
      var padding =   org.eclipse.swt.widgets.Scale.PADDING
                    + org.eclipse.swt.widgets.Scale.HALF_THUMB;
      if( this._horizontal ) {
        this._pxStep = ( this.getWidth() - 2 * padding ) / ( this._maximum - this._minimum );
      } else {
        this._pxStep = ( this.getHeight() - 2 * padding ) / ( this._maximum - this._minimum );
      }
    },
    
    _updateThumbPosition : function() {
      var pos =   org.eclipse.swt.widgets.Scale.PADDING
                + this._pxStep * ( this._selection - this._minimum );
      if( this._horizontal ) {
        this._thumb.setLeft( pos );
      } else {
        this._thumb.setTop( pos );
      }
    },
    
    _getSelectionFromThumbPosition : function( position ) {
      var sel = ( position - org.eclipse.swt.widgets.Scale.PADDING ) / this._pxStep + this._minimum;
      return this._normalizeSelection( Math.round( sel ) );
    },

    _normalizeSelection : function( value ) {
      var result = value;
      if( value < this._minimum ) {
        result = this._minimum;
      } 
      if( value > this._maximum ) {
        result = this._maximum;
      }
      return result;
    },

    _scheduleSendChanges : function() {
      if( this._readyToSendChanges ) {
        this._readyToSendChanges = false;
        qx.client.Timer.once( this._sendChanges, this, 500 );
      }
    },
    
    _sendChanges : function() {
      if( !org.eclipse.swt.EventUtil.getSuspended() ) {        
        var widgetManager = org.eclipse.swt.WidgetManager.getInstance();
        var req = org.eclipse.swt.Request.getInstance();
        var id = widgetManager.findIdByWidget( this );        
        req.addParameter( id + ".selection", this._selection );
        if( this._hasSelectionListener ) {
          req.addEvent( "org.eclipse.swt.events.widgetSelected", id );
          org.eclipse.swt.EventUtil.addWidgetSelectedModifier();
          req.send();
        }
        this._readyToSendChanges = true;
      }
    },
    
    setHasSelectionListener : function( value ) {
      this._hasSelectionListener = value;
    },
    
    setSelection : function( value ) {
      this._selection = this._normalizeSelection( value );
      this._updateThumbPosition();
      this.dispatchSimpleEvent( "selectionChanged" );
    },
    
    setMinimum : function( value ) {
      this._minimum = value;
      this._updateStep();
      this._updateThumbPosition();
      this.dispatchSimpleEvent( "minimumChanged" );
    },
    
    setMaximum : function( value ) {
      this._maximum = value;
      this._updateStep();
      this._updateThumbPosition();
      this.dispatchSimpleEvent( "maximumChanged" );
    },
    
    setIncrement : function( value ) {
      this._increment = value;
    },
    
    setPageIncrement : function( value ) {
      this._pageIncrement = value;
    },
    
    // overwritten:
    _visualizeFocus : function() {
      this.base( arguments );
      this._thumb.addState( "focused" );
    },
    
    // overwritten:
    _visualizeBlur : function() {
      this.base( arguments );
      this._thumb.removeState( "focused" );
    }

  }
} );
