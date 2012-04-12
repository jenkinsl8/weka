/*
 * Copyright 2002 University of Waikato
 */

package weka.classifiers.bayes;

import weka.classifiers.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.Instances;

/**
 * Tests BayesNetK2. Run from the command line with:<p>
 * java weka.classifiers.BayesNetK2Test
 *
 * @author <a href="mailto:eibe@cs.waikato.ac.nz">Eibe Frank</a>
 * @version $Revision: 1.1 $
 */
public class BayesNetK2Test extends AbstractClassifierTest {

  public BayesNetK2Test(String name) { super(name);  }

  /** Creates a default BayesNetK2 */
  public Classifier getClassifier() {
    return new BayesNetK2();
  }

  public static Test suite() {
    return new TestSuite(BayesNetK2Test.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
