package com.oneplus.camera;

import java.util.ArrayList;

import com.oneplus.camera.location.LocationManager;
import com.oneplus.camera.ui.menu.BooleanSettingsMenuItem;
import com.oneplus.camera.ui.menu.MenuItem;
import com.oneplus.camera.ui.menu.MenuListView;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity for advanced settings.
 */
public final class AdvancedSettingsActivity extends Activity
{
	/**
	 * Extra key to indicate whether settings is volatile or not.
	 */
	public static final String EXTRA_SETTINGS_IS_VOLATILE = "Settings.IsVolatile";
	/**
	 * Extra key for settings name.
	 */
	public static final String EXTRA_SETTINGS_NAME = "Settings.Name";
	
	
	// Private fields.
	private MenuItem m_LocationMenuItem;
	private MenuListView m_MenuListView;
	private Settings m_Settings;
	
	
	// Called when creating activity.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// call super
		super.onCreate(savedInstanceState);
		
		// set content view
		this.setContentView(R.layout.activity_adv_settings);
		m_MenuListView = (MenuListView)this.findViewById(R.id.adv_settings_menu_list_view);
		
		// load settings
		Bundle extras = this.getIntent().getExtras();
		m_Settings = new Settings(this, extras.getString(EXTRA_SETTINGS_NAME), extras.getBoolean(EXTRA_SETTINGS_IS_VOLATILE, false));
		
		// prepare menu items
		this.setupMenuItems();
	}
	
	
	// Setup menu items.
	private void setupMenuItems()
	{
		// prepare
		ArrayList<MenuItem> menuItems = new ArrayList<>();
		
		// location
		m_LocationMenuItem = new BooleanSettingsMenuItem(m_Settings, LocationManager.SETTINGS_KEY_SAVE_LOCATION);
		m_LocationMenuItem.set(MenuItem.PROP_TITLE, this.getString(R.string.adv_settings_location));
		menuItems.add(m_LocationMenuItem);
		
		// complete
		m_MenuListView.setMenuItems(menuItems);
	}
}