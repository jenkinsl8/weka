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
 *    RELEASE INFORMATION (December 27, 2004)
 *    
 *    FCBF algorithm:
 *      Template obtained from Weka
 *      Developed for Weka by Zheng Alan Zhao   
 *      December 27, 2004
 *
 *    FCBF algorithm is a feature selection method based on Symmetrical Uncertainty Measurement for 
 *    relevance redundancy analysis. The details of FCBF algorithm are in L. Yu and H. Liu.
 *    Feature selection for high-dimensional data: a fast correlation-based filter 
 *    solution. In Proceedings of the twentieth International Conference on Machine Learning, 
 *    pages 856--863, 2003.
 *    
 *    
 *    
 *    CONTACT INFORMATION
 *    
 *    For algorithm implementation:
 *    Zheng Zhao: zhaozheng at asu.edu
 *      
 *    For the algorithm:
 *    Lei Yu: leiyu at asu.edu
 *    Huan Liu: hliu at asu.edu
 *     
 *    Data Mining and Machine Learning Lab
 *    Computer Science and Engineering Department
 *    Fulton School of Engineering
 *    Arizona State University
 *    Tempe, AZ 85287
 *
 *    SymmetricalUncertAttributeSetEval.java
 *
 *    Copyright (C) 2004 Data Mining and Machine Learning Lab, 
 *                       Computer Science and Engineering Department, 
 *		    	 Fulton School of Engineering, 
 *                       Arizona State University
 *
 */

package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.filters.supervised.attribute.Discretize;
import  weka.filters.Filter;

/**
 * Class for Evaluating attribute sets by measuring symmetrical
 * uncertainty with respect to the class.
 *
 * Valid options are:<p>
 *
 * -M <br>
 * Treat missing values as a seperate value. <br>
 *
 * @author Zheng Zhao: zhaozheng at asu.edu
 * @version $Revision: 1.1 $
 */
public class SymmetricalUncertAttributeSetEval
  extends AttributeSetEvaluator
  implements OptionHandler
{

  /** The training instances */
  private Instances m_trainInstances;

  /** The class index */
  private int m_classIndex;

  /** The number of attributes */
  private int m_numAttribs;

  /** The number of instances */
  private int m_numInstances;

  /** The number of classes */
  private int m_numClasses;

  /** Treat missing values as a seperate value */
  private boolean m_missing_merge;

  /**
   * Returns a string describing this attribute evaluator
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "GainRatioAttributeSetEval :\n\nEvaluates the worth of a set attributes "
      +"by measuring the symmetrical uncertainty with respect to another set of attributes. "
      +"\n\n SymmU(AttributeSet2, AttributeSet1) = 2 * (H(AttributeSet2) - H(AttributeSet1 | AttributeSet2)) "
      +"/ H(AttributeSet2) + H(AttributeSet1).\n";
  }

  /**
   * Constructor
   */
  public SymmetricalUncertAttributeSetEval () {
    resetOptions();
  }


  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(1);
    newVector.addElement(new Option("\ttreat missing values as a seperate "
                                    + "value.", "M", 0, "-M"));
    return  newVector.elements();
  }


  /**
   * Parses a given list of options.
   *
   * -M <br>
   * Treat missing values as a seperate value. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   **/
  public void setOptions (String[] options)
    throws Exception {
    resetOptions();
    setMissingMerge(!(Utils.getFlag('M', options)));
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String missingMergeTipText() {
    return "Distribute counts for missing values. Counts are distributed "
      +"across other values in proportion to their frequency. Otherwise, "
      +"missing is treated as a separate value.";
  }

  /**
   * distribute the counts for missing values across observed values
   *
   * @param b true=distribute missing values.
   */
  public void setMissingMerge (boolean b) {
    m_missing_merge = b;
  }


  /**
   * get whether missing values are being distributed or not
   *
   * @return true if missing values are being distributed.
   */
  public boolean getMissingMerge () {
    return  m_missing_merge;
  }


  /**
   * Gets the current settings of WrapperSubsetEval.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[1];
    int current = 0;

    if (!getMissingMerge()) {
      options[current++] = "-M";
    }

    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }


  /**
   * Initializes a symmetrical uncertainty attribute evaluator.
   * Discretizes all attributes that are numeric.
   *
   * @param data set of instances serving as training data
   * @exception Exception if the evaluator has not been
   * generated successfully
   */
  public void buildEvaluator (Instances data)
    throws Exception {
    if (data.checkForStringAttributes()) {
      throw  new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }

    m_trainInstances = data;
    m_classIndex = m_trainInstances.classIndex();
    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();
    if (m_trainInstances.attribute(m_classIndex).isNumeric()) {
      throw  new Exception("Class must be nominal!");
    }

    Discretize disTransform = new Discretize();
    disTransform.setUseBetterEncoding(true);
    disTransform.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, disTransform);
    m_numClasses = m_trainInstances.attribute(m_classIndex).numValues();
  }


  /**
   * set options to default values
   */
  protected void resetOptions () {
    m_trainInstances = null;
    m_missing_merge = true;
  }

  /**
   * evaluates an individual attribute by measuring the symmetrical
   * uncertainty between it and the class.
   *
   * @param attribute the index of the attribute to be evaluated
   * @exception Exception if the attribute could not be evaluated
   */

  public double evaluateAttribute (int attribute)
    throws Exception {
    int i, j, ii, jj;
    int nnj, nni, ni, nj;
    double sum = 0.0;
    ni = m_trainInstances.attribute(attribute).numValues() + 1;
    nj = m_numClasses + 1;
    double[] sumi, sumj;
    Instance inst;
    double temp = 0.0;
    sumi = new double[ni];
    sumj = new double[nj];
    double[][] counts = new double[ni][nj];
    sumi = new double[ni];
    sumj = new double[nj];

    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        sumj[j] = 0.0;
        counts[i][j] = 0.0;
      }
    }

    // Fill the contingency table
    for (i = 0; i < m_numInstances; i++) {
      inst = m_trainInstances.instance(i);

      if (inst.isMissing(attribute)) {
        ii = ni - 1;
      }
      else {
        ii = (int)inst.value(attribute);
      }

      if (inst.isMissing(m_classIndex)) {
        jj = nj - 1;
      }
      else {
        jj = (int)inst.value(m_classIndex);
      }

      counts[ii][jj]++;
    }

    // get the row totals
    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        //there are how many happen of a special feature value
        sumi[i] += counts[i][j];
        sum += counts[i][j];
      }
    }

    // get the column totals
    for (j = 0; j < nj; j++) {
      sumj[j] = 0.0;

      for (i = 0; i < ni; i++) {
        //a class value include how many instance.
        sumj[j] += counts[i][j];
      }
    }

    // distribute missing counts
    if (m_missing_merge &&
        (sumi[ni-1] < m_numInstances) &&
        (sumj[nj-1] < m_numInstances)) {
      double[] i_copy = new double[sumi.length];
      double[] j_copy = new double[sumj.length];
      double[][] counts_copy = new double[sumi.length][sumj.length];

      for (i = 0; i < ni; i++) {
        System.arraycopy(counts[i], 0, counts_copy[i], 0, sumj.length);
      }

      System.arraycopy(sumi, 0, i_copy, 0, sumi.length);
      System.arraycopy(sumj, 0, j_copy, 0, sumj.length);
      double total_missing = (sumi[ni - 1] + sumj[nj - 1]
                              - counts[ni - 1][nj - 1]);

      // do the missing i's
      if (sumi[ni - 1] > 0.0) { //sumi[ni - 1]: missing value contains how many values.
        for (j = 0; j < nj - 1; j++) {
          if (counts[ni - 1][j] > 0.0) {
            for (i = 0; i < ni - 1; i++) {
              temp = ((i_copy[i]/(sum - i_copy[ni - 1])) *
                      counts[ni - 1][j]);
              counts[i][j] += temp; //according to the probability of value i we distribute account of the missing degree of a class lable to it
              sumi[i] += temp;
            }

            counts[ni - 1][j] = 0.0;
          }
        }
      }

      sumi[ni - 1] = 0.0;

      // do the missing j's
      if (sumj[nj - 1] > 0.0) {
        for (i = 0; i < ni - 1; i++) {
          if (counts[i][nj - 1] > 0.0) {
            for (j = 0; j < nj - 1; j++) {
              temp = ((j_copy[j]/(sum - j_copy[nj - 1]))*counts[i][nj - 1]);
              counts[i][j] += temp;
              sumj[j] += temp;
            }

            counts[i][nj - 1] = 0.0;
          }
        }
      }

      sumj[nj - 1] = 0.0;

      // do the both missing
      if (counts[ni - 1][nj - 1] > 0.0 && total_missing != sum) {
        for (i = 0; i < ni - 1; i++) {
          for (j = 0; j < nj - 1; j++) {
            temp = (counts_copy[i][j]/(sum - total_missing)) *
              counts_copy[ni - 1][nj - 1];
            counts[i][j] += temp;
            sumi[i] += temp;
            sumj[j] += temp;
          }
        }

        counts[ni - 1][nj - 1] = 0.0;
      }
    }

    return  ContingencyTables.symmetricalUncertainty(counts);
  }

  /**
   * calculate symmetrical uncertainty between sets of attributes
   *
   * @param attributes: the indexes of the attributes
   * @param classAttributes: the indexes of the attributes whose combination will
   *                         be used as class label
   * @exception Exception if the attribute could not be evaluated
   */

  public double evaluateAttribute (int[] attributes, int[] classAttributes)
    throws Exception {

    int i, j;     //variable for looping.
    int p, q;     //variable for looping.
    int ii, jj;   //specifying the position in the contingency table.
    int nnj, nni; //counting base for attributes[].
    int ni, nj;   //the nubmer of rows and columns in the ContingencyTables.

    double sum = 0.0;
    boolean b_missing_attribute = false;
    boolean b_missing_classAtrribute = false;

    if(attributes.length==0)
    {
      throw new Exception("the parameter attributes[] is empty;SEQ:W-FS-Eval-SUAS-001");
    }
    if(classAttributes.length==0)
    {
      throw new Exception("the parameter classAttributes[] is empty;SEQ:W-FS-Eval-SUAS-002");
    }

    /*calculate the number of the rows in ContingencyTable*/
    ni = m_trainInstances.attribute(attributes[0]).numValues();
    if (ni == 0)
    {
      throw new Exception("an attribute is empty;SEQ:W-FS-Eval-SUAS-003;"+1);
    }

    for (i = 1;i<attributes.length;i++)
    {
      if (m_trainInstances.attribute(attributes[i]).numValues() == 0)
      {
        throw new Exception("an attribute is empty;SEQ:W-FS-Eval-SUAS-003;" +
                            (i+1));
      }
      ni = ni*m_trainInstances.attribute(attributes[i]).numValues();
    }
    ni = ni+1;

    /*calculate the number of the colums in the ContingencyTable*/
    nj = m_trainInstances.attribute(classAttributes[0]).numValues();
    if (nj == 0)
    {
      throw new Exception("the a classAttribute is empty;SEQ:W-FS-Eval-SUAS-004;"+1);
    }

    for (i = 1;i<classAttributes.length;i++)
    {
      if (m_trainInstances.attribute(classAttributes[i]).numValues() == 0)
      {
        throw new Exception("the a classAttribute is empty;SEQ:W-FS-Eval-SUAS-004;" +
                            (i+1));
      }
      nj = nj*m_trainInstances.attribute(classAttributes[i]).numValues();
    }
    nj = nj+1;

    double[] sumi, sumj;
    Instance inst;
    double temp = 0.0;
    sumi = new double[ni];
    sumj = new double[nj];
    double[][] counts = new double[ni][nj];
    sumi = new double[ni];
    sumj = new double[nj];

    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        sumj[j] = 0.0;
        counts[i][j] = 0.0;
      }
    }

    // Fill the contingency table
    for (i = 0; i < m_numInstances; i++) {
      inst = m_trainInstances.instance(i);

      b_missing_attribute = false;
      b_missing_classAtrribute = false;

      /*get row position in contingency table*/
      nni = 1;
      ii = 0;
      for (p=attributes.length-1; p>=0; p--)
      {
        if (inst.isMissing(attributes[p])) {
           b_missing_attribute = true;
        }
        ii = ((int)inst.value(attributes[p])*nni)+ii;
        if (p<attributes.length-1){
          nni = nni * (m_trainInstances.attribute(attributes[p]).numValues());
        }
        else {
          nni = m_trainInstances.attribute(attributes[p]).numValues();
        }
      }
      if (b_missing_attribute) {
        ii = ni-1;
      }

      /*get colum position in contingency table*/
      nnj = 1;
      jj = 0;
      for (p=classAttributes.length-1; p>=0; p--)
      {
        if (inst.isMissing(classAttributes[p])) {
           b_missing_classAtrribute = true;
        }
        jj = ((int)inst.value(classAttributes[p])*nnj)+jj;
        if (p<attributes.length-1){
          nnj = nnj * (m_trainInstances.attribute(classAttributes[p]).numValues());
        }
        else {
          nnj = m_trainInstances.attribute(classAttributes[p]).numValues();
        }
      }
      if (b_missing_classAtrribute) {
        jj = nj-1;
      }

      counts[ii][jj]++;
    }

    // get the row totals
    for (i = 0; i < ni; i++) {
      sumi[i] = 0.0;

      for (j = 0; j < nj; j++) {
        //there are how many happen of a special feature value
        sumi[i] += counts[i][j];
        sum += counts[i][j];
      }
    }

    // get the column totals
    for (j = 0; j < nj; j++) {
      sumj[j] = 0.0;

      for (i = 0; i < ni; i++) {
        //a class value include how many instance.
        sumj[j] += counts[i][j];
      }
    }

    // distribute missing counts
    if (m_missing_merge &&
        (sumi[ni-1] < m_numInstances) &&
        (sumj[nj-1] < m_numInstances)) {
      double[] i_copy = new double[sumi.length];
      double[] j_copy = new double[sumj.length];
      double[][] counts_copy = new double[sumi.length][sumj.length];

      for (i = 0; i < ni; i++) {
        System.arraycopy(counts[i], 0, counts_copy[i], 0, sumj.length);
      }

      System.arraycopy(sumi, 0, i_copy, 0, sumi.length);
      System.arraycopy(sumj, 0, j_copy, 0, sumj.length);
      double total_missing = (sumi[ni - 1] + sumj[nj - 1]
                              - counts[ni - 1][nj - 1]);

      // do the missing i's
      if (sumi[ni - 1] > 0.0) { //sumi[ni - 1]: missing value contains how many values.
        for (j = 0; j < nj - 1; j++) {
          if (counts[ni - 1][j] > 0.0) {
            for (i = 0; i < ni - 1; i++) {
              temp = ((i_copy[i]/(sum - i_copy[ni - 1])) *
                      counts[ni - 1][j]);
              counts[i][j] += temp; //according to the probability of value i we distribute account of the missing degree of a class lable to it
              sumi[i] += temp;
            }

            counts[ni - 1][j] = 0.0;
          }
        }
      }

      sumi[ni - 1] = 0.0;

      // do the missing j's
      if (sumj[nj - 1] > 0.0) {
        for (i = 0; i < ni - 1; i++) {
          if (counts[i][nj - 1] > 0.0) {
            for (j = 0; j < nj - 1; j++) {
              temp = ((j_copy[j]/(sum - j_copy[nj - 1]))*counts[i][nj - 1]);
              counts[i][j] += temp;
              sumj[j] += temp;
            }

            counts[i][nj - 1] = 0.0;
          }
        }
      }

      sumj[nj - 1] = 0.0;

      // do the both missing
      if (counts[ni - 1][nj - 1] > 0.0 && total_missing != sum) {
        for (i = 0; i < ni - 1; i++) {
          for (j = 0; j < nj - 1; j++) {
            temp = (counts_copy[i][j]/(sum - total_missing)) *
              counts_copy[ni - 1][nj - 1];
            counts[i][j] += temp;
            sumi[i] += temp;
            sumj[j] += temp;
          }
        }

        counts[ni - 1][nj - 1] = 0.0;
      }
    }

    return  ContingencyTables.symmetricalUncertainty(counts);
  }


  /**
   * Return a description of the evaluator
   * @return description as a string
   */
  public String toString () {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tSymmetrical Uncertainty evaluator has not been built");
    }
    else {
      text.append("\tSymmetrical Uncertainty Ranking Filter");
      if (!m_missing_merge) {
        text.append("\n\tMissing values treated as seperate");
      }
    }

    text.append("\n");
    return  text.toString();
  }


  // ============
  // Test method.
  // ============
  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file
   */
  public static void main (String[] argv) {
    try {
      System.out.println(AttributeSelection.
                         SelectAttributes(new SymmetricalUncertAttributeSetEval()
                                          , argv));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }

}

