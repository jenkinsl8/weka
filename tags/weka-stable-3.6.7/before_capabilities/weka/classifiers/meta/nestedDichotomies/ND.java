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
 *    ND.java
 *    Copyright (C) 2003-2005 University of Waikato
 *
 */

package weka.classifiers.meta.nestedDichotomies;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Utils;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.Randomizable;
import weka.core.FastVector;
import weka.core.UnsupportedClassTypeException;

import weka.filters.Filter;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.instance.RemoveWithValues;

import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.rules.ZeroR;

import java.util.Random;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.io.Serializable;

/**
 * Class that represents and builds a random system of
 * nested dichotomies. For details, check<p>
 *
 * Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of
 * Balanced Nested Dichotomies for Multi-Class Problems. PKDD,
 * Porto. Springer-Verlag.
 *
 * <p>and<p>
 * 
 * Eibe Frank and Stefan Kramer (2004). Ensembles of Nested
 * Dichotomies for Multi-class Problems. Proceedings of the
 * International Conference on Machine Learning, Banff. Morgan
 * Kaufmann.
 *
 * @author Eibe Frank
 * @author Lin Dong
 */
public class ND extends RandomizableSingleClassifierEnhancer {
  
  protected class NDTree implements Serializable {
    
    /** The indices associated with this node */
    protected FastVector m_indices = null;
    
    /** The parent */
    protected NDTree m_parent = null;
    
    /** The left successor */
    protected NDTree m_left = null;
    
    /** The right successor */
    protected NDTree m_right = null;
    
    /**
     * Constructor.
     */
    protected NDTree() {
      
      m_indices = new FastVector(1);
      m_indices.addElement(new Integer(Integer.MAX_VALUE));
    }
    
    /**
     * Locates the node with the given index (depth-first traversal).
     */
    protected NDTree locateNode(int nodeIndex, int[] currentIndex) {
      
      if (nodeIndex == currentIndex[0]) {
	return this;
      } else if (m_left == null) {
	return null;
      } else {
	currentIndex[0]++;
	NDTree leftresult = m_left.locateNode(nodeIndex, currentIndex);
	if (leftresult != null) {
	  return leftresult;
	} else {
	  currentIndex[0]++;
	  return m_right.locateNode(nodeIndex, currentIndex);
	}
      }
    }
      
    /**
     * Inserts a class index into the tree. 
     */
    protected void insertClassIndex(int classIndex) {

      // Create new nodes
      NDTree right = new NDTree();
      if (m_left != null) {
	m_right.m_parent = right;
	m_left.m_parent = right;
	right.m_right = m_right;
	right.m_left = m_left;
      }
      m_right = right;
      m_right.m_indices = (FastVector)m_indices.copy();
      m_right.m_parent = this;
      m_left = new NDTree();
      m_left.insertClassIndexAtNode(classIndex);
      m_left.m_parent = this; 

      // Propagate class Index
      propagateClassIndex(classIndex);
    }

    /**
     * Propagates class index to the root.
     */
    protected void propagateClassIndex(int classIndex) {

      insertClassIndexAtNode(classIndex);
      if (m_parent != null) {
	m_parent.propagateClassIndex(classIndex);
      }
    }
    
    /**
     * Inserts the class index at a given node.
     */
    protected void insertClassIndexAtNode(int classIndex) {

      int i = 0;
      while (classIndex > ((Integer)m_indices.elementAt(i)).intValue()) {
	i++;
      }
      m_indices.insertElementAt(new Integer(classIndex), i);
    }

    /**
     * Gets the indices in an array of ints.
     */
    protected int[] getIndices() {

      int[] ints = new int[m_indices.size() - 1];
      for (int i = 0; i < m_indices.size() - 1; i++) {
	ints[i] = ((Integer)m_indices.elementAt(i)).intValue();
      }
      return ints;
    }

    /**
     * Checks whether an index is in the array.
     */
    protected boolean contains(int index) {

      for (int i = 0; i < m_indices.size() - 1; i++) {
	if (index == ((Integer)m_indices.elementAt(i)).intValue()) {
	  return true;
	}
      }
      return false;
    }

    /**
     * Returns the list of indices as a string.
     */
    protected String getString() {

      StringBuffer string = new StringBuffer();
      for (int i = 0; i < m_indices.size() - 1; i++) {
	if (i > 0) {
	  string.append(',');
	}
	string.append(((Integer)m_indices.elementAt(i)).intValue() + 1);
      }
      return string.toString();
    }

    /**
     * Unifies tree for improve hashing.
     */
    protected void unifyTree() {

      if (m_left != null) {
        if (((Integer)m_left.m_indices.elementAt(0)).intValue() >
            ((Integer)m_right.m_indices.elementAt(0)).intValue()) {
          NDTree temp = m_left;
          m_left = m_right;
          m_right = temp;
        }
        m_left.unifyTree();
        m_right.unifyTree();
      }
    }

    /**
     * Returns a description of the tree rooted at this node.
     */
    protected void toString(StringBuffer text, int[] id, int level) {

      for (int i = 0; i < level; i++) {
	text.append("   | ");
      }
      text.append(id[0] + ": " + getString() + "\n");
      if (m_left != null) {
	id[0]++;
	m_left.toString(text, id, level + 1);
	id[0]++;
	m_right.toString(text, id, level + 1);
      }
    }
  }

  /** The tree of classes */
  protected NDTree m_ndtree = null;
  
  /** The hashtable containing all the classifiers */
  protected Hashtable m_classifiers = null;

  /** Is Hashtable given from END? */
  protected boolean m_hashtablegiven = false;
    
  /**
   * Constructor.
   */
  public ND() {
    
    m_Classifier = new weka.classifiers.trees.J48();
  }
  
  /**
   * String describing default classifier.
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.trees.J48";
  }

  /**
   * Set hashtable from END.
   */
  public void setHashtable(Hashtable table) {

    m_hashtablegiven = true;
    m_classifiers = table;
  }

  /**
   * Builds the classifier.
   */
  public void buildClassifier(Instances data) throws Exception {

    // Check for non-nominal classes
    if (!data.classAttribute().isNominal()) {
      throw new UnsupportedClassTypeException("ND: class must be nominal!");
    }

    data.deleteWithMissingClass();

    if (data.numInstances() == 0) {
      throw new Exception("No instances in training file!");
    }

    Random random = data.getRandomNumberGenerator(m_Seed);

    if (!m_hashtablegiven) {
      m_classifiers = new Hashtable();
    }

    // Generate random class hierarchy
    int[] indices = new int[data.numClasses()];
    for (int i = 0; i < indices.length; i++) {
      indices[i] = i;
    }

    // Randomize list of class indices
    for (int i = indices.length - 1; i > 0; i--) {
      int help = indices[i];
      int index = random.nextInt(i + 1);
      indices[i] = indices[index];
      indices[index] = help;
    }

    // Insert random class index at randomly chosen node
    m_ndtree = new NDTree();
    m_ndtree.insertClassIndexAtNode(indices[0]);
    for (int i = 1; i < indices.length; i++) {
      int nodeIndex = random.nextInt(2 * i - 1);
     
      NDTree node = m_ndtree.locateNode(nodeIndex, new int[1]);
      node.insertClassIndex(indices[i]);
    }
    m_ndtree.unifyTree();
    

    // Build classifiers
    buildClassifierForNode(m_ndtree, data);
  }

  /**
   * Builds the classifier for one node.
   */
  public void buildClassifierForNode(NDTree node, Instances data) throws Exception {

    // Are we at a leaf node ?
    if (node.m_left != null) {
      
      // Create classifier
      MakeIndicator filter = new MakeIndicator();
      filter.setAttributeIndex("" + (data.classIndex() + 1));
      filter.setValueIndices(node.m_right.getString());
      filter.setNumeric(false);
      filter.setInputFormat(data);
      FilteredClassifier classifier = new FilteredClassifier();
      if (data.numInstances() > 0) {
	classifier.setClassifier(Classifier.makeCopies(m_Classifier, 1)[0]);
      } else {
	classifier.setClassifier(new ZeroR());
      }
      classifier.setFilter(filter);
      
      if (!m_classifiers.containsKey(node.m_left.getString() + "|" + node.m_right.getString())) {
	classifier.buildClassifier(data);
	m_classifiers.put(node.m_left.getString() + "|" + node.m_right.getString(), classifier);
      } else {
	classifier=(FilteredClassifier)m_classifiers.get(node.m_left.getString() + "|" + 
							 node.m_right.getString());
      }
      
      // Generate successors
      if (node.m_left.m_left != null) {
        RemoveWithValues rwv = new RemoveWithValues();
        rwv.setInvertSelection(true);
        rwv.setNominalIndices(node.m_left.getString());
        rwv.setAttributeIndex("" + (data.classIndex() + 1));
        rwv.setInputFormat(data);
        Instances firstSubset = Filter.useFilter(data, rwv);
        buildClassifierForNode(node.m_left, firstSubset);
      }
      if (node.m_right.m_left != null) {
        RemoveWithValues rwv = new RemoveWithValues();
        rwv.setInvertSelection(true);
        rwv.setNominalIndices(node.m_right.getString());
        rwv.setAttributeIndex("" + (data.classIndex() + 1));
        rwv.setInputFormat(data);
        Instances secondSubset = Filter.useFilter(data, rwv);
        buildClassifierForNode(node.m_right, secondSubset);
      }
    }
  }
    
  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
   */
  public double[] distributionForInstance(Instance inst) throws Exception {
	
    return distributionForInstance(inst, m_ndtree);
  }

  /**
   * Predicts the class distribution for a given instance
   *
   * @param inst the (multi-class) instance to be classified
   */
  protected double[] distributionForInstance(Instance inst, NDTree node) throws Exception {

    double[] newDist = new double[inst.numClasses()];
    if (node.m_left == null) {
      int[] indices = node.getIndices();
      newDist[node.getIndices()[0]] = 1.0;
      return newDist;
    } else {
      Classifier classifier = (Classifier)m_classifiers.get(node.m_left.getString() + "|" +
							    node.m_right.getString());
      double[] leftDist = distributionForInstance(inst, node.m_left);
      double[] rightDist = distributionForInstance(inst, node.m_right);
      double[] dist = classifier.distributionForInstance(inst);

      for (int i = 0; i < inst.numClasses(); i++) {
	if (node.m_right.contains(i)) {
	  newDist[i] = dist[1] * rightDist[i];
	} else {
	  newDist[i] = dist[0] * leftDist[i];
	}
      }
      return newDist;
    }
  }

  /**
   * Outputs the classifier as a string.
   */
  public String toString() {
	
    if (m_classifiers == null) {
      return "ND: No model built yet.";
    }
    StringBuffer text = new StringBuffer();
    text.append("ND\n\n");
    m_ndtree.toString(text, new int[1], 0);
	
    return text.toString();
  }
	
  /**
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
	    
    return "A meta classifier for handling multi-class datasets with 2-class "
      + "classifiers by building a random tree structure. For more info, check\n\n"
      + "Lin Dong, Eibe Frank, and Stefan Kramer (2005). Ensembles of "
      + "Balanced Nested Dichotomies for Multi-Class Problems. PKDD, Porto. Springer-Verlag\n\nand\n\n"
      + "Eibe Frank and Stefan Kramer (2004). Ensembles of Nested Dichotomies for Multi-class Problems. "
      + "Proceedings of the International Conference on Machine Learning, Banff. Morgan Kaufmann.";
  }
    
  /**
   * Main method for testing this class.
   *
   * @param argv the options
   */
  public static void main(String [] argv) {
	
    try {
      System.out.println(Evaluation.evaluateModel(new ND(), argv));
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
