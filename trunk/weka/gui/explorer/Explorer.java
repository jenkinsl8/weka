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
 *    Explorer.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.gui.explorer;

import weka.core.Capabilities;
import weka.core.Memory;
import weka.core.converters.ConverterUtils;
import weka.gui.LogPanel;
import weka.gui.LookAndFeel;
import weka.gui.WekaTaskMonitor;
import weka.gui.visualize.MatrixPanel;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventListener;
import java.util.HashSet;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;

/** 
 * The main class for the Weka explorer. Lets the user create,
 * open, save, configure, datasets, and perform ML analysis.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.36 $
 */
public class Explorer extends JPanel {

  /** The panel for preprocessing instances */
  protected PreprocessPanel m_PreprocessPanel = new PreprocessPanel();

  /** The panel for running classifiers */
  protected ClassifierPanel m_ClassifierPanel = new ClassifierPanel();

  /** Label for a panel that still need to be implemented */
  protected ClustererPanel m_ClustererPanel = new ClustererPanel();

  /** Label for a panel that still need to be implemented */
  protected AssociationsPanel m_AssociationPanel = new AssociationsPanel();

  /** Label for a panel that still need to be implemented */
  protected AttributeSelectionPanel m_AttributeSelectionPanel =
    new AttributeSelectionPanel();

  /** Label for a panel that still need to be implemented */
  protected MatrixPanel m_VisualizePanel =
    new MatrixPanel();
  
  /** The tabbed pane that controls which sub-pane we are working with */
  protected JTabbedPane m_TabbedPane = new JTabbedPane();
  
  /** The panel for log and status messages */
  protected LogPanel m_LogPanel = new LogPanel(new WekaTaskMonitor());
  
  /** the listeners that listen to filter changes */
  protected HashSet<CapabilitiesFilterChangeListener> m_CapabilitiesFilterChangeListeners = new HashSet<CapabilitiesFilterChangeListener>();

  /**
   * Interface for classes that listen to filter changes.
   */
  public static interface CapabilitiesFilterChangeListener 
    extends EventListener {
    
    /**
     * method gets called in case of a change event
     * 
     * @param e		the associated change event
     */
    public void capabilitiesFilterChanged(CapabilitiesFilterChangeEvent e);
  }

  /**
   * This event can be fired in case the capabilities filter got changed 
   */
  public static class CapabilitiesFilterChangeEvent
    extends ChangeEvent {

    /** for serialization */
    private static final long serialVersionUID = 1194260517270385559L;
    
    /** the capabilities filter */
    protected Capabilities m_Filter;
    
    /**
     * Constructs a GOECapabilitiesFilterChangeEvent object.
     * 
     * @param source	the Object that is the source of the event
     * @param filter	the responsible capabilities filter
     */
    public CapabilitiesFilterChangeEvent(Object source, Capabilities filter) {
      super(source);
      m_Filter = filter;
    }
    
    /**
     * returns the associated Capabilities filter
     * 
     * @return		the filter
     */
    public Capabilities getFilter() {
      return m_Filter;
    }
  }


  /**
   * Creates the experiment environment gui with no initial experiment
   */
  public Explorer() {
    
    String date = (new SimpleDateFormat("EEEE, d MMMM yyyy"))
      .format(new Date());
    m_LogPanel.logMessage("Weka Explorer");
    m_LogPanel.logMessage("(c) 1999-2005 The University of Waikato, Hamilton,"
			  + " New Zealand");
    m_LogPanel.logMessage("web: http://www.cs.waikato.ac.nz/~ml/weka");
    m_LogPanel.logMessage("Started on " + date);
    m_LogPanel.statusMessage("Welcome to the Weka Explorer");
    m_PreprocessPanel.setLog(m_LogPanel);
    m_ClassifierPanel.setLog(m_LogPanel);
    m_AssociationPanel.setLog(m_LogPanel);
    m_ClustererPanel.setLog(m_LogPanel);
    m_AttributeSelectionPanel.setLog(m_LogPanel);
    m_TabbedPane.addTab("Preprocess", null, m_PreprocessPanel,
			"Open/Edit/Save instances");
    m_TabbedPane.addTab("Classify", null, m_ClassifierPanel,
			"Classify instances");
    m_TabbedPane.addTab("Cluster", null, m_ClustererPanel,
		      "Identify instance clusters");
    m_TabbedPane.addTab("Associate", null, m_AssociationPanel,
		      "Discover association rules");
    m_TabbedPane.addTab("Select attributes", null, m_AttributeSelectionPanel,
		      "Determine relevance of attributes");
    m_TabbedPane.addTab("Visualize", null, m_VisualizePanel,
		      "Explore the data");
    m_TabbedPane.setSelectedIndex(0);
    m_TabbedPane.setEnabledAt(1, false);
    m_TabbedPane.setEnabledAt(2, false);
    m_TabbedPane.setEnabledAt(3, false);
    m_TabbedPane.setEnabledAt(4, false);
    m_TabbedPane.setEnabledAt(5, false);
    m_PreprocessPanel.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        m_ClassifierPanel.setInstances(m_PreprocessPanel.getInstances());
        m_AssociationPanel.setInstances(m_PreprocessPanel.getInstances());
        m_ClustererPanel.setInstances(m_PreprocessPanel.getInstances());
        m_AttributeSelectionPanel.setInstances(m_PreprocessPanel
                                               .getInstances());
        m_VisualizePanel.setInstances(m_PreprocessPanel.getInstances());
        
        m_TabbedPane.setEnabledAt(1, true);
        m_TabbedPane.setEnabledAt(2, true);
        m_TabbedPane.setEnabledAt(3, true);
        m_TabbedPane.setEnabledAt(4, true);
        m_TabbedPane.setEnabledAt(5, true);
      }
    });

    setLayout(new BorderLayout());
    add(m_TabbedPane, BorderLayout.CENTER);
   
    add(m_LogPanel, BorderLayout.SOUTH);

    // add listeners
    m_PreprocessPanel.setExplorer(this);
    addCapabilitiesFilterListener(m_PreprocessPanel);
    addCapabilitiesFilterListener(m_ClassifierPanel);
    addCapabilitiesFilterListener(m_ClustererPanel);
    addCapabilitiesFilterListener(m_AssociationPanel);
    addCapabilitiesFilterListener(m_AttributeSelectionPanel);
  }
  
  /**
   * adds the listener to the list of objects that listen for changes of the
   * CapabilitiesFilter
   * 
   * @param l		the listener to add
   * @see		#m_CapabilitiesFilter
   */
  public void addCapabilitiesFilterListener(CapabilitiesFilterChangeListener l) {
    m_CapabilitiesFilterChangeListeners.add(l);
  }

  /**
   * Removes the specified listener from the set of listeners if it is present.
   * 
   * @param l		the listener to remove
   * @return		true if the listener was registered
   */
  public boolean removeCapabilitiesFilterListener(CapabilitiesFilterChangeListener l) {
    return m_CapabilitiesFilterChangeListeners.remove(l);
  }
  
  /**
   * notifies all the listeners of a change
   * 
   * @param filter	the affected filter
   */
  public void notifyCapabilitiesFilterListener(Capabilities filter) {
    for (CapabilitiesFilterChangeListener l: m_CapabilitiesFilterChangeListeners) {
      if (l == this)
	continue;
      l.capabilitiesFilterChanged(new CapabilitiesFilterChangeEvent(this, filter));
    }
  }
  
  /** variable for the Explorer class which would be set to null by the memory 
      monitoring thread to free up some memory if we running out of memory
   */
  private static Explorer m_explorer;

  /** for monitoring the Memory consumption */
  private static Memory m_Memory = new Memory(true);

  /**
   * Tests out the explorer environment.
   *
   * @param args ignored.
   */
  public static void main(String [] args) {

    LookAndFeel.setLookAndFeel();
    
    try {
      // uncomment to disable the memory management:
      //m_Memory.setEnabled(false);

      m_explorer = new Explorer();
      final JFrame jf = new JFrame("Weka Explorer");
      jf.getContentPane().setLayout(new BorderLayout());
      jf.getContentPane().add(m_explorer, BorderLayout.CENTER);
      jf.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          jf.dispose();
          System.exit(0);
        }
      });
      jf.pack();
      jf.setSize(800, 600);
      jf.setVisible(true);
      Image icon = Toolkit.getDefaultToolkit().
      getImage(ClassLoader.getSystemResource("weka/gui/weka_icon.gif"));
      jf.setIconImage(icon);

      if (args.length == 1) {
        System.err.println("Loading instances from " + args[0]);
        m_explorer.m_PreprocessPanel.setInstancesFromFile(ConverterUtils.getLoaderForFile(args[0]));
      }

      Thread memMonitor = new Thread() {
        public void run() {
          while(true) {
            try {
              //System.out.println("Before sleeping.");
              this.sleep(4000);

              System.gc();

              if (m_Memory.isOutOfMemory()) {
                // clean up
                jf.dispose();
                m_explorer = null;
                System.gc();

                // stop threads
                m_Memory.stopThreads();

                // display error
                System.err.println("\ndisplayed message:");
                m_Memory.showOutOfMemory();
                System.err.println("\nexiting");
                System.exit(-1);
              }

            } catch(InterruptedException ex) { ex.printStackTrace(); }
          }
        }
      };

      memMonitor.setPriority(Thread.MAX_PRIORITY);
      memMonitor.start();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
