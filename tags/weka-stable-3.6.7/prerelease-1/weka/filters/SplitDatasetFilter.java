/*
 *    SplitDatasetFilter.java
 *    Copyright (C) 1999 Eibe Frank
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

package weka.filters;

import weka.core.*;
import java.util.*;

/**
 * This filter takes a dataset and outputs a subset of it.
 *
 * Valid options are: <p>
 *
 * -R inst1,inst2-inst4,... <br>
 * Specifies list of instances to select. First
 * and last are valid indexes.<p>
 *
 * -V <br>
 * Specifies if inverse of selection is to be output.<p>
 *
 * -N number of folds <br>
 * Specifies number of folds dataset is split into. <p>
 *
 * -F fold <br>
 * Specifies which fold is selected. (Default: 1)<p>
 *
 * -S seed <br>
 * Specifies a random number seed for shuffling the dataset. ( > 0)<p>
 *
 * -C attribute index <br> Specifies the attribute to be used for
 * stratification. Attribute will be class in new dataset. Doesn't
 * stratify if attribute is not nominal.<p>
 *  
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0 
*/
public class SplitDatasetFilter extends Filter implements OptionHandler {

  /** Range of instances provided by user. */
  private Range m_Range = null;

  /** Indicates if inverse of selection is to be output. */
  private boolean m_Inverse = false;

  /** Number of folds to split dataset into */
  private int m_NumFolds = 0;

  /** Fold to output */
  private int m_Fold = 1;

  /** Random number seed. */
  private long m_Seed = 0;

  /** Attribute to be used for stratification */
  private int m_AttributeIndex = 0;

  /**
   * Gets an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
              "\tSpecifies list of instances to select. First and last\n"
	      +"\tare valid indexes.\n",
              "R", 1, "-R <inst1,inst2-inst4,...>"));

    newVector.addElement(new Option(
	      "\tSpecifies if inverse of selection is to be output.\n",
	      "V", 0, "V"));

    newVector.addElement(new Option(
              "\tSpecifies number of folds dataset is split into.\n",
              "N", 1, "-N <number of folds>"));

    newVector.addElement(new Option(
	      "\tSpecifies which fold is selected. (Default: 1)\n",
	      "F", 1, "-F <fold>"));

    newVector.addElement(new Option(
	      "\tSpecifies random number seed.\n",
	      "S", 1, "-S <seed>"));

    newVector.addElement(new Option(
	      "\tIndex of attribute used for stratification. Attribute\n"+
	      "\twill be class in new dataset. Doesn't stratify if\n"+
	      "\tattribute is not nominal.",
	      "C", 1, "-C <attribute index>"));

    return newVector.elements();
  }

  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -R inst1,inst2-inst4,... <br>
   * Specifies list of instances to select. First
   * and last are valid indexes.<p>
   *
   * -V <br>
   * Specifies if inverse of selection is to be output.<p>
   *
   * -N number of folds <br>
   * Specifies number of folds dataset is split into. <p>
   *
   * -F fold <br>
   * Specifies which fold is selected. (Default: 1)<p>
   *
   * -S seed <br>
   * Specifies a random number seed for shuffling the dataset. ( > 0)<p>
   *
   * -C attribute index <br> 
   * Specifies the attribute to be used for
   * stratification. Attribute will be class in new dataset. Doesn't
   * stratify if attribute is not nominal.<p>
   *  
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported */
  public void setOptions(String[] options) throws Exception {

    setInstancesIndices(Utils.getOption('R', options));
    setInvertSelection(Utils.getFlag('V', options));
    String numFolds = Utils.getOption('N', options);
    if (numFolds.length() != 0) {
      setNumFolds(Integer.parseInt(numFolds));
    } else {
      setNumFolds(0);
    }
    String fold = Utils.getOption('F', options);
    if (fold.length() != 0) {
      setFold(Integer.parseInt(fold));
    } else {
      setFold(1);
    }
    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(0);
    }
    String attIndex = Utils.getOption('C', options);
    if (attIndex.length() != 0) {
      if (attIndex.toLowerCase().equals("last")) {
	setAttributeIndex(0);
      } else if (attIndex.toLowerCase().equals("first")) {
	setAttributeIndex(1);
      } else {
	setAttributeIndex(Integer.parseInt(attIndex));
      }
    } else {
      setAttributeIndex(0);
    }
    if (m_InputFormat != null) {
      inputFormat(m_InputFormat);
    }
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [11];
    int current = 0;

    if (getInvertSelection()) {
      options[current++] = "-V";
    }
    if (!getInstancesIndices().equals("")) {
      options[current++] = "-R"; options[current++] = getInstancesIndices();
    }
    options[current++] = "-C"; options[current++] = "" + getAttributeIndex();
    options[current++] = "-N"; options[current++] = "" + getNumFolds();
    options[current++] = "-F"; options[current++] = "" + getFold();
    options[current++] = "-S"; options[current++] = "" + getSeed();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Gets ranges of instances selected.
   *
   * @return a string containing a comma-separated list of ranges
   */
  public String getInstancesIndices() {

    if (m_Range == null) {
      return "";
    } else {
      return m_Range.getRanges();
    }
  }

  /**
   * Sets the ranges of instances to be selected. If provided string
   * is null, ranges won't be used for selecting instances.
   *
   * @param rangeList a string representing the list of instances. 
   * eg: first-3,5,6-last
   * @exception Exception if an invalid range list is supplied 
   */
  public void setInstancesIndices(String rangeList) throws Exception {

    if ((rangeList == null) || (rangeList.length() == 0)) {
      m_Range = null;
    } else {
      m_Range = new Range();
      m_Range.setRanges(rangeList);
    }
  }

  /**
   * Gets if selection is to be inverted.
   *
   * @return true if the selection is to be inverted
   */
  public boolean getInvertSelection() {

    return m_Inverse;
  }

  /**
   * Sets if selection is to be inverted.
   *
   * @param inverse true if inversion is to be performed
   */
  public void setInvertSelection(boolean inverse) {
    
    m_Inverse = inverse;
  }

  /**
   * Gets the number of folds in which dataset is to be split into.
   * 
   * @return the number of folds the dataset is to be split into.
   */
  public double getNumFolds() {

    return m_NumFolds;
  }

  /**
   * Sets the number of folds the dataset is split into. If the number
   * of folds is zero, it won't split it into folds. 
   *
   * @param numFolds number of folds dataset is to be split into
   * @exception Exception if number of folds is negative
   */
  public void setNumFolds(int numFolds) throws Exception {

    if (numFolds < 0) {
      throw new Exception("Number of folds has to be positive or zero.");
    }
    m_NumFolds = numFolds;
  }

  /**
   * Gets the fold which is selected.
   *
   * @return the fold which is selected
   */
  public int getFold() {

    return m_Fold;
  }

  /**
   * Selects a fold.
   *
   * @param fold the fold to be selected.
   * @exception Exception if fold's index is smaller than 1
   */
  public void setFold(int fold) throws Exception {

    if (fold < 1) {
      throw new Exception("Fold's index has to be greater than 0.");
    }
    m_Fold = fold;
  }

  /**
   * Gets the random number seed used for shuffling the dataset.
   *
   * @return the random number seed
   */
  public long getSeed() {

    return m_Seed;
  }

  /**
   * Sets the random number seed for shuffling the dataset. If seed
   * is negative, shuffling won't be performed.
   *
   * @param seed the random number seed
   * @exception Exception if the seed is smaller than 0
   */
  public void setSeed(long seed) throws Exception {
    
    m_Seed = seed;
  }

  /**
   * Gets index of attribute being used for stratification.
   *
   * @return index of attribute being used for stratification
   */
  public int getAttributeIndex() {

    return m_AttributeIndex;
  }

  /**
   * Sets the attribute to be used for stratification. In the new
   * dataset, this attribute will be the class.
   *
   * @param att the index of the attribute to be used for stratification
   */
  public void setAttributeIndex(int att) {

    m_AttributeIndex = att;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input instance
   * structure (any instances contained in the object are ignored - only the
   * structure is required).
   * @return true because outputFormat can be collected immediately
   * @exception Exception if the input format can't be set successfully
   */
  
  public boolean inputFormat(Instances instanceInfo) throws Exception {

    // Check if there are any conflicting options.
    if (((m_Range != null) && (m_NumFolds > 0)) ||
	((m_Range != null) && (m_AttributeIndex > 0)) ||
	((m_Range != null) && (m_Seed > 0))) {
      throw new Exception("Conflicting options.");
    }
    if ((m_NumFolds > 0) && (m_NumFolds < m_Fold)) {
      throw new Exception("Fold has to be smaller or equal to "+
			  "number of folds.");
    }
    m_InputFormat = new Instances(instanceInfo, 0);
    if (m_AttributeIndex > 0) {
      if (m_AttributeIndex > m_InputFormat.numAttributes()) {
	throw new Exception("Invalid attribute index.");
      }
      m_InputFormat.setClassIndex(m_AttributeIndex - 1);
    }
    setOutputFormat(m_InputFormat);
    b_NewBatch = true;
    return true;
  }

  /**
   * Signify that this batch of input to the filter is
   * finished. Output() may now be called to retrieve the filtered
   * instances.
   *
   * @return true if there are instances pending output
   * @exception Exception if no input structure has been defined 
   */
  public boolean batchFinished() throws Exception {

    if (m_InputFormat == null) {
      throw new Exception("No input instance format defined");
    }
    if (m_Seed > 0) {
      // User has provided a random number seed.
      m_InputFormat.randomize(new Random(m_Seed));
    }
    if (m_AttributeIndex > 0) {
      // User has provided an attribute for stratification.
      m_InputFormat.stratify(m_NumFolds);
    }
    // Push instances for output into output queue
    if (m_Range != null) {
      // User has provided a range
      m_Range.setInvert(m_Inverse);
      m_Range.setUpper(m_InputFormat.numInstances());
      for (int i = 0; i < m_InputFormat.numInstances(); i++) {
	if (m_Range.isInRange(i)) {
	  push(m_InputFormat.instance(i));
	}
      }
    } else if (m_NumFolds > 0) {
      // User has provided a number of folds.
      Instances instances;
      if (!m_Inverse) {
	instances = m_InputFormat.testCV(m_NumFolds, m_Fold - 1);
      } else {
	instances = m_InputFormat.trainCV(m_NumFolds, m_Fold - 1);
      }
      for (int i = 0; i < instances.numInstances(); i++) {
	push(instances.instance(i));
      }
    }
    b_NewBatch = true;
    return (numPendingOutput() != 0);
  }

  /**
   * Main method for testing this class.
   * @param argv should contain arguments to the filter: use -h for help
   */

  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new SplitDatasetFilter(),argv);
      } else {
	Filter.filterFile(new SplitDatasetFilter(),argv);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex.getMessage());
    }
  }
}
