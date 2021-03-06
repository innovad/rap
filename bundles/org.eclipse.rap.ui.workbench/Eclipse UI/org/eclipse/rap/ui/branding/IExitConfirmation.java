/*******************************************************************************
 * Copyright (c) 2008, 2009 Innoopract Informationssysteme GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Innoopract Informationssysteme GmbH - initial API and implementation
 *     EclipseSource - ongoing development
 ******************************************************************************/
package org.eclipse.rap.ui.branding;

/**
 * Implementations of this interface can be registered with the branding
 * extension point in order to control the exit confirmation that can be shown
 * by certain browsers whenever the user tries to close the browser window or
 * tab or to navigate to another URL.
 * </p>
 * Note that this is feature is not supported by every browser.
 * </p>
 * @since 1.1.1
 */
public interface IExitConfirmation {

  /**
   * Indicates whether an exit confirmation should be shown.
   * <p>
   * The exit confirmation is shown whenever the user tries to close the
   * browser window or tab or to navigate to another URL. Usually, browsers
   * pop up a dialog that allows the user to cancel the operation.
   * </p>
   * <p>
   * Note that this is a <em>hint</em> that some browsers do not support at all and others only 
   * partly (i.e. showing a generic message instead of the one provided by 
   * <code>getExitConfirmationText()</code>).
   * </p>
   * 
   * @return <code>true</code> if an exit confirmation should be shown
   * @see #getExitConfirmationText()
   */
  // keep Javadoc in sync with AbstractBranding
  public abstract boolean showExitConfirmation();

  /**
   * Returns the message to display in the exit confirmation. Note that
   * <code>showExitConfirmation()</code> must return <code>true</code> to enable
   * this message.
   * 
   * @return the message to be displayed in the exit confirmation
   * @see #showExitConfirmation()
   */
  // keep Javadoc in sync with AbstractBranding
  public abstract String getExitConfirmationText();
}
