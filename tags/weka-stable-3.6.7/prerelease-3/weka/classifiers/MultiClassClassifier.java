/*
 *    MultiClassClassifier.java
 *    Copyright (C) 1999 Eibe Frank,Len Trigg
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

import java.util.*;
import weka.core.*;
import weka.filters.*;

/**
 * Class for handling multi-class datasets with 2-class distribution
 * classifiers.<p>
 *
 * Valid options are:<p>
 *
 * -W classname <br>
 * Specify the full class name of a learner as the basis for 
 * the multi-class classifier (required).<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $
 */
public class MultiClassClassifier extends DistributionClassifier 
  implements OptionHandler, WeightedInstancesHandler {

  /** The classifiers. (One for each class.) */
  private Classifier [] m_Classifiers;

  /** The filters used to transform the class. */
  private MakeIndicatorFilter[] m_ClassFilters;

  /** The class name of the base classifier. */
  private Classifier m_Classifier = new weka.classifiers.ZeroR();

  /** ZeroR classifier for when all base classifier return zero probability. */
  private ZeroR m_ZeroR;

  /**
   * Builds the classifiers.
   *
   * @param insts the training data.
   * @exception Exception if a classifier can't be built
   */
  public void buildClassifier(Instances insts) throws Exception {

    Instances newInsts;

    if (m_Classifier == null) {
      throw new Exception("No base classifier has been set!");
    }
    if (!insts.classAttribute().isNominal()) {
      throw new 
	Exception("MultiClassClassifier needs a nominal class!");
    }
    if (m_Classifier instanceof WeightedInstancesHandler) {

      // Base classifier can handle weights
      insts = new Instances(insts);
    } else {

      // Use resampling if base classifer can't handle weights and weights not all one
      double[] weights = new double[insts.numInstances()];
      boolean foundOne = false;
      for (int i = 0; i < weights.length; i++) {
	weights[i] = insts.instance(i).weight();
	if (!Utils.eq(weights[i], weights[0])) {
	  foundOne = true;
	}
      }
      if (foundOne) {
	insts = insts.resampleWithWeights(new Random(42), weights);
      }
    }     
    insts.deleteWithMissingClass();
    if (insts.numInstances() == 0) {
      throw new Exception("No train instances without missing class!");
    }
    m_ZeroR = new ZeroR();
    m_ZeroR.buildClassifier(insts);
    m_Classifiers = Classifier.makeCopies(m_Classifier, insts.numClasses());
    m_ClassFilters = new MakeIndicatorFilter[insts.numClasses()];
    for (int i = 0; i < insts.numClasses(); i++) {
      m_ClassFilters[i] = new MakeIndicatorFilter();
      m_ClassFilters[i].setAttributeIndex(insts.classIndex());
      m_ClassFilters[i].setValueIndex(i);
      m_ClassFilters[i].setNumeric(false);
      m_ClassFilters[i].inputFormat(insts);
      newInsts = Filter.useFilter(insts, m_ClassFilters[i]);
      m_Classifiers[i].buildClassifier(newInsts);
    }
  }

  /**
   * Returns the distribution for an instance.
   *
   * @exception Exception if the distribution can't be computed successfully
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
    
    double[] probs = new double[inst.numClasses()];
    Instance newInst;

    for (int i = 0; i < inst.numClasses(); i++) {
      m_ClassFilters[i].input(inst);
      newInst = m_ClassFilters[i].output();
      probs[i] = ((DistributionClassifier)m_Classifiers[i])
	.distributionForInstance(newInst)[1];
    }
    if (Utils.gr(Utils.sum(probs), 0)) {
      Utils.normalize(probs);
      return probs;
    } else {
      return m_ZeroR.distributionForInstance(inst);
    }
  }

  /**
   * Prints the classifiers.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();

    for (int i = 0; i < m_Classifiers.length; i++) {
      text.append(m_Classifiers[i].toString() + "\n");
    }

    return text.toString();
  }

  /**
   * Returns an enumeration describing the available options
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions()  {

    Vector vec = new Vector(1);
    Object c;
    
    vec.addElement(new Option("\tSets the base classifier.",
			      "W", 1, "-W <base classifier>"));
    
    if (m_Classifier != null) {
      try {
	vec.addElement(new Option("",
				  "", 0, "\nOptions specific to weak learner "
				  + m_Classifier.getClass().getName() + ":"));
	Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
	while (enum.hasMoreElements()) {
	  vec.addElement(enum.nextElement());
	}
      } catch (Exception e) {
      }
    }
    return vec.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of a learner as the basis for 
   * the multiclassclassifier (required).<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
  
    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }
    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    
    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }
    String [] options = new String [classifierOptions.length + 3];
    int current = 0;

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Set the classifier for boosting. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the classifier used as the classifier
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }


  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    DistributionClassifier scheme;

    try {
      scheme = new MultiClassClassifier();
      System.out.println(Evaluation.evaluateModel(scheme, argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
