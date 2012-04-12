/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    GraphViewer.java
 *    Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.beans;

import weka.gui.ResultHistoryPanel;
import weka.gui.treevisualizer.*;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.ImageIcon;

import javax.swing.SwingConstants;
import javax.swing.JFrame;
import javax.swing.BorderFactory;
import java.awt.*;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.MouseEvent;

/**
 * A bean encapsulating weka.gui.treevisualize.TreeVisualizer
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revision: 1.4 $
 */
public class GraphViewer 
  extends JPanel
  implements Visible, GraphListener,
	     UserRequestAcceptor, Serializable {

  protected BeanVisual m_visual = 
    new BeanVisual("GraphViewer", 
		   BeanVisual.ICON_PATH+"DefaultGraph.gif",
		   BeanVisual.ICON_PATH+"DefaultGraph_animated.gif");


  private transient JFrame m_resultsFrame = null;

  protected transient ResultHistoryPanel m_history = 
    new ResultHistoryPanel(null);

  public GraphViewer() {
    setUpResultHistory();
    setLayout(new BorderLayout());
    add(m_visual, BorderLayout.CENTER);
  }

  /**
   * Global info for this bean
   *
   * @return a <code>String</code> value
   */
  public String globalInfo() {
    return "Graphically visualize trees produced by classifiers/clusterers.";
  }

  private void setUpResultHistory() {
    if (m_history == null) {
      m_history = new ResultHistoryPanel(null);
    }
    m_history.setBorder(BorderFactory.createTitledBorder("Graph list"));
    m_history.setHandleRightClicks(false);
    m_history.getList().
      addMouseListener(new ResultHistoryPanel.RMouseAdapter() {
	  public void mouseClicked(MouseEvent e) {
	    int index = m_history.getList().locationToIndex(e.getPoint());
	    if (index != -1) {
	      String name = m_history.getNameAtIndex(index);
	      doPopup(name);
	    }
	  }
	});
  }

  /**
   * Accept a graph
   *
   * @param e a <code>GraphEvent</code> value
   */
  public synchronized void acceptGraph(GraphEvent e) {
    if (m_history == null) {
      setUpResultHistory();
    }
    String name = (new SimpleDateFormat("HH:mm:ss - "))
      .format(new Date());

    name += e.getGraphTitle();
    m_history.addResult(name, new StringBuffer());
    m_history.addObject(name, e.getGraphString());
  }

  /**
   * Set the visual appearance of this bean
   *
   * @param newVisual a <code>BeanVisual</code> value
   */
  public void setVisual(BeanVisual newVisual) {
    m_visual = newVisual;
  }

  /**
   * Get the visual appearance of this bean
   *
   */
  public BeanVisual getVisual() {
    return m_visual;
  }
  
  /**
   * Use the default visual appearance
   *
   */
  public void useDefaultVisual() {
    m_visual.loadIcons(BeanVisual.ICON_PATH+"DefaultGraph.gif",
		       BeanVisual.ICON_PATH+"DefaultGraph_animated.gif");
  }

  /**
   * Popup a result list from which the user can select a graph to view
   *
   */
  public void showResults() {
    if (m_resultsFrame == null) {
      if (m_history == null) {
	setUpResultHistory();
      }
      m_resultsFrame = new JFrame("Graph Viewer");
      m_resultsFrame.getContentPane().setLayout(new BorderLayout());
      m_resultsFrame.getContentPane().add(m_history, BorderLayout.CENTER);
      m_resultsFrame.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    m_resultsFrame.dispose();
	    m_resultsFrame = null;
	  }
	});
      m_resultsFrame.pack();
      m_resultsFrame.setVisible(true);
    }
  }

  private void doPopup(String name) {
    String grphString = (String)m_history.getNamedObject(name);

    final javax.swing.JFrame jf = 
      new javax.swing.JFrame("Weka Classifier Tree Visualizer: "+name);
    jf.setSize(500,400);
    jf.getContentPane().setLayout(new BorderLayout());
    TreeVisualizer tv = 
      new TreeVisualizer(null,
			 grphString,
			 new PlaceNode2());
    jf.getContentPane().add(tv, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	}
      });
    
    jf.setVisible(true);
  }

  /**
   * Return an enumeration of user requests
   *
   * @return an <code>Enumeration</code> value
   */
  public Enumeration enumerateRequests() {
    Vector newVector = new Vector(0);
    if (m_resultsFrame == null) {
      newVector.addElement("Show results");
    }
    return newVector.elements();
  }

  /**
   * Perform the named request
   *
   * @param request a <code>String</code> value
   * @exception IllegalArgumentException if an error occurs
   */
  public void performRequest(String request) 
    {
    if (request.compareTo("Show results") == 0) {
      showResults();
    } else {
      throw new 
	IllegalArgumentException(request
		    + " not supported (GraphViewer)");
    }
  }
}
