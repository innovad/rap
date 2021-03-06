/*******************************************************************************
 * Copyright (c) 2007, 2012 Innoopract Informationssysteme GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Innoopract Informationssysteme GmbH - initial API and implementation
 *    EclipseSource - ongoing development
 ******************************************************************************/
var appearances = {
// BEGIN TEMPLATE //

  "text-field" : {
    style : function( states ) {
      var result = {};
      var tv = new org.eclipse.swt.theme.ThemeValues( states );
      result.font = tv.getCssFont( "Text", "font" );
      result.textColor = tv.getCssColor( "Text", "color" );
      result.backgroundColor = tv.getCssColor( "Text", "background-color" );
      result.backgroundImage = tv.getCssImage( "Text", "background-image" );
      result.backgroundGradient = tv.getCssGradient( "Text", "background-image" );
      result.border = tv.getCssBorder( "Text", "border" );
      // [if] Do not apply top/bottom paddings on the client
      var cssPadding = tv.getCssBoxDimensions( "Text", "padding" );
      result.paddingRight = cssPadding[ 1 ];
      result.paddingLeft = cssPadding[ 3 ];
      result.textShadow = tv.getCssShadow( "Text", "text-shadow" );
      result.shadow = tv.getCssShadow( "Text", "box-shadow" );
      return result;
    }
  },

  "text-field-message" : {
    style : function( states ) {
      var result = {};
      var tv = new org.eclipse.swt.theme.ThemeValues( states );
      result.font = tv.getCssFont( "Text", "font" );
      result.textColor = tv.getCssColor( "Text-Message", "color" );
      // [if] Do not apply top/bottom paddings on the client
      var cssPadding = tv.getCssBoxDimensions( "Text", "padding" );
      result.paddingRight = cssPadding[ 1 ];
      result.paddingLeft = cssPadding[ 3 ];
      result.horizontalChildrenAlign = "left";
      result.textShadow = tv.getCssShadow( "Text-Message", "text-shadow" );
      return result;
    }
  },

  "text-area" : {
    include : "text-field",
    style : function( states ) {
      return {
        padding : [ 0, 0, 0, 3 ]
      };
    }
  }

// END TEMPLATE //
};
