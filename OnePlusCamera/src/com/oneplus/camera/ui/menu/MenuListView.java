package com.oneplus.camera.ui.menu;

import java.util.ArrayList;
import java.util.List;

import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * {@link ListView} to display {@link MenuItem}.
 */
public class MenuListView extends ListView
{
	// Private fields.
	private final Adapter m_Adapter;
	private int m_MenuItemViewResId = R.layout.layout_adv_settings_item;
	private List<MenuItem> m_MenuItems = new ArrayList<>();
	
	
	// Call-backs.
	private final PropertyChangedCallback<Boolean> m_IsCheckedChangedCallback = new PropertyChangedCallback<Boolean>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
		{
			onMenuItemCheckedChanged((MenuItem)source);
		}
	};
	private final PropertyChangedCallback<Boolean> m_IsEnabledChangedCallback = new PropertyChangedCallback<Boolean>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<Boolean> key, PropertyChangeEventArgs<Boolean> e)
		{
			m_Adapter.notifyDataSetInvalidated();
		}
	};
	private final PropertyChangedCallback<CharSequence> m_SummaryChangedCallback = new PropertyChangedCallback<CharSequence>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<CharSequence> key, PropertyChangeEventArgs<CharSequence> e)
		{
			m_Adapter.notifyDataSetInvalidated();
		}
	};
	private final PropertyChangedCallback<CharSequence> m_TitleChangedCallback = new PropertyChangedCallback<CharSequence>()
	{
		@Override
		public void onPropertyChanged(PropertySource source, PropertyKey<CharSequence> key, PropertyChangeEventArgs<CharSequence> e)
		{
			m_Adapter.notifyDataSetInvalidated();
		}
	};
	
	
	// Listeners.
	private final AdapterView.OnItemClickListener m_OnItemClickListener = new AdapterView.OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			onItemClicked(position);
		}
	};
	
	
	// Item adapter.
	private final class Adapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return m_MenuItems.size();
		}

		@Override
		public Object getItem(int position)
		{
			return m_MenuItems.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return m_MenuItems.get(position).hashCode();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			return getMenuItemView(position, convertView, parent);
		}
	}
	
	
	// Class to keep menu item information in related view.
	private static final class ViewInfo
	{
		public boolean isUpdatingViews;
		public MenuItem menuItem;
		public TextView summaryTextView;
		public Switch switchView;
		public TextView titleTextView;
	}
	
	
	/**
	 * Initialize new MenuListView instance.
	 * @param context Context.
	 * @param attrs Attributes.
	 */
	public MenuListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		m_Adapter = new Adapter();
		super.setAdapter(m_Adapter);
		super.setOnItemClickListener(m_OnItemClickListener);
	}
	
	
	// Attach to given menu item.
	private void attachToMenuItem(MenuItem menuItem)
	{
		if(menuItem != null)
		{
			menuItem.addCallback(MenuItem.PROP_IS_CHECKED, m_IsCheckedChangedCallback);
			menuItem.addCallback(MenuItem.PROP_IS_ENABLED, m_IsEnabledChangedCallback);
			menuItem.addCallback(MenuItem.PROP_SUMMARY, m_SummaryChangedCallback);
			menuItem.addCallback(MenuItem.PROP_TITLE, m_TitleChangedCallback);
		}
	}
	
	
	// Detach from given menu item.
	private void detachFromMenuItem(MenuItem menuItem)
	{
		if(menuItem != null)
		{
			menuItem.removeCallback(MenuItem.PROP_IS_CHECKED, m_IsCheckedChangedCallback);
			menuItem.removeCallback(MenuItem.PROP_IS_ENABLED, m_IsEnabledChangedCallback);
			menuItem.removeCallback(MenuItem.PROP_SUMMARY, m_SummaryChangedCallback);
			menuItem.removeCallback(MenuItem.PROP_TITLE, m_TitleChangedCallback);
		}
	}
	
	
	// Get view for menu item.
	private View getMenuItemView(int position, View convertView, ViewGroup parent)
	{
		// create view
		ViewInfo viewInfo;
		if(convertView == null)
		{
			convertView = View.inflate(this.getContext(), m_MenuItemViewResId, null);
			viewInfo = new ViewInfo();
			viewInfo.titleTextView = (TextView)convertView.findViewById(R.id.menu_item_title);
			viewInfo.summaryTextView = (TextView)convertView.findViewById(R.id.menu_item_summary);
			viewInfo.switchView = (Switch)convertView.findViewById(R.id.menu_item_switch);
			if(viewInfo.switchView != null)
			{
				final View itemView = convertView;
				viewInfo.switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
				{
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
					{
						ViewInfo viewInfo = (ViewInfo)itemView.getTag();
						if(!viewInfo.isUpdatingViews)
							onMenuItemSwitchChanged(viewInfo, isChecked);
					}
				});
			}
			convertView.setTag(viewInfo);
		}
		else
			viewInfo = (ViewInfo)convertView.getTag();
		
		// detach from previous menu item
		this.detachFromMenuItem(viewInfo.menuItem);
		
		// attach to new menu item
		MenuItem menuItem = m_MenuItems.get(position);
		viewInfo.menuItem = menuItem;
		this.attachToMenuItem(menuItem);
		
		// setup views
		viewInfo.isUpdatingViews = true;
		this.setupMenuItemSummary(viewInfo);
		this.setupMenuItemSwitch(viewInfo);
		this.setupMenuItemTitle(viewInfo);
		viewInfo.isUpdatingViews = false;
		
		// complete
		return convertView;
	}
	
	
	// Called when item clicked.
	private void onItemClicked(int position)
	{
		//
	}
	
	
	// Called when menu item checked state changes.
	private void onMenuItemCheckedChanged(MenuItem menuItem)
	{
		//
	}
	
	
	// Called when switch changes.
	private void onMenuItemSwitchChanged(ViewInfo viewInfo, boolean isChecked)
	{
		if(viewInfo.menuItem != null)
		{
			Boolean oldValue = viewInfo.menuItem.get(MenuItem.PROP_IS_CHECKED);
			if(oldValue != null)
				viewInfo.menuItem.set(MenuItem.PROP_IS_CHECKED, isChecked);
		}
	}
	
	
	// Set adapter
	@Override
	public void setAdapter(ListAdapter adapter)
	{
		throw new RuntimeException("Cannot change adapter.");
	}
	
	
	/**
	 * Set menu items.
	 * @param menuItems Menu items.
	 */
	public void setMenuItems(List<MenuItem> menuItems)
	{
		m_MenuItems.clear();
		if(menuItems != null)
			m_MenuItems.addAll(menuItems);
		m_Adapter.notifyDataSetChanged();
	}
	
	
	// Setup menu item summary.
	private void setupMenuItemSummary(ViewInfo viewInfo)
	{
		if(viewInfo.summaryTextView != null && viewInfo.menuItem != null)
		{
			CharSequence summary = viewInfo.menuItem.get(MenuItem.PROP_SUMMARY);
			if(summary == null)
				viewInfo.summaryTextView.setVisibility(View.GONE);
			else
			{
				viewInfo.summaryTextView.setVisibility(View.VISIBLE);
				viewInfo.summaryTextView.setText(summary);
			}
		}
	}
	
	
	// Setup menu item switch.
	private void setupMenuItemSwitch(ViewInfo viewInfo)
	{
		if(viewInfo.switchView != null && viewInfo.menuItem != null)
		{
			Boolean isChecked = viewInfo.menuItem.get(MenuItem.PROP_IS_CHECKED);
			if(isChecked == null)
				viewInfo.switchView.setVisibility(View.GONE);
			else
			{
				viewInfo.switchView.setVisibility(View.VISIBLE);
				viewInfo.switchView.setChecked(isChecked);
			}
		}
	}
	
	
	// Setup menu item title.
	private void setupMenuItemTitle(ViewInfo viewInfo)
	{
		if(viewInfo.titleTextView != null && viewInfo.menuItem != null)
			viewInfo.titleTextView.setText(viewInfo.menuItem.get(MenuItem.PROP_TITLE));
	}
}
