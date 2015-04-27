package com.oneplus.util;

import java.util.Comparator;
import java.util.List;

/**
 * Utility methods for {@link List}.
 */
public final class ListUtils
{
	// Constructor
	private ListUtils()
	{}
	
	
	/**
	 * Check whether given object is the last one in list or not.
	 * @param list List.
	 * @param obj Object to check.
	 * @return Whether given object is the last one in list or not.
	 */
	public static <T> boolean isLastObject(List<T> list, T obj)
	{
		if(list == null)
			return false;
		int size = list.size();
		if(size <= 0)
			return false;
		T lastObj = list.get(size - 1);
		if(lastObj != null)
			return lastObj.equals(obj);
		return (obj == null);
	}
	
	
	/**
	 * Find object in list which is nearest to given target.
	 * @param list List.
	 * @param target Target.
	 * @param comparator Comparator.
	 * @param mode Search mode.
	 * @return Nearest object, or Null if no object found.
	 */
	public static <T extends Comparable<T>> T findNearestObject(List<T> list, T target, SearchMode mode)
	{
		if(list == null)
			return null;
		T candicate = null;
		for(int i = list.size() - 1 ; i >= 0 ; --i)
		{
			T obj = list.get(i);
			int result = obj.compareTo(target);
			if(result == 0)
				return obj;
			if(result < 0)
			{
				if(mode == SearchMode.NEAREST_ABOVE_OR_EQUALS)
					continue;
				if(candicate == null || obj.compareTo(candicate) > 0)
					candicate = obj;
			}
			else
			{
				if(mode == SearchMode.NEAREST_BELOW_OR_EQUALS)
					continue;
				if(candicate == null || obj.compareTo(candicate) < 0)
					candicate = obj;
			}
		}
		return candicate;
	}
	
	
	/**
	 * Find object in list which is nearest to given target.
	 * @param list List.
	 * @param target Target.
	 * @param comparator Comparator.
	 * @param mode Search mode.
	 * @return Nearest object, or Null if no object found.
	 */
	public static <T> T findNearestObject(List<T> list, T target, Comparator<T> comparator, SearchMode mode)
	{
		if(list == null)
			return null;
		T candicate = null;
		for(int i = list.size() - 1 ; i >= 0 ; --i)
		{
			T obj = list.get(i);
			int result = comparator.compare(obj, target);
			if(result == 0)
				return obj;
			if(result < 0)
			{
				if(mode == SearchMode.NEAREST_ABOVE_OR_EQUALS)
					continue;
				if(candicate == null || comparator.compare(obj, candicate) > 0)
					candicate = obj;
			}
			else
			{
				if(mode == SearchMode.NEAREST_BELOW_OR_EQUALS)
					continue;
				if(candicate == null || comparator.compare(obj, candicate) < 0)
					candicate = obj;
			}
		}
		return candicate;
	}
}
