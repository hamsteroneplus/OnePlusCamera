package com.oneplus.base;

/**
 * Key to represent a property.
 * @param <TValue> Type of property value.
 */
public final class PropertyKey<TValue>
{
	/**
	 * Flag to indicate that property is read-only.
	 */
	public static final int FLAG_READONLY = 0x1;
	/**
	 * Flag to indicate that property value cannot be null.
	 */
	public static final int FLAG_NOT_NULL = 0x2;
	
	
	/**
	 * Default property value.
	 */
	public final TValue defaultValue;
	/**
	 * Property flags.
	 */
	public final int flags;
	/**
	 * Unique property ID.
	 */
	public final int id;
	/**
	 * Property name.
	 */
	public final String name;
	/**
	 * Type of object owns this property.
	 */
	public final Class<? extends PropertySource> ownerType;
	/**
	 * Type of property value.
	 */
	public final Class<TValue> valueType;
	
	
	// Private static fields
	private static volatile int m_NextId = 1;
	
	
	/**
	 * Initialize new PropertyKey instance with {@link #FLAG_NOT_NULL FLAG_NOT_NULL} | {@link #FLAG_READONLY FLAG_READONLY} flags.
	 * @param name Property name.
	 * @param valueType Type of property value.
	 * @param ownerType Type of object owns this property.
	 * @param defaultValue Default property value.
	 */
	public PropertyKey(String name, Class<TValue> valueType, Class<? extends PropertySource> ownerType, TValue defaultValue)
	{
		this(name, valueType, ownerType, FLAG_NOT_NULL | FLAG_READONLY, defaultValue);
	}
	
	
	/**
	 * Initialize new PropertyKey instance.
	 * @param name Property name.
	 * @param valueType Type of property value.
	 * @param ownerType Type of object owns this property.
	 * @param flags Property flags:
	 * <ul>
	 *   <li>{@link #FLAG_NOT_NULL FLAG_NOT_NULL}</li>
	 *   <li>{@link #FLAG_READONLY FLAG_READONLY}</li>
	 * </ul>
	 * @param defaultValue Default property value.
	 */
	public PropertyKey(String name, Class<TValue> valueType, Class<? extends PropertySource> ownerType, int flags, TValue defaultValue)
	{
		// check parameter
		if(name == null)
			throw new IllegalArgumentException("No property name.");
		if(valueType == null)
			throw new IllegalArgumentException("No value type.");
		if(ownerType == null)
			throw new IllegalArgumentException("No owner type.");
		if((flags & FLAG_NOT_NULL) != 0 && defaultValue == null)
			throw new IllegalArgumentException("Default value cannot be null.");
		
		// initialize
		this.defaultValue = defaultValue;
		this.flags = flags;
		this.id = generateId();
		this.name = name;
		this.ownerType = ownerType;
		this.valueType = valueType;
	}
	
	
	// Generate ID
	private static synchronized int generateId()
	{
		return (m_NextId++);
	}
	
	
	/**
	 * Check whether property is read-only or not.
	 * @return Whether property is read-only or not.
	 */
	public boolean isReadOnly()
	{
		return ((this.flags & FLAG_READONLY) != 0);
	}
	
	
	// Get string represents this property.
	@Override
	public String toString()
	{
		return this.name + "(id=" + this.id + ")";
	}
}
