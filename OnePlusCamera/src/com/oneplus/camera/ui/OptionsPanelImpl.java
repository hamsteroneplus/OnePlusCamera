package com.oneplus.camera.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.MainActivity;
import com.oneplus.camera.PhotoCaptureState;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.VideoCaptureState;
import com.oneplus.camera.media.MediaType;
import com.oneplus.camera.media.Resolution;
import com.oneplus.camera.media.ResolutionManager;
import com.oneplus.camera.scene.Scene;
import com.oneplus.camera.scene.SceneEventArgs;
import com.oneplus.camera.scene.SceneManager;

final class OptionsPanelImpl extends UIComponent implements OptionsPanel
{
	// Private fields.
	private View m_OptionsPanel;
	private ResolutionManager m_ResolutionManager;
	private final List<Item> m_SceneItems = new ArrayList<>();
	private ViewGroup m_SceneItemsContainer;
	private SceneManager m_SceneManager;
	private final List<Item> m_VideoResolutionItems = new ArrayList<>();
	private ViewGroup m_VideoResolutionItemsContainer;
	
	
	// Class for panel item.
	private final class Item
	{
		public ImageView iconImageView;
		public View itemView;
		public Object object;
		public TextView titleTextView;
		
		public Item(CameraActivity cameraActivity, Object obj)
		{
			Resources res = cameraActivity.getResources();
			LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(res.getDimensionPixelSize(R.dimen.options_panel_item_width), res.getDimensionPixelSize(R.dimen.options_panel_item_height));
			layoutParams.leftMargin = res.getDimensionPixelSize(R.dimen.options_panel_item_margin_left);
			layoutParams.rightMargin = res.getDimensionPixelSize(R.dimen.options_panel_item_margin_right);
			this.itemView = cameraActivity.getLayoutInflater().inflate(R.layout.layout_options_panel_item, null);
			this.itemView.setLayoutParams(layoutParams);
			this.itemView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onItemClicked(Item.this);
				}
			});
			this.iconImageView = (ImageView)this.itemView.findViewById(R.id.options_panel_item_image);
			this.titleTextView = (TextView)this.itemView.findViewById(R.id.options_panel_item_text);
			this.object = obj;
		}
	}
	
	
	// Constructor.
	OptionsPanelImpl(CameraActivity cameraActivity)
	{
		super("Options Panel", cameraActivity, false);
	}
	
	
	// Check item count.
	private void checkItemCount()
	{
		boolean hasItems = false;
		if(!m_SceneItems.isEmpty())
			hasItems = true;
		else if(this.getMediaType() == MediaType.VIDEO && !m_VideoResolutionItems.isEmpty())
			hasItems = true;
		this.setReadOnly(PROP_HAS_ITEMS, hasItems);
	}
	
	
	// Close options panel.
	@Override
	public void closeOptionsPanel(int flags)
	{
		this.verifyAccess();
		this.setViewVisibility(m_OptionsPanel, false);
		this.setReadOnly(PROP_IS_VISIBLE, false);
	}
	
	
	// Create item for resolution.
	private Item createResolutionItem(Resolution resolution)
	{
		// create item
		CameraActivity cameraActivity = this.getCameraActivity();
		Item item = new Item(cameraActivity, resolution);
		
		// select icon and title
		int iconResId = 0;
		int titleResId = 0;
		if(resolution.is4kVideo())
		{
			iconResId = R.drawable.options_panel_icon_4k_video;
			titleResId = R.string.resolution_video_2160p;
		}
		else if(resolution.is1080pVideo())
		{
			iconResId = R.drawable.options_panel_icon_1080p_video;
			titleResId = R.string.resolution_video_1080p;
		}
		else if(resolution.is720pVideo())
		{
			iconResId = R.drawable.options_panel_icon_720p_video;
			titleResId = R.string.resolution_video_720p;
		}
		if(iconResId != 0)
			item.iconImageView.setImageResource(iconResId);
		if(titleResId != 0)
			item.titleTextView.setText(titleResId);
		else
			item.titleTextView.setText(resolution.getMegaPixelsDescription());
		
		// complete
		return item;
	}
	
	
	// Initialize.
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// find components
		m_ResolutionManager = this.findComponent(ResolutionManager.class);
		m_SceneManager = this.findComponent(SceneManager.class);
		
		// setup layouts
		CameraActivity cameraActivity = this.getCameraActivity();
		m_OptionsPanel = ((MainActivity)cameraActivity).getCaptureUIContainer().findViewById(R.id.options_panel);
		m_SceneItemsContainer = (ViewGroup)m_OptionsPanel.findViewById(R.id.options_panel_scene_items_container);
		m_VideoResolutionItemsContainer = (ViewGroup)m_OptionsPanel.findViewById(R.id.options_panel_video_resolution_items_container);
		
		// add property changed call-backs.
		PropertyChangedCallback closePanelCallback = new PropertyChangedCallback()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey key, PropertyChangeEventArgs e)
			{
				closeOptionsPanel(0);
			}
		};
		cameraActivity.addCallback(CameraActivity.PROP_CAMERA, closePanelCallback);
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, new PropertyChangedCallback<MediaType>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<MediaType> key, PropertyChangeEventArgs<MediaType> e)
			{
				updateItemsContainerVisibility();
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_PHOTO_CAPTURE_STATE, new PropertyChangedCallback<PhotoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<PhotoCaptureState> key, PropertyChangeEventArgs<PhotoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case PREPARING:
					case READY:
						break;
					default:
						closeOptionsPanel(0);
						break;
				}
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_VIDEO_CAPTURE_STATE, new PropertyChangedCallback<VideoCaptureState>()
		{
			@Override
			public void onPropertyChanged(PropertySource source, PropertyKey<VideoCaptureState> key, PropertyChangeEventArgs<VideoCaptureState> e)
			{
				switch(e.getNewValue())
				{
					case PREPARING:
					case READY:
						break;
					default:
						closeOptionsPanel(0);
						break;
				}
			}
		});
		cameraActivity.addCallback(CameraActivity.PROP_MEDIA_TYPE, closePanelCallback);
		if(m_ResolutionManager != null)
		{
			m_ResolutionManager.addCallback(ResolutionManager.PROP_VIDEO_RESOLUTION, new PropertyChangedCallback<Resolution>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Resolution> key, PropertyChangeEventArgs<Resolution> e)
				{
					onVideoResolutionChanged(e.getNewValue());
				}
			});
			m_ResolutionManager.addCallback(ResolutionManager.PROP_VIDEO_RESOLUTION_LIST, new PropertyChangedCallback<List<Resolution>>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<List<Resolution>> key, PropertyChangeEventArgs<List<Resolution>> e)
				{
					onVideoResolutionListChanged(e.getNewValue());
				}
			});
		}
		else
			Log.e(TAG, "onInitialize() - No ResolutionManager");
		if(m_SceneManager != null)
		{
			m_SceneManager.addCallback(SceneManager.PROP_SCENE, new PropertyChangedCallback<Scene>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<Scene> key, PropertyChangeEventArgs<Scene> e)
				{
					onSceneChanged(e.getNewValue());
				}
			});
		}
		else
			Log.e(TAG, "onInitialize() - No SceneManager");
		
		// Add event handlers
		if(m_SceneManager != null)
		{
			m_SceneManager.addHandler(SceneManager.EVENT_SCENE_ADDED, new EventHandler<SceneEventArgs>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<SceneEventArgs> key, SceneEventArgs e)
				{
					onSceneAdded(e.getScene());
				}
			});
			m_SceneManager.addHandler(SceneManager.EVENT_SCENE_REMOVED, new EventHandler<SceneEventArgs>()
			{
				@Override
				public void onEventReceived(EventSource source, EventKey<SceneEventArgs> key, SceneEventArgs e)
				{
					onSceneRemoved(e.getScene());
				}
			});
		}
		
		// Setup scene items
		List<Scene> sceneList = (m_SceneManager != null ? m_SceneManager.get(SceneManager.PROP_SCENES) : null);
		if(sceneList != null)
		{
			for(int i = 0, count = sceneList.size() ; i < count ; ++i)
				this.onSceneAdded(sceneList.get(i));
		}
		
		// Setup resolution items
		if(m_ResolutionManager != null)
			this.onVideoResolutionListChanged(m_ResolutionManager.get(ResolutionManager.PROP_VIDEO_RESOLUTION_LIST));
		
		// Setup containers state
		this.updateItemsContainerVisibility();
	}
	
	
	// Called when item clicked.
	private void onItemClicked(Item item)
	{
		// check state
		CameraActivity cameraActivity = this.getCameraActivity();
		if(!cameraActivity.get(CameraActivity.PROP_IS_CAPTURE_UI_ENABLED))
			return;
		
		// set scene
		Object obj = item.object;
		if(obj instanceof Scene)
		{
			Scene scene = (Scene)obj;
			if(m_SceneManager != null)
			{
				if(m_SceneManager.get(SceneManager.PROP_SCENE) == scene)
					scene = Scene.NO_SCENE;
				if(!m_SceneManager.setScene(scene, 0))
					Log.e(TAG, "onItemClicked() - Fail to change scene to " + scene);
			}
			else
				Log.e(TAG, "onItemClicked() - No SceneManager");
			return;
		}
		
		// change resolution
		if(obj instanceof Resolution)
		{
			if(m_ResolutionManager != null)
				m_ResolutionManager.set(ResolutionManager.PROP_VIDEO_RESOLUTION, (Resolution)obj);
			return;
		}
	}
	
	
	// Called when scene added.
	private void onSceneAdded(Scene scene)
	{
		// find scene index
		int index = m_SceneManager.get(SceneManager.PROP_SCENES).indexOf(scene);
		if(index < 0)
			return;
		
		// create item
		Item item = new Item(this.getCameraActivity(), scene);
		
		// setup icon
		item.iconImageView.setImageDrawable(scene.getImage(Scene.ImageUsage.OPTIONS_PANEL_ICON));
		
		// setup title
		item.titleTextView.setText(scene.getDisplayName());
		
		// setup initial state
		this.updateItem(item, m_SceneManager.get(SceneManager.PROP_SCENE) == scene);
		
		// add item
		m_SceneItems.add(index, item);
		m_SceneItemsContainer.addView(item.itemView, index);
		this.addAutoRotateView(item.itemView);
		
		// update state
		this.checkItemCount();
	}
	
	
	// Called when scene changes.
	private void onSceneChanged(Scene scene)
	{
		for(int i = m_SceneItems.size() - 1 ; i >= 0 ; --i)
		{
			Item item = m_SceneItems.get(i);
			this.updateItem(item, item.object == scene);
		}
	}
	
	
	// Called when scene removed.
	private void onSceneRemoved(Scene scene)
	{
		for(int i = m_SceneItems.size() - 1 ; i >= 0 ; --i)
		{
			Item item = m_SceneItems.get(i);
			if(item.object == scene)
			{
				// remove item
				m_SceneItems.remove(i);
				m_SceneItemsContainer.removeView(item.itemView);
				this.removedAutoRotateView(item.itemView);
				
				// update state
				this.checkItemCount();
				break;
			}
		}
	}
	
	
	// Called when video resolution changed.
	private void onVideoResolutionChanged(Resolution resolution)
	{
		for(int i = m_VideoResolutionItems.size() - 1 ; i >= 0 ; --i)
		{
			Item item = m_VideoResolutionItems.get(i);
			this.updateItem(item, item.object.equals(resolution));
		}
	}
	
	
	// Called when video resolution list changed.
	private void onVideoResolutionListChanged(List<Resolution> resolutions)
	{
		// remove old items
		for(int i = m_VideoResolutionItems.size() - 1 ; i >= 0 ; --i)
		{
			Item item = m_VideoResolutionItems.get(i);
			m_VideoResolutionItemsContainer.removeView(item.itemView);
			this.removedAutoRotateView(item.itemView);
		}
		m_VideoResolutionItems.clear();
		
		// create new items
		Resolution resolution = m_ResolutionManager.get(ResolutionManager.PROP_VIDEO_RESOLUTION);
		for(int i = 0, count = resolutions.size() ; i < count ; ++i)
		{
			Item item = this.createResolutionItem(resolutions.get(i));
			int index = m_VideoResolutionItems.size();
			m_VideoResolutionItems.add(item);
			m_VideoResolutionItemsContainer.addView(item.itemView, index);
			this.updateItem(item, item.object.equals(resolution));
			this.addAutoRotateView(item.itemView);
		}
		
		// update item count
		this.checkItemCount();
	}
	
	
	// Show options panel.
	@Override
	public boolean openOptionsPanel(int flags)
	{
		// check state
		this.verifyAccess();
		if(!this.isRunningOrInitializing())
		{
			Log.e(TAG, "openOptionsPanel() - Component is not running");
			return false;
		}
		if(m_OptionsPanel == null)
		{
			Log.e(TAG, "openOptionsPanel() - No options panel layout");
			return false;
		}
		
		// show panel
		this.setViewVisibility(m_OptionsPanel, true);
		this.setReadOnly(PROP_IS_VISIBLE, true);
		
		// complete
		return true;
	}
	
	
	// Update items container state.
	private void updateItemsContainerVisibility()
	{
		if(m_VideoResolutionItemsContainer != null)
		{
			m_VideoResolutionItemsContainer.setVisibility(this.getMediaType() == MediaType.VIDEO ? View.VISIBLE : View.GONE);
			this.checkItemCount();
		}
	}
	
	
	// Update item state.
	private void updateItem(Item item, boolean isSelected)
	{
		if(isSelected)
		{
			item.iconImageView.setSelected(true);
			item.titleTextView.setTextAppearance(this.getCameraActivity(), R.style.OptionsPanelItemText_Selected);
		}
		else
		{
			item.iconImageView.setSelected(false);
			item.titleTextView.setTextAppearance(this.getCameraActivity(), R.style.OptionsPanelItemText);
		}
	}
}
