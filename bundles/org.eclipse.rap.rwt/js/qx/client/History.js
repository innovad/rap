/*******************************************************************************
 *  Copyright: 2004, 2012 1&1 Internet AG, Germany, http://www.1und1.de,
 *                        and EclipseSource
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    1&1 Internet AG and others - original API and implementation
 *    EclipseSource - adaptation for the Eclipse Rich Ajax Platform
 *
 *
 *   This class contains code based on the following work:
 *
 *   * Yahoo! UI Library
 *     http://developer.yahoo.com/yui
 *     Version 2.2.0
 *
 *     Copyright:
 *       (c) 2007, Yahoo! Inc.
 *
 *     License:
 *       BSD: http://developer.yahoo.com/yui/license.txt
 *
 ******************************************************************************/

/**
 * A helper for using the browser history in JavaScript Applications without
 * reloading the main page.
 *
 * Adds entries to the browser history and fires a "request" event when one of
 * the entries was requested by the user (e.g. by clicking on the back button).
 *
 * Browser history support is currently available for Internet Explorer 6/7,
 * Firefox, Opera 9 and WebKit. Safari 2 and older are not yet supported.
 *
 * This module is based on the ideas behind the YUI Browser History Manager
 * by Julien Lecomte (Yahoo), which is described at
 * http://yuiblog.com/blog/2007/02/21/browser-history-manager/. The Yahoo
 * implementation can be found at http://developer.yahoo.com/yui/history.
 * The original code is licensed under a BSD license
 * (http://developer.yahoo.com/yui/license.txt).
 */
qx.Class.define("qx.client.History",
{
  type : "singleton",
  extend : qx.core.Target,




  /*
  *****************************************************************************
     CONSTRUCTOR
  *****************************************************************************
  */

  /**
   * @signature function()
   */
  construct : qx.core.Variant.select("qx.client",
  {
    "mshtml" : function()
    {
      this.base(arguments);
      this._hasNavigationListener = false;

      this._iframe = document.createElement("iframe");
      this._iframe.style.visibility = "hidden";
      this._iframe.style.position = "absolute";
      this._iframe.style.left = "-1000px";
      this._iframe.style.top = "-1000px";

      /*
       * IMPORTANT NOTE FOR IE:
       * Setting the source before adding the iframe to the document.
       * Otherwise IE will bring up a "Unsecure items ..." warning in SSL mode
       */
      var src = qx.io.Alias.getInstance().resolve("static/html/blank.html");
      this._iframe.src = src;
      document.body.appendChild(this._iframe);

      this._titles = {};
      this._state = decodeURIComponent(this.__getHash());
      this._locationState = decodeURIComponent(this.__getHash());

      this.__waitForIFrame(function()
      {
        this.__storeState(this._state);
        this.__startTimer();
      }, this);
    },

    "default" : function()
    {
      this.base(arguments);
      this._hasNavigationListener = false;

      this._titles = {};
      this._state = this.__getState();

      this.__startTimer();
    }
  }),




  /*
  *****************************************************************************
     EVENTS
  *****************************************************************************
  */

  events: {
    /**
     * Fired when the user moved in the history. The data property of the event
     * holds the state, which was passed to {@link #addToHistory}.
     */
    "request" : "qx.event.type.DataEvent"
  },






  /*
  *****************************************************************************
     PROPERTIES
  *****************************************************************************
  */

  properties :
  {
    /**
     * Interval for the timer, which periodically checks the browser history state
     * in milliseconds.
     */
    timeoutInterval :
    {
      check: "Number",
      init : 100,
      apply : "_applyTimeoutInterval"
    }
  },






  /*
  *****************************************************************************
     MEMBERS
  *****************************************************************************
  */

  members :
  {

    /**
     * Adds an entry to the browser history.
     *
     * @type member
     * @param state {String} a string representing the state of the
     *          application. This command will be delivered in the data property of
     *          the "request" event.
     * @param newTitle {String ? null} the page title to set after the history entry
     *          is done. This title should represent the new state of the application.
     */
    addToHistory : function( state, newTitle ) {
      if( newTitle != null ) {
        document.title = newTitle;
      }
      this._titles[ state ] = document.title;
      if( state != this._state ) {
        // RAP [if] Prevent the event dispatch
        this._state = state;
        this.__storeState( state );
      }
    },


    /**
     * Get the current state of the browser history.
     *
     * @return {String} The current state
     */
    getState : function() {
      return this._state;
    },


    /**
     * Navigates back in the browser history.
     * Simulates a back button click.
     */
     navigateBack : function() {
       qx.client.Timer.once(function() {history.back();}, 0);
     },


    /**
     * Navigates forward in the browser history.
     * Simulates a forward button click.
     */
     navigateForward : function() {
       qx.client.Timer.once(function() {history.forward();}, 0);
     },


    /**
     * Apply the interval of the timer.
     *
     * @type member
     * @param newInterval {Integer} new timeout interval
     */
    _applyTimeoutInterval : function(value) {
      this._timer.setInterval(value);
    },


    /**
     * called on changes to the history using the browser buttons
     *
     * @param state {String} new state of the history
     */
    __onHistoryLoad : function(state) {
      this._state = state;
      this.createDispatchDataEvent("request", state);
      if (this._titles[state] != null) {
        document.title = this._titles[state];
      }
    },


    /**
     * Starts the timer polling for updates to the history IFrame on IE
     * or the fragment identifier on other browsers.
     */
    __startTimer : function() {
      this._timer = new qx.client.Timer(this.getTimeoutInterval());

      this._timer.addEventListener( "interval", function( e ) {
        var newHash = this.__getState();
        // RAP [if] Ignore all non application states
        //if (newHash != this._state) {
        if( newHash != this._state ) {
          this.__onHistoryLoad(newHash);
        }
      }, this );

      if( this._hasNavigationListener ) {
        this._timer.start();
      }
    },


    /**
     * Returns the fragment identifier of the top window URL
     *
     * @return {String} the fragment identifier
     */
    __getHash : function()
    {
      // RAP [if] Fix for bug 295816
      //var href = top.location.href;
      var href = window.location.href;
      var idx = href.indexOf( "#" );
      return idx >= 0 ? href.substring(idx+1) : "";
    },


    /**
     * Browser dependent function to read the current state of the history
     *
     * @return {String} current state of the browser history
     */
    __getState : qx.core.Variant.select("qx.client",
    {
      "mshtml" : function()
      {
        // the location only changes if the user manually changes the fragment
        // identifier.
        var locationState = decodeURIComponent(this.__getHash());
        if (locationState != this._locationState)
        {
          this._locationState = locationState;
          this.__storeState(locationState);
          return locationState;
        }

        var doc = this._iframe.contentWindow.document;
        var elem = doc.getElementById("state");
        var iframeState = elem ? decodeURIComponent(elem.innerText) : "";

        return iframeState;
      },

      "default" : function() {
        return decodeURIComponent(this.__getHash());
      }
    }),


    /**
     * Save a state into the browser history.
     *
     * @param state {String} state to save
     * @return {Boolean} Whether the state could be saved. This function may
     *   fail on the Internet Explorer if the hidden IFrame is not yet fully
     *   loaded.
     */
    __storeState : qx.core.Variant.select("qx.client",
    {
      "mshtml" : function( state ) {
        var html = '<html><body><div id="state">' + encodeURIComponent(state) + '</div></body></html>';
        try {
          var doc = this._iframe.contentWindow.document;
          doc.open();
          doc.write(html);
          doc.close();
        } catch( ex ) {
          return false;
        }
        return true;
      },

      "default" : function( state ) {
        // RAP [if] Fix for bug 295816
        //top.location.hash = "#" + encodeURIComponent(state);
        window.location.hash = "#" + encodeURIComponent(state);
        return true;
      }
    }),


    /**
     * Waits for the IFrame being loaded. Once the IFrame is loaded
     * the callback is called with the provided context.
     *
     * @param callback {Function} This function will be called once the iframe is loaded
     * @param context {Object?window} The context for the callback.
     */
    __waitForIFrame : qx.core.Variant.select("qx.client",
    {
      "mshtml" : function(callback, context)
      {
        if ( !this._iframe.contentWindow || !this._iframe.contentWindow.document ) {
            // Check again in 10 msec...
            qx.client.Timer.once(function() {
              this.__waitForIFrame(callback, context);
            }, this, 10);
            return;
        }
        callback.call(context || window);
      },

      "default" : null
    }),

    setHasNavigationListener : function( value ) {
      this._hasNavigationListener = value;
      if( value ) {
        this.addEventListener( "request", this._historyNavigated, this );
        if( this._timer ) {
          this._timer.start();
        }
      } else {
        this.removeEventListener( "request", this._historyNavigated, this );
        if( this._timer ) {
          this._timer.stop();
        }
      }
    },

    _historyNavigated : function( event ) {
      var entryId = event.getData();
      var req = org.eclipse.swt.Request.getInstance();
      req.addParameter( "org.eclipse.rwt.events.historyNavigated", "true" );
      req.addParameter( "org.eclipse.rwt.events.historyNavigated.entryId", entryId );
      req.send();
    }

  },

  /*
  *****************************************************************************
     DESTRUCTOR
  *****************************************************************************
  */

  destruct : function()
  {
    this._timer.stop();
    this._disposeObjects("_timer");
    this._disposeFields("_iframe", "_titles");
  }
});
