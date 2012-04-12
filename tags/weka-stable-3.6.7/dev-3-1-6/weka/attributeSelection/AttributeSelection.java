/*
 *    AttributeSelection.java
 *    Copyright (C) 1999 Mark Hall
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
package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;

/** 
 * Attribute selection class. Takes the name of a search class and
 * an evaluation class on the command line. <p>
 *
 * Valid options are: <p>
 *
 * -h <br>
 * Display help. <p>
 *
 * -I <name of input file> <br>
 * Specify the training arff file. <p>
 * 
 * -C <class index> <br>
 * The index of the attribute to use as the class. <p>
 * 
 * -S <search method> <br>
 * The full class name of the search method followed by search method options
 * (if any).<br>
 * Eg. -S "weka.attributeSelection.BestFirst -N 10" <p>
 *
 * -X <number of folds> <br>
 * Perform a cross validation. <p>
 *
 * -N <random number seed> <br>
 * Specify a random number seed. Use in conjuction with -X. (Default = 1). <p>
 * 
 * ------------------------------------------------------------------------ <p>
 * 
 * Example usage as the main of an attribute evaluator (called FunkyEvaluator):
 * <code> <pre>
 * public static void main(String [] args) {
 *   try {
 *     ASEvaluator eval = new FunkyEvaluator();
 *     System.out.println(SelectAttributes(Evaluator, args));
 *   } catch (Exception e) {
 *     System.err.println(e.getMessage());
 *   }
 * }
 * </code> </pre>
 * <p>
 *
 * ------------------------------------------------------------------------ <p>
 *
 * @author   Mark Hall (mhall@cs.waikato.ac.nz)
 * @version  $Revision: 1.11 $
 */
public class AttributeSelection implements Serializable {

  /** the instances to select attributes from */
  private Instances m_trainInstances;

  /** the attribute/subset evaluator */
  private ASEvaluation m_ASEvaluator;

  /** the search method */
  private ASSearch m_searchMethod;

  /** the number of folds to use for cross validation */
  private int m_numFolds;

  /** holds a string describing the results of the attribute selection */
  private StringBuffer m_selectionResults;

  /** rank features (if allowed by the search method) */
  private boolean m_doRank;
  
  /** do cross validation */
  private boolean m_doXval;

  /** seed used to randomly shuffle instances for cross validation */
  private int m_seed;

  /** cutoff value by which to select attributes for ranked results */
  private double m_threshold;
  
  /** the selected attributes */
  private int [] m_selectedAttributeSet;

  /** the attribute indexes and associated merits if a ranking is produced */
  private double [][] m_attributeRanking;

  /** hold statistics for repeated feature selection, such as
      under cross validation */
  private double [][] m_rankResults = null;
  private double [] m_subsetResults = null;
  private int m_trials = 0;

  /**
   * get the final selected set of attributes.
   * @return an array of attribute indexes
   * @exception Exception if attribute selection has not been performed yet
   */
  public int [] selectedAttributes () throws Exception {
    if (m_selectedAttributeSet == null) {
      throw new Exception("Attribute selection has not been performed yet!");
    }
    return m_selectedAttributeSet;
  }

  /**
   * get the final ranking of the attributes.
   * @return a two dimensional array of ranked attribute indexes and their
   * associated merit scores as doubles.
   * @exception Exception if a ranking has not been produced
   */
  public double [][] rankedAttributes () throws Exception {
    if (m_attributeRanking == null) {
      throw new Exception("Ranking has not been performed");
    }
    return m_attributeRanking;
  }

  /**
   * set the attribute/subset evaluator
   * @param evaluator the evaluator to use
   */
  public void setEvaluator (ASEvaluation evaluator) {
    m_ASEvaluator = evaluator;
  }

  /**
   * set the search method
   * @param search the search method to use
   */
  public void setSearch (ASSearch search) {
    m_searchMethod = search;

    if (m_searchMethod instanceof RankedOutputSearch) {
      setRanking(((RankedOutputSearch)m_searchMethod).getGenerateRanking());
    }
  }

  /**
   * set the number of folds for cross validation
   * @param folds the number of folds
   */
  public void setFolds (int folds) {
    m_numFolds = folds;
  }

  /**
   * produce a ranking (if possible with the set search an evaluator
   * @param r true if a ranking is to be produced
   */
  public void setRanking (boolean r) {
    m_doRank = r;
  }

  /**
   * do a cross validation
   * @param x true if a cross validation is to be performed
   */
  public void setXval (boolean x) {
    m_doXval = x;
  }

  /**
   * set the seed for use in cross validation
   * @param s the seed
   */
  public void setSeed (int s) {
    m_seed = s;
  }

  /**
   * set the threshold by which to select features from a ranked list
   * @param t the threshold
   */
  public void setThreshold (double t) {
    m_threshold = t;
  }

  /**
   * get a description of the attribute selection
   * @return a String describing the results of attribute selection
   */
  public String toResultsString() {
    return m_selectionResults.toString();
  }


  /**
   * constructor. Sets defaults for each member varaible. Default
   * attribute evaluator is CfsSubsetEval; default search method is
   * BestFirst.
   */
  public AttributeSelection () {
    setFolds(10);
    setRanking(false);
    setXval(false);
    setSeed(1);
    //    m_threshold = -Double.MAX_VALUE;
    setEvaluator(new CfsSubsetEval());
    setSearch(new ForwardSelection());
    m_selectionResults = new StringBuffer();
    m_selectedAttributeSet = null;
    m_attributeRanking = null;
  }

  /**
   * Perform attribute selection with a particular evaluator and
   * a set of options specifying search method and input file etc.
   *
   * @param ASEvaluator an evaluator object
   * @param options an array of options, not only for the evaluator
   * but also the search method (if any) and an input data file
   * @return the results of attribute selection as a String
   * @exception Exception if no training file is set
   */
  public static String SelectAttributes (ASEvaluation ASEvaluator, 
					 String[] options)
    throws Exception
  {
    String trainFileName, searchName;
    Instances train = null;
    ASSearch searchMethod = null;

    try {
      // get basic options (options the same for all attribute selectors
      trainFileName = Utils.getOption('I', options);

      if (trainFileName.length() == 0) {
        searchName = Utils.getOption('S', options);
	
        if (searchName.length() != 0) {
          searchMethod = (ASSearch)Class.forName(searchName).newInstance();
        }

        throw  new Exception("No training file given.");
      }
    }
    catch (Exception e) {
      throw  new Exception('\n' + e.getMessage() 
			   + makeOptionString(ASEvaluator, searchMethod));
    }

    train = new Instances(new FileReader(trainFileName));
    return  SelectAttributes(ASEvaluator, options, train);
  }

  /**
   * returns a string summarizing the results of repeated attribute
   * selection runs on splits of a dataset.
   * @return a summary of attribute selection results
   * @exception Exception if no attribute selection has been performed.
   */
  public String CVResultsString () throws Exception {
    StringBuffer CvString = new StringBuffer();
    
    if ((m_subsetResults == null && m_rankResults == null) ||
	( m_trainInstances == null)) {
      throw new Exception("Attribute selection has not been performed yet!");
    }

    int fieldWidth = (int)(Math.log(m_trainInstances.numAttributes()) +1.0);

    CvString.append("\n\n=== Attribute selection " + m_numFolds 
		    + " fold cross-validation ");

    if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) && 
	!(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator) &&
	(m_trainInstances.classAttribute().isNominal())) {
	CvString.append("(stratified), seed: ");
	CvString.append(m_seed+" ===\n\n");
    }
    else {
      CvString.append("seed: "+m_seed+" ===\n\n");
    }

    if ((m_searchMethod instanceof RankedOutputSearch) && (m_doRank == true)) {
      CvString.append("average merit      average rank  attribute\n");

      // calcualte means and std devs
      for (int i = 0; i < m_rankResults[0].length; i++) {
	m_rankResults[0][i] /= m_numFolds; // mean merit
	double var = m_rankResults[0][i]*m_rankResults[0][i]*m_numFolds;
	var = (m_rankResults[2][i] - var);
	var /= m_numFolds;

	if (var <= 0.0) {
	  var = 0.0;
	  m_rankResults[2][i] = 0;
	}
	else {
	  m_rankResults[2][i] = Math.sqrt(var);
	}

	m_rankResults[1][i] /= m_numFolds; // mean rank
	var = m_rankResults[1][i]*m_rankResults[1][i]*m_numFolds;
	var = (m_rankResults[3][i] - var);
	var /= m_numFolds;

	if (var <= 0.0) {
	  var = 0.0;
	  m_rankResults[3][i] = 0;
	}
	else {
	  m_rankResults[3][i] = Math.sqrt(var);
	}
      }

      // now sort them by mean merit
      int[] s = Utils.sort(m_rankResults[0]);
      for (int i = s.length - 1; i >= 0; i--) {
	CvString.append(Utils.doubleToString(m_rankResults[0][s[i]], 6, 3) 
			+ " +-" 
			+ Utils.doubleToString(m_rankResults[2][s[i]], 6, 3) 
			+ "   " 
			+ Utils.doubleToString(m_rankResults[1][s[i]],
					       fieldWidth+2, 1) 
			+ " +-" 
			+ Utils.doubleToString(m_rankResults[3][s[i]], 5, 2) 
			+"  "
			+ Utils.doubleToString(((double)(s[i] + 1)), 
					       fieldWidth, 0)
			+ " " 
			+ m_trainInstances.attribute(s[i]).name() 
			+ "\n");
      }
    }
    else {
      CvString.append("number of folds (%)  attribute\n");

      for (int i = 0; i < m_subsetResults.length; i++) {
	CvString.append(Utils.doubleToString(m_subsetResults[i], 12, 0) 
			+ "(" 
			+ Utils.doubleToString((m_subsetResults[i] / 
						m_numFolds * 100.0)
					       , 3, 0) 
			+ " %)  " 
			+ Utils.doubleToString(((double)(i + 1)),
					       fieldWidth, 0)
			+ " " 
			+ m_trainInstances.attribute(i).name() 
			+ "\n");
      }
    }

    return CvString.toString();
  }

  /**
   * Select attributes for a split of the data. Calling this function
   * updates the statistics on attribute selection. CVResultsString()
   * returns a string summarizing the results of repeated calls to
   * this function. Assumes that splits are from the same dataset---
   * ie. have the same number and types of attributes as previous
   * splits.
   *
   * @param split the instances to select attributes from
   * @exception Exception if an error occurs
   */
  public void selectAttributesCVSplit(Instances split) throws Exception {
    double[][] attributeRanking = null;

    // if the train instances are null then set equal to this split.
    // If this is the case then this function is more than likely being
    // called from outside this class in order to obtain CV statistics
    // and all we need m_trainIstances for is to get at attribute names
    // and types etc.
    if (m_trainInstances == null) {
      m_trainInstances = split;
    }

    // create space to hold statistics
    if (m_rankResults == null && m_subsetResults == null) {
      if (m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) {
	m_subsetResults = new double[split.numAttributes()];
      }
      else {
	m_subsetResults = new double[split.numAttributes() - 1];
      }
      
      if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) && 
	  !(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator)) {

	m_rankResults = new double[4][split.numAttributes() - 1];
      }
      else {
	m_rankResults = new double[4][split.numAttributes()];
      }
    }

    m_ASEvaluator.buildEvaluator(split);
    // Do the search
    int[] attributeSet = m_searchMethod.search(m_ASEvaluator, 
					       split);
    // Do any postprocessing that a attribute selection method might 
    // require
    attributeSet = m_ASEvaluator.postProcess(attributeSet);
    
    if ((m_searchMethod instanceof RankedOutputSearch) && 
	(m_doRank == true)) {
      attributeRanking = ((RankedOutputSearch)m_searchMethod).
	rankedAttributes();
      
      // System.out.println(attributeRanking[0][1]);
      for (int j = 0; j < attributeRanking.length; j++) {
	// merit
	m_rankResults[0][(int)attributeRanking[j][0]] += 
	  attributeRanking[j][1];
	// squared merit
	m_rankResults[2][(int)attributeRanking[j][0]] += 
	  (attributeRanking[j][1]*attributeRanking[j][1]);
	// rank
	m_rankResults[1][(int)attributeRanking[j][0]] += (j + 1);
	// squared rank
	m_rankResults[3][(int)attributeRanking[j][0]] += (j + 1)*(j + 1);
	// += (attributeRanking[j][0] * attributeRanking[j][0]);
      }
    } else {
      for (int j = 0; j < attributeSet.length; j++) {
	m_subsetResults[attributeSet[j]]++;
      }
    }

    m_trials++;
  }

  /**
   * Perform a cross validation for attribute selection. With subset
   * evaluators the number of times each attribute is selected over
   * the cross validation is reported. For attribute evaluators, the
   * average merit and average ranking + std deviation is reported for
   * each attribute.
   *
   * @return the results of cross validation as a String
   * @exception Exception if an error occurs during cross validation
   */
  public String CrossValidateAttributes () throws Exception {
    Instances cvData = new Instances(m_trainInstances);
    Instances train;
    double[][] rankResults;
    double[] subsetResults;
    double[][] attributeRanking = null;

    cvData.randomize(new Random(m_seed));

    if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) && 
	!(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator)) {
      if (cvData.classAttribute().isNominal()) {
	cvData.stratify(m_numFolds);
      }

    }

    for (int i = 0; i < m_numFolds; i++) {
      // Perform attribute selection
      train = cvData.trainCV(m_numFolds, i);
      selectAttributesCVSplit(train);
    }

    return  CVResultsString();
  }

  /**
   * Perform attribute selection on the supplied training instances.
   *
   * @param data the instances to select attributes from
   * @exception Exception if there is a problem during selection
   */
  public void SelectAttributes (Instances data) throws Exception {
    int [] attributeSet;
    
    m_trainInstances = data;
    int fieldWidth = (int)(Math.log(m_trainInstances.numAttributes()) +1.0);
    
    if (m_ASEvaluator instanceof SubsetEvaluator &&
	m_searchMethod instanceof Ranker) {
      throw new Exception(m_ASEvaluator.getClass().getName()
			  +" must use a search method other than Ranker");
    }

    if (m_ASEvaluator instanceof AttributeEvaluator &&
	!(m_searchMethod instanceof Ranker)) {
      //      System.err.println("AttributeEvaluators must use a Ranker search "
      //			 +"method. Switching to Ranker...");
      //      m_searchMethod = new Ranker();
      throw new Exception("AttributeEvaluators must use the Ranker search "
			  + "method");
    }

    if (m_searchMethod instanceof RankedOutputSearch) {
      m_doRank = ((RankedOutputSearch)m_searchMethod).getGenerateRanking();
    }

    if (m_ASEvaluator instanceof UnsupervisedAttributeEvaluator ||
	m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) {
      // unset the class index
      m_trainInstances.setClassIndex(-1);
    } else {
      // check that a class index has been set
      if (m_trainInstances.classIndex() < 0) {
	m_trainInstances.setClassIndex(m_trainInstances.numAttributes()-1);
      }
    }

    // Initialize the attribute evaluator
    m_ASEvaluator.buildEvaluator(m_trainInstances);
    // Do the search
    attributeSet = m_searchMethod.search(m_ASEvaluator, 
					 m_trainInstances);
    // Do any postprocessing that a attribute selection method might require
    attributeSet = m_ASEvaluator.postProcess(attributeSet);
    if (!m_doRank) {
      m_selectionResults.append(printSelectionResults(m_ASEvaluator, 
						      m_searchMethod, 
						      m_trainInstances));
    }
    
    if ((m_searchMethod instanceof RankedOutputSearch) && m_doRank == true) {
      m_attributeRanking = 
	((RankedOutputSearch)m_searchMethod).rankedAttributes();
      m_selectionResults.append(printSelectionResults(m_ASEvaluator, 
						       m_searchMethod, 
						       m_trainInstances));
      m_selectionResults.append("Ranked attributes:\n");

      // retrieve the threshold by which to select attributes
      m_threshold = ((RankedOutputSearch)m_searchMethod).getThreshold();

      for (int i = 0; i < m_attributeRanking.length; i++) {
	if (m_attributeRanking[i][1] > m_threshold) {
	  m_selectionResults.
	    append(Utils.doubleToString(m_attributeRanking[i][1],6,3) 
		   + Utils.doubleToString((m_attributeRanking[i][0] + 1),
					  fieldWidth+1,0) 
		   + " " 
		   + m_trainInstances.
		   attribute((int)m_attributeRanking[i][0]).name() 
		   + "\n");
	}
      }

      int count = 0;

      for (int i = 0; i < m_attributeRanking.length; i++) {
	if (m_attributeRanking[i][1] > m_threshold) {
	  count++;
	}
      }
      
      // set up the selected attributes array - usable by a filter or
      // whatever
      if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) 
	  && !(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator)) 
	// one more for the class
	{
	  m_selectedAttributeSet = new int[count + 1];
	  m_selectedAttributeSet[count] = m_trainInstances.classIndex();
	}
      else {
	m_selectedAttributeSet = new int[count];
      }

      m_selectionResults.append("\nSelected attributes: ");

      for (int i = 0; i < m_attributeRanking.length; i++) {
	if (m_attributeRanking[i][1] > m_threshold) {
	  m_selectedAttributeSet[i] = (int)m_attributeRanking[i][0];
	}

	if (i == (m_attributeRanking.length - 1)) {
	  if (m_attributeRanking[i][1] > m_threshold) {
	    m_selectionResults.append(((int)m_attributeRanking[i][0] + 1) 
				      + " : " 
				      + (i + 1) 
				      + "\n");
	  }
	}
	else {
	  if (m_attributeRanking[i][1] > m_threshold) {
	    m_selectionResults.append(((int)m_attributeRanking[i][0] + 1));

	    if (m_attributeRanking[i + 1][1] > m_threshold) {
	      m_selectionResults.append(",");
	    }
	    else {
	      m_selectionResults.append(" : " + (i + 1) + "\n");
	    }
	  }
	}
      }
    } else {
       // set up the selected attributes array - usable by a filter or
       // whatever
       if (!(m_ASEvaluator instanceof UnsupervisedSubsetEvaluator) 
	  && !(m_ASEvaluator instanceof UnsupervisedAttributeEvaluator)) 
	// one more for the class
	{
	  m_selectedAttributeSet = new int[attributeSet.length + 1];
	  m_selectedAttributeSet[attributeSet.length] = 
	    m_trainInstances.classIndex();
	}
      else {
	m_selectedAttributeSet = new int[attributeSet.length];
      }

      for (int i = 0; i < attributeSet.length; i++) {
	m_selectedAttributeSet[i] = attributeSet[i];
      }

      m_selectionResults.append("Selected attributes: ");

      for (int i = 0; i < attributeSet.length; i++) {
	if (i == (attributeSet.length - 1)) {
	  m_selectionResults.append((attributeSet[i] + 1) 
		      + " : " 
		      + attributeSet.length 
		      + "\n");
	}
	else {
	  m_selectionResults.append((attributeSet[i] + 1) + ",");
	}
      }

      for (int i=0;i<attributeSet.length;i++) {
	m_selectionResults.append("                     "
				  +m_trainInstances
				  .attribute(attributeSet[i]).name()
				  +"\n");
      }
    }

    // Cross validation should be called from here
    if (m_doXval == true) {
      m_selectionResults.append(CrossValidateAttributes()); 
    }
  }

  /**
   * Perform attribute selection with a particular evaluator and
   * a set of options specifying search method and options for the
   * search method and evaluator.
   *
   * @param ASEvaluator an evaluator object
   * @param options an array of options, not only for the evaluator
   * but also the search method (if any) and an input data file
   * @param outAttributes index 0 will contain the array of selected
   * attribute indices
   * @param train the input instances
   * @return the results of attribute selection as a String
   * @exception Exception if incorrect options are supplied
   */
  public static String SelectAttributes (ASEvaluation ASEvaluator, 
					 String[] options, 
					 Instances train)
    throws Exception
  {
    int seed = 1, folds = 10;
    String cutString, foldsString, seedString, searchName;
    String classString;
    String searchClassName;
    String[] searchOptions = null; //new String [1];
    Random random;
    ASSearch searchMethod = null;
    boolean doCrossVal = false;
    Range initialRange;
    int classIndex = -1;
    int[] selectedAttributes;
    double cutoff = -Double.MAX_VALUE;
    boolean helpRequested = false;
    StringBuffer text = new StringBuffer();
    initialRange = new Range();
    AttributeSelection trainSelector = new AttributeSelection();

    try {
      if (Utils.getFlag('h', options)) {
	helpRequested = true;
      }

      // get basic options (options the same for all attribute selectors
      classString = Utils.getOption('C', options);

      if (classString.length() != 0) {
	if (classString.equals("first")) {
	  classIndex = 1;
	} else if (classString.equals("last")) {
	  classIndex = train.numAttributes();
	} else {
	  classIndex = Integer.parseInt(classString);
	}
      }

      if ((classIndex != -1) && 
	  ((classIndex == 0) || (classIndex > train.numAttributes()))) {
	throw  new Exception("Class index out of range.");
      }

      if (classIndex != -1) {
	train.setClassIndex(classIndex - 1);
      }
      else {
	classIndex = train.numAttributes();
	train.setClassIndex(classIndex - 1);
      }
      
      foldsString = Utils.getOption('X', options);

      if (foldsString.length() != 0) {
	folds = Integer.parseInt(foldsString);
	doCrossVal = true;
      }
      
      trainSelector.setFolds(folds);
      trainSelector.setXval(doCrossVal);

      seedString = Utils.getOption('N', options);

      if (seedString.length() != 0) {
	seed = Integer.parseInt(seedString);
      }

      trainSelector.setSeed(seed);

      searchName = Utils.getOption('S', options);

      if ((searchName.length() == 0) && 
	  (!(ASEvaluator instanceof AttributeEvaluator))) {
	throw  new Exception("No search method given.");
      }

      if (searchName.length() != 0) {
	searchName = searchName.trim();
	// split off any search options
	int breakLoc = searchName.indexOf(' ');
	searchClassName = searchName;
	String searchOptionsString = "";

	if (breakLoc != -1) {
	  searchClassName = searchName.substring(0, breakLoc);
	  searchOptionsString = searchName.substring(breakLoc).trim();
	  searchOptions = Utils.splitOptions(searchOptionsString);
	}
      }
      else {
	try {
	  searchClassName = new String("weka.attributeSelection.Ranker");
	  searchMethod = (ASSearch)Class.
	    forName(searchClassName).newInstance();
	}
	catch (Exception e) {
	  throw  new Exception("Can't create Ranker object");
	}
      }

      // if evaluator is a subset evaluator
      // create search method and set its options (if any)
      if (searchMethod == null) {
	searchMethod = ASSearch.forName(searchClassName, searchOptions);
      }

      // set the search method
      trainSelector.setSearch(searchMethod);
    }
    catch (Exception e) {
      throw  new Exception('\n' + e.getMessage() 
			   + makeOptionString(ASEvaluator, searchMethod));
    }

    try {
      // Set options for ASEvaluator
      if (ASEvaluator instanceof OptionHandler) {
	((OptionHandler)ASEvaluator).setOptions(options);
      }

      /* // Set options for Search method
	 if (searchMethod instanceof OptionHandler)
	 {
	 if (searchOptions != null)
	 {
	 ((OptionHandler)searchMethod).setOptions(searchOptions);
	 }
	 }
	 Utils.checkForRemainingOptions(searchOptions); */
    }
    catch (Exception e) {
      throw  new Exception("\n" + e.getMessage() 
			   + makeOptionString(ASEvaluator, searchMethod));
    }

    try {
      Utils.checkForRemainingOptions(options);
    }
    catch (Exception e) {
      throw  new Exception('\n' + e.getMessage() 
			   + makeOptionString(ASEvaluator, searchMethod));
    }

    if (helpRequested) {
      System.out.println(makeOptionString(ASEvaluator, searchMethod));
      System.exit(0);
    }

    // set the attribute evaluator
    trainSelector.setEvaluator(ASEvaluator);

    // do the attribute selection
    trainSelector.SelectAttributes(train);

    // return the results string
    return trainSelector.toResultsString();
  }


  /**
   * Assembles a text description of the attribute selection results.
   *
   * @param ASEvaluator the attribute/subset evaluator
   * @param searchMethod the search method
   * @param train the input instances
   * @return a string describing the results of attribute selection.
   */
  private static String printSelectionResults (ASEvaluation ASEvaluator, 
					       ASSearch searchMethod, 
					       Instances train) {
    StringBuffer text = new StringBuffer();
    text.append("\n\n=== Attribute Selection on all input data ===\n\n" 
		+ "Search Method:\n");
    text.append(searchMethod.toString());
    text.append("\nAttribute ");

    if (ASEvaluator instanceof SubsetEvaluator) {
      text.append("Subset Evaluator (");
    }
    else {
      text.append("Evaluator (");
    }

    if (!(ASEvaluator instanceof UnsupervisedSubsetEvaluator) 
	&& !(ASEvaluator instanceof UnsupervisedAttributeEvaluator)) {
      text.append("supervised, ");
      text.append("Class (");

      if (train.attribute(train.classIndex()).isNumeric()) {
	text.append("numeric): ");
      }
      else {
	text.append("nominal): ");
      }

      text.append((train.classIndex() + 1) 
		  + " " 
		  + train.attribute(train.classIndex()).name() 
		  + "):\n");
    }
    else {
      text.append("unsupervised):\n");
    }

    text.append(ASEvaluator.toString() + "\n");
    return  text.toString();
  }


  /**
   * Make up the help string giving all the command line options
   *
   * @param ASEvaluator the attribute evaluator to include options for
   * @param searchMethod the search method to include options for
   * @return a string detailing the valid command line options
   */
  private static String makeOptionString (ASEvaluation ASEvaluator, 
					  ASSearch searchMethod)
    throws Exception
  {
    StringBuffer optionsText = new StringBuffer("");
    // General options
    optionsText.append("\n\nGeneral options:\n\n");
    optionsText.append("-h display this help\n");
    optionsText.append("-I <name of input file>\n");
    optionsText.append("\tSets training file.\n");
    optionsText.append("-C <class index>\n");
    optionsText.append("\tSets the class index for supervised attribute\n");
    optionsText.append("\tselection. Default=last column.\n");
    optionsText.append("-S <Class name>\n");
    optionsText.append("\tSets search method for subset evaluators.\n");
    optionsText.append("-X <number of folds>\n");
    optionsText.append("\tPerform a cross validation.\n");
    optionsText.append("-N <random number seed>\n");
    optionsText.append("\tUse in conjunction with -X.\n");

    // Get attribute evaluator-specific options
    if (ASEvaluator instanceof OptionHandler) {
      optionsText.append("\nOptions specific to " 
			 + ASEvaluator.getClass().getName() 
			 + ":\n\n");
      Enumeration enum = ((OptionHandler)ASEvaluator).listOptions();

      while (enum.hasMoreElements()) {
	Option option = (Option)enum.nextElement();
	optionsText.append(option.synopsis() + '\n');
	optionsText.append(option.description() + "\n");
      }
    }

    if (searchMethod != null) {
      if (searchMethod instanceof OptionHandler) {
	optionsText.append("\nOptions specific to " 
			   + searchMethod.getClass().getName() 
			   + ":\n\n");
	Enumeration enum = ((OptionHandler)searchMethod).listOptions();

	while (enum.hasMoreElements()) {
	  Option option = (Option)enum.nextElement();
	  optionsText.append(option.synopsis() + '\n');
	  optionsText.append(option.description() + "\n");
	}
      }
    }
    else {
      if (ASEvaluator instanceof SubsetEvaluator) {
	System.out.println("No search method given.");
      }
    }

    return  optionsText.toString();
  }


  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      if (args.length == 0) {
	throw  new Exception("The first argument must be the name of an " 
			     + "attribute/subset evaluator");
      }

      String EvaluatorName = args[0];
      args[0] = "";
      ASEvaluation newEval = ASEvaluation.forName(EvaluatorName, null);
      System.out.println(SelectAttributes(newEval, args));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }

}

