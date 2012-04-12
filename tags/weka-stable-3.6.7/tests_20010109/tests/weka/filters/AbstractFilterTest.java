/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringWriter;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.test.Regression;

/**
 * Abstract Test class for Filters.
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision: 1.1.1.1 $
 */
public abstract class AbstractFilterTest extends TestCase {

  /** Set to true to print out extra info during testing */
  protected static boolean VERBOSE = false;

  /** The filter to be tested */
  protected Filter m_Filter;

  /** A set of instances to test with */
  protected Instances m_Instances;

  /**
   * Constructs the <code>AbstractFilterTest</code>. Called by subclasses.
   *
   * @param name the name of the test class
   */
  public AbstractFilterTest(String name) { super(name); }

  /**
   * Called by JUnit before each test method. This implementation creates
   * the default filter to test and loads a test set of Instances.
   *
   * @exception Exception if an error occurs reading the example instances.
   */
  protected void setUp() throws Exception {
    m_Filter = getFilter();
    m_Instances = new Instances(new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("weka/filters/data/FilterTest.arff"))));
  }

  /** Called by JUnit after each test method */
  protected void tearDown() {
    m_Filter = null;
    m_Instances = null;
  }

  /**
   * Used to create an instance of a specific filter. The filter
   * should be configured to operate on a dataset that contains
   * attributes in this order:<p>
   *
   * String, Nominal, Numeric, String, Nominal, Numeric<p>
   *
   * Where the first three attributes do not contain any missing values,
   * but the last three attributes do. If the filter is for some reason
   * incapable of accepting a dataset of this type, override setUp() to 
   * either manipulate the default dataset to be compatible, or load another
   * test dataset. <p>
   *
   * The configured filter should preferrably do something
   * meaningful, since the results of filtering are used as the default
   * regression output (and it would hardly be interesting if the filtered 
   * data was the same as the input data).
   *
   * @return a suitably configured <code>Filter</code> value
   */
  public abstract Filter getFilter();

  /**
   * Simple method to return the filtered set of test instances after
   * passing through the test filter. m_Filter contains the filter and
   * m_Instances contains the test instances.
   *
   * @return the Instances after filtering through the filter we have set
   * up to test.  
   */
  protected Instances useFilter() {

    Instances result = null;
    Instances icopy = new Instances(m_Instances);
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }
    return result;
  }
  
  /**
   * Test buffered operation. Output instances are only collected after
   * all instances are passed through
   */
  public void testBuffered() {

    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    // Check the output is valid for printing by trying to write out to 
    // a stringbuffer
    StringWriter sw = new StringWriter(2000);
    sw.write(result.toString());

    // Check the input hasn't been modified
    // We just check the headers are the same and that the instance
    // count is the same.
    assert(icopy.equalHeaders(m_Instances));
    assertEquals(icopy.numInstances(), m_Instances.numInstances());

    // Try repeating the filtering and check we get the same results
    Instances result2 = null;
    try {
      m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    try {
      result2 = Filter.useFilter(icopy, m_Filter);
      assertNotNull(result2);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on useFilter(): \n" + ex.getMessage());
    }

    // Again check the input hasn't been modified
    // We just check the headers are the same and that the instance
    // count is the same.
    assert(icopy.equalHeaders(m_Instances));
    assertEquals(icopy.numInstances(), m_Instances.numInstances());

    // Check the same results for both runs
    assert(result.equalHeaders(result2));
    assertEquals(result.numInstances(), result2.numInstances());
    
  }

  /**
   * Test incremental operation. Each instance is removed as soon as it
   * is made available
   */
  public void testIncremental() {

    Instances icopy = new Instances(m_Instances);
    Instances result = null;
    boolean headerImmediate = false;
    try {
      headerImmediate = m_Filter.setInputFormat(icopy);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on setInputFormat(): \n" + ex.getMessage());
    }
    if (headerImmediate) {
      if (VERBOSE) System.err.println("Filter makes header immediately available.");
      result = m_Filter.getOutputFormat();
    }
    // Pass all the instances to the filter
    for (int i = 0; i < icopy.numInstances(); i++) {
      if (VERBOSE) System.err.println("Input instance to filter");
      boolean collectNow = false;
      try {
        collectNow = m_Filter.input(icopy.instance(i));
      } catch (Exception ex) {
        ex.printStackTrace();
        fail("Exception thrown on input(): \n" + ex.getMessage());
      }
      if (collectNow) {
        if (VERBOSE) System.err.println("Filter said collect immediately");
	if (!headerImmediate) {
	  fail("Filter didn't return true from setInputFormat() earlier!");
	}
        if (VERBOSE) System.err.println("Getting output instance");
	result.add(m_Filter.output());
      }
    }
    // Say that input has finished, and print any pending output instances
    if (VERBOSE) System.err.println("Setting end of batch");
    boolean toCollect = false;
    try {
      toCollect = m_Filter.batchFinished();
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception thrown on batchFinished(): \n" + ex.getMessage());
    }
    if (toCollect) {
      if (VERBOSE) System.err.println("Filter said collect output");
      if (!headerImmediate) {
        if (VERBOSE) System.err.println("Getting output format");
	result = m_Filter.getOutputFormat();
      }
      if (VERBOSE) System.err.println("Getting output instance");
      while (m_Filter.numPendingOutput() > 0) {
	result.add(m_Filter.output());
        if (VERBOSE) System.err.println("Getting output instance");
      }
    }
    
    assertNotNull(result);

    // Check the output iss valid for printing by trying to write out to 
    // a stringbuffer
    StringWriter sw = new StringWriter(2000);
    sw.write(result.toString());
  }

  /**
   * Describe <code>testRegression</code> method here.
   *
   */
  public void testRegression() {

    Regression reg = new Regression(this.getClass());
    Instances result = useFilter();
    reg.println(result.toString());
    try {
      String diff = reg.diff();
      if (diff == null) {
        System.err.println("Warning: No reference available, creating."); 
      } else if (!diff.equals("")) {
        fail("Regression test failed. Difference:\n" + diff);
      }
    } catch (java.io.IOException ex) {
      fail("Problem during regression testing.\n" + ex);
    }
  }
  
  // TODO: 
  // * Check that results between incremental and batch use are
  //   the same
  // * Check batch operation is OK
  // * Check memory use between subsequent runs
  // * Check memory use when multiplying data?

}
