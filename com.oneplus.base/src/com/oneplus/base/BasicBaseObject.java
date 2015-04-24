package com.oneplus.base;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import android.util.SparseArray;

/**
 * Basic implementation of {@link BaseObject} interface.
 */
public abstract class BasicBaseObject extends BasicThreadDependentObject implements BaseObject
{
	/**
	 * Flag to print log when property value changes.
	 */
	public static final int LOG_PROPERTY_CHANGE = 0x1;
	/**
	 * Flag to print log when property call-back changes.
	 */
	public static final int LOG_PROPERTY_CALLBACK_CHANGE = 0x1;
	/**
	 * Flag to print log when calling property changed call-backs.
	 */
	public static final int LOG_PROPERTY_CALLBACK = 0x4;
	/**
	 * Flag to print log when event raises.
	 */
	public static final int LOG_EVENT_RAISE = 0x100;
	/**
	 * Flag to print log when event handler changes.
	 */
	public static final int LOG_EVENT_HANDLER_CHANGE = 0x200;
	/**
	 * Flag to print log when calling event handler.
	 */
	public static final int LOG_EVENT_HANDLER = 0x400;
	
	
	// Private fields
	private final SparseArray<Event> m_Events = new SparseArray<>();
	private volatile boolean m_IsReleased;
	private final SparseArray<Property> m_Properties = new SparseArray<>();
	
	
	// Class for property.
	private final static class Property
	{
		public List<PropertyChangedCallback<?>> addingCallbacks;
		public List<PropertyChangedCallback<?>> callbacks;
		public volatile boolean hasValue;
		public final PropertyKey<?> key;
		public int logFlags;
		public List<PropertyChangedCallback<?>> removingCallbacks;
		public int updatingCounter;
		public volatile Object value;
		public int version;
		
		public Property(PropertyKey<?> key)
		{
			this.key = key;
		}
	}
	
	
	// Class for event
	private static final class Event
	{
		public List<EventHandler<?>> addingHandlers;
		public List<EventHandler<?>> handlers;
		//public final EventKey<?> key;
		public int logFlags;
		public int raisingCounter;
		public List<EventHandler<?>> removingHandlers;
		
		public Event(EventKey<?> key)
		{
			//this.key = key;
		}
	}
	
	
	/**
	 * Initialize new BasicBaseObject instance.
	 */
	protected BasicBaseObject()
	{}
	
	
	/**
	 * Initialize new BasicBaseObject instance.
	 * @param tag Log tag.
	 */
	protected BasicBaseObject(String tag)
	{
		super(tag);
	}
	
	
	// Add property changed call-back.
	@Override
	public <TValue> void addCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		// check parameter and state
		if(callback == null)
			throw new IllegalArgumentException("No call-back.");
		this.verifyAccess();
		if(m_IsReleased)
			return;
		
		// get property
		Property property = m_Properties.get(key.id);
		if(property == null)
		{
			property = new Property(key);
			m_Properties.put(key.id, property);
		}
		
		// add call-back
		if(property.updatingCounter <= 0)
		{
			if(property.callbacks == null)
				property.callbacks = new ArrayList<>();
			if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
				this.printPropertyLog(Log.DEBUG, property, "Add call-back [" + property.callbacks.size() + "] " + callback);
			property.callbacks.add(callback);
		}
		else
		{
			if(property.removingCallbacks != null && property.removingCallbacks.remove(callback))
			{
				if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
					this.printPropertyLog(Log.DEBUG, property, "Cancel deferred removing call-back " + callback);
				return;
			}
			if(property.addingCallbacks == null)
				property.addingCallbacks = new ArrayList<>();
			if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
				this.printPropertyLog(Log.DEBUG, property, "Create deferred adding call-back " + callback);
			property.addingCallbacks.add(callback);
		}
	}
	
	
	// Add event handler.
	@Override
	public <TArgs extends EventArgs> void addHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		// check state and parameter
		this.verifyAccess();
		if(handler == null)
			throw new IllegalArgumentException("No handler.");
		if(m_IsReleased)
			return;
		
		// find event
		Event event = m_Events.get(key.id);
		if(event == null)
		{
			event = new Event(key);
			m_Events.put(key.id, event);
		}
		
		// add handler
		if(event.raisingCounter <= 0)
		{
			if(event.handlers == null)
				event.handlers = new ArrayList<>();
			if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
				this.printEventLog(Log.DEBUG, key, "Add handler [" + event.handlers.size() + "] " + handler);
			event.handlers.add(handler);
		}
		else
		{
			if(event.removingHandlers != null && event.removingHandlers.remove(handler))
			{
				if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
					this.printEventLog(Log.DEBUG, key, "Cancel deferred removing handler " + handler);
				return;
			}
			if(event.addingHandlers == null)
				event.addingHandlers = new ArrayList<>();
			if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
				this.printEventLog(Log.DEBUG, key, "Create deferred adding handler " + handler);
			event.addingHandlers.add(handler);
		}
	}
	
	
	// Check whether value changes or not.
	private boolean checkValueChanges(Object oldValue, Object newValue)
	{
		if(oldValue != null)
			return !oldValue.equals(newValue);
		return (newValue != null);
	}
	
	
	/**
	 * Disable logs related to given event.
	 * @param key Event key.
	 * @param logs Logs to disable.
	 */
	public final void disableEventLogs(EventKey<?> key, int logs)
	{
		// check state
		this.verifyAccess();
		
		// enable logs
		Event event = m_Events.get(key.id);
		if(event != null)
			event.logFlags &= ~logs;
	}
	
	
	/**
	 * Disable logs related to given property.
	 * @param key Property key.
	 * @param logs Logs to disable.
	 */
	public final void disablePropertyLogs(PropertyKey<?> key, int logs)
	{
		// check state
		this.verifyAccess();
		
		// enable logs
		Property property = m_Properties.get(key.id);
		if(property != null)
			property.logFlags &= ~logs;
	}
	
	
	/**
	 * Enable logs related to given event.
	 * @param key Event key.
	 * @param logs Logs to enable.
	 */
	public final void enableEventLogs(EventKey<?> key, int logs)
	{
		// check state
		this.verifyAccess();
		
		// enable logs
		Event event = m_Events.get(key.id);
		if(event == null)
		{
			event = new Event(key);
			m_Events.put(key.id, event);
		}
		event.logFlags |= logs;
	}
	
	
	/**
	 * Enable logs related to given property.
	 * @param key Property key.
	 * @param logs Logs to enable.
	 */
	public final void enablePropertyLogs(PropertyKey<?> key, int logs)
	{
		// check state
		this.verifyAccess();
		
		// enable logs
		Property property = m_Properties.get(key.id);
		if(property == null)
		{
			property = new Property(key);
			m_Properties.put(key.id, property);
		}
		property.logFlags |= logs;
	}
	
	
	// Get property value.
	@SuppressWarnings("unchecked")
	@Override
	public <TValue> TValue get(PropertyKey<TValue> key)
	{
		if(key == PROP_IS_RELEASED)
			return (TValue)(Boolean)m_IsReleased;
		else
		{
			Property property = m_Properties.get(key.id);
			while(property != null && property.key != key)
				property = m_Properties.get(key.id);
			if(property != null && property.hasValue)
				return (TValue)property.value;
			return key.defaultValue;
		}
	}
	
	
	/**
	 * Notify that value of given property has been changed.
	 * @param key Property key.
	 * @param oldValue Old property value.
	 * @param newValue New property value.
	 * @return Whether property value changes or not.
	 */
	protected <TValue> boolean notifyPropertyChanged(PropertyKey<TValue> key, TValue oldValue, TValue newValue)
	{
		this.verifyAccess();
		Property property = m_Properties.get(key.id);
		if(property != null)
			return this.notifyPropertyChanged(property, oldValue, newValue);
		return this.checkValueChanges(oldValue, newValue);
	}
	
	
	// Notify that value of given property has been changed.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean notifyPropertyChanged(Property property, Object oldValue, Object newValue)
	{
		// check values
		if(!this.checkValueChanges(oldValue, newValue))
			return false;
		
		// update version
		++property.version;
		
		// call-backs
		boolean result = true;
		int logFlags = property.logFlags;
		boolean printCallbackChangeLog = ((logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0);
		++property.updatingCounter;
		try
		{
			// print log
			if((logFlags & LOG_PROPERTY_CHANGE) != 0)
				this.printPropertyLog(Log.DEBUG, property, oldValue + " -> " + newValue);
			
			// call-back
			List<PropertyChangedCallback<?>> callbacks = property.callbacks;
			if(callbacks != null && !callbacks.isEmpty())
			{
				int version = property.version;
				PropertyChangeEventArgs<?> e = PropertyChangeEventArgs.obtain(oldValue, newValue);
				PropertyKey<?> key = property.key;
				boolean printCallbackLog = ((logFlags & LOG_PROPERTY_CALLBACK) != 0);
				for(int i = 0, count = callbacks.size() ; i < count ; ++i)
				{
					// call-back
					PropertyChangedCallback<?> callback = callbacks.get(i);
					if(printCallbackLog)
						this.printPropertyLog(Log.DEBUG, key, "Call [" + i + "] " + callback);
					callback.onPropertyChanged(this, (PropertyKey)key, (PropertyChangeEventArgs)e);
					
					// check version
					if(version != property.version)
					{
						if((logFlags & LOG_PROPERTY_CHANGE) != 0)
							this.printPropertyLog(Log.WARN, key, "Value changed after calling call-back [" + i + "] " + callback);
						result = this.checkValueChanges(oldValue, this.get(key));
						break;
					}
				}
				e.recycle();
			}
			
			// complete
			return result;
		}
		finally
		{
			// restore state
			--property.updatingCounter;
			
			// add/remove deferred call-backs
			if(property.updatingCounter <= 0)
			{
				// remove call-backs
				if(property.removingCallbacks != null)
				{
					if(property.callbacks != null)
					{
						for(int i = property.removingCallbacks.size() - 1 ; i >= 0 ; --i)
						{
							PropertyChangedCallback<?> callback = property.removingCallbacks.get(i);
							int index = property.callbacks.indexOf(callback);
							if(index >= 0)
							{
								if(printCallbackChangeLog)
									this.printPropertyLog(Log.DEBUG, property, "Remove deferred removing call-back [" + index + "] " + callback);
								property.callbacks.remove(index);
							}
						}
					}
					property.removingCallbacks = null;
				}
				
				// add call-backs
				if(property.addingCallbacks != null)
				{
					if(!property.addingCallbacks.isEmpty())
					{
						if(property.callbacks == null)
							property.callbacks = new ArrayList<>();
						for(int i = 0, count = property.addingCallbacks.size() ; i < count ; ++i)
						{
							PropertyChangedCallback<?> callback = property.addingCallbacks.get(i);
							if(printCallbackChangeLog)
								this.printPropertyLog(Log.DEBUG, property, "Add deferred adding call-back [" + property.callbacks.size() + "] " + callback);
							property.callbacks.add(callback);
						}
					}
					property.addingCallbacks = null;
				}
			}
		}
	}
	
	
	/**
	 * Called when releasing object.
	 */
	protected void onRelease()
	{}
	
	
	// Print event related log
	private void printEventLog(int priority, EventKey<?> key, String message)
	{
		Log.println(priority, TAG, "[Event] " + key + " : " + message);
	}
	
	
	// Print property related log
	private void printPropertyLog(int priority, Property property, String message)
	{
		this.printPropertyLog(priority, property.key, message);
	}
	private void printPropertyLog(int priority, PropertyKey<?> key, String message)
	{
		Log.println(priority, TAG, "[Property] " + key + " : " + message);
	}
	
	
	/**
	 * Raise event.
	 * @param key Key of event to raise.
	 * @param e Event data.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <TArgs extends EventArgs> void raise(EventKey<TArgs> key, TArgs e)
	{
		// check key
		this.verifyEvent(key);
		
		// check state
		this.verifyAccess();
		if(m_IsReleased)
			return;
		
		// raise event
		Event event = m_Events.get(key.id);
		if(event != null)
		{
			++event.raisingCounter;
			int logFlags = event.logFlags;
			try
			{
				boolean printRaiseLog = ((logFlags & LOG_EVENT_RAISE) != 0);
				List<EventHandler<?>> handlers = event.handlers;
				if(printRaiseLog)
					this.printEventLog(Log.DEBUG, key, "Raise [start]");
				if(handlers != null && !handlers.isEmpty())
				{
					boolean printHandlerLog = ((logFlags & LOG_EVENT_HANDLER) != 0);
					for(int i = 0, count = handlers.size() ; i < count ; ++i)
					{
						EventHandler handler = handlers.get(i);
						if(printHandlerLog)
							this.printEventLog(Log.DEBUG, key, "Call [" + i + "] " + handler);
						handler.onEventReceived(this, key, e);
					}
				}
				if(printRaiseLog)
					this.printEventLog(Log.DEBUG, key, "Raise [end]");
			}
			finally
			{
				// restore state
				--event.raisingCounter;
				
				// add/remove deferred handlers
				if(event.raisingCounter <= 0)
				{
					// remove handlers
					boolean printHandlerChangeLog = ((logFlags & LOG_EVENT_HANDLER_CHANGE) != 0);
					if(event.removingHandlers != null)
					{
						if(event.handlers != null)
						{
							for(int i = event.removingHandlers.size() - 1 ; i >= 0 ; --i)
							{
								EventHandler<?> handler = event.removingHandlers.get(i);
								int index = event.handlers.indexOf(handler);
								if(index >= 0)
								{
									if(printHandlerChangeLog)
										this.printEventLog(Log.DEBUG, key, "Remove deferred removing handler [" + index + "] " + handler);
									event.handlers.remove(index);
								}
							}
						}
						event.removingHandlers = null;
					}
					
					// add handlers
					if(event.addingHandlers != null)
					{
						if(!event.addingHandlers.isEmpty())
						{
							if(event.handlers == null)
								event.handlers = new ArrayList<>();
							for(int i = 0, count = event.addingHandlers.size() ; i < count ; ++i)
							{
								EventHandler<?> handler = event.addingHandlers.get(i);
								if(printHandlerChangeLog)
									this.printEventLog(Log.DEBUG, key, "Add deferred adding handler [" + event.handlers.size() + "] " + handler);
								event.handlers.add(handler);
							}
						}
						event.addingHandlers = null;
					}
				}
			}
		}
	}
	
	
	// Release object.
	@Override
	public final void release()
	{
		// check state
		this.verifyAccess();
		if(m_IsReleased)
			return;
		
		// release
		this.onRelease();
		
		// clear property changed call-backs
		for(int i = m_Properties.size() - 1 ; i >= 0 ; --i)
		{
			Property property = m_Properties.valueAt(i);
			property.addingCallbacks = null;
			property.removingCallbacks = null;
			property.callbacks = null;
		}
		
		// clear event handlers
		for(int i = m_Events.size() - 1 ; i >= 0 ; --i)
		{
			Event event = m_Events.valueAt(i);
			event.addingHandlers = null;
			event.removingHandlers = null;
			event.handlers = null;
		}
		
		// update state
		m_IsReleased = true;
	}
	
	
	// Remove property changed call-back
	@Override
	public <TValue> void removeCallback(PropertyKey<TValue> key, PropertyChangedCallback<TValue> callback)
	{
		// check parameter and state
		if(callback == null)
			return;
		this.verifyAccess();
		if(m_IsReleased)
			return;
		
		// get property
		Property property = m_Properties.get(key.id);
		if(property == null)
			return;
		
		// remove call-back
		if(property.updatingCounter <= 0)
		{
			if(property.callbacks != null)
			{
				int index = property.callbacks.indexOf(callback);
				if(index >= 0)
				{
					if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
						this.printPropertyLog(Log.DEBUG, property, "Remove call-back [" + index + "] " + callback);
					property.callbacks.remove(index);
				}
			}
		}
		else
		{
			if(property.addingCallbacks != null && property.addingCallbacks.remove(callback))
			{
				if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
					this.printPropertyLog(Log.DEBUG, property, "Cancel deferred adding call-back " + callback);
				return;
			}
			if(property.removingCallbacks == null)
				property.removingCallbacks = new ArrayList<>();
			if((property.logFlags & LOG_PROPERTY_CALLBACK_CHANGE) != 0)
				this.printPropertyLog(Log.DEBUG, property, "Create deferred removing call-back " + callback);
			property.removingCallbacks.add(callback);
		}
	}
	
	
	// Remove event handler.
	@Override
	public <TArgs extends EventArgs> void removeHandler(EventKey<TArgs> key, EventHandler<TArgs> handler)
	{
		// check state and parameter
		this.verifyAccess();
		if(handler == null || m_IsReleased)
			return;
		
		// find event
		Event event = m_Events.get(key.id);
		if(event == null)
			return;
		
		// remove handler
		if(event.raisingCounter <= 0)
		{
			if(event.handlers != null)
			{
				int index = event.handlers.indexOf(handler);
				if(index >= 0)
				{
					if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
						this.printEventLog(Log.DEBUG, key, "Remove handler [" + index + "] " + handler);
					event.handlers.remove(index);
				}
			}
		}
		else
		{
			if(event.addingHandlers != null && event.addingHandlers.remove(handler))
			{
				if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
					this.printEventLog(Log.DEBUG, key, "Cancel deferred adding handler " + handler);
				return;
			}
			if(event.removingHandlers == null)
				event.removingHandlers = new ArrayList<>();
			if((event.logFlags & LOG_EVENT_HANDLER_CHANGE) != 0)
				this.printEventLog(Log.DEBUG, key, "Create deferred removing handler " + handler);
			event.removingHandlers.add(handler);
		}
	}
	
	
	// Set property value
	@Override
	public <TValue> boolean set(PropertyKey<TValue> key, TValue value)
	{
		if(key.isReadOnly())
			throw new RuntimeException("Property " + key + " is read-only.");
		return this.setInternal(key, value);
	}
	
	
	// Set property value
	@SuppressWarnings("unchecked")
	private <TValue> boolean setInternal(PropertyKey<TValue> key, TValue value)
	{
		// check key
		this.verifyProperty(key);
		
		// find property
		this.verifyAccess();
		TValue oldValue;
		Property property = m_Properties.get(key.id);
		if(property != null)
			oldValue = (TValue)(property.hasValue ? property.value : property.key.defaultValue);
		else
		{
			property = new Property(key);
			m_Properties.put(key.id, property);
			oldValue = key.defaultValue;
		}
		
		// set value
		property.hasValue = true;
		property.value = value;
		
		// notify change
		return this.notifyPropertyChanged(property, oldValue, value);
	}
	
	
	/**
	 * Set read-only property value.
	 * @param key Property key.
	 * @param value New value to set.
	 * @return Whether property value changes or not.
	 */
	protected <TValue> boolean setReadOnly(PropertyKey<TValue> key, TValue value)
	{
		if(key == PROP_IS_RELEASED)
			throw new IllegalArgumentException("Cannot set property " + key + ".");
		return this.setInternal(key, value);
	}
	
	
	/**
	 * Throws exception if given event key is invalid.
	 * @param key Event key to check.
	 */
	protected void verifyEvent(EventKey<?> key)
	{
		if(!key.ownerType.isAssignableFrom(this.getClass()))
			throw new IllegalArgumentException("Event " + key + " is not owned by type " + this.getClass() + ".");
	}
	
	
	/**
	 * Throws exception if given property key is invalid.
	 * @param key Property key to check.
	 */
	protected void verifyProperty(PropertyKey<?> key)
	{
		if(!key.ownerType.isAssignableFrom(this.getClass()))
			throw new IllegalArgumentException("Property " + key + " is not owned by type " + this.getClass() + ".");
	}
	
	
	/**
	 * Throws {@link RuntimeException} if object is released.
	 */
	protected final void verifyReleaseState()
	{
		if(m_IsReleased)
			throw new RuntimeException("Object has been released.");
	}
}
