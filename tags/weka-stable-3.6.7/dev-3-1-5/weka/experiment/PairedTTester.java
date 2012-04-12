/*
 *    PairedTTester.java
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

package weka.experiment;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Range;
import weka.core.Attribute;
import weka.core.Utils;
import weka.core.FastVector;
import weka.core.Statistics;
import weka.core.OptionHandler;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Option;

/**
 * Calculates T-Test statistics on data stored in a set of instances.<p>
 *
 * Valid options from the command-line are:<p>
 *
 * -D num <br>
 * The column number containing the dataset name.
 * (default last) <p>
 *
 * -R num <br>
 * The column number containing the run number.
 * (default last) <p>
 *
 * -S num <br>
 * The significance level for T-Tests.
 * (default 0.05) <p>
 *
 * -R num,num2... <br>
 * The column numbers that uniquely specify one result generator (eg:
 * scheme name plus options).
 * (default last) <p>
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $
 */
public class PairedTTester implements OptionHandler {

  /** The set of instances we will analyse */
  protected Instances m_Instances;

  /** The index of the column containing the run number */
  protected int m_RunColumn = 0;

  /** The option setting for the run number column (-1 means last) */
  protected int m_RunColumnSet = -1;

  /** The index of the column containing the dataset names */
  protected int m_DatasetColumn = 0;

  /** The option setting for the dataset name column (-1 means last) */
  protected int m_DatasetColumnSet = -1;

  /** The significance level for comparisons */
  protected double m_SignificanceLevel = 0.05;

  /**
   * The range of columns that specify a unique result set
   * (eg: scheme plus configuration)
   */
  protected Range m_ResultsetKeyColumnsRange = new Range();

  /** An array containing the indexes of just the selected columns */ 
  protected int [] m_ResultsetKeyColumns;
  
  /** Stores the number of datasets we have results for */
  protected int m_NumDatasets;

  /** Stores the number of unique result sets */
  protected int m_NumResultsets;

  /** Stores a vector for each resultset holding all instances in each set */
  protected FastVector m_Resultsets = new FastVector();

  /** Indicates whether the instances have been partitioned */
  protected boolean m_ResultsetsValid;
  
  /* Utility class to store the instances in a resultset */
  private class Resultset {

    Instance m_Template;
    FastVector [] m_Datasets;

    public Resultset(Instance template) {

      m_Template = template;
      m_Datasets = new FastVector [m_NumDatasets];
      add(template);
    }
    
    /**
     * Returns true if the two instances match on those attributes that have
     * been designated key columns (eg: scheme name and scheme options)
     *
     * @param first the first instance
     * @param second the second instance
     * @return true if first and second match on the currently set key columns
     */
    protected boolean matchesTemplate(Instance first) {
      
      for (int i = 0; i < m_ResultsetKeyColumns.length; i++) {
	if (first.value(m_ResultsetKeyColumns[i]) !=
	    m_Template.value(m_ResultsetKeyColumns[i])) {
	  return false;
	}
      }
      return true;
    }

    /**
     * Returns a string descriptive of the resultset key column values
     * for this resultset
     *
     * @return a value of type 'String'
     */
    protected String templateString() {

      String result = "";
      for (int i = 0; i < m_ResultsetKeyColumns.length; i++) {
	result += m_Template.toString(m_ResultsetKeyColumns[i]) + ' ';
      }
      if (result.startsWith("weka.classifiers.")) {
	result = result.substring("weka.classifiers.".length());
      }
      return result.trim();
    }
    
    /**
     * Returns a vector containing all instances belonging to one dataset.
     *
     * @param index a value of type 'int'
     * @return a value of type 'FastVector'
     */
    public FastVector dataset(int index) {

      return m_Datasets[index];
    }
    
    /**
     * Adds an instance to this resultset
     *
     * @param newInst a value of type 'Instance'
     */
    public void add(Instance newInst) {

      int newInstDataset = (int) newInst.value(m_DatasetColumn);
      FastVector dataset = m_Datasets[newInstDataset];
      if (dataset == null) {
	dataset = new FastVector();
	m_Datasets[newInstDataset] = dataset;
      }
      dataset.addElement(newInst);
    }

    /**
     * Sorts the instances in each dataset by the run number.
     *
     * @param runColumn a value of type 'int'
     */
    public void sort(int runColumn) {

      for (int i = 0; i < m_Datasets.length; i++) {
	FastVector dataset = m_Datasets[i];
	if (dataset != null) {
	  double [] runNums = new double [dataset.size()];
	  for (int j = 0; j < runNums.length; j++) {
	    runNums[j] = ((Instance) dataset.elementAt(j)).value(runColumn);
	  }
	  int [] index = Utils.sort(runNums);
	  FastVector newDataset = new FastVector(runNums.length);
	  for (int j = 0; j < index.length; j++) {
	    newDataset.addElement(dataset.elementAt(index[j]));
	  }
	  m_Datasets[i] = newDataset;
	}
      }
    }
  } // Resultset


  
  /**
   * Separates the instances into resultsets and by dataset/run.
   *
   * @exception Exception if the TTest parameters have not been set.
   */
  protected void prepareData() throws Exception {

    if (m_Instances == null) {
      throw new Exception("No instances have been set");
    }
    if (m_RunColumnSet == -1) {
      m_RunColumn = m_Instances.numAttributes() - 1;
    } else {
      m_RunColumn = m_RunColumnSet;
    }
    if (m_DatasetColumnSet == -1) {
      m_DatasetColumn = m_Instances.numAttributes() - 1;
    } else {
      m_DatasetColumn = m_DatasetColumnSet;
    }

    if (m_Instances.attribute(m_DatasetColumn).type() != Attribute.NOMINAL) {
      throw new Exception("Dataset column " + (m_DatasetColumn + 1)
			  + " ("
			  + m_Instances.attribute(m_DatasetColumn).name()
			  + ") is not nominal");
    }
    if (m_ResultsetKeyColumnsRange == null) {
      throw new Exception("No result specifier columns have been set");
    }

    m_ResultsetKeyColumnsRange.setUpper(m_Instances.numAttributes() - 1);
    m_ResultsetKeyColumns = m_ResultsetKeyColumnsRange.getSelection();
    m_NumDatasets = m_Instances.attribute(m_DatasetColumn).numValues();
    
    //  Split the data up into result sets
    m_Resultsets.removeAllElements();  
    for (int i = 0; i < m_Instances.numInstances(); i++) {
      Instance current = m_Instances.instance(i);
      if (current.isMissing(m_DatasetColumn)) {
	throw new Exception("Instance has missing value in dataset "
			    + "column!\n" + current);
      } else if (current.isMissing(m_RunColumn)) {
	throw new Exception("Instance has missing value in run "
			    + "column!\n" + current);
      } else {
	for (int j = 0; j < m_ResultsetKeyColumns.length; j++) {
	  if (current.isMissing(m_ResultsetKeyColumns[j])) {
	    throw new Exception("Instance has missing value in resultset key "
				+ "column " + (m_ResultsetKeyColumns[j] + 1)
				+ "!\n" + current);
	  }
	}
      }
      boolean found = false;
      for (int j = 0; j < m_Resultsets.size(); j++) {
	Resultset resultset = (Resultset) m_Resultsets.elementAt(j);
	if (resultset.matchesTemplate(current)) {
	  resultset.add(current);
	  found = true;
	  break;
	}
      }
      if (!found) {
	Resultset resultset = new Resultset(current);
	m_Resultsets.addElement(resultset);
      }
    }
    // Tell each resultset to sort on the run column
    for (int j = 0; j < m_Resultsets.size(); j++) {
      Resultset resultset = (Resultset) m_Resultsets.elementAt(j);
      resultset.sort(m_RunColumn);
    }
    m_ResultsetsValid = true;
  }

  /**
   * Gets the number of datasets in the resultsets
   *
   * @return the number of datasets in the resultsets
   */
  public int getNumDatasets() {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	return 0;
      }
    }
    return m_NumDatasets;
  }

  /**
   * Gets the number of resultsets in the data.
   *
   * @return the number of resultsets in the data
   */
  public int getNumResultsets() {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	return 0;
      }
    }
    return m_Resultsets.size();
  }

  /**
   * Gets a string descriptive of the specified resultset.
   *
   * @param index the index of the resultset
   * @return a descriptive string for the resultset
   */
  public String getResultsetName(int index) {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	return null;
      }
    }
    return ((Resultset) m_Resultsets.elementAt(index)).templateString();
  }
  
  /**
   * Computes a paired t-test comparison for a specified dataset between
   * two resultsets.
   *
   * @param datasetIndex the index of the dataset
   * @param resultset1Index the index of the first resultset
   * @param resultset2Index the index of the second resultset
   * @param comparisonColumn the column containing values to compare
   * @return the results of the paired comparison
   * @exception Exception if an error occurs
   */
  public PairedStats calculateStatistics(int datasetIndex,
				     int resultset1Index,
				     int resultset2Index,
				     int comparisonColumn) throws Exception {

    if (m_Instances.attribute(comparisonColumn).type()
	!= Attribute.NUMERIC) {
      throw new Exception("Comparison column " + (comparisonColumn + 1)
			  + " ("
			  + m_Instances.attribute(comparisonColumn).name()
			  + ") is not numeric");
    }
    if (!m_ResultsetsValid) {
      prepareData();
    }

    Resultset resultset1 = (Resultset) m_Resultsets.elementAt(resultset1Index);
    Resultset resultset2 = (Resultset) m_Resultsets.elementAt(resultset2Index);
    FastVector dataset1 = resultset1.dataset(datasetIndex);
    FastVector dataset2 = resultset2.dataset(datasetIndex);
    String datasetName = m_Instances.attribute(m_DatasetColumn)
      .value(datasetIndex);
    if (dataset1 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset1.templateString());
    } else if (dataset2 == null) {
      throw new Exception("No results for dataset=" + datasetName
			 + " for resultset=" + resultset2.templateString());
    } else if (dataset1.size() != dataset2.size()) {
      throw new Exception("Results for dataset=" + datasetName
			  + " differ in size for resultset="
			  + resultset1.templateString()
			  + " and resultset="
			  + resultset2.templateString()
			  );
    }

    
    PairedStats pairedStats = new PairedStats(m_SignificanceLevel);
    for (int k = 0; k < dataset1.size(); k ++) {
      Instance current1 = (Instance) dataset1.elementAt(k);
      Instance current2 = (Instance) dataset2.elementAt(k);
      if (current1.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current1);
      }
      if (current2.isMissing(comparisonColumn)) {
	throw new Exception("Instance has missing value in comparison "
			    + "column!\n" + current2);
      }
      if (current1.value(m_RunColumn) != current2.value(m_RunColumn)) {
	System.err.println("Run numbers do not match!\n"
			    + current1 + current2);
      }
      double value1 = current1.value(comparisonColumn);
      double value2 = current2.value(comparisonColumn);
      pairedStats.add(value1, value2);
    }
    pairedStats.calculateDerived();
    return pairedStats;

  }
  
  /**
   * Creates a key that maps resultset numbers to their descriptions.
   *
   * @return a value of type 'String'
   */
  public String resultsetKey() {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	return ex.getMessage();
      }
    }
    String result = "";
    for (int j = 0; j < getNumResultsets(); j++) {
      result += "(" + (j + 1) + ") " + getResultsetName(j) + '\n';
    }
    return result + '\n';
  }
  
  /**
   * Creates a "header" string describing the current resultsets.
   *
   * @param comparisonColumn a value of type 'int'
   * @return a value of type 'String'
   */
  public String header(int comparisonColumn) {

    if (!m_ResultsetsValid) {
      try {
	prepareData();
      } catch (Exception ex) {
	return ex.getMessage();
      }
    }
    return "Analysing:  "
      + m_Instances.attribute(comparisonColumn).name() + '\n'
      + "Datasets:   " + getNumDatasets() + '\n'
      + "Resultsets: " + getNumResultsets() + '\n'
      + "Confidence: " + getSignificanceLevel() + " (two tailed)\n"
      + "Date:       " + (new SimpleDateFormat()).format(new Date()) + "\n\n";
  }

  /**
   * Carries out a comparison between all resultsets, counting the number
   * of datsets where one resultset outperforms the other.
   *
   * @param comparisonColumn the index of the comparison column
   * @return a 2d array where element [i][j] is the number of times resultset
   * j performed significantly better than resultset i.
   * @exception Exception if an error occurs
   */
  public int [][] multiResultsetWins(int comparisonColumn)
    throws Exception {

    int numResultsets = getNumResultsets();
    int [][] win = new int [numResultsets][numResultsets];
    for (int i = 0; i < numResultsets; i++) {
      for (int j = i + 1; j < numResultsets; j++) {
	System.err.print("Comparing (" + (i + 1) + ") with ("
			 + (j + 1) + ")\r");
	System.err.flush();
	for (int k = 0; k < getNumDatasets(); k++) {
	  try {
	    PairedStats pairedStats = calculateStatistics(k, i, j,
							  comparisonColumn);
	    if (pairedStats.differencesSignificance < 0) {
	      win[i][j]++;
	    } else if (pairedStats.differencesSignificance > 0) {
	      win[j][i]++;
	    }
	  } catch (Exception ex) {
	    System.err.println(ex.getMessage());
	  }
	}
      }
    }
    return win;
  }
  
  /**
   * Carries out a comparison between all resultsets, counting the number
   * of datsets where one resultset outperforms the other. The results
   * are summarized in a table.
   *
   * @param comparisonColumn the index of the comparison column
   * @return the results in a string
   * @exception Exception if an error occurs
   */
  public String multiResultsetSummary(int comparisonColumn)
    throws Exception {
    
    int [][] win = multiResultsetWins(comparisonColumn);
    int numResultsets = getNumResultsets();
    int resultsetLength = 1 + Math.max((int)(Math.log(numResultsets)
					     / Math.log(10)),
				       (int)(Math.log(getNumDatasets()) / 
					     Math.log(10)));
    String result = "";
    String titles = "";
    for (int i = 0; i < numResultsets; i++) {
      titles += ' ' + Utils.padLeft("" + (char)((int)'a' + i % 26),
				    resultsetLength);
    }
    result += titles + "  (No. of datasets where [col] >> [row])\n";
    for (int i = 0; i < numResultsets; i++) {
      for (int j = 0; j < numResultsets; j++) {
	if (j == i) {
	  result += ' ' + Utils.padLeft("-", resultsetLength);
	} else {
	  result += ' ' + Utils.padLeft("" + win[i][j], resultsetLength);
	}
      }
      result += " | " + (char)((int)'a' + i % 26)
	+ " = " + getResultsetName(i) + '\n';
    }
    return result;
  }

  public String multiResultsetRanking(int comparisonColumn)
    throws Exception {

    int [][] win = multiResultsetWins(comparisonColumn);
    int numResultsets = getNumResultsets();
    int [] wins = new int [numResultsets];
    int [] losses = new int [numResultsets];
    int [] diff = new int [numResultsets];
    for (int i = 0; i < win.length; i++) {
      for (int j = 0; j < win[i].length; j++) {
	wins[j] += win[i][j];
	diff[j] += win[i][j];
	losses[i] += win[i][j];
	diff[i] -= win[i][j];
      }
    }
    int biggest = Math.max(wins[Utils.maxIndex(wins)],
			   losses[Utils.maxIndex(losses)]);
    int width = Math.max(2 + (int)(Math.log(biggest) / Math.log(10)),
			 ">-<".length());
    String result = Utils.padLeft(">-<", width) + ' '
      + Utils.padLeft(">", width) + ' '
      + Utils.padLeft("<", width) + " Resultset\n";
    int [] ranking = Utils.sort(diff);
    for (int i = numResultsets - 1; i >= 0; i--) {
      int curr = ranking[i];
      result += Utils.padLeft("" + diff[curr], width) + ' '
	+ Utils.padLeft("" + wins[curr], width) + ' '
	+ Utils.padLeft("" + losses[curr], width) + ' '
	+ getResultsetName(curr) + '\n';
    }
    return result;
  }

  /**
   * Creates a comparison table where a base resultset is compared to the
   * other resultsets. Results are presented for every dataset.
   *
   * @param baseResultset the index of the base resultset
   * @param comparisonColumn the index of the column to compare over
   * @return the comparison table string
   * @exception Exception if an error occurs
   */
  public String multiResultsetFull(int baseResultset,
				   int comparisonColumn) throws Exception {

    StringBuffer result = new StringBuffer(1000);
    int datasetLength = 25;
    int resultsetLength = 9;

    // Set up the titles
    StringBuffer titles = new StringBuffer(Utils.padRight("Dataset",
							  datasetLength));
    titles.append(' ');
    StringBuffer label = new StringBuffer(
			     Utils.padLeft("("
					   + (baseResultset + 1)
					   + ") "
					   + getResultsetName(baseResultset),
					   resultsetLength + 3));
    titles.append(label);
    StringBuffer separator = new StringBuffer(Utils.padRight("",
							     datasetLength));
    while (separator.length() < titles.length()) {
      separator.append('-');
    }
    separator.append("---");
    titles.append(" | ");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (j != baseResultset) {
	label = new StringBuffer(Utils.padLeft("(" + (j + 1) + ") "
			      + getResultsetName(j), resultsetLength));
	titles.append(label).append(' ');
	for (int i = 0; i < label.length(); i++) {
	  separator.append('-');
	}
	separator.append('-');
      }
    }
    result.append(titles).append('\n').append(separator).append('\n');
    
    // Iterate over datasets
    int [] win = new int [getNumResultsets()];
    int [] loss = new int [getNumResultsets()];
    int [] tie = new int [getNumResultsets()];
    StringBuffer skipped = new StringBuffer("");
    for (int i = 0; i < getNumDatasets(); i++) {
      // Print the name of the dataset
      String datasetName = m_Instances.attribute(m_DatasetColumn).value(i);
      try {
	PairedStats pairedStats = calculateStatistics(i, baseResultset,
						      baseResultset,
						      comparisonColumn);
	datasetName = Utils.padRight(datasetName, datasetLength);
	result.append(datasetName);
	result.append(Utils.padLeft('('
				+ Utils.doubleToString(pairedStats.count, 0)
				+ ')', 5)).append(' ');
	result.append(Utils.doubleToString(pairedStats.xStats.mean,
				       resultsetLength - 2, 2)).append(" | ");
	// Iterate over the resultsets
	for (int j = 0; j < getNumResultsets(); j++) {
	  if (j != baseResultset) {
	    try {
	      pairedStats = calculateStatistics(i, baseResultset, j,
						comparisonColumn);
	      char sigChar = ' ';
	      if (pairedStats.differencesSignificance < 0) {
		sigChar = 'v';
		win[j]++;
	      } else if (pairedStats.differencesSignificance > 0) {
		sigChar = '*';
		loss[j]++;
	      } else {
		tie[j]++;
	      }
	      result.append(Utils.doubleToString(pairedStats.yStats.mean,
						 resultsetLength - 2,
						 2)).append(' ')
		.append(sigChar).append(' ');
	    } catch (Exception ex) {
	      result.append(Utils.padLeft("", resultsetLength + 1));
	    }
	  }
	}
	result.append('\n');
      } catch (Exception ex) {
	skipped.append(datasetName).append(' ');
      }
    }
    result.append(separator).append('\n');
    result.append(Utils.padLeft("(v/ /*)", datasetLength + 4 +
				resultsetLength)).append(" | ");
    for (int j = 0; j < getNumResultsets(); j++) {
      if (j != baseResultset) {
	result.append(Utils.padLeft("(" + win[j] + '/' + tie[j]
				    + '/' + loss[j] + ')',
				    resultsetLength)).append(' ');
      }
    }
    result.append('\n');
    if (!skipped.equals("")) {
      result.append("Skipped: ").append(skipped).append('\n');
    }
    return result.toString();
  }

  /**
   * Lists options understood by this object.
   *
   * @return an enumeration of Options.
   */
  public Enumeration listOptions() {
    
    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
	      "\tSet the index of the column containing the dataset name",
              "D", 1, "-D <index>"));
    newVector.addElement(new Option(
	      "\tSet the index of the column containing the run number",
              "R", 1, "-R <index>"));
    newVector.addElement(new Option(
              "\tSpecify list of columns to that specify a unique\n"
	      + "\t'result generator' (eg: classifier name and options).\n"
	      + "\tFirst and last are valid indexes. (default none)",
              "G", 1, "-G <index1,index2-index4,...>"));
    newVector.addElement(new Option(
	      "\tSet the significance level for comparisons (default 0.05)",
              "S", 1, "-S <significance level>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -D num <br>
   * The column number containing the dataset name.
   * (default last) <p>
   *
   * -R num <br>
   * The column number containing the run number.
   * (default last) <p>
   *
   * -S num <br>
   * The significance level for T-Tests.
   * (default 0.05) <p>
   *
   * -R num,num2... <br>
   * The column numbers that uniquely specify one result generator (eg:
   * scheme name plus options).
   * (default last) <p>
   *
   * @param options an array containing options to set.
   * @exception Exception if invalid options are given
   */
  public void setOptions(String[] options) throws Exception {

    String indexStr = Utils.getOption('D', options);
    if (indexStr.length() != 0) {
      if (indexStr.equals("first")) {
	setDatasetColumn(0);
      } else if (indexStr.equals("last")) {
	setDatasetColumn(-1);
      } else {
	setDatasetColumn(Integer.parseInt(indexStr) - 1);
      }    
    } else {
      setDatasetColumn(-1);
    }

    indexStr = Utils.getOption('R', options);
    if (indexStr.length() != 0) {
      if (indexStr.equals("first")) {
	setRunColumn(0);
      } else if (indexStr.equals("last")) {
	setRunColumn(-1);
      } else {
	setRunColumn(Integer.parseInt(indexStr) - 1);
      }    
    } else {
      setRunColumn(-1);
    }

    String sigStr = Utils.getOption('S', options);
    if (sigStr.length() != 0) {
      setSignificanceLevel((new Double(sigStr)).doubleValue());
    } else {
      setSignificanceLevel(0.05);
    }
    
    String resultsetList = Utils.getOption('G', options);
    Range generatorRange = new Range();
    if (resultsetList.length() != 0) {
      generatorRange.setRanges(resultsetList);
    }
    setResultsetKeyColumns(generatorRange);
  }
  
  /**
   * Gets current settings of the PairedTTester.
   *
   * @return an array of strings containing current options.
   */
  public String[] getOptions() {

    String [] options = new String [8];
    int current = 0;

    if (!getResultsetKeyColumns().getRanges().equals("")) {
      options[current++] = "-G";
      options[current++] = getResultsetKeyColumns().getRanges();
    }
    options[current++] = "-D";
    options[current++] = "" + (getDatasetColumn() + 1);
    options[current++] = "-R";
    options[current++] = "" + (getRunColumn() + 1);
    options[current++] = "-S";
    options[current++] = "" + getSignificanceLevel();

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Get the value of ResultsetKeyColumns.
   *
   * @return Value of ResultsetKeyColumns.
   */
  public Range getResultsetKeyColumns() {
    
    return m_ResultsetKeyColumnsRange;
  }
  
  /**
   * Set the value of ResultsetKeyColumns.
   *
   * @param newResultsetKeyColumns Value to assign to ResultsetKeyColumns.
   */
  public void setResultsetKeyColumns(Range newResultsetKeyColumns) {
    
    m_ResultsetKeyColumnsRange = newResultsetKeyColumns;
    m_ResultsetsValid = false;
  }
  
  /**
   * Get the value of SignificanceLevel.
   *
   * @return Value of SignificanceLevel.
   */
  public double getSignificanceLevel() {
    
    return m_SignificanceLevel;
  }
  
  /**
   * Set the value of SignificanceLevel.
   *
   * @param newSignificanceLevel Value to assign to SignificanceLevel.
   */
  public void setSignificanceLevel(double newSignificanceLevel) {
    
    m_SignificanceLevel = newSignificanceLevel;
  }
  
  /**
   * Get the value of DatasetColumn.
   *
   * @return Value of DatasetColumn.
   */
  public int getDatasetColumn() {
    
    return m_DatasetColumnSet;
  }
  
  /**
   * Set the value of DatasetColumn.
   *
   * @param newDatasetColumn Value to assign to DatasetColumn.
   */
  public void setDatasetColumn(int newDatasetColumn) {
    
    m_DatasetColumnSet = newDatasetColumn;
    m_ResultsetsValid = false;
  }
  
  /**
   * Get the value of RunColumn.
   *
   * @return Value of RunColumn.
   */
  public int getRunColumn() {
    
    return m_RunColumnSet;
  }
  
  /**
   * Set the value of RunColumn.
   *
   * @param newRunColumn Value to assign to RunColumn.
   */
  public void setRunColumn(int newRunColumn) {
    
    m_RunColumnSet = newRunColumn;
  }
  
  /**
   * Get the value of Instances.
   *
   * @return Value of Instances.
   */
  public Instances getInstances() {
    
    return m_Instances;
  }
  
  /**
   * Set the value of Instances.
   *
   * @param newInstances Value to assign to Instances.
   */
  public void setInstances(Instances newInstances) {
    
    m_Instances = newInstances;
    m_ResultsetsValid = false;
  }
  
  /**
   * Test the class from the command line.
   *
   * @param args contains options for the instance ttests
   */
  public static void main(String args[]) {

    try {
      PairedTTester tt = new PairedTTester();
      String datasetName = Utils.getOption('t', args);
      String compareColStr = Utils.getOption('c', args);
      String baseColStr = Utils.getOption('b', args);
      boolean summaryOnly = Utils.getFlag('s', args);
      boolean rankingOnly = Utils.getFlag('r', args);
      try {
	if ((datasetName.length() == 0)
	    || (compareColStr.length() == 0)) {
	  throw new Exception("-t and -c options are required");
	}
	tt.setOptions(args);
	Utils.checkForRemainingOptions(args);
      } catch (Exception ex) {
	String result = "";
	Enumeration enum = tt.listOptions();
	while (enum.hasMoreElements()) {
	  Option option = (Option) enum.nextElement();
	  result += option.synopsis() + '\n'
	    + option.description() + '\n';
	}
	throw new Exception(
	      "Usage:\n\n"
	      + "-t <file>\n"
	      + "\tSet the dataset containing data to evaluate\n"
	      + "-b <index>\n"
	      + "\tSet the resultset to base comparisons against (optional)\n"
	      + "-c <index>\n"
	      + "\tSet the column to perform a comparison on\n"
	      + "-s\n"
	      + "\tSummarize wins over all resultset pairs\n\n"
	      + "-r\n"
	      + "\tGenerate a resultset ranking\n\n"
	      + result);
      }
      Instances data = new Instances(new BufferedReader(
				  new FileReader(datasetName)));
      tt.setInstances(data);
      //      tt.prepareData();
      int compareCol = Integer.parseInt(compareColStr) - 1;
      System.out.println(tt.header(compareCol));
      if (rankingOnly) {
	System.out.println(tt.multiResultsetRanking(compareCol));
      } else if (summaryOnly) {
	System.out.println(tt.multiResultsetSummary(compareCol));
      } else {
	System.out.println(tt.resultsetKey());
	if (baseColStr.length() == 0) {
	  for (int i = 0; i < tt.getNumResultsets(); i++) {
	    System.out.println(tt.multiResultsetFull(i, compareCol));
	  }
	} else {
	  int baseCol = Integer.parseInt(baseColStr) - 1;
	  System.out.println(tt.multiResultsetFull(baseCol, compareCol));
	}
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
