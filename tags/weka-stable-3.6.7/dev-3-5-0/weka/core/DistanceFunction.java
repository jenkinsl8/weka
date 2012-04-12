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
 *    DistanceFunction.java
 *    Copyright (C) 1999-2005 University of Waikato
 *
 */

package weka.core;

/**
 * Interface for any class that can compute and return distances between two
 * instances.
 *
 * @author  Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 1.4 $ 
 */
public interface DistanceFunction extends OptionHandler {
    
  /** Sets the instances */
  public void setInstances(Instances insts);
  
  /** returns the instances currently set */
  public Instances getInstances();

  /**
   * Calculates the distance between two instances.
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances,
   */
 public double distance(Instance first, Instance second) throws Exception;
 
  /**
   * Calculates the distance between two instances. Offers speed up (if the 
   * distance function class in use supports it) in nearest neighbour search by 
   * taking into account the cutOff or maximum distance. Depending on the 
   * distance function class, post processing of the distances by 
   * postProcessDistances(double []) may be required if this function is used.
   *
   * @param first the first instance
   * @param second the second instance
   * @param cutOffValue If the distance being calculated becomes larger than 
   *                    cutOffValue then the rest of the calculation is 
   *                    discarded.
   * @return the distance between the two given instances or Double.MAX_VALUE
   * if the distance being calculated becomes larger than cutOffValue. 
   */
  public double distance(Instance first, Instance second, 
                                   double cutOffValue) throws Exception;
  
  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * may be necessary, depending on the distance function, to do post processing
   * to set the distances on the correct scale. Some distance function classes
   * may not return correct distances using the cutOffValue distance function to 
   * minimize the inaccuracies resulting from floating point comparison and 
   * manipulation.
   */
  public void postProcessDistances(double distances[]);
  
  /**
   * Update the distance function (if necessary) for the newly added instance.
   */
  public void update(Instance ins) throws Exception;
  
}
