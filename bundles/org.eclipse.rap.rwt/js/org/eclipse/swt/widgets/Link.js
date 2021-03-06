/*******************************************************************************
 * Copyright (c) 2009, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/

/**
 * This class provides the client-side implementation for
 * org.eclipse.swt.widgets.Link
 */
qx.Class.define( "org.eclipse.swt.widgets.Link", {
  extend : qx.ui.layout.CanvasLayout,

  construct : function() {
    this.base( arguments );
    this.setAppearance( "link" );
    this._text = "";
    this._hasSelectionListener = false;
    this._hyperlinksHaveListeners = false;
    this._readyToSendChanges = true;
    this._focusedLinkIndex = -1;
    this._linksCount = 0;
    this._link = new qx.ui.embed.HtmlEmbed();
    this._link.setAppearance( "link-text" );
    this.add( this._link );
    this.setSelectable( false );
    this.setHideFocus( true );
    this.__onMouseDown = qx.lang.Function.bindEvent( this._onMouseDown, this );
    this.__onKeyDown = qx.lang.Function.bindEvent( this._onKeyDown, this );
    this.addEventListener( "appear", this._onAppear, this );
    this.addEventListener( "changeEnabled", this._onChangeEnabled, this );
    this.addEventListener( "keypress", this._onKeyPress );
    this.addEventListener( "focusout", this._onFocusOut );
    this._link.addEventListener( "changeHtml", this._onChangeHtml, this );
  },

  destruct : function() {
    this._removeEventListeners();
    delete this.__onMouseDown;
    delete this.__onKeyDown;
    this.removeEventListener( "appear", this._onAppear, this );
    this.removeEventListener( "changeEnabled", this._onChangeEnabled, this );
    this.removeEventListener( "keypress", this._onKeyPress );
    this.removeEventListener( "focusout", this._onFocusOut );
    this._link.removeEventListener( "changeHtml", this._onChangeHtml, this );
    this._link.dispose();
  },

  members : {

    _onAppear : function( evt ) {
      this._link.setTabIndex( null );
      this._link.setHideFocus( true );
      this._applyHyperlinksStyleProperties();
      this._addEventListeners();
    },

    _onChangeHtml : function( evt ) {
      this._applyHyperlinksStyleProperties();
      this._addEventListeners();
    },

    _applyTextColor : function( value, old ) {
      this.base( arguments, value, old );
      this._applyHyperlinksStyleProperties();
    },

    _onChangeEnabled : function( evt ) {
      this._applyHyperlinksStyleProperties();
      this._changeHyperlinksTabIndexProperty();
    },

    _getStates : function() {
      if( !this.__states ) {
        this.__states = {};
      }
      return this.__states;
    },

    addState : function( state ) {
      this.base( arguments, state );
      this._link.addState( state );
    },

    removeState : function( state ) {
      this.base( arguments, state );
      this._link.removeState( state );
    },

    setHasSelectionListener : function( value ) {
      this._hasSelectionListener = value;
    },

    addText : function( text ) {
      this._text += text;
    },

    addLink : function( text, index ) {
      var widgetManager = org.eclipse.swt.WidgetManager.getInstance();
      var id = widgetManager.findIdByWidget( this ) + "#" + index;
      this._text += "<span tabIndex=\"1\" ";
      this._text += "style=\"";
      this._text += "text-decoration:underline; ";
      this._text += "\" ";
      this._text += "id=\"" + id + "\"";
      this._text += ">";
      this._text += text;
      this._text += "</span>";
      this._linksCount++;
    },

    applyText : function() {
      this._link.setHtml( this._text );
      if( this._linksCount === 0 ) {
        this.setTabIndex( null );
      } else {
        this.setTabIndex( 1 );
      }
    },

    clear : function() {
      this._removeEventListeners();
      this._text = "";
      this._linksCount = 0;
      this._focusedLinkIndex = -1;
    },

    _applyHyperlinksStyleProperties : function() {
      var themeValues = new org.eclipse.swt.theme.ThemeValues( this._getStates() );
      var linkColor = themeValues.getCssColor( "Link-Hyperlink", "color" );
      var linkShadow = themeValues.getCssShadow( "Link-Hyperlink", "text-shadow" );
      themeValues.dispose();
      var hyperlinks = this._getHyperlinkElements();
      for( var i = 0; i < hyperlinks.length; i++ ) {
        org.eclipse.rwt.HtmlUtil.setStyleProperty( hyperlinks[ i ], "color", linkColor );
        org.eclipse.rwt.HtmlUtil.setTextShadow( hyperlinks[ i ], linkShadow );
        if( this.isEnabled() ) {
          hyperlinks[ i ].style.cursor = "pointer";
        } else {
          hyperlinks[ i ].style.cursor = "default";
        }
      }
    },

    _changeHyperlinksTabIndexProperty : function() {
      var hyperlinks = this._getHyperlinkElements();
      for( var i = 0; i < hyperlinks.length; i++ ) {
        if( this.isEnabled() ) {
          hyperlinks[ i ].tabIndex = "1";
        } else {
          hyperlinks[ i ].tabIndex = "-1";
        }
      }
    },

    _addEventListeners : function() {
      var hyperlinks = this._getHyperlinkElements();
      if( hyperlinks.length > 0 && !this._hyperlinksHaveListeners ) {
        for( var i = 0; i < hyperlinks.length; i++ ) {
          qx.html.EventRegistration.addEventListener( hyperlinks[ i ],
                                                      "mousedown",
                                                      this.__onMouseDown );
          qx.html.EventRegistration.addEventListener( hyperlinks[ i ],
                                                      "keydown",
                                                      this.__onKeyDown );
        }
        this._hyperlinksHaveListeners = true;
      }
    },

    _removeEventListeners : function() {
      var hyperlinks = this._getHyperlinkElements();
      if( hyperlinks.length > 0 && this._hyperlinksHaveListeners ) {
        for( var i = 0; i < hyperlinks.length; i++ ) {
          qx.html.EventRegistration.removeEventListener( hyperlinks[ i ],
                                                         "mousedown",
                                                         this.__onMouseDown );
          qx.html.EventRegistration.removeEventListener( hyperlinks[ i ],
                                                         "keydown",
                                                         this.__onKeyDown );
        }
        this._hyperlinksHaveListeners = false;
      }
    },

    _onMouseDown : function( evt ) {
      var target = this._getEventTarget( evt );
      var index = this._getLinkIndex( target );
      this._setFocusedLink( index );
      var leftBtnPressed = this._isLeftMouseButtonPressed( evt );
      if( this.isEnabled() && leftBtnPressed && this._readyToSendChanges ) {
        // [if] Fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=252559
        this._readyToSendChanges = false;
        qx.client.Timer.once( function() {
          this._sendChanges( index );
        }, this, org.eclipse.swt.EventUtil.DOUBLE_CLICK_TIME );
      }
    },

    _isLeftMouseButtonPressed : function( evt ) {
      var leftBtnPressed;
      if( evt.which ) {
        leftBtnPressed = ( evt.which === 1 );
      } else if( evt.button ) {
        if( org.eclipse.rwt.Client.isMshtml() ) {
          leftBtnPressed = ( evt.button === 1 );
        } else {
          leftBtnPressed = ( evt.button === 0 );
        }
      }
      return leftBtnPressed;
    },

    _onKeyDown : function( evt ) {
      if( this.isEnabled() && evt.keyCode === 13 ) {
        var target = this._getEventTarget( evt );
        var index = this._getLinkIndex( target );
        this._sendChanges( index );
      }
    },

    _getLinkIndex : function( element ) {
      var id = element.id;
      var index = id.substr( id.lastIndexOf( "#" ) + 1 );
      return parseInt( index, 10 );
    },

    _getEventTarget : function( evt ) {
      var target;
      if( org.eclipse.rwt.Client.isMshtml() ) {
        target = window.event.srcElement;
      } else {
        target = evt.target;
      }
      return target;
    },

    // Override of the _ontabfocus method from qx.ui.core.Widget
    _ontabfocus : function() {
      if( this._focusedLinkIndex === -1 && this._linksCount > 0 ) {
        this._setFocusedLink( 0 );
      }
    },

    _onKeyPress : function( evt ) {
      if( this.isFocused() && evt.getKeyIdentifier() === "Tab" && this._linksCount > 0 ) {
        var index = this._focusedLinkIndex;
        if( !evt.isShiftPressed() && index >= 0 && index < this._linksCount - 1 ) {
          evt.stopPropagation();
          evt.preventDefault();
          this._setFocusedLink( index + 1 );
        } else if( !evt.isShiftPressed() && index === -1 ) {
          evt.stopPropagation();
          evt.preventDefault();
          this._setFocusedLink( 0 );
        } else if( evt.isShiftPressed() && index > 0 && index <= this._linksCount - 1 ) {
          evt.stopPropagation();
          evt.preventDefault();
          this._setFocusedLink( index - 1 );
        }
      }
    },

    _onFocusOut : function( evt ) {
      this._setFocusedLink( -1 );
    },

    _setFocusedLink : function( index ) {
      var hyperlink = this._getFocusedHyperlinkElement();
      if( hyperlink !== null ) {
        hyperlink.blur();
        hyperlink.style.outline = "none";
      }
      this._focusedLinkIndex = index;
      hyperlink = this._getFocusedHyperlinkElement();
      if( hyperlink !== null ) {
        hyperlink.focus();
        hyperlink.style.outline = "1px dotted";
      }
    },

    _getFocusedHyperlinkElement : function() {
      var result = null;
      var hyperlinks = this._getHyperlinkElements();
      var index = this._focusedLinkIndex;
      if( index >= 0 && index < hyperlinks.length ) {
        result = hyperlinks[ index ];
      }
      return result;
    },

    _getHyperlinkElements : function() {
      var result;
      var linkElement = this.getElement();
      if( linkElement ) {
        result = linkElement.getElementsByTagName( "span" );
      } else {
        result = [];
      }
      return result;
    },

    _sendChanges : function( index ) {
      if( !org.eclipse.swt.EventUtil.getSuspended() ) {
        var widgetManager = org.eclipse.swt.WidgetManager.getInstance();
        var id = widgetManager.findIdByWidget( this );
        var req = org.eclipse.swt.Request.getInstance();
        if( this._hasSelectionListener ) {
          req.addEvent( "org.eclipse.swt.events.widgetSelected", id );
          org.eclipse.swt.EventUtil.addWidgetSelectedModifier();
          req.addEvent( "org.eclipse.swt.events.widgetSelected.index", index );
          req.send();
        }
      }
      this._readyToSendChanges = true;
    }

  }

} );
