package com.oneplus.camera;

import java.util.HashSet;
import java.util.Hashtable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Message;
import android.preference.PreferenceManager;

import com.oneplus.base.EventKey;
import com.oneplus.base.HandlerBaseObject;
import com.oneplus.base.HandlerUtils;

/**
 * Application settings.
 */
public class Settings extends HandlerBaseObject
{
	/**
	 * Raised when one of values changes.
	 */
	public static final EventKey<SettingsValueChangedEventArgs> EVENT_VALUE_CHANGED = new EventKey<>("ValueChanged", SettingsValueChangedEventArgs.class, Settings.class);
	
	
	// Constants
	private static final int MSG_VALUE_CHANGED = 10000;
	
	
	// Private static fields
	private static final ThreadLocal<SharedPreferences> GLOBAL_PREFS = new ThreadLocal<>();
	private static final Hashtable<String, Object> GLOBAL_DEFAULT_VALUES = new Hashtable<>();
	private static final HashSet<String> PRIVATE_KEYS = new HashSet<>();
	
	
	// Private fields
	private final SharedPreferences m_GlobalPreferences;
	private final boolean m_IsVolatile;
	private final String m_Name;
	private final SharedPreferences.OnSharedPreferenceChangeListener m_PreferenceChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener()
	{
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			if(isDependencyThread())
				onValueChanged(key);
			else
				HandlerUtils.sendMessage(Settings.this, MSG_VALUE_CHANGED, 0, 0, key);
		}
	};
	private final Hashtable<String, Object> m_PrivateDefaultValues;
	private final SharedPreferences m_PrivatePreferences;
	private final Hashtable<String, Object> m_PrivateVolatileValues;
	
	
	/**
	 * Initialize new Settings instance.
	 * @param context Context.
	 * @param name Settings (preferences) name, Null means global settings.
	 * @param isVolatile Whether this is volatile settings or not.
	 */
	public Settings(Context context, String name, boolean isVolatile)
	{
		// call super
		super(true);
		
		// open global preferences
		SharedPreferences prefs = GLOBAL_PREFS.get();
		if(prefs == null)
		{
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
			GLOBAL_PREFS.set(prefs);
		}
		m_GlobalPreferences = prefs;
		
		// open private preferences
		if(name == null)
		{
			m_PrivatePreferences = m_GlobalPreferences;
			m_PrivateVolatileValues = null;
			m_PrivateDefaultValues = null;
		}
		else if(!isVolatile)
		{
			m_PrivatePreferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
			m_PrivateVolatileValues = null;
			m_PrivateDefaultValues = new Hashtable<>();
		}
		else
		{
			m_PrivatePreferences = null;
			m_PrivateVolatileValues = new Hashtable<>();
			m_PrivateDefaultValues = new Hashtable<>();
		}
		
		// setup listener
		m_GlobalPreferences.registerOnSharedPreferenceChangeListener(m_PreferenceChangedListener);
		if(m_PrivatePreferences != null && m_PrivatePreferences != m_GlobalPreferences)
			m_PrivatePreferences.registerOnSharedPreferenceChangeListener(m_PreferenceChangedListener);
		
		// save states
		m_Name = name;
		m_IsVolatile = isVolatile;
	}
	
	
	/**
	 * Add new private key.
	 * @param key Key to add.
	 */
	public static void addPrivateKey(String key)
	{
		synchronized(PRIVATE_KEYS)
		{
			PRIVATE_KEYS.add(key);
		}
	}
	
	
	/**
	 * Get default value.
	 * @param key Key.
	 * @return Default value.
	 */
	public Object getDefaultValue(String key)
	{
		// get private default value
		if(m_PrivateDefaultValues != null)
		{
			synchronized(m_PrivateDefaultValues)
			{
				Object value = m_PrivateDefaultValues.get(key);
				if(value != null)
					return value;
			}
		}
		
		// get global default value
		synchronized(GLOBAL_DEFAULT_VALUES)
		{
			Object value = GLOBAL_DEFAULT_VALUES.get(key);
			if(value != null)
				return value;
		}
		
		// no default value
		return null;
	}
	
	
	/**
	 * Get enumeration value.
	 * @param key Key.
	 * @param type Enumeration type.
	 * @return Value of given key, or default value if key does not exists.
	 */
	@SuppressWarnings("unchecked")
	public final <T extends Enum<T>> T getEnum(String key, Class<T> type)
	{
		// get default value
		Object obj = this.getDefaultValue(key);
		T defalutValue;
		if(obj != null && type == obj.getClass())
			defalutValue = (T)obj;
		else
		{
			T[] values = type.getEnumConstants();
			defalutValue = (values.length > 0 ? values[0] : null);
		}
		
		// get value
		return this.getEnum(key, type, defalutValue);
	}
	
	
	/**
	 * Get enumeration value.
	 * @param key Key.
	 * @param type Enumeration type.
	 *  @param defaultValue Default value.
	 * @return Value of given key, or default value if key does not exists.
	 */
	public final <T extends Enum<T>> T getEnum(String key, Class<T> type, T defalutValue)
	{
		// get string
		String defStringValue = (defalutValue != null ? defalutValue.toString() : null);
		String name = this.getString(key, defStringValue);
		
		// convert to constant
		if(name != null)
		{
			try
			{
				return Enum.valueOf(type, name);
			}
			catch(Throwable ex)
			{}
		}
		return defalutValue;
	}
	
	
	/**
	 * Get integer value.
	 * @param key Key.
	 * @return Value of given key, or default value if key does not exists.
	 */
	public final int getInt(String key)
	{
		Object obj = this.getDefaultValue(key);
		int defaultValue = (obj instanceof Integer ? (Integer)obj : 0);
		return this.getInt(key, defaultValue);
	}
	
	
	/**
	 * Get integer value.
	 * @param key Key.
	 * @param defaultValue Default value.
	 * @return Value of given key, or default value if key does not exists.
	 */
	public final int getInt(String key, int defaultValue)
	{
		synchronized(PRIVATE_KEYS)
		{
			if(!PRIVATE_KEYS.contains(key))
				return m_GlobalPreferences.getInt(key, defaultValue);
			if(!m_IsVolatile)
				return m_PrivatePreferences.getInt(key, defaultValue);
		}
		synchronized(m_PrivateVolatileValues)
		{
			Object value = m_PrivateVolatileValues.get(key);
			return (value instanceof Integer ? (Integer)value : defaultValue);
		}
	}
	
	
	/**
	 * Get string value.
	 * @param key Key.
	 * @return Value of given key, or default value if key does not exists.
	 */
	public final String getString(String key)
	{
		Object obj = this.getDefaultValue(key);
		String defaultValue = (obj != null ? obj.toString() : null);
		return this.getString(key, defaultValue);
	}
	
	
	/**
	 * Get string value.
	 * @param key Key.
	 * @param defaultValue Default value.
	 * @return Value of given key, or default value if key does not exists.
	 */
	public final String getString(String key, String defaultValue)
	{
		synchronized(PRIVATE_KEYS)
		{
			if(!PRIVATE_KEYS.contains(key))
				return m_GlobalPreferences.getString(key, defaultValue);
			if(!m_IsVolatile)
				return m_PrivatePreferences.getString(key, defaultValue);
		}
		synchronized(m_PrivateVolatileValues)
		{
			Object value = m_PrivateVolatileValues.get(key);
			return (value != null ? value.toString() : defaultValue);
		}
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			case MSG_VALUE_CHANGED:
				this.onValueChanged((String)msg.obj);
				break;
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when releasing.
	@Override
	protected void onRelease()
	{
		// remove listener
		m_GlobalPreferences.unregisterOnSharedPreferenceChangeListener(m_PreferenceChangedListener);
		if(m_PrivatePreferences != null && m_PrivatePreferences != m_GlobalPreferences)
			m_PrivatePreferences.unregisterOnSharedPreferenceChangeListener(m_PreferenceChangedListener);
		
		// call super
		super.onRelease();
	}
	
	
	// Called when value changes.
	private void onValueChanged(String key)
	{
		SettingsValueChangedEventArgs e = SettingsValueChangedEventArgs.obtain(key);
		this.raise(EVENT_VALUE_CHANGED, e);
		e.recycle();
	}
	
	
	/**
	 * Reset given key to default value.
	 * @param key Key to reset.
	 */
	public final void reset(String key)
	{
		this.set(key, null);
	}
	
	
	/**
	 * Set value.
	 * @param key Key.
	 * @param value New value, or Null to reset to default value.
	 */
	public final void set(String key, Object value)
	{
		synchronized(PRIVATE_KEYS)
		{
			if(!PRIVATE_KEYS.contains(key))
			{
				SharedPreferences.Editor editor = m_GlobalPreferences.edit();
				this.set(editor, key, value);
				editor.apply();
				return;
			}
			else if(!m_IsVolatile)
			{
				SharedPreferences.Editor editor = m_PrivatePreferences.edit();
				this.set(editor, key, value);
				editor.apply();
				return;
			}
		}
		synchronized(m_PrivateVolatileValues)
		{
			if(value instanceof Integer)
				m_PrivateVolatileValues.put(key, value);
			else if(value != null)
				m_PrivateVolatileValues.put(key, value.toString());
			else if(m_PrivateVolatileValues.contains(key))
				m_PrivateVolatileValues.remove(key);
			if(this.isDependencyThread())
				this.onValueChanged(key);
			else
				HandlerUtils.sendMessage(this, MSG_VALUE_CHANGED, 0, 0, key);
		}
	}
	
	
	// Set value to shared preferences.
	private void set(SharedPreferences.Editor editor, String key, Object value)
	{
		if(value instanceof Integer)
			editor.putInt(key, (Integer)value);
		else if(value != null)
			editor.putString(key, value.toString());
		else
			editor.remove(key);
	}
	
	
	/**
	 * Set default value.
	 * @param key Key.
	 * @param value Default value.
	 */
	public final void setDefaultValue(String key, Object value)
	{
		synchronized(m_PrivateDefaultValues)
		{
			m_PrivateDefaultValues.put(key, value);
		}
	}
	
	
	/**
	 * Set global default value.
	 * @param key Key.
	 * @param value Default value.
	 */
	public static void setGlobalDefaultValue(String key, Object value)
	{
		synchronized(GLOBAL_DEFAULT_VALUES)
		{
			GLOBAL_DEFAULT_VALUES.put(key, value);
		}
	}
	
	
	// Get string represents this settings.
	@Override
	public String toString()
	{
		if(m_Name != null)
			return (m_Name + "@" + this.hashCode());
		return ("(Global)@" + this.hashCode());
	}
}
