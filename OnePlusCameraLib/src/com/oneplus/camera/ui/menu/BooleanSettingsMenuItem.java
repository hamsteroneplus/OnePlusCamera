package com.oneplus.camera.ui.menu;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.Settings;

/**
 * Menu item for boolean settings.
 */
public class BooleanSettingsMenuItem extends MenuItem
{
	// Private fields.
	private final String m_Key;
	private final Settings m_Settings;
	
	
	/**
	 * Initialize new BooleanSettingsMenuItem instance.
	 * @param settings Settings.
	 * @param key Key of value.
	 */
	public BooleanSettingsMenuItem(Settings settings, String key)
	{
		m_Settings = settings;
		m_Key = key;
		this.set(PROP_IS_CHECKED, settings.getBoolean(key));
		this.addCallback(PROP_IS_CHECKED, new PropertyChangedCallback<Boolean>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
			{
				m_Settings.set(m_Key, e.getNewValue());
			}
		});
	}
}
