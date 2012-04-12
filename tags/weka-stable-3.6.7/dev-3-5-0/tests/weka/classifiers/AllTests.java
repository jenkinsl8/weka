package weka.classifiers;

import weka.classifiers.rules.*;
import weka.classifiers.trees.*;
import weka.classifiers.bayes.*;
import weka.classifiers.misc.*;
import weka.classifiers.lazy.*;
import weka.classifiers.meta.*;
import weka.classifiers.functions.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;         

/**
 * Test class for all tests in this directory. Run from the command line 
 * with:<p>
 * java weka.classifiers.AllTests
 *
 * @author <a href="mailto:len@reeltwo.com">Len Trigg</a>
 * @version $Revision: 1.11 $
 */
public class AllTests extends TestSuite {

  public static Test suite() {

    TestSuite suite = new TestSuite();

    suite.addTest(BayesNetTest.suite());
    suite.addTest(NaiveBayesTest.suite());
    suite.addTest(NaiveBayesSimpleTest.suite());
    suite.addTest(NaiveBayesUpdateableTest.suite());
    suite.addTest(LeastMedSqTest.suite());
    suite.addTest(LinearRegressionTest.suite());
    suite.addTest(LogisticTest.suite());
    suite.addTest(SMOTest.suite());
    suite.addTest(SMOregTest.suite());
    suite.addTest(VotedPerceptronTest.suite());
    suite.addTest(WinnowTest.suite());
    suite.addTest(IB1Test.suite());
    suite.addTest(IBkTest.suite());
    suite.addTest(LWLTest.suite());
    suite.addTest(AdaBoostM1Test.suite());
    suite.addTest(AdditiveRegressionTest.suite());
    suite.addTest(AttributeSelectedClassifierTest.suite());
    suite.addTest(BaggingTest.suite());
    suite.addTest(CVParameterSelectionTest.suite());
    suite.addTest(ClassificationViaRegressionTest.suite());
    suite.addTest(CostSensitiveClassifierTest.suite());
    suite.addTest(FilteredClassifierTest.suite());
    suite.addTest(LogitBoostTest.suite());
    suite.addTest(MetaCostTest.suite());
    suite.addTest(MultiClassClassifierTest.suite());
    suite.addTest(MultiSchemeTest.suite());
    suite.addTest(OrdinalClassClassifierTest.suite());
    suite.addTest(RegressionByDiscretizationTest.suite());
    suite.addTest(StackingTest.suite());
    suite.addTest(HyperPipesTest.suite());
    suite.addTest(VFITest.suite());
    suite.addTest(ConjunctiveRuleTest.suite());
    suite.addTest(DecisionTableTest.suite());
    suite.addTest(JRipTest.suite());
    suite.addTest(M5RulesTest.suite());
    suite.addTest(OneRTest.suite());
    suite.addTest(PrismTest.suite());
    suite.addTest(RidorTest.suite());
    suite.addTest(DecisionStumpTest.suite());
    suite.addTest(Id3Test.suite());
    suite.addTest(REPTreeTest.suite());
    suite.addTest(UserClassifierTest.suite());
    suite.addTest(KStarTest.suite());
    suite.addTest(PARTTest.suite());
    suite.addTest(ADTreeTest.suite());
    suite.addTest(J48Test.suite());
    suite.addTest(M5PTest.suite());
    suite.addTest(ThresholdSelectorTest.suite());
    suite.addTest(ZeroRTest.suite());
    suite.addTest(MultilayerPerceptronTest.suite());
//      suite.addTest(weka.classifiers.evaluation.AllTests.suite());
//      suite.addTest(weka.classifiers.j48.AllTests.suite());
//      suite.addTest(weka.classifiers.kstar.AllTests.suite());
//      suite.addTest(weka.classifiers.m5.AllTests.suite());
//?      suite.addTest(UpdateableClassifierTest.suite());
//?      suite.addTest(SourcableTest.suite());
    return suite;
  }

  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
