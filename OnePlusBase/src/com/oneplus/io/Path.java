package com.oneplus.io;

/**
 * Utility methods for file or directory path.
 */
public final class Path
{
	// Constructor
	private Path()
	{}
	
	
	/**
	 * Get parent directory path of given path.
	 * @param path File or directory path.
	 * @return Parent directory path.
	 */
	public static String getDirectoryPath(String path)
	{
		if(path == null)
			throw new IllegalArgumentException("No file or directory path.");
		for(int i = path.length() - 2 ; i > 0 ; --i)
		{
			if(path.charAt(i) == '/')
				return path.substring(0, i - 1);
		}
		return "/";
	}
	
	
	/**
	 * Get file or directory name from given path.
	 * @param path File or directory path.
	 * @return File or directory name.
	 */
	public static String getFileName(String path)
	{
		if(path == null)
			throw new IllegalArgumentException("No file or directory path.");
		for(int i = path.length() - 2 ; i > 0 ; --i)
		{
			if(path.charAt(i) == '/')
				return path.substring(i + 1);
		}
		return path;
	}
	
	/**
	 * Get file name from given path.
	 * @param path File.
	 * @return File name without extension.
	 */
	public static String getFileNameWithoutExtension(String path)
	{
		if(path == null)
			throw new IllegalArgumentException("No file or directory path.");
		return path.substring(path.lastIndexOf("/")+1 , path.lastIndexOf("."));
	}
}
