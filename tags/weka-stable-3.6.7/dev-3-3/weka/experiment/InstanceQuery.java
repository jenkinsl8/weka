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
 *    InstanceQuery.java
 *    Copyright (C) 1999 Len Trigg
 *
 */


package weka.experiment;

import java.sql.*;
import java.net.InetAddress;
import java.util.*;
import java.math.*;
import java.io.*;

import weka.core.*;

/**
 * Convert the results of a database query into instances. The jdbc
 * driver and database to be used default to "jdbc.idbDriver" and
 * "jdbc:idb=experiments.prp". These may be changed by creating
 * a java properties file called .weka.experimentrc in user.home. eg:<p>
 *
 * <code><pre>
 * jdbcDriver=jdbc.idbDriver
 * jdbcURL=jdbc:idb=experiments.prp
 * </pre></code><p>
 *
 * Command line use just outputs the instances to System.out.
 *
 * @author Len Trigg (trigg@cs.waikato.ac.nz)
 * @version $Revision: 1.11 $
 */
public class InstanceQuery extends DatabaseUtils implements OptionHandler {

  /** Determines whether sparse data is created */
  boolean m_CreateSparseData = false;
  
  /** Query to execute */
  String m_Query = "SELECT * from ?";

  /**
   * Sets up the database drivers
   *
   * @exception Exception if an error occurs
   */
  public InstanceQuery() throws Exception {

    super();
  }

  /**
   * Returns an enumeration describing the available options <p>
   *
   */
   public Enumeration listOptions () {
     Vector newVector = new Vector(2);

     newVector.addElement(new Option("\tSQL query to execute.",
				     "Q",1,"-Q <query>"));
     newVector.addElement(new Option("\tReturn sparse rather than normal "
				    +"instances." 
				    , "S", 0, "-S"));
     return  newVector.elements();
   }

  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   * 
   * -S <br>
   * Return a set of sparse instances rather than normal instances.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions (String[] options)
    throws Exception
  {
    setSparseData(Utils.getFlag('S',options));

    String optionString;
    
    optionString = Utils.getOption('Q',options);
    if (optionString.length() != 0) {
      setQuery(optionString);
    } 
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String queryTipText() {
    return "The SQL query to execute against the database.";
  }
  
  /**
   * Set the query to execute against the database
   * @param q the query to execute
   */
  public void setQuery(String q) {
    m_Query = q;
  }

  /**
   * Get the query to execute against the database
   * @return the query
   */
  public String getQuery() {
    return m_Query;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String sparseDataTipText() {
    return "Encode data as sparse instances.";
  }

  /**
   * Sets whether data should be encoded as sparse instances
   * @param s true if data should be encoded as a set of sparse instances
   */
  public void setSparseData(boolean s) {
    m_CreateSparseData = s;
  }

  /**
   * Gets whether data is to be returned as a set of sparse instances
   * @return true if data is to be encoded as sparse instances
   */
  public boolean getSparseData() {
    return m_CreateSparseData;
  }

  /**
   * Gets the current settings of InstanceQuery
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {

    String[] options = new String[3];
    int current = 0;

    options[current] = "-Q"; options[current++] = getQuery();
 
    if (getSparseData()) {
      options[current++] = "-S";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    
    return  options;
  }

  /**
   * Makes a database query using the query set through the -Q option 
   * to convert a table into a set of instances
   *
   * @return the instances contained in the result of the query
   * @exception Exception if an error occurs
   */
  public Instances retrieveInstances() throws Exception {
    return retrieveInstances(m_Query);
  }

  /**
   * Makes a database query to convert a table into a set of instances
   *
   * @param query the query to convert to instances
   * @return the instances contained in the result of the query
   * @exception Exception if an error occurs
   */
  public Instances retrieveInstances(String query) throws Exception {

    System.err.println("Executing query: " + query);
    connectToDatabase();
    if (execute(query) == false) {
      throw new Exception("Query didn't produce results");
    }
    ResultSet rs = getResultSet();
    System.err.println("Getting metadata...");
    ResultSetMetaData md = rs.getMetaData();

    // Determine structure of the instances
    int numAttributes = md.getColumnCount();
    int [] attributeTypes = new int [numAttributes];
    Hashtable [] nominalIndexes = new Hashtable [numAttributes];
    FastVector [] nominalStrings = new FastVector [numAttributes];
    for (int i = 1; i <= numAttributes; i++) {
      switch (md.getColumnType(i)) {
      case Types.CHAR:
      case Types.VARCHAR:
      case Types.LONGVARCHAR:
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
	//System.err.println("String --> nominal");
	attributeTypes[i - 1] = Attribute.NOMINAL;
	nominalIndexes[i - 1] = new Hashtable();
	nominalStrings[i - 1] = new FastVector();
	break;
      case Types.BIT:
	////System.err.println("boolean --> nominal");
	attributeTypes[i - 1] = Attribute.NOMINAL;
	nominalIndexes[i - 1] = new Hashtable();
	nominalIndexes[i - 1].put("false", new Double(0));
	nominalIndexes[i - 1].put("true", new Double(1));
	nominalStrings[i - 1] = new FastVector();
	nominalStrings[i - 1].addElement("false");
	nominalStrings[i - 1].addElement("true");
	break;
      case Types.NUMERIC:
      case Types.DECIMAL:
	//System.err.println("BigDecimal --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.TINYINT:
	//System.err.println("byte --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.SMALLINT:
	//System.err.println("short --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.INTEGER:
	//System.err.println("int --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.BIGINT:
	//System.err.println("long --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.REAL:
	//System.err.println("float --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
      case Types.FLOAT:
      case Types.DOUBLE:
	//System.err.println("double --> numeric");
	attributeTypes[i - 1] = Attribute.NUMERIC;
	break;
	/*case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
	//System.err.println("byte[] --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break; */
      case Types.DATE:
	//System.err.println("Date --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      case Types.TIME:
	//System.err.println("Time --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      case Types.TIMESTAMP:
	//System.err.println("Time --> unsupported");
	attributeTypes[i - 1] = Attribute.STRING;
	break;
      default:
	//System.err.println("Unknown column type");
	attributeTypes[i - 1] = Attribute.STRING;
      }
    }

    // Step through the tuples
    System.err.println("Creating instances...");
    FastVector instances = new FastVector();
    int rowCount = 0;
    while(rs.next()) {
      if (rowCount % 100 == 0) {
	System.err.print("read " + rowCount + " instances \r");
	System.err.flush();
      }
      double[] vals = new double[numAttributes];
      for(int i = 1; i <= numAttributes; i++) {
	switch (md.getColumnType(i)) {
	case Types.CHAR:
	case Types.VARCHAR:
	case Types.LONGVARCHAR:
	case Types.BINARY:
	case Types.VARBINARY:
	case Types.LONGVARBINARY:
	  String str = rs.getString(i);
	  
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    Double index = (Double)nominalIndexes[i - 1].get(str);
	    if (index == null) {
	      index = new Double(nominalStrings[i - 1].size());
	      nominalIndexes[i - 1].put(str, index);
	      nominalStrings[i - 1].addElement(str);
	    }
	    vals[i - 1] = index.doubleValue();
	  }
	  break;
	case Types.BIT:
	  boolean boo = rs.getBoolean(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (boo ? 1.0 : 0.0);
	  }
	  break;
	case Types.NUMERIC:
	case Types.DECIMAL:
	  //	  BigDecimal bd = rs.getBigDecimal(i, 4); 
	  double dd = rs.getDouble(i);
	  // Use the column precision instead of 4?
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    //	    newInst.setValue(i - 1, bd.doubleValue());
	    vals[i - 1] =  dd;
	  }
	  break;
	case Types.TINYINT:
	  byte by = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)by;
	  }
	  break;
	case Types.SMALLINT:
	  short sh = rs.getByte(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)sh;
	  }
	  break;
	case Types.INTEGER:
	  int in = rs.getInt(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)in;
	  }
	  break;
	case Types.BIGINT:
	  long lo = rs.getLong(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)lo;
	  }
	  break;
	case Types.REAL:
	  float fl = rs.getFloat(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)fl;
	  }
	  break;
	case Types.FLOAT:
	case Types.DOUBLE:
	  double dou = rs.getDouble(i);
	  if (rs.wasNull()) {
	    vals[i - 1] = Instance.missingValue();
	  } else {
	    vals[i - 1] = (double)dou;
	  }
	  break;
	  /*case Types.BINARY:
	case Types.VARBINARY:
	case Types.LONGVARBINARY: */
	case Types.DATE:
	case Types.TIME:
	case Types.TIMESTAMP:
	default:
	  vals[i - 1] = Instance.missingValue();
	}
      }
      Instance newInst;
      if (m_CreateSparseData) {
	newInst = new SparseInstance(1.0, vals);
      } else {
	newInst = new Instance(1.0, vals);
      }
      instances.addElement(newInst);
      rowCount++;
    }
    //disconnectFromDatabase();  (perhaps other queries might be made)
    
    // Create the header and add the instances to the dataset
    System.err.println("Creating header...");
    FastVector attribInfo = new FastVector();
    for (int i = 0; i < numAttributes; i++) {
      String attribName = md.getColumnName(i + 1);
      switch (attributeTypes[i]) {
      case Attribute.NOMINAL:
	attribInfo.addElement(new Attribute(attribName, nominalStrings[i]));
	break;
      case Attribute.NUMERIC:
	attribInfo.addElement(new Attribute(attribName));
	break;
      case Attribute.STRING:
	attribInfo.addElement(new Attribute(attribName, null));
	break;
      default:
	throw new Exception("Unknown attribute type");
      }
    }
    Instances result = new Instances("QueryResult", attribInfo, 
				     instances.size());
    for (int i = 0; i < instances.size(); i++) {
      result.add((Instance)instances.elementAt(i));
    }
    rs.close();
    return result;
  }

  /**
   * Test the class from the command line. The instance
   * query should be specified with -Q sql_query
   *
   * @param args contains options for the instance query
   */
  public static void main(String args[]) {

    try {
      InstanceQuery iq = new InstanceQuery();
      String query = Utils.getOption('Q', args);
      if (query.length() == 0) {
	iq.setQuery("select * from Experiment_index");
      } else {
	iq.setQuery(query);
      }
      iq.setOptions(args);
      try {
	Utils.checkForRemainingOptions(args);
      } catch (Exception e) {
	System.err.println("Options for weka.experiment.InstanceQuery:\n");
	Enumeration en = iq.listOptions();
	while (en.hasMoreElements()) {
	  Option o = (Option)en.nextElement();
	  System.err.println(o.synopsis()+"\n"+o.description());
	}
	System.exit(1);
      }
     
      Instances aha = iq.retrieveInstances();
      iq.disconnectFromDatabase();
      // The dataset may be large, so to make things easier we'll
      // output an instance at a time (rather than having to convert
      // the entire dataset to one large string)
      System.out.println(new Instances(aha, 0));
      for (int i = 0; i < aha.numInstances(); i++) {
	System.out.println(aha.instance(i));
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
}
