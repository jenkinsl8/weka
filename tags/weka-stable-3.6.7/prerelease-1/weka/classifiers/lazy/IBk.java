/*
 *    IBk.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg,Stuart Inglis
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

package weka.classifiers;

import java.io.*;
import java.util.*;
import weka.core.*;


/**
 * IB1-type classifier class. Predicts enumerated class attributes. May
 * specify <i>k</i> nearest neighbors to use for prediction. <p>
 *
 * Valid options are:<p>
 *
 * -K num <br>
 * Set the number of nearest neighbors to use in prediction
 * (default 1) <p>
 *
 * -W num <br>
 * Set a fixed window size for incremental train/testing. As
 * new training instances are added, oldest instances are removed
 * to maintain the number of training instances at this size.
 * (default no window) <p>
 *
 * -D <br>
 * Neighbors will be weighted by the inverse of their distance
 * when voting. (default equal weighting) <p>
 *
 * -F <br>
 * Neighbors will be weighted by their similarity when voting.
 * (default equal weighting) <p>
 *
 * -X <br>
 * Select the number of neighbors to use by cross validation, with
 * an upper limit given by the -K option.
 *
 * -S <br>
 * When k is selected by cross-validation for numeric class attributes,
 * minimize mean-squared error. (default mean absolute error) <p>
 *
 * -B <br>
 * Bayes' weight all models with k=1 .. k=kmax (given by -K) <p>
 *
 * @version 1.2 - 1 Apr 1998 - Multiple neighbors, windowing. (Len) <br>
 *          1.1 - 6 Aug 1997 - changed main (Eibe) <br>
 *          1.0 - 7 Jul 1997 - Initial version (Stu, Eibe)
 * @author Stuart Inglis (singlis@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 */
public class IBk extends DistributionClassifier implements
  OptionHandler, UpdateableClassifier, WeightedInstancesHandler {

  // Classes for a linked list to store the nearest k neighbours
  // to an instance. We use a list so that we can take care of
  // cases where multiple neighbours are the same distance away.
  // i.e. the minimum length of the list is k
  public class NeighborNode {

    protected double m_distance;
    protected Instance m_instance;
    protected NeighborNode m_next;
    
    public NeighborNode(double distance, Instance instance, NeighborNode next){
      m_distance = distance;
      m_instance = instance;
      m_next = next;
    }
    public NeighborNode(double distance, Instance instance) {
      m_distance = distance;
      m_instance = instance;
      m_next = null;
    }
  }

  class NeighborList {

    NeighborNode m_first, m_last;
    int m_length;
    
    public NeighborList(int length) {

      m_length = length;
      m_first = m_last = null;
    }

    public NeighborList() {

      this(1);
    }

    public boolean isEmpty() {

      return (m_first == null);
    }

    public int currentLength() {

      int i = 0;
      NeighborNode current = m_first;
      while (current != null) {
	i++;
	current = current.m_next;
      }
      return i;
    }

    public void insertSorted(double distance, Instance instance) {

      if (isEmpty()) {
	m_first = m_last = new NeighborNode(distance, instance);
      } else {
	NeighborNode current = m_first;
	if (distance < m_first.m_distance) {// Insert at head
	  m_first = new NeighborNode(distance, instance, m_first);
	} else { // Insert further down the list
	  for( ;(current.m_next != null) && 
		 (current.m_next.m_distance < distance); 
	       current = current.m_next);
	  current.m_next = new NeighborNode(distance, instance,
					    current.m_next);
	  if (current.equals(m_last)) {
	    m_last = current.m_next;
	  }
	}
	// Trip down the list until we've got k list elements (or more if the
	// distance to the last elements is the same).
	int valcount = 0;
	for(current = m_first; current.m_next != null; 
	    current = current.m_next) {
	  valcount++;
	  if ((valcount >= m_length) && (current.m_distance != 
					 current.m_next.m_distance)) {
	    m_last = current;
	    current.m_next = null;
	    break;
	  }
	}
      }
    }

    public void pruneToK(int k) {

      if (isEmpty()) {
	return;
      }
      if (k < 1) {
	k = 1;
      }
      int currentK = 0;
      double currentDist = m_first.m_distance;
      NeighborNode current = m_first;
      for(; current.m_next != null; current = current.m_next) {
	currentK++;
	currentDist = current.m_distance;
	if ((currentK >= k) && (currentDist != current.m_next.m_distance)) {
	  m_last = current;
	  current.m_next = null;
	  break;
	}
      }
      //      System.err.println("Pruned to " + k);
      // printList();
    }

    public void printList() {

      if (isEmpty()) {
	System.err.println("Empty list");
      } else {
	NeighborNode current = m_first;
	while (current != null) {
	  System.err.println("Node: instance " + current.m_instance 
			     + ", distance " + current.m_distance);
	  current = current.m_next;
	}
	System.err.println();
      }
    }
  }
  
  
  // =================
  // Private variables
  // =================

  /** The training instances used for classification. */
  protected Instances m_Train;

  /** The number of class values (or 1 if predicting numeric) */
  protected int m_NumClasses;

  /** The class attribute type */
  protected int m_ClassType;

  /** The minimum values for numeric attributes. */
  protected double [] m_Min;

  /** The maximum values for numeric attributes. */
  protected double [] m_Max;

  /** The number of neighbours to use for classification (currently) */
  protected int m_kNN;

  /**
   * The value of kNN provided by the user. This may differ from
   * m_kNN if cross-validation is being used
   */
  protected int m_kNNUpper;

  /**
   * Whether the value of k selected by cross validation has
   * been invalidated by a change in the training instances
   */
  protected boolean m_kNNValid;

  /**
   * The maximum number of training instances allowed. When
   * this limit is reached, old training instances are removed,
   * so the training data is "windowed". Set to 0 for unlimited
   * numbers of instances.
   */
  protected int m_WindowSize;

  /** Whether the neighbours should be distance-weighted */
  protected int m_DistanceWeighted;

  /** Whether to select k by cross validation */
  protected boolean m_CrossValidate;

  /**
   * Whether to minimise mean squared error rather than mean absolute
   * error when cross-validating on numeric prediction tasks
   */
  protected boolean m_MeanSquared;

  /* Define possible instance weighting methods */
  public static final int WEIGHT_NONE = 0;
  public static final int WEIGHT_INVERSE = 1;
  public static final int WEIGHT_SIMILARITY = 2;

  // ============
  // Experimental
  // ============

  /** Whether to simulate sensor validation during cross-validation */
  protected boolean xxx_Validation;

  /** Whether to perform bayesian averaging of possible k models */
  protected boolean xxx_BayesAverage;

  /** Store the model weights */
  protected double [] xxx_BayesWeights;

  // ==============
  // Public methods
  // ==============

  /**
   * IBk classifier. Simple instance-based learner that uses the class
   * of the nearest k training instances for the class of the test
   * instances.
   *
   * @param k the number of nearest neighbors to use for prediction
   */
  public IBk(int k) {

    init();
    setKNN(k);
  }  

  /**
   * IB1 classifer. Instance-based learner. Predicts the class of the
   * single nearest training instance for each test instance.
   */
  public IBk() {

    init();
  }
  
  /**
   * Set the number of neighbours the learner is to use.
   *
   * @param k the number of neighbours.
   */
  public void setKNN(int k) {

    m_kNN = k;
    m_kNNUpper = k;
    m_kNNValid = false;
  }

  /**
   * Gets the number of neighbours the learner will use.
   *
   * @return the number of neighbours.
   */
  public int getKNN() {

    return m_kNN;
  }

  /**
   * Get the number of training instances the classifier is currently using
   */
  public int getNumTraining() {

    return m_Train.numInstances();
  }

  /**
   * Get an attributes minimum observed value
   */
  public double getAttributeMin(int index) {

    return m_Min[index];
  }

  /**
   * Get an attributes maximum observed value
   */
  public double getAttributeMax(int index) {

    return m_Max[index];
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */

  public void buildClassifier(Instances instances) throws Exception {

    if (instances.classIndex() < 0) {
      throw new Exception ("No class attribute assigned to instances");
    }
    if (instances.checkForStringAttributes()) {
      throw new Exception("Can't handle string attributes!");
    }
    try {
      m_NumClasses = instances.numClasses();
      m_ClassType = instances.classAttribute().type();
    } catch (Exception ex) {
      System.err.println("This should never be reached");
      System.exit(0);
    }

    // Throw away training instances with missing class
    m_Train = new Instances(instances, 0, instances.numInstances());
    m_Train.deleteWithMissingClass();

    // Throw away initial instances until within the specified window size
    if ((m_WindowSize > 0) && (instances.numInstances() > m_WindowSize)) {
      m_Train = new Instances(m_Train, 
			      m_Train.numInstances()-m_WindowSize, 
			      m_WindowSize);
    }

    // Calculate the minimum and maximum values
    m_Min=new double [m_Train.numAttributes()];
    m_Max=new double [m_Train.numAttributes()];
    for (int i = 0; i < m_Train.numAttributes(); i++) {
      m_Min[i] = m_Max[i] = Double.NaN;
    }
    Enumeration enum = m_Train.enumerateInstances();
    while (enum.hasMoreElements()) {
      updateMinMax((Instance) enum.nextElement());
    }

    // Invalidate any currently cross-validation selected k
    m_kNNValid = false;
  }

  /**
   * Adds the supplied instance to the training set
   *
   * @param instance the instance to add
   * @exception Exception if instance could not be incorporated
   * successfully
   */
  public void updateClassifier(Instance instance) throws Exception {

    if (m_Train.equalHeaders(instance.dataset()) == false) {
      throw new Exception("Incompatible instance types");
    }
    if (instance.classIsMissing()) {
      return;
    }
    updateMinMax(instance);
    m_Train.add(instance);
    //    System.err.println("Currently "+m_Train.numInstances()+"\n"+m_Train.toString());
    m_kNNValid = false;
    if ((m_WindowSize > 0) && (m_Train.numInstances() > m_WindowSize)) {
      while (m_Train.numInstances() > m_WindowSize) {
	m_Train.delete(0);
      }
    }
  }

  
  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if an error occurred during the prediction
   */
  public double [] distributionForInstance(Instance instance) throws Exception{

    if ((m_WindowSize > 0) && (m_Train.numInstances() > m_WindowSize)) {
      m_kNNValid = false;
      while (m_Train.numInstances() > m_WindowSize) {
	m_Train.delete(0);
      }
    }

    // Select k by cross validation (and determine Bayes model weights)
    if (!m_kNNValid && (m_CrossValidate) && (m_kNN > 1)) {
      crossValidate();
    }
    updateMinMax(instance);

    NeighborList neighborlist = findNeighbors(instance);
    double [] distribution;
    if (xxx_BayesAverage) {
      distribution = new double [m_NumClasses];
      for(int j = m_kNNUpper-1; j >= 0; j--) {
	double [] tempDist = makeDistribution(neighborlist);
	//	System.err.println("Incorporating "+(j+1)+" neighbor model (weight "
	//		   +xxx_BayesWeights[j]+")");
	for(int i = 0; i < m_NumClasses; i++) {
	  distribution[i] += tempDist[i] * xxx_BayesWeights[j];
	  //	  System.err.println("P["+i+"]="+distribution[i]
			     //	     +"   T"+(j+1)+"["+i+"]="+tempDist[i]);
	}
	if (j >= 1) {
	  neighborlist.pruneToK(j);
	}
      }
      //      System.err.println("Combined models");
      try {
	Utils.normalize(distribution);
	//System.err.println("Final distribution");
	//for(int i = 0; i < numClasses; i++)
	//  System.err.println("P["+i+"]="+distribution[i]);

      } catch (Exception ex) {
	throw new Exception("Bayes weighting - couldn't normalize distribution");
      }
    } else {
      distribution = makeDistribution(neighborlist);
    }
    return distribution;
  }
 

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
	      "\tWeight neighbours by the inverse of their distance\n"
	      +"\t(use when k > 1)",
	      "D", 0, "-D"));
    newVector.addElement(new Option(
	      "\tWeight neighbours by 1 - their distance\n"
	      +"\t(use when k > 1)",
	      "F", 0, "-F"));
    newVector.addElement(new Option(
	      "\tNumber of nearest neighbours (k) used in classification.\n"
	      +"\t(Default = 1)",
	      "K", 1,"-K <number of neighbors>"));
    newVector.addElement(new Option(
              "\tMinimise mean squared error rather than mean absolute\n"
	      +"\terror when using -X option with numeric prediction.",
	      "S", 0,"-S"));
    newVector.addElement(new Option(
              "\tMaximum number of training instances maintained.\n"
	      +"\tTraining instances are dropped FIFO. (Default = no window)",
	      "W", 1,"-W <window size>"));
    newVector.addElement(new Option(
	      "\tSelect the number of nearest neighbours between 1\n"
	      +"\tand the k value specified using hold-one-out evaluation\n"
	      +"\ton the training data (use when k > 1)",
	      "X", 0,"-X"));
    newVector.addElement(new Option(
              "\tCombine models from k=1 to k=<-K> with bayes weighting (experimental)\n",
	      "B",0,"-B"));
    newVector.addElement(new Option(
              "\tSimulate sensor validation after cross validation (experimental)\n",
	      "V",0,"-V"));
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -K num <br>
   * Set the number of nearest neighbors to use in prediction
   * (default 1) <p>
   *
   * -W num <br>
   * Set a fixed window size for incremental train/testing. As
   * new training instances are added, oldest instances are removed
   * to maintain the number of training instances at this size.
   * (default no window) <p>
   *
   * -D <br>
   * Neighbors will be weighted by the inverse of their distance
   * when voting. (default equal weighting) <p>
   *
   * -F <br>
   * Neighbors will be weighted by their similarity when voting.
   * (default equal weighting) <p>
   *
   * -X <br>
   * Select the number of neighbors to use by cross validation, with
   * an upper limit given by the -K option.
   *
   * -S <br>
   * When k is selected by cross-validation for numeric class attributes,
   * minimize mean-squared error. (default mean absolute error) <p>
   *
   * -B <br>
   * Bayes' weight all models with k=1 .. k=kmax (given by -K) <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String knnString = Utils.getOption('K', options);
    if (knnString.length() != 0) {
      setKNN(Integer.parseInt(knnString));
    }
    String windowString = Utils.getOption('W', options);
    if (windowString.length() != 0) {
      m_WindowSize = Integer.parseInt(windowString);
    }
    if (Utils.getFlag('D', options)) {
      m_DistanceWeighted = WEIGHT_INVERSE;
    } else if (Utils.getFlag('F', options)) {
      m_DistanceWeighted = WEIGHT_SIMILARITY;
    } else
      m_DistanceWeighted = WEIGHT_NONE;
    m_CrossValidate = Utils.getFlag('X', options);
    m_MeanSquared = Utils.getFlag('S', options);
    xxx_Validation = Utils.getFlag('V', options);
    xxx_BayesAverage = Utils.getFlag('B', options);
    if (xxx_BayesAverage) {
      m_CrossValidate = true;
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of IBk.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;
    options[current++] = "-K"; options[current++] = "" + getKNN();
    options[current++] = "-W"; options[current++] = "" + m_WindowSize;
    if (xxx_BayesAverage) {
      options[current++] = "-B";
    } else if (m_CrossValidate) {
      options[current++] = "-X";
    }
    if (xxx_Validation) {
      options[current++] = "-V";
    }
    if (m_MeanSquared) {
      options[current++] = "-S";
    }
    if (m_DistanceWeighted == WEIGHT_INVERSE) {
      options[current++] = "-D";
    } else if (m_DistanceWeighted == WEIGHT_SIMILARITY) {
      options[current++] = "-F";
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {

    String result = "IB1 instance-based classifier\n" +
      "using " + m_kNN;

    switch (m_DistanceWeighted) {
    case WEIGHT_INVERSE:
      result += " inverse-distance-weighted";
      break;
    case WEIGHT_SIMILARITY:
      result += " similarity-weighted";
      break;
    }
    result += " nearest neighbour(s) for classification\n";

    if (m_WindowSize != 0) {
      result += "using a maximum of " 
	+ m_WindowSize + " (windowed) training instances\n";
    }
    return result;
  }

  // ===============
  // Private methods
  // ===============

  /**
   * Initialise scheme variables.
   */
  private void init() {

    setKNN(1);
    m_WindowSize = 0;
    m_DistanceWeighted = WEIGHT_NONE;
    m_CrossValidate = false;
    m_MeanSquared = false;
    xxx_Validation = false;
    xxx_BayesAverage = false;
    xxx_BayesWeights = null;
  }

  /**
   * Calculates the distance between two instances
   *
   * @param test the first instance
   * @param train the second instance
   * @return the distance between the two given instances, between 0 and 1
   */          
  private double distance(Instance first, Instance second) {  

    double diff,distance = 0;

    for(int i = 0; i < m_Train.numAttributes(); i++) { 
      if (i == m_Train.classIndex()) {
	continue;
      }
      if (m_Train.attribute(i).isNominal()) {
	
	// If attribute is nominal

	if (first.isMissing(i) || second.isMissing(i) ||
	    ((int)first.value(i) != (int)second.value(i))) {
	  diff = 1;
	} else {
	  diff = 0;
	}
      } else {
	// If attribute is numeric
	
	if (first.isMissing(i) || second.isMissing(i)) {
	  if (first.isMissing(i) && second.isMissing(i)) {
	    diff = 1;
	  } else {
	    if (second.isMissing(i)) {
	      diff = norm(first.value(i),i);
	    } else {
	      diff = norm(second.value(i),i);
	    }
	    if (diff < 0.5) {
	      diff = 1.0-diff;
	    }
	  }
	} else {
	  diff = norm(first.value(i),i)-norm(second.value(i),i);
	}
      }
      distance += diff*diff;
    }
    
    return Math.sqrt(distance);
  }

  /**
   * Normalizes a given value of a numeric attribute.
   *
   * @param x the value to be normalized
   * @param i the attribute's index
   */
  private double norm(double x,int i) {

    if (Double.isNaN(m_Min[i]) || Utils.eq(m_Max[i],m_Min[i])) {
      return 0;
    } else {
      return (x - m_Min[i]) / (m_Max[i] - m_Min[i]);
    }
  }
                      
  /**
   * Updates the minimum and maximum values for all the attributes
   * based on a new instance.
   *
   * @param instance the new instance
   */
  private void updateMinMax(Instance instance) {  

    for (int j = 0;j < m_Train.numAttributes(); j++) {
      if (!instance.isMissing(j)) {
	if (Double.isNaN(m_Min[j])) {
	  m_Min[j] = instance.value(j);
	  m_Max[j] = instance.value(j);
	} else {
	  if (instance.value(j) < m_Min[j]) {
	    m_Min[j] = instance.value(j);
	  } else {
	    if (instance.value(j) > m_Max[j]) {
	      m_Max[j] = instance.value(j);
	    }
	  }
	}
      }
    }
  }
    
  /**
   * Build the list of nearest k neighbors to the given test instance.
   *
   * @param instance the instance to search for neighbours of
   * @return a list of neighbors
   */
  private NeighborList findNeighbors(Instance instance) {

    double distance;
    NeighborList neighborlist = new NeighborList(m_kNN);
    Enumeration enum = m_Train.enumerateInstances();
    int i = 0;

    while (enum.hasMoreElements()) {
      Instance trainInstance = (Instance) enum.nextElement();
      if (instance != trainInstance) { // for hold-one-out cross-validation
	distance = distance(instance, trainInstance);
	//	System.err.println("dist to train "+i+" "+trainInstance.toString()
	//		   +" "+distance);
	if (neighborlist.isEmpty() || (i < m_kNN) || 
	    (distance <= neighborlist.m_last.m_distance)) {
	  neighborlist.insertSorted(distance, trainInstance);
	  //  neighborlist.printList();
	}
	i++;
      }
    }

    return neighborlist;
  }

  /**
   * Turn the list of nearest neighbors into a probability distribution
   *
   * @param neighborlist the list of nearest neighboring instances
   * @return the probability distribution
   */
  private double [] makeDistribution(NeighborList neighborlist) 
    throws Exception {

    double total = 0, weight;
    double [] distribution = new double [m_NumClasses];
    
    //neighborlist.printList();

    // Set up a correction to the estimator
    if (m_ClassType == Attribute.NOMINAL) {
      for(int i = 0; i < m_NumClasses; i++) {
	distribution[i] = 1.0 / Math.max(1,m_Train.numInstances());
      }
      total = (double)m_NumClasses / Math.max(1,m_Train.numInstances());
    }

    if (!neighborlist.isEmpty()) {
      // Collect class counts
      NeighborNode current = neighborlist.m_first;
      while (current != null) {
	switch (m_DistanceWeighted) {
	case WEIGHT_INVERSE:
	  weight = 1.0 / (current.m_distance + 0.001); // to avoid div by zero
	  break;
	case WEIGHT_SIMILARITY:
	  weight = 1.0 - current.m_distance;           // to avoid div by zero
	  break;
	default:                                       // WEIGHT_NONE:
	  weight = 1.0;
	  break;
	}
	weight *= current.m_instance.weight();
	try {
	  switch (m_ClassType) {
	  case Attribute.NOMINAL:
	    distribution[(int)current.m_instance.classValue()] += weight;
	    break;
	  case Attribute.NUMERIC:
	    distribution[0] += current.m_instance.classValue() * weight;
	    break;
	  }
	} catch (Exception ex) {
	  System.err.println("Data has no class attribute!");
	  System.exit(0);
	}
	total += weight;

	current = current.m_next;
      }
    }

    // Normalise distribution
    try {
      if (total > 0) {
	Utils.normalize(distribution, total);
      }
    } catch (Exception ex) {
      throw new Exception("Distribution normalization failed!");
    }
    return distribution;
  }

  /**
   * Select the best value for k by hold-one-out cross-validation.
   * If the class attribute is nominal, classification error is
   * minimised. If the class attribute is numeric, mean absolute
   * error is minimised
   */
  private void crossValidate() {

    try {
      double [] performanceStats = new double [m_kNNUpper];
      double [] performanceStatsSq = new double [m_kNNUpper];
      xxx_BayesWeights = new double [m_kNNUpper];

      for(int i = 0; i < m_kNNUpper; i++) {
	performanceStats[i] = 0;
	performanceStatsSq[i] = 0;
	xxx_BayesWeights[i] = 1;
      }


      m_kNN = m_kNNUpper;
      Instance instance;
      NeighborList neighborlist;
      for(int i = 0; i < m_Train.numInstances(); i++) {
	if (i % 50 == 0) {
	  System.err.print("Cross validating "
			   + i + "/" + m_Train.numInstances() + "\r");
	}
	instance = m_Train.instance(i);
	neighborlist = findNeighbors(instance);

	for(int j = m_kNNUpper-1; j >= 0; j--) {
	  // Update the performance stats
	  double [] distribution = makeDistribution(neighborlist);
	  double thisPrediction = Utils.maxIndex(distribution);
	  if (m_Train.classAttribute().isNumeric()) {
	    double err = thisPrediction - instance.classValue();
	    performanceStatsSq[j] += err * err;   // Squared error
	    performanceStats[j] += Math.abs(err); // Absolute error
	  } else {
	    xxx_BayesWeights[j] *= distribution[(int)instance.classValue()];
	    if (thisPrediction != instance.classValue()) {
	      performanceStats[j] ++;             // Classification error
	    }
	  }
	  if (j >= 1) {
	    neighborlist.pruneToK(j);
	  }
	}

	if (i % 10 == 0) { // Renormalize probabilities
	  Utils.normalize(xxx_BayesWeights);
	}
      }

      // Display the results of the cross-validation
      for(int i = 0; i < m_kNNUpper; i++) {
	System.err.print("Hold-one-out performance of " + (i + 1)
			 + " neighbors " );
	if (m_Train.classAttribute().isNumeric()) {
	  if (m_MeanSquared) {
	    System.err.println("(RMSE) = "
			       + Math.sqrt(performanceStatsSq[i]
					   / m_Train.numInstances()));
	  } else {
	    System.err.println("(MAE) = "
			       + performanceStats[i] / m_Train.numInstances());
	  }
	} else {
	  Utils.normalize(xxx_BayesWeights);
	  System.err.print("(Prob) = "
			   + xxx_BayesWeights[i] + "  ");
	  System.err.println("(%ERR) = "
			     + 100.0 * performanceStats[i]
			     / m_Train.numInstances());
	}
      }


      if (xxx_BayesAverage) {
	System.err.println("Will weight the models with kMax="+m_kNN);
      } else {
	// Check through the performance stats and select the best
	// k value (or the lowest k if more than one best)
	double [] searchStats = performanceStats;
	if (m_Train.classAttribute().isNumeric() && m_MeanSquared) {
	  searchStats = performanceStatsSq;
	}
	if (m_Train.classAttribute().isNominal() && m_MeanSquared) {
	  searchStats = new double [m_kNNUpper];
	  for(int i = 0; i < m_kNNUpper; i++) {
	    searchStats[i] = 1.0-xxx_BayesWeights[i];
	  }
	}
	double bestPerformance = Double.NaN;
	int bestK = 1;
	for(int i = 0; i < m_kNNUpper; i++) {
	  if (Double.isNaN(bestPerformance)
	      || (bestPerformance > searchStats[i])) {
	    bestPerformance = searchStats[i];
	    bestK = i + 1;
	  }
	}
	m_kNN = bestK;
	System.err.println("Selected k = "+bestK);

	// Simulate sensor validation by picking out instances where
	// a large prediction error was made
	if (xxx_Validation && m_Train.classAttribute().isNumeric()) {
	  double mean = performanceStats[bestK - 1] / m_Train.numInstances();
	  double stdDev = Math.sqrt((performanceStatsSq[bestK - 1]
				     - mean * mean * m_Train.numInstances())
				    / m_Train.numInstances());
	  System.err.println("Error -- mean: " + mean +
			     "  std dev: " + stdDev);
      
	  for(int i = 0; i < m_Train.numInstances(); i++) {
	    if (i % 50 == 0) {
	      System.err.print("Validating " + i + "/" 
			       + m_Train.numInstances() + "\r");
	    }
	    instance = m_Train.instance(i);
	
	    double err = Math.abs(Utils.maxIndex(makeDistribution
						 (findNeighbors(instance)))
				  - instance.classValue());
	    if (err > stdDev * 3) {
	      System.err.println("Possible sensor failure for instance " 
				 + (i + 1) + " prediction error " + err);
	      System.err.println(instance);
	    }
	  }
	}
      }


      m_kNNValid = true;
    } catch (Exception ex) {
      System.err.println("Couldn't optimize by cross-validation");
      System.err.println(ex.getMessage());
      System.exit(0);
    }
  }


  // ============
  // Test method.
  // ============

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new IBk(), argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}





