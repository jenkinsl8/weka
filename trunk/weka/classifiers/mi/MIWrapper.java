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
 * MIWrapper.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 * 
 */

package weka.classifiers.mi;

import weka.classifiers.Evaluation;
import weka.classifiers.SingleClassifierEnhancer;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MultiInstanceToPropositional;

import java.util.Enumeration;
import java.util.Vector;

/**
 * A simple Wrapper method for applying standard propositional learners to
 * multi-instance data. <p/>
 * 
 * For more information see:<br/>
 * Frank, E. T. and Xu, X. (2003), <i>Applying propositional learning
 * algorithms to multi-instance data</i>, Working Paper 06/03, Department of
 * Computer Science, The University of Waikato, Hamilton.
 * <p/>
 *
 * Valid options are:<p/>
 *
 * -D <br/>
 * Turn on debugging output.<p/>
 *
 * -W classname <br/>
 * Specify the full class name of a classifier as the basis (required).<p/>
 *
 * -P 1|2|3 <br/>
 * Set which method to use in testing: <br/>
 * 1.arithmetic average <br/>
 * 2.geometric average <br/>
 * 3.max probability of positive bag. <br/>
 * (default: 1)<p/>
 *
 * -A 0|1|2|3 <br/>
 *  four different methods of setting the weight of each single-instance:
 *  (default method: 3) <br/>
 *  0. keep the weight to be the same as the original value <br/>
 *  1. weight = 1.0 <br/>
 *  2. weight = 1.0/Total number of single-instance in the corresponding bag
 *  <br>
 *  3. weight = Total number of single-instance / (Total number of bags * Total
 *     number of single-instance in the corresponding bag) <p/>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision: 1.1 $ 
 */
public class MIWrapper 
  extends SingleClassifierEnhancer
  implements MultiInstanceCapabilitiesHandler, OptionHandler {  

  static final long serialVersionUID = -7707766152904315910L;
  
  /** The number of the class labels */
  protected int m_NumClasses;

  public static final int TESTMETHOD_ARITHMETIC = 1;
  public static final int TESTMETHOD_GEOMETRIC = 2;
  public static final int TESTMETHOD_MAXPROB = 3;
  public static final Tag[] TAGS_TESTMETHOD = {
    new Tag(TESTMETHOD_ARITHMETIC, "arithmetic average"),
    new Tag(TESTMETHOD_GEOMETRIC, "geometric average"),
    new Tag(TESTMETHOD_MAXPROB, "max probability of positive bag")
  };

  /** the test method  */
  protected int m_Method = TESTMETHOD_GEOMETRIC;

  /** Filter used to convert MI dataset into single-instance dataset */
  protected MultiInstanceToPropositional m_ConvertToProp = new MultiInstanceToPropositional();

  /** the single-instance weight setting method */
  protected int m_WeightMethod = MultiInstanceToPropositional.WEIGHTMETHOD_INVERSE2;

  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
         "A simple Wrapper method for applying standard propositional learners "
       + "to multi-instance data.\n\n"
       + "For more information see:\n"
       + "Frank, E. T. and Xu, X. (2003), Applying propositional learning "
       + "algorithms to multi-instance data, Working Paper 06/03, Department "
       + "of Computer Science, The University of Waikato, Hamilton.";
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    result.addElement(new Option(
          "\tThe method used in testing:\n"
          + "\t1.arithmetic average\n"
          + "\t2.geometric average\n"
          + "\t3.max probability of positive bag.\n"
          + "\t(default: 1)",
          "P", 1, "-P [1|2|3]"));
    
    result.addElement(new Option(
          "\tThe type of weight setting for each single-instance:\n"
          + "\t0.keep the weight to be the same as the original value;\n"
          + "\t1.weight = 1.0\n"
          + "\t2.weight = 1.0/Total number of single-instance in the\n"
          + "\t\tcorresponding bag\n"
          + "\t3. weight = Total number of single-instance / (Total\n"
          + "\t\tnumber of bags * Total number of single-instance \n"
          + "\t\tin the corresponding bag).\n"
          + "\t(default: 3)",
          "A", 1, "-A [0|1|2|3]"));	

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements()) {
      result.addElement(enu.nextElement());
    }

    return result.elements();
  }


  /**
   * Parses a given list of options. 
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDebug(Utils.getFlag('D', options));

    String methodString = Utils.getOption('P', options);
    if (methodString.length() != 0) {
      setMethod(
          new SelectedTag(Integer.parseInt(methodString), TAGS_TESTMETHOD));
    } else {
      setMethod(
          new SelectedTag(TESTMETHOD_ARITHMETIC, TAGS_TESTMETHOD));
    }

    String weightString = Utils.getOption('A', options);
    if (weightString.length() != 0) {
      setWeightMethod(
          new SelectedTag(
            Integer.parseInt(weightString), 
            MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
    } else {
      setWeightMethod(
          new SelectedTag(
            MultiInstanceToPropositional.WEIGHTMETHOD_INVERSE2, 
            MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
    }	

    super.setOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result  = new Vector();
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);

    if (getDebug())
      result.add("-D");
    
    result.add("-P");
    result.add("" + m_Method);

    result.add("-A");
    result.add("" + m_WeightMethod);

    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightMethodTipText() {
    return "The method used for weighting the instances.";
  }

  /**
   * The new method for weighting the instances.
   *
   * @param method      the new method
   */
  public void setWeightMethod(SelectedTag method){
    if (method.getTags() == MultiInstanceToPropositional.TAGS_WEIGHTMETHOD)
      m_WeightMethod = method.getSelectedTag().getID();
  }

  /**
   * Returns the current weighting method for instances.
   */
  public SelectedTag getWeightMethod(){
    return new SelectedTag(
                  m_WeightMethod, MultiInstanceToPropositional.TAGS_WEIGHTMETHOD);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String methodTipText() {
    return "The method used for testing.";
  }

  /**
   * Set the method used in testing. 
   *
   * @param newMethod the index of method to use.
   */
  public void setMethod(SelectedTag method) {
    if (method.getTags() == TAGS_TESTMETHOD)
      m_Method = method.getSelectedTag().getID();
  }

  /**
   * Get the method used in testing.
   *
   * @return the index of method used in testing.
   */
  public SelectedTag getMethod() {
    return new SelectedTag(m_Method, TAGS_TESTMETHOD);
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // class
    result.disableAllClasses();
    if (super.getCapabilities().handles(Capability.NOMINAL_CLASS))
      result.enable(Capability.NOMINAL_CLASS);
    if (super.getCapabilities().handles(Capability.BINARY_CLASS))
      result.enable(Capability.BINARY_CLASS);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);
    
    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   *
   * @return            the capabilities of this object
   * @see               Capabilities
   */
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    
    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);
    
    return result;
  }

  /**
   * Builds the classifier
   *
   * @param data the training data to be used for generating the
   * boosted classifier.
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    Instances train = new Instances(data);
    train.deleteWithMissingClass();
    
    if (m_Classifier == null) {
      throw new Exception("A base classifier has not been specified!");
    }

    if (getDebug())
      System.out.println("Start training ...");
    m_NumClasses = train.numClasses();

    //convert the training dataset into single-instance dataset
    m_ConvertToProp.setWeightMethod(getWeightMethod());
    m_ConvertToProp.setInputFormat(train);
    train = Filter.useFilter(train, m_ConvertToProp);
    train.deleteAttributeAt(0); // remove the bag index attribute

    m_Classifier.buildClassifier(train);
  }		

  /**
   * Computes the distribution for a given exemplar
   *
   * @param exmp the exemplar for which distribution is computed
   * @return the distribution
   * @throws Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance exmp) 
    throws Exception {	

    Instances testData = new Instances (exmp.dataset(),0);
    testData.add(exmp);

    // convert the training dataset into single-instance dataset
    m_ConvertToProp.setWeightMethod(
        new SelectedTag(
          MultiInstanceToPropositional.WEIGHTMETHOD_ORIGINAL, 
          MultiInstanceToPropositional.TAGS_WEIGHTMETHOD));
    testData = Filter.useFilter(testData, m_ConvertToProp);
    testData.deleteAttributeAt(0); //remove the bag index attribute

    // Compute the log-probability of the bag
    double [] distribution = new double[m_NumClasses];
    double nI = (double)testData.numInstances();
    double [] maxPr = new double [m_NumClasses];

    for(int i=0; i<nI; i++){
      double[] dist = m_Classifier.distributionForInstance(testData.instance(i));
      for(int j=0; j<m_NumClasses; j++){

        switch(m_Method){
          case TESTMETHOD_ARITHMETIC:
            distribution[j] += dist[j]/nI;
            break;
          case TESTMETHOD_GEOMETRIC:
            // Avoid 0/1 probability
            if(dist[j]<0.001)
              dist[j] = 0.001;
            else if(dist[j]>0.999)
              dist[j] = 0.999;

            distribution[j] += Math.log(dist[j])/nI;
            break;
          case TESTMETHOD_MAXPROB:
            if (dist[j]>maxPr[j]) 
              maxPr[j] = dist[j];
            break;
        }
      }
    }

    if(m_Method == TESTMETHOD_GEOMETRIC)
      for(int j=0; j<m_NumClasses; j++)
        distribution[j] = Math.exp(distribution[j]);

    if(m_Method == TESTMETHOD_MAXPROB){   // for positive bag
      distribution[1] = maxPr[1];
      distribution[0] = 1 - distribution[1];
    }

    if (Utils.eq(Utils.sum(distribution), 0)) {
      for (int i = 0; i < distribution.length; i++)
	distribution[i] = 1.0 / (double) distribution.length;
    }
    else {
      Utils.normalize(distribution);
    }
    
    return distribution;
  }

  /**
   * Gets a string describing the classifier.
   *
   * @return a string describing the classifer built.
   */
  public String toString() {	
    return "MIWrapper with base classifier: \n"+m_Classifier.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the command line arguments to the
   * scheme (see Evaluation)
   */
  public static void main(String[] argv) {
    try {
      System.out.println(Evaluation.evaluateModel(new MIWrapper(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
