/*
 * @(#)ColorComponent.java
 *
 * $Date: 2014-03-13 01:15:48 -0700 (Thu, 13 Mar 2014) $
 *
 * Copyright (c) 2011 by Jeremy Wood.
 * All rights reserved.
 *
 * The copyright of this software is owned by Jeremy Wood. 
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * Jeremy Wood. For details see accompanying license terms.
 * 
 * This software is probably, but not necessarily, discussed here:
 * https://javagraphics.java.net/
 * 
 * That site should also contain the most recent official version
 * of this software.  (See the SVN repository for more details.)
 */
package com.bric.swing;

import java.awt.Color;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/** This is an abstract <code>JComponent</code> that stores
 * and somehow presents a color.  When <code>setColor()</code>
 * is called this component's <code>ChangeListeners</code> are
 * notified.
 * <P>The initial color is black.
 */
public abstract class ColorComponent extends JComponent {
	private static final long serialVersionUID = 1L;

	protected Color color = Color.black;
	Vector<ChangeListener> changeListeners = new Vector<ChangeListener>();

	/** Add a <code>ChangeListener</code> to be notified when
	 * <code>setColor()</code> is called.
	 */
	public void addChangeListener(ChangeListener l) {
		if(changeListeners.contains(l))
			return;
		changeListeners.add(l);
	}
	
	/** Remove a <code>ChangeListener</code>.
	 * 
	 */
	public void removeChangeListener(ChangeListener l) {
		changeListeners.remove(l);
	}
	
	/** Returns the current color.
	 * 
	 * @return the current color.
	 */
	public Color getColor() {
		return color;
	}
	
	protected void fireChangeListeners() {
		for(int a = 0; a<changeListeners.size(); a++) {
			ChangeListener l = changeListeners.get(a);
			try {
				l.stateChanged(new ChangeEvent(this));
			} catch(RuntimeException e) {
				e.printStackTrace();
			}
		}
	}
	
	/** Defines the current color.
	 * 
	 * @param c the new color.
	 * @return <code>true</code> if a change occurred.  Or <code>false</code>
	 * if the argument was already the current color and no listeners
	 * were notified.
	 */
	public boolean setColor(Color c) {
		if(c==null)
			throw new NullPointerException();
		if(color!=null && c!=null && c.equals(color))
			return false;
		Color oldColor = null;
		color = c;
		fireChangeListeners();
		firePropertyChange("color", oldColor, color);
		return true;
	}
	
	public void bind(final ColorComponent slave) {
		slave.setColor(getColor());
		ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				ColorComponent c = (ColorComponent)e.getSource();
				if(c==ColorComponent.this) {
					slave.setColor( getColor() );
				} else {
					setColor( slave.getColor() );
				}
			}
		};
		
		addChangeListener(changeListener);
		slave.addChangeListener(changeListener);
	}
}
