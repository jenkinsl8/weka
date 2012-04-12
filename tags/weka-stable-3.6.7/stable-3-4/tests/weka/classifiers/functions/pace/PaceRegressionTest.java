/*
 * Copyright 2001 Malcolm Ware. 
 */

package weka.classifiers.functions.pace;

import java.util.Random;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.classifiers.evaluation.NominalPrediction;
import weka.classifiers.*;

/**
 * Tests PaceRegression. Run from the command line with:<p>
 * java weka.classifiers.nn.PaceRegressionTest
 *
 * @author <a href="mailto:mfw4@cs.waikato.ac.nz">Malcolm Ware</a>
 * @version $Revision: 1.1 $
 */
public class PaceRegressionTest extends AbstractClassifierTest {


  public PaceRegressionTest(String name) { super(name);  }

  /** Creates a default ThresholdSelector */
  public Classifier getClassifier() {
    return new PaceRegression();
  }

  public static Test suite() {
    return new TestSuite(PaceRegressionTest.class);
  }

  public static void main(String[] args){
    junit.textui.TestRunner.run(suite());
  }

}
