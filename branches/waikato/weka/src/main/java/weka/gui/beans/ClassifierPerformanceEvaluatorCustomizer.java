/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    ClassifierPerformanceEvaluatorCustomizer.java
 *    Copyright (C) 2011-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.gui.beans;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JButton;
import javax.swing.JPanel;

import weka.gui.PropertySheetPanel;

/**
 * GUI customizer for the classifier performance evaluator component
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ClassifierPerformanceEvaluatorCustomizer extends JPanel implements
    BeanCustomizer, CustomizerCloseRequester, CustomizerClosingListener {

  /**
   * For serialization
   */
  private static final long serialVersionUID = -1055460295546483853L;

  private final PropertyChangeSupport m_pcSupport = new PropertyChangeSupport(
      this);

  private final PropertySheetPanel m_splitEditor = new PropertySheetPanel();

  private ClassifierPerformanceEvaluator m_cpe;
  private ModifyListener m_modifyListener;
  private int m_executionSlotsBackup;

  private Window m_parent;

  /**
   * Constructor
   */
  public ClassifierPerformanceEvaluatorCustomizer() {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));

    setLayout(new BorderLayout());
    add(m_splitEditor, BorderLayout.CENTER);
    add(new javax.swing.JLabel("ClassifierPerformanceEvaluatorCustomizer"),
        BorderLayout.NORTH);

    addButtons();
  }

  private void addButtons() {
    JButton okBut = new JButton("OK");
    JButton cancelBut = new JButton("Cancel");

    JPanel butHolder = new JPanel();
    butHolder.setLayout(new GridLayout(1, 2));
    butHolder.add(okBut);
    butHolder.add(cancelBut);
    add(butHolder, BorderLayout.SOUTH);

    okBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (m_modifyListener != null) {
          m_modifyListener.setModifiedStatus(
              ClassifierPerformanceEvaluatorCustomizer.this, true);
        }
        if (m_parent != null) {
          m_parent.dispose();
        }
      }
    });

    cancelBut.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {

        customizerClosing();
        if (m_parent != null) {
          m_parent.dispose();
        }
      }
    });
  }

  /**
   * Set the ClassifierPerformanceEvaluator to be customized
   * 
   * @param object an <code>Object</code> value
   */
  @Override
  public void setObject(Object object) {
    m_cpe = (ClassifierPerformanceEvaluator) object;
    m_executionSlotsBackup = m_cpe.getExecutionSlots();
    m_splitEditor.setTarget(m_cpe);
  }

  /**
   * Add a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void addPropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.addPropertyChangeListener(pcl);
  }

  /**
   * Remove a property change listener
   * 
   * @param pcl a <code>PropertyChangeListener</code> value
   */
  @Override
  public void removePropertyChangeListener(PropertyChangeListener pcl) {
    m_pcSupport.removePropertyChangeListener(pcl);
  }

  @Override
  public void setModifiedListener(ModifyListener l) {
    m_modifyListener = l;
  }

  @Override
  public void customizerClosing() {
    // restore the backup
    m_cpe.setExecutionSlots(m_executionSlotsBackup);
  }

  @Override
  public void setParentWindow(Window parent) {
    m_parent = parent;

  }

}
