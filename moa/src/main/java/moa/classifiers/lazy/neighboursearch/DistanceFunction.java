/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    DistanceFunction.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;

/**
 * Interface for any class that can compute and return distances between two
 * instances.
 *
 * @author  Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 8034 $ 
 */
public interface DistanceFunction  {

  /**
   * Sets the instances.
   * 
   * @param insts 	the instances to use
   */
  public void setInstances(Instances insts);

  /**
   * returns the instances currently set.
   * 
   * @return 		the current instances
   */
  public Instances getInstances();

  /**
   * Sets the range of attributes to use in the calculation of the distance.
   * The indices start from 1, 'first' and 'last' are valid as well. 
   * E.g.: first-3,5,6-last
   * 
   * @param value	the new attribute index range
   */
  public void setAttributeIndices(String value);
  
  /**
   * Gets the range of attributes used in the calculation of the distance.
   * 
   * @return		the attribute index range
   */
  public String getAttributeIndices();
  
  /**
   * Sets whether the matching sense of attribute indices is inverted or not.
   * 
   * @param value	if true the matching sense is inverted
   */
  public void setInvertSelection(boolean value);
  
  /**
   * Gets whether the matching sense of attribute indices is inverted or not.
   * 
   * @return		true if the matching sense is inverted
   */
  public boolean getInvertSelection();

  /**
   * Calculates the distance between two instances.
   * 
   * @param first 	the first instance
   * @param second 	the second instance
   * @return 		the distance between the two given instances
   */
  public double distance(Instance first, Instance second);

  public void setDontNormalize(boolean normalize);

  /**
   * Calculates the distance between two instances. Offers speed up (if the 
   * distance function class in use supports it) in nearest neighbour search by 
   * taking into account the cutOff or maximum distance. Depending on the 
   * distance function class, post processing of the distances by 
   * postProcessDistances(double []) may be required if this function is used.
   *
   * @param first 	the first instance
   * @param second 	the second instance
   * @param cutOffValue If the distance being calculated becomes larger than 
   *                    cutOffValue then the rest of the calculation is 
   *                    discarded.
   * @return 		the distance between the two given instances or 
   * 			Double.POSITIVE_INFINITY if the distance being 
   * 			calculated becomes larger than cutOffValue. 
   */
  public double distance(Instance first, Instance second, double cutOffValue);

  public double difference(int index, double val1, double val2);

  public double sqDifference(int index, double val1, double val2);

  public double[][] getRanges() throws Exception;

  public void updateRanges(Instance instance);

  public double[][] updateRanges(Instance instance, double[][] ranges);

  public double[][] initializeRanges();

  public double[][] initializeRanges(int[] instList) throws Exception;

  public double[][] initializeRanges(int[] instList, int startIdx, int endIdx) throws Exception;

  /**
   * Does post processing of the distances (if necessary) returned by
   * distance(distance(Instance first, Instance second, double cutOffValue). It
   * may be necessary, depending on the distance function, to do post processing
   * to set the distances on the correct scale. Some distance function classes
   * may not return correct distances using the cutOffValue distance function to 
   * minimize the inaccuracies resulting from floating point comparison and 
   * manipulation.
   * 
   * @param distances	the distances to post-process
   */
  public void postProcessDistances(double distances[]);

  /**
   * Update the distance function (if necessary) for the newly added instance.
   * 
   * @param ins		the instance to add
   */
  public void update(Instance ins);

}
