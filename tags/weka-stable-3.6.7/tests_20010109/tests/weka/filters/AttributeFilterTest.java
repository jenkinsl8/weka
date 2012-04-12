/*
 * Copyright 2000 Webmind Inc. 
 */

package weka.filters;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;
import weka.core.Instance;

/**
 * Tests AttributeFilter. Run from the command line with:<p>
 * java weka.filters.AttributeFilterTest
 *
 * @author <a href="mailto:len@webmind.com">Len Trigg</a>
 * @version $Revision:
 */
public class AttributeFilterTest extends AbstractFilterTest {
  
  public AttributeFilterTest(String name) { super(name);  }

  /** Creates a default AttributeFilter */
  public Filter getFilter() {
    return getFilter("1-3");
  }

  /** Creates a specialized AttributeFilter */
  public Filter getFilter(String rangelist) {
    
    try {
      AttributeFilter af = new AttributeFilter();
      af.setAttributeIndices(rangelist);
      return af;
    } catch (Exception ex) {
      ex.printStackTrace();
      fail("Exception setting attribute range: " + rangelist 
           + "\n" + ex.getMessage()); 
    }
    return null;
  }

  public void testTypical() {
    m_Filter = getFilter("1,2");
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(0).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(1).name(), result.attribute(1).name());
  }

  public void testTypical2() {
    m_Filter = getFilter("3-4");
    Instances result = useFilter();
    assertEquals(2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted() {
    m_Filter = getFilter("1,2");
    ((AttributeFilter)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 2, result.numAttributes());
    assertEquals(m_Instances.attribute(2).name(), result.attribute(0).name());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(1).name());
  }

  public void testNonInverted2() {
    m_Filter = getFilter("first-3");
    ((AttributeFilter)m_Filter).setInvertSelection(false);
    Instances result = useFilter();
    assertEquals(m_Instances.numAttributes() - 3, result.numAttributes());
    assertEquals(m_Instances.attribute(3).name(), result.attribute(0).name());
  }

  public static Test suite() {
    return new TestSuite(AttributeFilterTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
