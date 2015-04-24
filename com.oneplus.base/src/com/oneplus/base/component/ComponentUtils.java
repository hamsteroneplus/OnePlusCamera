package com.oneplus.base.component;

import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.EventSource;
import com.oneplus.base.HandlerObject;
import com.oneplus.base.Log;

/**
 * Utility methods for {@link Component} related operations.
 */
public final class ComponentUtils
{
	// Constants
	private static final String TAG = "ComponentUtils";
	
	
	// Constructor
	private ComponentUtils()
	{}
	
	
	// Call back to given ComponentSearchCallback.
	private static <TComponent extends Component> void callComponentSearchCallback(HandlerObject callbackTarget, final ComponentSearchCallback<TComponent> callback, final TComponent component)
	{
		if(component != null && callback != null)
		{
			if(callbackTarget == null || callbackTarget.isDependencyThread())
				callback.onComponentFound(component);
			else if(!HandlerUtils.post(callbackTarget, new Runnable()
			{
				@Override
				public void run()
				{
					callback.onComponentFound(component);
				}
			}))
			{
				Log.e(TAG, "callComponentSearchCallback() - Fail to perform cross-thread call-back");
			}
		}
	}
	
	
	/**
	 * Find component in given owner, and call-back to given Handler.
	 * @param owner Component owner.
	 * @param componentType Type extended or implemented.
	 * @param callbackTarget {@link HandlerObject} for call-back, or Null to call-back from owner thread.
	 * @param callback Call-back.
	 * @return Whether component is found immediately (synchronously) or not.
	 */
	public static <TComponent extends Component> boolean findComponent(final ComponentOwner owner, final Class<TComponent> componentType, final HandlerObject callbackTarget, final ComponentSearchCallback<TComponent> callback)
	{
		// find component directly
		TComponent component = owner.findComponent(componentType);
		if(component != null)
		{
			callComponentSearchCallback(callbackTarget, callback, component);
			return (callbackTarget == null || callbackTarget.isDependencyThread());
		}
		
		// check call-back
		if(callback == null)
			return false;
		
		// add event handler
		if(owner.isDependencyThread())
		{
			owner.addHandler(ComponentOwner.EVENT_COMPONENT_ADDED, new EventHandler<ComponentEventArgs<Component>>()
			{
				@SuppressWarnings("unchecked")
				@Override
				public void onEventReceived(EventSource source, EventKey<ComponentEventArgs<Component>> key, ComponentEventArgs<Component> e)
				{
					Component newComponent = e.getComponent();
					if(componentType.isAssignableFrom(newComponent.getClass()))
					{
						owner.removeHandler(ComponentOwner.EVENT_COMPONENT_ADDED, this);
						callComponentSearchCallback(callbackTarget, callback, (TComponent)newComponent);
					}
				}
			});
		}
		else if(!HandlerUtils.post(owner, new Runnable()
		{
			@Override
			public void run()
			{
				findComponent(owner, componentType, callbackTarget, callback);
			}
		}))
		{
			Log.e(TAG, "findComponent() - Fail to find component in owner thread");
		}
		
		// complete
		return false;
	}
}
