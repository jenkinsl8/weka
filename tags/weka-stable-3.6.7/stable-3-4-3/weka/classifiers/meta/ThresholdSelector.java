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
 *    ThresholdSelector.java
 *    Copyright (C) 1999 Eibe Frank
 *
 */

package weka.classifiers.meta;

import weka.classifiers.Evaluation;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.core.Attribute;
import weka.core.AttributeStats;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Utils;
import weka.core.Drawable;
import weka.core.UnsupportedClassTypeException;

/**
 * Class for selecting a threshold on a probability output by a
 * distribution classifier. The threshold is set so that a given
 * performance measure is optimized. Currently this is the
 * F-measure. Performance is measured either on the training data, a hold-out
 * set or using cross-validation. In addition, the probabilities returned
 * by the base learner can have their range expanded so that the output
 * probabilities will reside between 0 and 1 (this is useful if the scheme
 * normally produces probabilities in a very narrow range).<p>
 *
 * Valid options are:<p>
 *
 * -C num <br>
 * The class for which threshold is determined. Valid values are:
 * 1, 2 (for first and second classes, respectively), 3 (for whichever
 * class is least frequent), 4 (for whichever class value is most 
 * frequent), and 5 (for the first class named any of "yes","pos(itive)",
 * "1", or method 3 if no matches). (default 5). <p>
 *
 * -W classname <br>
 * Specify the full class name of the base classifier. <p>
 *
 * -X num <br> 
 * Number of folds used for cross validation. If just a
 * hold-out set is used, this determines the size of the hold-out set
 * (default 3).<p>
 *
 * -R integer <br>
 * Sets whether confidence range correction is applied. This can be used
 * to ensure the confidences range from 0 to 1. Use 0 for no range correction,
 * 1 for correction based on the min/max values seen during threshold selection
 * (default 0).<p>
 *
 * -S seed <br>
 * Random number seed (default 1).<p>
 *
 * -E integer <br>
 * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
 * 1 for evaluation using hold-out set, and 2 for evaluation on the
 * training data (default 1).<p>
 *
 * Options after -- are passed to the designated sub-classifier. <p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version $Revision: 1.29 $ 
 */
public class ThresholdSelector extends Classifier 
  implements OptionHandler, Drawable {

  /* Type of correction applied to threshold range */ 
  public static final int RANGE_NONE = 0;
  public static final int RANGE_BOUNDS = 1;
  public static final Tag [] TAGS_RANGE = {
    new Tag(RANGE_NONE, "No range correction"),
    new Tag(RANGE_BOUNDS, "Correct based on min/max observed")
  };

  /* The evaluation modes */
  public static final int EVAL_TRAINING_SET = 2;
  public static final int EVAL_TUNED_SPLIT = 1;
  public static final int EVAL_CROSS_VALIDATION = 0;
  public static final Tag [] TAGS_EVAL = {
    new Tag(EVAL_TRAINING_SET, "Entire training set"),
    new Tag(EVAL_TUNED_SPLIT, "Single tuned fold"),
    new Tag(EVAL_CROSS_VALIDATION, "N-Fold cross validation")
  };

  /* How to determine which class value to optimize for */
  public static final int OPTIMIZE_0     = 0;
  public static final int OPTIMIZE_1     = 1;
  public static final int OPTIMIZE_LFREQ = 2;
  public static final int OPTIMIZE_MFREQ = 3;
  public static final int OPTIMIZE_POS_NAME = 4;
  public static final Tag [] TAGS_OPTIMIZE = {
    new Tag(OPTIMIZE_0, "First class value"),
    new Tag(OPTIMIZE_1, "Second class value"),
    new Tag(OPTIMIZE_LFREQ, "Least frequent class value"),
    new Tag(OPTIMIZE_MFREQ, "Most frequent class value"),
    new Tag(OPTIMIZE_POS_NAME, "Class value named: \"yes\", \"pos(itive)\",\"1\"")
  };

  /** The generated base classifier */
  protected Classifier m_Classifier = new weka.classifiers.functions.Logistic();

  /** The upper threshold used as the basis of correction */
  protected double m_HighThreshold = 1;

  /** The lower threshold used as the basis of correction */
  protected double m_LowThreshold = 0;

  /** The threshold that lead to the best performance */
  protected double m_BestThreshold = -Double.MAX_VALUE;

  /** The best value that has been observed */
  protected double m_BestValue = - Double.MAX_VALUE;
  
  /** The number of folds used in cross-validation */
  protected int m_NumXValFolds = 3;

  /** Random number seed */
  protected int m_Seed = 1;

  /** Designated class value, determined during building */
  protected int m_DesignatedClass = 0;

  /** Method to determine which class to optimize for */
  protected int m_ClassMode = OPTIMIZE_POS_NAME;

  /** The evaluation mode */
  protected int m_EvalMode = EVAL_TUNED_SPLIT;

  /** The range correction mode */
  protected int m_RangeMode = RANGE_NONE;

  /** The minimum value for the criterion. If threshold adjustment
      yields less than that, the default threshold of 0.5 is used. */
  protected static final double MIN_VALUE = 0.05;

  /**
   * Collects the classifier predictions using the specified evaluation method.
   *
   * @param instances the set of <code>Instances</code> to generate
   * predictions for.
   * @param mode the evaluation mode.
   * @param numFolds the number of folds to use if not evaluating on the
   * full training set.
   * @return a <code>FastVector</code> containing the predictions.
   * @exception Exception if an error occurs generating the predictions.
   */
  protected FastVector getPredictions(Instances instances, int mode, int numFolds) 
    throws Exception {

    EvaluationUtils eu = new EvaluationUtils();
    eu.setSeed(m_Seed);
    
    switch (mode) {
    case EVAL_TUNED_SPLIT:
      Instances trainData = null, evalData = null;
      Instances data = new Instances(instances);
      Random random = new Random(m_Seed);
      data.randomize(random);
      data.stratify(numFolds);
      
      // Make sure that both subsets contain at least one positive instance
      for (int subsetIndex = 0; subsetIndex < numFolds; subsetIndex++) {
        trainData = data.trainCV(numFolds, subsetIndex, random);
        evalData = data.testCV(numFolds, subsetIndex);
        if (checkForInstance(trainData) && checkForInstance(evalData)) {
          break;
        }
      }
      return eu.getTrainTestPredictions(m_Classifier, trainData, evalData);
    case EVAL_TRAINING_SET:
      return eu.getTrainTestPredictions(m_Classifier, instances, instances);
    case EVAL_CROSS_VALIDATION:
      return eu.getCVPredictions(m_Classifier, instances, numFolds);
    default:
      throw new RuntimeException("Unrecognized evaluation mode");
    }
  }

  /**
   * Finds the best threshold, this implementation searches for the
   * highest FMeasure. If no FMeasure higher than MIN_VALUE is found,
   * the default threshold of 0.5 is used.
   *
   * @param predictions a <code>FastVector</code> containing the predictions.
   */
  protected void findThreshold(FastVector predictions) {

    Instances curve = (new ThresholdCurve()).getCurve(predictions, m_DesignatedClass);

    //System.err.println(curve);
    double low = 1.0;
    double high = 0.0;
    if (curve.numInstances() > 0) {
      Instance maxFM = curve.instance(0);
      int indexFM = curve.attribute(ThresholdCurve.FMEASURE_NAME).index();
      int indexThreshold = curve.attribute(ThresholdCurve.THRESHOLD_NAME).index();
      for (int i = 1; i < curve.numInstances(); i++) {
        Instance current = curve.instance(i);
        if (current.value(indexFM) > maxFM.value(indexFM)) {
          maxFM = current;
        }
        if (m_RangeMode == RANGE_BOUNDS) {
          double thresh = current.value(indexThreshold);
          if (thresh < low) {
            low = thresh;
          }
          if (thresh > high) {
            high = thresh;
          }
        }
      }
      if (maxFM.value(indexFM) > MIN_VALUE) {
        m_BestThreshold = maxFM.value(indexThreshold);
        m_BestValue = maxFM.value(indexFM);
        //System.err.println("maxFM: " + maxFM);
      }
      if (m_RangeMode == RANGE_BOUNDS) {
        m_LowThreshold = low;
        m_HighThreshold = high;
        //System.err.println("Threshold range: " + low + " - " + high);
      }
    }
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(6);

    newVector.addElement(new Option(
              "\tThe class for which threshold is determined. Valid values are:\n" +
              "\t1, 2 (for first and second classes, respectively), 3 (for whichever\n" +
              "\tclass is least frequent), and 4 (for whichever class value is most\n" +
              "\tfrequent), and 5 (for the first class named any of \"yes\",\"pos(itive)\"\n" +
              "\t\"1\", or method 3 if no matches). (default 5).",
	      "C", 1, "-C <integer>"));
    newVector.addElement(new Option(
	      "\tFull name of classifier to perform parameter selection on.\n"
	      + "\teg: weka.classifiers.bayes.NaiveBayes",
	      "W", 1, "-W <classifier class name>"));
    newVector.addElement(new Option(
	      "\tNumber of folds used for cross validation. If just a\n" +
	      "\thold-out set is used, this determines the size of the hold-out set\n" +
	      "\t(default 3).",
	      "X", 1, "-X <number of folds>"));
    newVector.addElement(new Option(
	      "\tSets whether confidence range correction is applied. This\n" +
              "\tcan be used to ensure the confidences range from 0 to 1.\n" +
              "\tUse 0 for no range correction, 1 for correction based on\n" +
              "\tthe min/max values seen during threshold selection\n"+
              "\t(default 0).",
	      "R", 1, "-R <integer>"));
    newVector.addElement(new Option(
	      "\tSets the random number seed (default 1).",
	      "S", 1, "-S <random number seed>"));
    newVector.addElement(new Option(
	      "\tSets the evaluation mode. Use 0 for\n" +
	      "\tevaluation using cross-validation,\n" +
	      "\t1 for evaluation using hold-out set,\n" +
	      "\tand 2 for evaluation on the\n" +
	      "\ttraining data (default 1).",
	      "E", 1, "-E <integer>"));

    if ((m_Classifier != null) &&
	(m_Classifier instanceof OptionHandler)) {
      newVector.addElement(new Option("",
	        "", 0,
		"\nOptions specific to sub-classifier "
	        + m_Classifier.getClass().getName()
		+ ":\n(use -- to signal start of sub-classifier options)"));
      Enumeration enum = ((OptionHandler)m_Classifier).listOptions();
      while (enum.hasMoreElements()) {
	newVector.addElement(enum.nextElement());
      }
    }
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -C num <br>
   * The class for which threshold is determined. Valid values are:
   * 1, 2 (for first and second classes, respectively), 3 (for whichever
   * class is least frequent), 4 (for whichever class value is most 
   * frequent), and 5 (for the first class named any of "yes","pos(itive)",
   * "1", or method 3 if no matches). (default 3). <p>
   *
   * -W classname <br>
   * Specify the full class name of classifier to perform cross-validation
   * selection on.<p>
   *
   * -X num <br> 
   * Number of folds used for cross validation. If just a
   * hold-out set is used, this determines the size of the hold-out set
   * (default 3).<p>
   *
   * -R integer <br>
   * Sets whether confidence range correction is applied. This can be used
   * to ensure the confidences range from 0 to 1. Use 0 for no range correction,
   * 1 for correction based on the min/max values seen during threshold 
   * selection (default 0).<p>
   *
   * -S seed <br>
   * Random number seed (default 1).<p>
   *
   * -E integer <br>
   * Sets the evaluation mode. Use 0 for evaluation using cross-validation,
   * 1 for evaluation using hold-out set, and 2 for evaluation on the
   * training data (default 1).<p>
   *
   * Options after -- are passed to the designated sub-classifier. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    
    String classString = Utils.getOption('C', options);
    if (classString.length() != 0) {
      setDesignatedClass(new SelectedTag(Integer.parseInt(classString) - 1, 
                                         TAGS_OPTIMIZE));
    } else {
      setDesignatedClass(new SelectedTag(OPTIMIZE_LFREQ, TAGS_OPTIMIZE));
    }

    String modeString = Utils.getOption('E', options);
    if (modeString.length() != 0) {
      setEvaluationMode(new SelectedTag(Integer.parseInt(modeString), 
                                         TAGS_EVAL));
    } else {
      setEvaluationMode(new SelectedTag(EVAL_TUNED_SPLIT, TAGS_EVAL));
    }

    String rangeString = Utils.getOption('R', options);
    if (rangeString.length() != 0) {
      setRangeCorrection(new SelectedTag(Integer.parseInt(rangeString) - 1, 
                                         TAGS_RANGE));
    } else {
      setRangeCorrection(new SelectedTag(RANGE_NONE, TAGS_RANGE));
    }

    String foldsString = Utils.getOption('X', options);
    if (foldsString.length() != 0) {
      setNumXValFolds(Integer.parseInt(foldsString));
    } else {
      setNumXValFolds(3);
    }

    String randomString = Utils.getOption('S', options);
    if (randomString.length() != 0) {
      setSeed(Integer.parseInt(randomString));
    } else {
      setSeed(1);
    }

    String classifierName = Utils.getOption('W', options);
    if (classifierName.length() == 0) {
      throw new Exception("A classifier must be specified with"
			  + " the -W option.");
    }

    setClassifier(Classifier.forName(classifierName,
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] classifierOptions = new String [0];
    if ((m_Classifier != null) && 
	(m_Classifier instanceof OptionHandler)) {
      classifierOptions = ((OptionHandler)m_Classifier).getOptions();
    }

    int current = 0;
    String [] options = new String [classifierOptions.length + 13];

    options[current++] = "-C"; options[current++] = "" + (m_DesignatedClass + 1);
    options[current++] = "-X"; options[current++] = "" + getNumXValFolds();
    options[current++] = "-S"; options[current++] = "" + getSeed();

    if (getClassifier() != null) {
      options[current++] = "-W";
      options[current++] = getClassifier().getClass().getName();
    }
    options[current++] = "-E"; options[current++] = "" + m_EvalMode;
    options[current++] = "-R"; options[current++] = "" + m_RangeMode;
    options[current++] = "--";

    System.arraycopy(classifierOptions, 0, options, current, 
		     classifierOptions.length);
    current += classifierOptions.length;
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @exception Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) 
    throws Exception {

    if (instances.numClasses() > 2) {
      throw new UnsupportedClassTypeException("Only works for two-class datasets!");
    }
    if (!instances.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("Class attribute must be nominal!");
    }
    AttributeStats stats = instances.attributeStats(instances.classIndex());
    m_BestThreshold = 0.5;
    m_BestValue = MIN_VALUE;
    m_HighThreshold = 1;
    m_LowThreshold = 0;
    // If data contains only one instance of positive data
    // optimize on training data
    if (stats.distinctCount != 2) {
      System.err.println("Couldn't find examples of both classes. No adjustment.");
      m_Classifier.buildClassifier(instances);
    } else {
      
      // Determine which class value to look for
      switch (m_ClassMode) {
      case OPTIMIZE_0:
        m_DesignatedClass = 0;
        break;
      case OPTIMIZE_1:
        m_DesignatedClass = 1;
        break;
      case OPTIMIZE_POS_NAME:
        Attribute cAtt = instances.classAttribute();
        boolean found = false;
        for (int i = 0; i < cAtt.numValues() && !found; i++) {
          String name = cAtt.value(i).toLowerCase();
          if (name.startsWith("yes") || name.equals("1") || 
              name.startsWith("pos")) {
            found = true;
            m_DesignatedClass = i;
          }
        }
        if (found) {
          break;
        }
        // No named class found, so fall through to default of least frequent
      case OPTIMIZE_LFREQ:
        m_DesignatedClass = (stats.nominalCounts[0] > stats.nominalCounts[1]) ? 1 : 0;
        break;
      case OPTIMIZE_MFREQ:
        m_DesignatedClass = (stats.nominalCounts[0] > stats.nominalCounts[1]) ? 0 : 1;
        break;
      default:
        throw new Exception("Unrecognized class value selection mode");
      }
      
      /*
        System.err.println("ThresholdSelector: Using mode=" 
        + TAGS_OPTIMIZE[m_ClassMode].getReadable());
        System.err.println("ThresholdSelector: Optimizing using class "
        + m_DesignatedClass + "/" 
        + instances.classAttribute().value(m_DesignatedClass));
      */
      
      
      if (stats.nominalCounts[m_DesignatedClass] == 1) {
        System.err.println("Only 1 positive found: optimizing on training data");
        findThreshold(getPredictions(instances, EVAL_TRAINING_SET, 0));
      } else {
        int numFolds = Math.min(m_NumXValFolds, stats.nominalCounts[m_DesignatedClass]);
        //System.err.println("Number of folds for threshold selector: " + numFolds);
        findThreshold(getPredictions(instances, m_EvalMode, numFolds));
        if (m_EvalMode != EVAL_TRAINING_SET) {
          m_Classifier.buildClassifier(instances);
        }
      }
    }
  }

  /**
   * Checks whether instance of designated class is in subset.
   */
  private boolean checkForInstance(Instances data) throws Exception {

    for (int i = 0; i < data.numInstances(); i++) {
      if (((int)data.instance(i).classValue()) == m_DesignatedClass) {
	return true;
      }
    }
    return false;
  }


  /**
   * Calculates the class membership probabilities for the given test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if instance could not be classified
   * successfully
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {
    
    double [] pred = m_Classifier.distributionForInstance(instance);
    double prob = pred[m_DesignatedClass];

    // Warp probability
    if (prob > m_BestThreshold) {
      prob = 0.5 + (prob - m_BestThreshold) / 
        ((m_HighThreshold - m_BestThreshold) * 2);
    } else {
      prob = (prob - m_LowThreshold) / 
        ((m_BestThreshold - m_LowThreshold) * 2);
    }
    if (prob < 0) {
      prob = 0.0;
    } else if (prob > 1) {
      prob = 1.0;
    }

    // Alter the distribution
    pred[m_DesignatedClass] = prob;
    if (pred.length == 2) { // Handle case when there's only one class
      pred[(m_DesignatedClass + 1) % 2] = 1.0 - prob;
    }
    return pred;
  }

  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "A metaclassifier that selecting a mid-point threshold on the "
      + "probability output by a Classifier. The midpoint "
      + "threshold is set so that a given performance measure is optimized. "
      + "Currently this is the F-measure. Performance is measured either on "
      + "the training data, a hold-out set or using cross-validation. In "
      + "addition, the probabilities returned by the base learner can "
      + "have their range expanded so that the output probabilities will "
      + "reside between 0 and 1 (this is useful if the scheme normally "
      + "produces probabilities in a very narrow range).";
  }
    
  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String designatedClassTipText() {

    return "Sets the class value for which the optimization is performed. "
      + "The options are: pick the first class value; pick the second "
      + "class value; pick whichever class is least frequent; pick whichever "
      + "class value is most frequent; pick the first class named any of "
      + "\"yes\",\"pos(itive)\", \"1\", or the least frequent if no matches).";
  }

  /**
   * Gets the method to determine which class value to optimize. Will
   * be one of OPTIMIZE_0, OPTIMIZE_1, OPTIMIZE_LFREQ, OPTIMIZE_MFREQ,
   * OPTIMIZE_POS_NAME.
   *
   * @return the class selection mode.
   */
  public SelectedTag getDesignatedClass() {

    return new SelectedTag(m_ClassMode, TAGS_OPTIMIZE);
  }
  
  /**
   * Sets the method to determine which class value to optimize. Will
   * be one of OPTIMIZE_0, OPTIMIZE_1, OPTIMIZE_LFREQ, OPTIMIZE_MFREQ,
   * OPTIMIZE_POS_NAME.
   *
   * @param newMethod the new class selection mode.
   */
  public void setDesignatedClass(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_OPTIMIZE) {
      m_ClassMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String evaluationModeTipText() {

    return "Sets the method used to determine the threshold/performance "
      + "curve. The options are: perform optimization based on the entire "
      + "training set (may result in overfitting); perform an n-fold "
      + "cross-validation (may be time consuming); perform one fold of "
      + "an n-fold cross-validation (faster but likely less accurate).";
  }

  /**
   * Sets the evaluation mode used. Will be one of
   * EVAL_TRAINING, EVAL_TUNED_SPLIT, or EVAL_CROSS_VALIDATION
   *
   * @param newMethod the new evaluation mode.
   */
  public void setEvaluationMode(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_EVAL) {
      m_EvalMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Gets the evaluation mode used. Will be one of
   * EVAL_TRAINING, EVAL_TUNED_SPLIT, or EVAL_CROSS_VALIDATION
   *
   * @return the evaluation mode.
   */
  public SelectedTag getEvaluationMode() {

    return new SelectedTag(m_EvalMode, TAGS_EVAL);
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String rangeCorrectionTipText() {

    return "Sets the type of prediction range correction performed. "
      + "The options are: do not do any range correction; "
      + "expand predicted probabilities so that the minimum probability "
      + "observed during the optimization maps to 0, and the maximum "
      + "maps to 1 (values outside this range are clipped to 0 and 1).";
  }

  /**
   * Sets the confidence range correction mode used. Will be one of
   * RANGE_NONE, or RANGE_BOUNDS
   *
   * @param newMethod the new correciton mode.
   */
  public void setRangeCorrection(SelectedTag newMethod) {
    
    if (newMethod.getTags() == TAGS_RANGE) {
      m_RangeMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Gets the confidence range correction mode used. Will be one of
   * RANGE_NONE, or RANGE_BOUNDS
   *
   * @return the confidence correction mode.
   */
  public SelectedTag getRangeCorrection() {

    return new SelectedTag(m_RangeMode, TAGS_RANGE);
  }
  
  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {

    return "Sets the seed used for randomization. This is used when "
      + "randomizing the data during optimization.";
  }

  /**
   * Sets the seed for random number generation.
   *
   * @param seed the random number seed
   */
  public void setSeed(int seed) {
    
    m_Seed = seed;
  }

  /**
   * Gets the random number seed.
   * 
   * @return the random number seed
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numXValFoldsTipText() {

    return "Sets the number of folds used during full cross-validation "
      + "and tuned fold evaluation. This number will be automatically "
      + "reduced if there are insufficient positive examples.";
  }

  /**
   * Get the number of folds used for cross-validation.
   *
   * @return the number of folds used for cross-validation.
   */
  public int getNumXValFolds() {
    
    return m_NumXValFolds;
  }
  
  /**
   * Set the number of folds used for cross-validation.
   *
   * @param newNumFolds the number of folds used for cross-validation.
   */
  public void setNumXValFolds(int newNumFolds) {
    
    if (newNumFolds < 2) {
      throw new IllegalArgumentException("Number of folds must be greater than 1");
    }
    m_NumXValFolds = newNumFolds;
  }

  /**
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classifierTipText() {

    return "Sets the base Classifier to which the optimization "
      + "will be made.";
  }

  /**
   * Set the Classifier for which threshold is set. 
   *
   * @param newClassifier the Classifier to use.
   */
  public void setClassifier(Classifier newClassifier) {

    m_Classifier = newClassifier;
  }

  /**
   * Get the Classifier used as the classifier.
   *
   * @return the classifier used as the classifier
   */
  public Classifier getClassifier() {

    return m_Classifier;
  }
 
  /**
   * Gets the classifier specification string, which contains the class name of
   * the classifier and any options to the classifier
   *
   * @return the classifier string.
   */
  protected String getClassifierSpec() {
    
    Classifier c = getClassifier();
    if (c instanceof OptionHandler) {
      return c.getClass().getName() + " "
	+ Utils.joinOptions(((OptionHandler)c).getOptions());
    }
    return c.getClass().getName();
  }

  /**
   *  Returns the type of graph this classifier
   *  represents.
   */   
  public int graphType() {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graphType();
    else 
      return Drawable.NOT_DRAWABLE;
  }

  /**
   * Returns graph describing the classifier (if possible).
   *
   * @return the graph of the classifier in dotty format
   * @exception Exception if the classifier cannot be graphed
   */
  public String graph() throws Exception {
    
    if (m_Classifier instanceof Drawable)
      return ((Drawable)m_Classifier).graph();
    else throw new Exception("Classifier: " + getClassifierSpec()
			     + " cannot be graphed");
  }
 
  /**
   * Returns description of the cross-validated classifier.
   *
   * @return description of the cross-validated classifier as a string
   */
  public String toString() {

    if (m_BestValue == -Double.MAX_VALUE)
      return "ThresholdSelector: No model built yet.";

    String result = "Threshold Selector.\n"
    + "Classifier: " + m_Classifier.getClass().getName() + "\n";

    result += "Index of designated class: " + m_DesignatedClass + "\n";

    result += "Evaluation mode: ";
    switch (m_EvalMode) {
    case EVAL_CROSS_VALIDATION:
      result += m_NumXValFolds + "-fold cross-validation";
      break;
    case EVAL_TUNED_SPLIT:
      result += "tuning on 1/" + m_NumXValFolds + " of the data";
      break;
    case EVAL_TRAINING_SET:
    default:
      result += "tuning on the training data";
    }
    result += "\n";

    result += "Threshold: " + m_BestThreshold + "\n";
    result += "Best value: " + m_BestValue + "\n";
    if (m_RangeMode == RANGE_BOUNDS) {
      result += "Expanding range [" + m_LowThreshold + "," + m_HighThreshold
        + "] to [0, 1]\n";
    }
    result += m_Classifier.toString();
    return result;
  }
  
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {

    try {
      System.out.println(Evaluation.evaluateModel(new ThresholdSelector(), 
						  argv));
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
}
