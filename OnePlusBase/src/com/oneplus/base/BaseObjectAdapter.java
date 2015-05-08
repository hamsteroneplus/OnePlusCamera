package com.oneplus.base;

/**
 * Adapter for object which implements {@link BaseObject} interface but cannot extends from {@link BasicBaseObject}.
 */
public final class BaseObjectAdapter extends BasicBaseObject
{
	/**
	 * Type of object owns this adapter.
	 */
	public final Class<?> ownerType;
	
	
	/**
	 * Initialize new BaseObjectAdapter instance.
	 * @param tag Log tag.
	 */
	public BaseObjectAdapter(Object owner, String tag)
	{
		super(tag);
		this.ownerType = owner.getClass();
	}
	
	
	// Check whether there is at least one call-back added to property or not.
	@Override
	public boolean hasCallbacks(PropertyKey<?> key)
	{
		return super.hasCallbacks(key);
	}
	
	
	// Check whether there is at least one handler added to event or not.
	@Override
	public boolean hasHandlers(EventKey<?> key)
	{
		return super.hasHandlers(key);
	}
	
	
	// Notify that value of given property has been changed.
	@Override
	public <TValue> boolean notifyPropertyChanged(PropertyKey<TValue> key, TValue oldValue, TValue newValue)
	{
		return super.notifyPropertyChanged(key, oldValue, newValue);
	}
	
	
	// Raise event.
	@Override
	public <TArgs extends EventArgs> void raise(EventKey<TArgs> key, TArgs e)
	{
		super.raise(key, e);
	}
	
	
	// Set read-only property value.
	@Override
	public <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		return super.setReadOnly(key, value);
	}
	
	
	// Throws exception if given event key is invalid.
	@Override
	protected void verifyEvent(EventKey<?> key)
	{
		if(!key.ownerType.isAssignableFrom(this.ownerType))
			throw new IllegalArgumentException("Event " + key + " is not owned by type " + this.ownerType + ".");
	}
	
	
	// Throws exception if given property key is invalid.
	@Override
	protected void verifyProperty(PropertyKey<?> key)
	{
		if(!key.ownerType.isAssignableFrom(this.ownerType))
			throw new IllegalArgumentException("Property " + key + " is not owned by type " + this.ownerType + ".");
	}
}
