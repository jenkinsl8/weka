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
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 */

package weka.clusterers;

import weka.core.Attribute;

import junit.framework.TestCase;

/**
 * Abstract Test class for Clusterers. Internally it uses the class
 * <code>CheckClusterer</code> to determine success or failure of the
 * tests. It follows basically the <code>runTests</code> method.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.1 $
 *
 * @see CheckClusterer
 * @see CheckClusterer#runTests(boolean, boolean)
 */
public abstract class AbstractClustererTest 
  extends TestCase {

  /** The clusterer to be tested */
  protected Clusterer m_Clusterer;

  /** For testing the clusterer */
  protected CheckClusterer m_Tester;

  /** whether clusterer handles weighted instances */
  protected boolean m_weightedInstancesHandler;

  /** whether clusterer handles multi-instance data */
  protected boolean m_multiInstanceHandler;

  /** whether to run CheckClusterer in DEBUG mode */
  protected boolean DEBUG = false;
  
  /** wether clusterer can predict nominal attributes (array index is attribute type of class) */
  protected boolean m_NominalPredictors;
  
  /** wether clusterer can predict numeric attributes (array index is attribute type of class) */
  protected boolean m_NumericPredictors;
  
  /** wether clusterer can predict string attributes (array index is attribute type of class) */
  protected boolean m_StringPredictors;
  
  /** wether clusterer can predict date attributes (array index is attribute type of class) */
  protected boolean m_DatePredictors;
  
  /** wether clusterer can predict relational attributes (array index is attribute type of class) */
  protected boolean m_RelationalPredictors;
  
  /** whether clusterer handles missing values */
  protected boolean m_handleMissingPredictors;
  
  /**
   * Constructs the <code>AbstractClustererTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractClustererTest(String name) { 
    super(name); 
  }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default clusterer to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Clusterer = getClusterer();
    m_Tester     = new CheckClusterer();
    m_Tester.setSilent(true);
    m_Tester.setClusterer(m_Clusterer);
    m_Tester.setNumInstances(20);
    m_Tester.setDebug(DEBUG);

    m_weightedInstancesHandler     = m_Tester.weightedInstancesHandler()[0];
    m_multiInstanceHandler         = m_Tester.multiInstanceHandler()[0];
    m_NominalPredictors            = false;
    m_NumericPredictors            = false;
    m_StringPredictors             = false;
    m_DatePredictors               = false;
    m_RelationalPredictors         = false;
    m_handleMissingPredictors      = false;

    // initialize attributes
    checkAttributes(true,  false, false, false, false, false);
    checkAttributes(false, true,  false, false, false, false);
    checkAttributes(false, false, true,  false, false, false);
    checkAttributes(false, false, false, true,  false, false);
    checkAttributes(false, false, false, false, true,  false);
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Clusterer = null;
    m_Tester     = null;

    m_weightedInstancesHandler     = false;
    m_NominalPredictors            = false;
    m_NumericPredictors            = false;
    m_StringPredictors             = false;
    m_DatePredictors               = false;
    m_RelationalPredictors         = false;
    m_handleMissingPredictors      = false;
  }

  /**
   * Used to create an instance of a specific clusterer.
   *
   * @return a suitably configured <code>Clusterer</code> value
   */
  public abstract Clusterer getClusterer();

  /**
   * checks whether at least one attribute type can be handled
   */
  protected boolean canPredict() {
    return    m_NominalPredictors
           || m_NumericPredictors
           || m_StringPredictors
           || m_DatePredictors
           || m_RelationalPredictors;
  }

  /** 
   * returns a string for the class type
   */
  protected String getClassTypeString(int type) {
    if (type == Attribute.NOMINAL)
      return "Nominal";
    else if (type == Attribute.NUMERIC)
      return "Numeric";
    else if (type == Attribute.STRING)
      return "String";
    else if (type == Attribute.DATE)
      return "Date";
    else if (type == Attribute.RELATIONAL)
      return "Relational";
    else
      throw new IllegalArgumentException("Class type '" + type + "' unknown!");
  }

  /**
   * tests whether the clusterer can handle certain attributes and if not,
   * if the exception is OK
   *
   * @param nom         to check for nominal attributes
   * @param num         to check for numeric attributes
   * @param str         to check for string attributes
   * @param dat         to check for date attributes
   * @param rel         to check for relational attributes
   * @param allowFail   whether a junit fail can be executed
   * @see CheckClusterer#canPredict(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  protected void checkAttributes(boolean nom, boolean num, boolean str, 
                                 boolean dat, boolean rel,
                                 boolean allowFail) {
    boolean[]     result;
    String        att;

    // determine text for type of attributes
    att = "";
    if (nom)
      att = "nominal";
    else if (num)
      att = "numeric";
    else if (str)
      att = "string";
    else if (dat)
      att = "date";
    else if (rel)
      att = "relational";
    
    result = m_Tester.canPredict(nom, num, str, dat, rel, m_multiInstanceHandler);
    if (nom)
      m_NominalPredictors = result[0];
    else if (num)
      m_NumericPredictors = result[0];
    else if (str)
      m_StringPredictors = result[0];
    else if (dat)
      m_DatePredictors = result[0];
    else if (rel)
      m_RelationalPredictors = result[0];

    if (!result[0] && !result[1] && allowFail)
      fail("Error handling " + att + " attributes!");
  }

  /**
   * tests whether the clusterer can handle different types of attributes and
   * if not, if the exception is OK
   *
   * @see #checkAttributes(boolean, boolean, boolean, boolean, boolean, boolean)
   */
  public void testAttributes() {
    // nominal
    checkAttributes(true,  false, false, false, false, true);
    // numeric
    checkAttributes(false, true,  false, false, false, true);
    // string
    checkAttributes(false, false, true,  false, false, true);
    // date
    checkAttributes(false, false, false, true,  false, true);
    // relational
    if (!m_multiInstanceHandler)
      checkAttributes(false, false, false, false, true,  true);
  }

  /**
   * tests whether the clusterer handles instance weights correctly
   *
   * @see CheckClusterer#instanceWeights(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  public void testInstanceWeights() {
    boolean[]     result;
    
    if (m_weightedInstancesHandler) {
      if (!canPredict())
	return;
      
      result = m_Tester.instanceWeights(
          m_NominalPredictors,
          m_NumericPredictors,
          m_StringPredictors,
          m_DatePredictors, 
          m_RelationalPredictors, 
          m_multiInstanceHandler);

      if (!result[0])
        System.err.println("Error handling instance weights!");
    }
  }

  /**
   * tests whether the clusterer can handle zero training instances
   *
   * @see CheckClusterer#canHandleZeroTraining(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  public void testZeroTraining() {
    boolean[]     result;
    
    if (!canPredict())
      return;
    
    result = m_Tester.canHandleZeroTraining(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler);

    if (!result[0] && !result[1])
      fail("Error handling zero training instances!");
  }

  /**
   * checks whether the clusterer can handle the given percentage of
   * missing predictors
   *
   * @param percent     the percentage of missing predictors
   * @return            true if the clusterer can handle it
   */
  protected boolean checkMissingPredictors(int percent) {
    boolean[]     result;
    
    result = m_Tester.canHandleMissing(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler, 
        true,
        percent);

    if (!result[0] && !result[1])
      fail("Error handling " + percent + "% missing predictors!");
    
    return result[0];
  }

  /**
   * tests whether the clusterer can handle missing predictors (20% and 100%)
   *
   * @see CheckClusterer#canHandleMissing(boolean, boolean, boolean, boolean, boolean, boolean, boolean, int)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  public void testMissingPredictors() {
    if (!canPredict())
      return;
    
    // 20% missing
    m_handleMissingPredictors = checkMissingPredictors(20);

    // 100% missing
    if (m_handleMissingPredictors)
      checkMissingPredictors(100);
  }

  /**
   * tests whether the clusterer correctly initializes in the
   * buildClusterer method
   *
   * @see CheckClusterer#correctBuildInitialisation(boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  public void testBuildInitialization() {
    boolean[]     result;
    
    if (!canPredict())
      return;
    
    result = m_Tester.correctBuildInitialisation(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler);

    if (!result[0] && !result[1])
      fail("Incorrect build initialization!");
  }

  /**
   * tests whether the clusterer alters the training set during training.
   *
   * @see CheckClusterer#datasetIntegrity(boolean, boolean, boolean, boolean, boolean, boolean, boolean)
   * @see CheckClusterer#runTests(boolean, boolean)
   */
  public void testDatasetIntegrity() {
    boolean[]     result;
  
    if (!canPredict())
      return;
    
    result = m_Tester.datasetIntegrity(
        m_NominalPredictors, 
        m_NumericPredictors, 
        m_StringPredictors, 
        m_DatePredictors, 
        m_RelationalPredictors, 
        m_multiInstanceHandler, 
        m_handleMissingPredictors);

    if (!result[0] && !result[1])
      fail("Training set is altered during training!");
  }
}
