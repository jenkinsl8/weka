/*
 *    LogPanel.java
 *    Copyright (C) 1999 Len Trigg
 *
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

package weka.gui;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.SwingConstants;

/** 
 * This panel allows log and status messages to be posted. Log messages
 * appear in a scrollable text area, and status messages appear as one-line
 * transient messages.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $
 */
public class LogPanel extends JPanel implements Logger {

  /** Displays the current status */
  protected JLabel m_StatusLab = new JLabel("OK");
  
  /** Displays the log messages */
  protected JTextArea m_LogText = new JTextArea(5, 20);

  /**
   * Creates the log panel
   */
  public LogPanel() {

    m_LogText.setEditable(false);
    m_LogText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    m_StatusLab.setBorder(BorderFactory.createCompoundBorder(
			  BorderFactory.createTitledBorder("Status"),
			  BorderFactory.createEmptyBorder(0, 5, 5, 5)));
    
    JPanel p1 = new JPanel();
    p1.setBorder(BorderFactory.createTitledBorder("Log"));
    p1.setLayout(new BorderLayout());
    p1.add(new JScrollPane(m_LogText), BorderLayout.CENTER);
    
    setLayout(new BorderLayout());
    add(p1, BorderLayout.CENTER);
    add(m_StatusLab, BorderLayout.SOUTH);
  }

    
  /**
   * Gets a string containing current date and time.
   *
   * @return a string containing the date and time.
   */
  protected static String getTimestamp() {

    return (new SimpleDateFormat("HH:mm:ss:")).format(new Date());
  }

  /**
   * Sends the supplied message to the log area. The current timestamp will
   * be prepended.
   *
   * @param message a value of type 'String'
   */
  public void logMessage(String message) {
    
    m_LogText.append(LogPanel.getTimestamp() + ' ' + message + '\n');
  }

  /**
   * Sends the supplied message to the status line.
   *
   * @param message the status message
   */
  public void statusMessage(String message) {

    m_StatusLab.setText(message);
  }

  
  /**
   * Tests out the log panel from the command line.
   *
   * @param args ignored
   */
  public static void main(String [] args) {

    try {
      final javax.swing.JFrame jf = new javax.swing.JFrame("Log Panel");
      jf.getContentPane().setLayout(new BorderLayout());
      final LogPanel lp = new LogPanel();
      jf.getContentPane().add(lp, BorderLayout.CENTER);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	public void windowClosing(java.awt.event.WindowEvent e) {
	  jf.dispose();
	  System.exit(0);
	}
      });
      jf.pack();
      jf.setVisible(true);
      lp.logMessage("Welcome to the generic log panel!");
      lp.statusMessage("Hi there");
      lp.logMessage("Funky chickens");
      
    } catch (Exception ex) {
      ex.printStackTrace();
      System.err.println(ex.getMessage());
    }
  }
}
