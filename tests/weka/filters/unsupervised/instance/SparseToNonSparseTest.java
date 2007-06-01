/*
 * Copyright (C) 2002 University of Waikato 
 */

package weka.filters.unsupervised.instance;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.AbstractFilterTest;

/**
 * Tests SparseToNonSparse. Run from the command line with:<p>
 * java weka.filters.SparseToNonSparseTest
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.2 $
 */
public class SparseToNonSparseTest extends AbstractFilterTest {
  
  public SparseToNonSparseTest(String name) { super(name);  }

  /** Creates an example SparseToNonSparse */
  public Filter getFilter() {
    SparseToNonSparse f = new SparseToNonSparse();
    return f;
  }

  public void testTypical() {
    Instances result = useFilter();
    // Number of attributes and instances shouldn't change
    assertEquals(m_Instances.numAttributes(), result.numAttributes());
    assertEquals(m_Instances.numInstances(),  result.numInstances());
    // Check conversion is OK
    for (int i = 0; i < result.numInstances(); i++) {
      assertTrue("Instance should not be an instanceof SparseInstance:" + (i + 1),
             !(result.instance(i) instanceof SparseInstance));
    }
  }


  public static Test suite() {
    return new TestSuite(SparseToNonSparseTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
