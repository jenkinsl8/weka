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
 *   BoundaryPanel.java
 *   Copyright (C) 2002 Mark Hall
 *
 */

package weka.gui.boundaryvisualizer;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JPanel;
import java.util.Vector;

import weka.core.*;
import weka.classifiers.Classifier;
import weka.classifiers.DistributionClassifier;
import weka.classifiers.bayes.NaiveBayesSimple;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.j48.J48;
import weka.classifiers.lazy.IBk;
import weka.classifiers.functions.Logistic;
import weka.clusterers.EM;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Add;

/**
 * BoundaryPanel. A class to handle the plotting operations
 * associated with generating a 2D picture of a classifier's decision
 * boundaries.
 *
 * @author <a href="mailto:mhall@cs.waikato.ac.nz">Mark Hall</a>
 * @version $Revison: 1.0 $
 * @since 1.0
 * @see JPanel
 */

public class BoundaryPanel extends JPanel {

  // training data
  private Instances m_trainingData;

  // distribution classifier to use
  private DistributionClassifier m_classifier;

  // data generator to use
  private DataGenerator m_dataGenerator;

  // index of the class attribute
  private int m_classIndex = -1;

  // indexes of class values (any other classes are grouped together
  // and correspond to the colour black
  private int [] m_rgbClassValues = new int [3];

  // attributes for visualizing on
  private int m_xAttribute;
  private int m_yAttribute;

  // min, max and ranges of these attributes
  private double m_minX;
  private double m_minY;
  private double m_maxX;
  private double m_maxY;
  private double m_rangeX;
  private double m_rangeY;

  // pixel width and height in terms of attribute values
  private double m_pixHeight;
  private double m_pixWidth;

  // used for offscreen drawing
  private Image m_osi = null;

  // width and height of the display area
  private int m_panelWidth;
  private int m_panelHeight;

  // the number of samples to take from each generating model for each
  // pixel
  private int m_numberOfSamplesFromEachGeneratingModel = 2;
  
  // listeners to be notified when plot is complete
  private Vector m_listeners = new Vector();

  // small inner class for rendering the bitmap on to
  private class PlotPanel extends JPanel {
    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (m_osi != null) {
	g.drawImage(m_osi,0,0,this);
      }
    }
  }

  // the actual plotting area
  private PlotPanel m_plotPanel = new PlotPanel();

  // thread for running the plotting operation in
  private Thread m_plotThread = null;

  // Stop the plotting thread
  private boolean m_stopPlotting = false;

  /**
   * Creates a new <code>BoundaryPanel</code> instance.
   *
   * @param panelWidth the width in pixels of the panel
   * @param panelHeight the height in pixels of the panel
   */
  public BoundaryPanel(int panelWidth, int panelHeight) {
    m_panelWidth = panelWidth;
    m_panelHeight = panelHeight;
    setLayout(new BorderLayout());
    m_plotPanel.setMinimumSize(new Dimension(m_panelWidth, m_panelHeight));
    m_plotPanel.setPreferredSize(new Dimension(m_panelWidth, m_panelHeight));
    m_plotPanel.setMaximumSize(new Dimension(m_panelWidth, m_panelHeight));
    add(m_plotPanel, BorderLayout.CENTER);
    setPreferredSize(m_plotPanel.getPreferredSize());
    setMaximumSize(m_plotPanel.getMaximumSize());
    setMinimumSize(m_plotPanel.getMinimumSize());
    for (int i = 0; i < 3; i++) {
      m_rgbClassValues[i] = -1;
    }
  }

  /**
   * Set up the off screen bitmap for rendering to
   */
  protected void initialize() {
    int iwidth = m_plotPanel.getWidth();
    int iheight = m_plotPanel.getHeight();
    //    System.err.println(iwidth+" "+iheight);
    m_osi = m_plotPanel.createImage(iwidth, iheight);
    Graphics m = m_osi.getGraphics();
    m.fillRect(0,0,iwidth,iheight);
  }

  /**
   * Stop the plotting thread
   */
  public void stopPlotting() {
    m_stopPlotting = true;
  }

  private void computeMinMaxAtts() {
    m_minX = Double.MAX_VALUE;
    m_minY = Double.MAX_VALUE;
    m_maxX = Double.MIN_VALUE;
    m_maxY = Double.MIN_VALUE;

    for (int i = 0; i < m_trainingData.numInstances(); i++) {
      Instance inst = m_trainingData.instance(i);
      double x = inst.value(m_xAttribute);
      double y = inst.value(m_yAttribute);
      if (x != Instance.missingValue()) {
	if (x < m_minX) {
	  m_minX = x;
	}
	if (x > m_maxX) {
	  m_maxX = x;
	}
      }
      if (y != Instance.missingValue()) {
	if (y < m_minY) {
	  m_minY = y;
	}
	if (y > m_maxY) {
	  m_maxY = y;
	}
      }
    }
    m_rangeX = (m_maxX - m_minX);
    m_rangeY = (m_maxY - m_minY);
    m_pixWidth = m_rangeX / (double)m_panelWidth;
    m_pixHeight = m_rangeY / (double) m_panelHeight;
  }

  /**
   * Return the x attribute value that corresponds to the middle of
   * the pix'th horizontal pixel
   *
   * @param pix the horizontal pixel number
   * @return a value in attribute space
   */
  private double getMidX(int pix) {
    double midX = m_minX + (pix * m_pixWidth);
    midX += (m_pixWidth / 2.0);
    return midX;
  }

  /**
   * Return the y attribute value that corresponds to the middle of
   * the pix'th vertical pixel
   *
   * @param pix the vertical pixel number
   * @return a value in attribute space
   */
  private double getMidY(int pix) {
    double midY = m_minY + (pix * m_pixHeight);
    midY += (m_pixHeight / 2.0);
    return midY;
  }

  /**
   * Start the plotting thread
   *
   * @exception Exception if an error occurs
   */
  public void start() throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (m_classifier == null) {
      throw new Exception("No classifier set (BoundaryPanel)");
    }
    if (m_dataGenerator == null) {
      throw new Exception("No data generator set (BoundaryPanel)");
    }
    if (m_trainingData.attribute(m_xAttribute).isNominal() || 
	m_trainingData.attribute(m_yAttribute).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    
    computeMinMaxAtts();

    if (m_plotThread == null) {
      m_plotThread = new Thread() {
	  public void run() {
	    m_stopPlotting = false;
	    try {
	      /*  if (m_osi == null) {
		initialize();
		repaint();
		} */
	      initialize();
	      repaint();

	      // train the classifier
	      //	      System.err.println("Building classifier...");
	      m_classifier.buildClassifier(m_trainingData);
	      // make a copy of the training data minus the class
	      Remove rmF = new Remove();
	      rmF.setAttributeIndices(""+(m_classIndex+1));
	      rmF.setInvertSelection(false);
	      rmF.setInputFormat(m_trainingData);
	      rmF.setAttributeIndices(""+(m_classIndex+1));
	      Instances trainNoClass = Filter.useFilter(m_trainingData, rmF);
	      Instances trainNoClassHeader = new Instances(trainNoClass,0);
	      
	      // build DataGenerator
	      //	      System.err.println("Building data generator...");
	      boolean [] attsToWeightOn = 
		new boolean[trainNoClass.numAttributes()];
	      attsToWeightOn[m_xAttribute] = true;
	      attsToWeightOn[m_yAttribute] = true;

	      m_dataGenerator.setWeightingDimensions(attsToWeightOn);

	      m_dataGenerator.buildGenerator(trainNoClass);
	      int samplesPerPixel = 
		m_numberOfSamplesFromEachGeneratingModel * 
		m_dataGenerator.getNumGeneratingModels();
	      
	      // generate samples
	      Add addF = new Add();
	      addF.setInputFormat(trainNoClass);
	      addF.setAttributeIndex(m_classIndex);
	      double pixelMidX = 0;
	      double pixelMidY = 0;
	      double [] dist;
	      double [] weightingAttsValues = 
		new double [attsToWeightOn.length];
	      double [] vals = new double[m_trainingData.numAttributes()];
	      Instance predInst = new Instance(1.0, vals);
	      predInst.setDataset(m_trainingData);
	      abortPlot: for (int i = 0; i < m_panelHeight; i++) {
		pixelMidY = getMidY(m_panelHeight-i-1);
		for (int j = 0; j < m_panelWidth; j++) {
		  if (m_stopPlotting) {
		    break abortPlot;
		  }
		  pixelMidX = getMidX(j);
		  double sumOfWeights = 0;
		  double [] sumOfProbs = 
		    new double [m_trainingData.classAttribute().numValues()];
		  for (int z = 0; z < samplesPerPixel; z++) {

		    weightingAttsValues[m_xAttribute] = pixelMidX;
		    weightingAttsValues[m_yAttribute] = pixelMidY;

		    m_dataGenerator.setWeightingValues(weightingAttsValues);

		    Instance newInst = 
		      m_dataGenerator.generateInstanceFast();
		    sumOfWeights += newInst.weight();
		    int index = 0;
		    for (int k = 0; k < predInst.numAttributes(); k++) {
		      if (k != m_trainingData.classIndex()) {
			vals[k] = newInst.value(index);
			index++;
		      }
		    }

		    // classify the instance
		    dist = m_classifier.distributionForInstance(predInst);
		    for (int k = 0; k < sumOfProbs.length; k++) {
		      sumOfProbs[k] += (dist[k] * newInst.weight()); 
		    }
		  }
		  // average
		  Utils.normalize(sumOfProbs, sumOfWeights);

		  // plot the point
		  Graphics osg = m_osi.getGraphics();
		  Graphics g = m_plotPanel.getGraphics();
		  float [] colVal = new float[3];
		  for (int k = 0; k < 3; k++) {
		    if (k < sumOfProbs.length) {
		      if (m_rgbClassValues[k] != -1) {
			colVal[k] = 
			  (float)sumOfProbs[m_rgbClassValues[k]];
		      }
		    }
		    if (colVal[k] < 0) {
		      colVal[k] = 0;
		    }
		    if (colVal[k] > 1) {
		      colVal[k] = 1;
		    }
		  }

		  osg.setColor(new Color(colVal[0], 
					 colVal[1], 
					 colVal[2]));
		  osg.drawLine(j,i,j,i);
		  if (j == 0) {
		    g.drawImage(m_osi,0,0,m_plotPanel);
		  }
		}
	      }
	    } catch (Exception ex) {
	      ex.printStackTrace();
	    } finally {
	      m_plotThread = null;
	      // notify any listeners that we are finished
	      Vector l;
	      ActionEvent e = new ActionEvent(this, 0, "");
	      synchronized(this) {
		l = (Vector)m_listeners.clone();
	      }
	      for (int i = 0; i < l.size(); i++) {
		ActionListener al = (ActionListener)l.elementAt(i);
		al.actionPerformed(e);
	      }
	    }
	  }
	};
      m_plotThread.setPriority(Thread.MIN_PRIORITY);
      m_plotThread.start();
    }
  }

  /**
   * Set how many samples to take from each generating model for use in
   * computing the colour of a pixel
   *
   * @param ns the number of samples to use from each generating model
   */
  public void setNumberOfSamplesFromEachGeneratingModel(int ns) {
    if (ns >= 1) {
      m_numberOfSamplesFromEachGeneratingModel = ns;
    }
  }

  /**
   * Set the training data to use
   *
   * @param trainingData the training data
   * @exception Exception if an error occurs
   */
  public void setTrainingData(Instances trainingData) throws Exception {
    m_trainingData = trainingData;
    if (m_trainingData.classIndex() < 0) {
      throw new Exception("No class attribute set (BoundaryPanel)");
    }
    m_classIndex = m_trainingData.classIndex();
  }

  /**
   * Register a listener to be notified when plotting completes
   *
   * @param newListener the listener to add
   */
  public void addActionListener(ActionListener newListener) {
    m_listeners.add(newListener);
  }

  /**
   * Remove a listener
   *
   * @param removeListener the listener to remove
   */
  public void removeActionListener(ActionListener removeListener) {
    m_listeners.removeElement(removeListener);
  }

  /**
   * Set the classifier to use.
   *
   * @param classifier the classifier to use
   */
  public void setClassifier(DistributionClassifier classifier) {
    m_classifier = classifier;
  }

  /**
   * Set the data generator to use for generating new instances
   *
   * @param dataGenerator the data generator to use
   */
  public void setDataGenerator(DataGenerator dataGenerator) {
    m_dataGenerator = dataGenerator;
  }
  
  /**
   * Set the class value index for the red colour
   *
   * @param classVal an <code>int</code> value
   * @exception Exception if an error occurs
   */
  public void setRedClassValue(int classVal) throws Exception {
    setClassValue(0, classVal);
  }

  /**
   * Set the class value index for the green colour
   *
   * @param classVal an <code>int</code> value
   * @exception Exception if an error occurs
   */
  public void setGreenClassValue(int classVal) throws Exception {
    setClassValue(1, classVal);
  }

  /**
   * Set the class value index for the blue colour
   *
   * @param classVal an <code>int</code> value
   * @exception Exception if an error occurs
   */
  public void setBlueClassValue(int classVal) throws Exception {
    setClassValue(2, classVal);
  }

  /**
   * Set a class value for a particular colour (RGB)
   *
   * @param index the colour - 0 = red, 1 = green, 2 = blue
   * @param classVal the class value index to associate with the colour
   * @exception Exception if an error occurs
   */
  private void setClassValue(int index, int classVal) throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (classVal < 0 || 
	classVal > m_trainingData.classAttribute().numValues()) {
      throw new Exception("Class value out of range (BoundaryPanel)");
    }
    m_rgbClassValues[index] = classVal;
  }
  
  /**
   * Set the x attribute index
   *
   * @param xatt index of the attribute to use on the x axis
   * @exception Exception if an error occurs
   */
  public void setXAttribute(int xatt) throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (xatt < 0 || 
	xatt > m_trainingData.numAttributes()) {
      throw new Exception("X attribute out of range (BoundaryPanel)");
    }
    if (m_trainingData.attribute(xatt).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    if (m_trainingData.numDistinctValues(xatt) < 2) {
      throw new Exception("Too few distinct values for X attribute "
			  +"(BoundaryPanel)");
    }
    m_xAttribute = xatt;
  }

  /**
   * Set the y attribute index
   *
   * @param yatt index of the attribute to use on the y axis
   * @exception Exception if an error occurs
   */
  public void setYAttribute(int yatt) throws Exception {
    if (m_trainingData == null) {
      throw new Exception("No training data set (BoundaryPanel)");
    }
    if (yatt < 0 || 
	yatt > m_trainingData.numAttributes()) {
      throw new Exception("X attribute out of range (BoundaryPanel)");
    }
    if (m_trainingData.attribute(yatt).isNominal()) {
      throw new Exception("Visualization dimensions must be numeric "
			  +"(BoundaryPanel)");
    }
    if (m_trainingData.numDistinctValues(yatt) < 2) {
      throw new Exception("Too few distinct values for Y attribute "
			  +"(BoundaryPanel)");
    }
    m_yAttribute = yatt;
  }

  /**
   * Main method for testing this class
   *
   * @param args a <code>String[]</code> value
   */
  public static void main (String [] args) {
    try {
      if (args.length < 7) {
	System.err.println("Usage : BoundaryPanel <dataset> "
			   +"<class col> <Red classVal(index)> "
			   +"<Green classVal(index)> "
			   +"<Blue classVal(index)> <xAtt> <yAtt>");
	System.exit(1);
      }
      final javax.swing.JFrame jf = 
	new javax.swing.JFrame("Weka classification boundary visualizer");
      jf.getContentPane().setLayout(new BorderLayout());
      final BoundaryPanel bv = new BoundaryPanel(200,200);
      jf.getContentPane().add(bv, BorderLayout.CENTER);
      jf.setSize(bv.getMinimumSize());
      //      jf.setSize(200,200);
      jf.addWindowListener(new java.awt.event.WindowAdapter() {
	  public void windowClosing(java.awt.event.WindowEvent e) {
	    jf.dispose();
	    System.exit(0);
	  }
	});

      jf.pack();
      jf.setVisible(true);
      //      bv.initialize();
      bv.repaint();
      
      System.err.println("Loading instances from : "+args[0]);
      java.io.Reader r = new java.io.BufferedReader(
			 new java.io.FileReader(args[0]));
      Instances i = new Instances(r);
      i.setClassIndex(Integer.parseInt(args[1]));
      bv.setTrainingData(i);
      bv.setClassifier(new Logistic());
      bv.setDataGenerator(new KDDataGenerator());
      bv.setRedClassValue(Integer.parseInt(args[2]));
      bv.setGreenClassValue(Integer.parseInt(args[3]));
      bv.setBlueClassValue(Integer.parseInt(args[4]));
      bv.setXAttribute(Integer.parseInt(args[5]));
      bv.setYAttribute(Integer.parseInt(args[6]));
      bv.start();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

