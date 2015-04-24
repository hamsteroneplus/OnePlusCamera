package com.oneplus.camera;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.oneplus.base.BaseThread;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.PropertyChangeEventArgs;
import com.oneplus.base.PropertyChangedCallback;
import com.oneplus.base.PropertyKey;
import com.oneplus.base.PropertySource;
import com.oneplus.base.component.Component;
import com.oneplus.base.component.ComponentCreationPriority;
import com.oneplus.base.component.ComponentManager;
import com.oneplus.base.component.ComponentOwner;

/**
 * Camera access and control thread.
 */
public class CameraThread extends BaseThread implements ComponentOwner
{
	// Default component builders
	private static final CameraThreadComponentBuilder[] DEFAULT_COMPONENT_BUILDERS = new CameraThreadComponentBuilder[]{
		new CameraDeviceManagerBuilder(),
	};
	
	
	// Constants
	//
	
	
	/**
	 * Property for available camera list.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static final PropertyKey<List<Camera>> PROP_AVAILABLE_CAMERAS = new PropertyKey<List<Camera>>("AvailableCameras", (Class)List.class, CameraThread.class, Collections.EMPTY_LIST);
	
	
	// Private fields
	private final Context m_Context;
	private CameraDeviceManager m_CameraDeviceManager;
	private volatile ComponentManager m_ComponentManager;
	private final List<CameraThreadComponentBuilder> m_InitialComponentBuilders = new ArrayList<>();
	
	
	/**
	 * Initialize new CameraThread instance.
	 * @param context Related {@link android.content.Context Context}.
	 * @param callback Call-back when camera thread starts.
	 * @param callbackHandler Handler for call-back.
	 */
	public CameraThread(Context context, ThreadStartCallback callback, Handler callbackHandler)
	{
		super("Camera Thread", callback, callbackHandler);
		if(context == null)
			throw new IllegalArgumentException("No context.");
		m_Context = context;
	}
	
	
	/**
	 * Add component builders to camera thread.
	 * @param builders Component builders to add.
	 */
	public final void addComponentBuilders(final CameraThreadComponentBuilder[] builders)
	{
		if(this.isDependencyThread())
			m_ComponentManager.addComponentBuilders(builders, this);
		else
		{
			synchronized(this)
			{
				if(m_ComponentManager != null)
				{
					HandlerUtils.post(this, new Runnable()
					{
						@Override
						public void run()
						{
							m_ComponentManager.addComponentBuilders(builders, CameraThread.this);
						}
					});
				}
				else
					m_InitialComponentBuilders.addAll(Arrays.asList(builders));
			}
		}
	}
	
	
	// Bind to components.
	private boolean bindToComponents()
	{
		// bind to CameraDeviceManager
		m_CameraDeviceManager = m_ComponentManager.findComponent(CameraDeviceManager.class);
		if(m_CameraDeviceManager != null)
		{
			m_CameraDeviceManager.addCallback(CameraDeviceManager.PROP_AVAILABLE_CAMERAS, new PropertyChangedCallback<List<Camera>>()
			{
				@Override
				public void onPropertyChanged(PropertySource source, PropertyKey<List<Camera>> key, PropertyChangeEventArgs<List<Camera>> e)
				{
					onAvailableCamerasChanged(e.getNewValue());
				}
			});
		}
		else
		{
			Log.e(TAG, "bindToComponents() - No CameraDeviceManager");
			return false;
		}
		
		// complete
		return true;
	}
	
	
	/**
	 * Get related context.
	 * @return {@link android.content.Context Context}.
	 */
	public final Context getContext()
	{
		return m_Context;
	}
	
	
	// Find component extends or implements given type.
	@Override
	public <TComponent extends Component> TComponent findComponent(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponent(componentType, this);
		return null;
	}
	
	
	// Find all components extend or implement given type.
	@SuppressWarnings("unchecked")
	@Override
	public <TComponent extends Component> TComponent[] findComponents(Class<TComponent> componentType)
	{
		if(m_ComponentManager != null)
			return m_ComponentManager.findComponents(componentType, this);
		return (TComponent[])new Component[0];
	}
	
	
	// Handle message.
	@Override
	protected void handleMessage(Message msg)
	{
		switch(msg.what)
		{
			//
				
			default:
				super.handleMessage(msg);
				break;
		}
	}
	
	
	// Called when available camera list changes.
	private void onAvailableCamerasChanged(List<Camera> cameras)
	{
		this.setReadOnly(PROP_AVAILABLE_CAMERAS, cameras);
	}
	
	
	// Called when thread starts.
	@Override
	protected void onStarted()
	{
		// call super
		super.onStarted();
		
		// create component with HIGH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.HIGH, this);
		
		// bind to components
		if(!this.bindToComponents())
			throw new RuntimeException("Fail to bind components.");
	}
	
	
	// Called when starting thread.
	@Override
	protected void onStarting()
	{
		// call super
		super.onStarting();
		
		// create component manager
		synchronized(this)
		{
			m_ComponentManager = new ComponentManager();
			m_ComponentManager.addComponentBuilders(DEFAULT_COMPONENT_BUILDERS, this);
			if(!m_InitialComponentBuilders.isEmpty())
			{
				CameraThreadComponentBuilder[] builders = new CameraThreadComponentBuilder[m_InitialComponentBuilders.size()];
				m_InitialComponentBuilders.toArray(builders);
				m_InitialComponentBuilders.clear();
				m_ComponentManager.addComponentBuilders(builders, this);
			}
		}
		
		// create component with LAUNCH priority
		m_ComponentManager.createComponents(ComponentCreationPriority.LAUNCH, this);
	}
	
	
	// Release and remove given component.
	@Override
	public void removeComponent(Component component)
	{
		this.verifyAccess();
		m_ComponentManager.removeComponent(component);
	}
}
