package com.oneplus.util;

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
}
