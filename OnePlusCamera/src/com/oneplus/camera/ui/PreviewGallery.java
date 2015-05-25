package com.oneplus.camera.ui;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.ViewPager.PageTransformer;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.Rotation;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.io.FileManager;
import com.oneplus.camera.io.FileManager.PhotoCallback;
import com.oneplus.camera.media.MediaEventArgs;
import com.oneplus.camera.widget.RotateRelativeLayout;

final class PreviewGallery extends UIComponent {
	// Constants
	static private final int MESSAGE_UPDATE_RESET = 1000;
	static private final int MESSAGE_UPDATE_ADDED = 1001;
	static private final int MESSAGE_UPDATE_DELETED = 1002;

	// Private fields
	private RotateRelativeLayout m_PreviewGallery;
	private ViewPager m_ViewPager;
	private VerticalViewPager m_VerticalViewPager;
	private PreviewPagerAdapter m_Adapter, m_VerticalAdapter;
	private FileManager m_FileManager;
	private int m_OrignalZ, m_PreviousPosition;
	//
	static final private int PAGE_OFFSET = 2, TARGET = PAGE_OFFSET+1;;

	// Constructor
	PreviewGallery(CameraActivity cameraActivity) {
		super("Preview Gallery", cameraActivity, true);
	}

	// Handle message.
	@Override
	protected void handleMessage(Message msg) {
		if (m_ViewPager == null)
			return;
		switch (msg.what) {
		case MESSAGE_UPDATE_DELETED: {
			File file = (File) (msg.obj);
			m_ViewPager.setAdapter(null);
			m_VerticalViewPager.setAdapter(null);
			m_Adapter.resetCache();
			m_VerticalAdapter.resetCache();
			m_Adapter.deleteFile(file);
			m_VerticalAdapter.deleteFile(file);
			m_ViewPager.setAdapter(m_Adapter);
			m_VerticalViewPager.setAdapter(m_VerticalAdapter);
			bringToBack();
			break;
		}
		case MESSAGE_UPDATE_RESET: {
			m_ViewPager.setAdapter(null);
			m_VerticalViewPager.setAdapter(null);
			m_Adapter.initialize(PreviewGallery.this);
			m_VerticalAdapter.initialize(PreviewGallery.this);
			m_Adapter.resetCache();
			m_VerticalAdapter.resetCache();
			m_PreviousPosition = 0;
			m_ViewPager.setAdapter(m_Adapter);
			m_VerticalViewPager.setAdapter(m_VerticalAdapter);
			bringToBack();
			break;
		}
		case MESSAGE_UPDATE_ADDED: {
			File file = new File((String) (msg.obj));
			int current = m_ViewPager.getCurrentItem();
			m_Adapter.addFile(file);
			m_VerticalAdapter.addFile(file);
			if (current != 0) {
				m_PreviousPosition = current + 1;
			} else{
				m_PreviousPosition = 0;
			}
			if (Rotation.PORTRAIT == getRotation() || Rotation.INVERSE_PORTRAIT == getRotation()){
				m_ViewPager.setCurrentItem(m_PreviousPosition);
			}else{
				m_VerticalViewPager.setCurrentItem(m_PreviousPosition);
			}
			break;
		}
		default:
			super.handleMessage(msg);
			break;
		}
	}

	// Initialize.
	@Override
	protected void onInitialize() {
		// call super
		super.onInitialize();

		// setup UI
		final CameraActivity cameraActivity = getCameraActivity();
		m_PreviewGallery = (RotateRelativeLayout) cameraActivity.findViewById(R.id.preview_gallery);

		ViewGroup parent = ((ViewGroup) m_PreviewGallery.getParent());
		for (int index = 0; index < parent.getChildCount(); index++) {
			if (parent.getChildAt(index).getId() == R.id.preview_gallery) {
				m_OrignalZ = index;
			}
		}

		initPager(getCameraActivity());

	}

	/*
	 * @see
	 * com.oneplus.camera.UIComponent#onRotationChanged(com.oneplus.base.Rotation
	 * , com.oneplus.base.Rotation)
	 */
	@Override
	protected void onRotationChanged(Rotation prevRotation, Rotation newRotation) {
		super.onRotationChanged(prevRotation, newRotation);

		if (Rotation.PORTRAIT == newRotation || Rotation.INVERSE_PORTRAIT == newRotation) {
			if (Rotation.LANDSCAPE == prevRotation || Rotation.INVERSE_LANDSCAPE == prevRotation){
				m_VerticalViewPager.setVisibility(View.INVISIBLE);	
				m_PreviousPosition = m_VerticalViewPager.getCurrentItem();
				m_ViewPager.setCurrentItem(m_PreviousPosition, true);
				preFetch(m_Adapter, m_PreviousPosition);
				m_ViewPager.setVisibility(View.VISIBLE);
			}
			//
			m_PreviewGallery.setRotation(newRotation);
		} else {
			if (Rotation.PORTRAIT == prevRotation || Rotation.INVERSE_PORTRAIT == prevRotation){
				m_ViewPager.setVisibility(View.INVISIBLE);
				m_VerticalViewPager.setVisibility(View.VISIBLE);
				m_PreviousPosition = m_ViewPager.getCurrentItem();
				m_VerticalViewPager.setCurrentItem(m_PreviousPosition);
				preFetch(m_VerticalAdapter, m_PreviousPosition);
			}
			//
			if (Rotation.LANDSCAPE == newRotation) {
				m_PreviewGallery.setRotation(Rotation.PORTRAIT);
			} else {
				m_PreviewGallery.setRotation(Rotation.INVERSE_PORTRAIT);
			}
		}
	}

	void initPager(final CameraActivity cameraActivity) {
		// find components
		ComponentUtils.findComponent(getCameraThread(), FileManager.class, this, new ComponentSearchCallback<FileManager>() {

			@Override
			public void onComponentFound(FileManager component) {
				Log.d(TAG, "onComponentFound");
				m_FileManager = component;
				HandlerUtils.post(m_FileManager, new Runnable() {

					@Override
					public void run() {
						m_FileManager.addHandler(FileManager.EVENT_MEDIA_FILES_RESET, new EventHandler<EventArgs>() {

							@Override
							public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e) {
								HandlerUtils.sendMessage(PreviewGallery.this, MESSAGE_UPDATE_RESET);

							}

						});

						m_FileManager.addHandler(FileManager.EVENT_MEDIA_FILES_ADDED, new EventHandler<MediaEventArgs>() {

							@Override
							public void onEventReceived(EventSource source, EventKey<MediaEventArgs> key, MediaEventArgs e) {
								HandlerUtils.sendMessage(PreviewGallery.this, MESSAGE_UPDATE_ADDED, 0, 0, e.getFilePath());

							}

						});

					}
				});

			}
		});

		initPortrait(cameraActivity);
		initLandscape(cameraActivity);
		if (Rotation.PORTRAIT == getRotation() || Rotation.INVERSE_PORTRAIT == getRotation()) {
			m_VerticalViewPager.setVisibility(View.INVISIBLE);
			m_ViewPager.setVisibility(View.VISIBLE);
		} else {
			m_ViewPager.setVisibility(View.INVISIBLE);
			m_VerticalViewPager.setVisibility(View.VISIBLE);
		}
	}

	void initPortrait(final CameraActivity cameraActivity) {
		m_ViewPager = (ViewPager) m_PreviewGallery.findViewById(R.id.preview_gallery_pager);
		m_ViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
		m_ViewPager.setOffscreenPageLimit(PAGE_OFFSET);
		m_Adapter = new PreviewPagerAdapter(false);

		m_Adapter.initialize(PreviewGallery.this);
		m_ViewPager.setAdapter(m_Adapter);
		m_ViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int state) {

			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int position) {
				if (position == 0) {
					bringToBack();
				} else {
					bringToFront();
				}
				
				//
				preFetch(m_Adapter, position);
				m_PreviousPosition = position;
			}
		});
		
		for (int i = 1; i < Math.min(m_Adapter.getCount(), PAGE_OFFSET + 2); i++) {
			m_Adapter.setPageData(i);
		}


		m_ViewPager.setPageTransformer(false, new PageTransformer() {

			@Override
			public void transformPage(View view, float position) {
				final float MIN_SCALE = 0.85f;
				final float MIN_ALPHA = 0.6f;
				int pageWidth = view.getWidth();
				int pageHeight = view.getHeight();

				if (position < -1) { // [-Infinity,-1)
					// This page is way off-screen to the left.
					view.setAlpha(0);

				} else if (position <= 1) { // [-1,1]
					// Modify the default slide transition to shrink the page as
					// well
					float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
					float vertMargin = pageHeight * (1 - scaleFactor) / 2;
					float horzMargin = pageWidth * (1 - scaleFactor) / 2;
					if (position < 0) {
						view.setTranslationX(horzMargin - vertMargin / 2);
					} else {
						if (m_ViewPager.getCurrentItem() == 0) {
							horzMargin *= 3.3;
						} else {
							horzMargin *= 4;
						}
						view.setTranslationX(-horzMargin + vertMargin / 2);
					}

					// Scale the page down (between MIN_SCALE and 1)
					view.setScaleX(scaleFactor);
					view.setScaleY(scaleFactor);

					// Fade the page relative to its size.
					view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));

				} else { // (1,+Infinity]
					// This page is way off-screen to the right.
					view.setAlpha(0);
				}

			}
		});

		m_ViewPager.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (m_ViewPager.getCurrentItem() == 0) {
					MotionEvent newEvent = MotionEvent.obtain(event);
					newEvent.setLocation(event.getRawX(), event.getRawY());
					cameraActivity.onTouchEvent(newEvent);
				}
				return false;
			}
		});
	}
	
	void preFetch(PreviewPagerAdapter adapter, int position){
		for(int i=0; i<=TARGET; i++){
			if(i == 0){
				if(position != 0){
					adapter.setPageData(position);
				}
			}else{
				adapter.setPageData(Math.min(m_Adapter.getCount()-1 , position + i));
				adapter.setPageData(Math.max(1, position - i));
			}
		}
	}

	void initLandscape(final CameraActivity cameraActivity) {
		m_VerticalViewPager = (VerticalViewPager) m_PreviewGallery.findViewById(R.id.preview_gallery_pager_landscape);
		m_VerticalViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
		m_VerticalViewPager.setOffscreenPageLimit(PAGE_OFFSET);
		m_VerticalAdapter = new PreviewPagerAdapter(true);

		m_VerticalAdapter.initialize(PreviewGallery.this);
		m_VerticalViewPager.setAdapter(m_VerticalAdapter);
		m_VerticalViewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int state) {

			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

			}

			public void onPageSelected(int position) {
				if (position == 0) {
					bringToBack();
				} else {
					bringToFront();
				}
				
				//
				preFetch(m_VerticalAdapter, position);
				m_PreviousPosition = position;
			}
		});
		
		for (int i = 1; i < Math.min(m_Adapter.getCount(), PAGE_OFFSET + 2); i++) {
			m_VerticalAdapter.setPageData(i);
		}
		
		m_VerticalViewPager.setPageTransformer(false, new PageTransformer() {

			@Override
			public void transformPage(View view, float position) {
				int offset = -730;
				if (m_VerticalViewPager.getCurrentItem() == 0) {
					offset = -420;
				}
				view.setTranslationY(0);

				if (position < -1) { // [-Infinity,-1)

				} else if (position <= 1) { // [-1,1]

					if (position > 0) {
						view.setTranslationY(offset * position);
					}

				} else { // (1,+Infinity]
					view.setTranslationY(offset);
				}

			}
		});

		m_VerticalViewPager.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (m_VerticalViewPager.getCurrentItem() == 0) {
					MotionEvent newEvent = MotionEvent.obtain(event);
					newEvent.setLocation(event.getRawX(), event.getRawY());
					cameraActivity.onTouchEvent(newEvent);
				}
				return false;
			}
		});
	}

	void bringToBack() {
		m_PreviewGallery.setBackgroundDrawable(null);
		ViewGroup parent = ((ViewGroup) m_PreviewGallery.getParent());
		parent.removeView(m_PreviewGallery);
		parent.addView(m_PreviewGallery, m_OrignalZ);
	}

	void bringToFront() {
		m_PreviewGallery.setBackgroundColor(getCameraActivity().getResources().getColor(R.color.Previerw_gallery_background));
		m_PreviewGallery.bringToFront();
	}

	private static class PreviewPagerAdapter extends PagerAdapter {
		private boolean m_IsVertical;
		private List<File> m_Files;
		private FileManager m_FileManager;
		private PreviewGallery m_PreviewGallery;
		//
		private int m_PageSize = PAGE_OFFSET * 3 + 1, m_ReqWidth, m_ReqHeight;
		private List<View> m_Pagers = new ArrayList<View>();
		private SparseArray<String> m_Map = new SparseArray<String>();
		//
		static private final String TAG = PreviewPagerAdapter.class.getSimpleName();


		public PreviewPagerAdapter(boolean isVertical) {
			super();
			m_IsVertical = isVertical;
		}

		void initialize(PreviewGallery gallery) {
			m_FileManager = gallery.m_FileManager;
			m_Files = m_FileManager.getMediaFiles();
			m_PreviewGallery = gallery;
			Context context = m_PreviewGallery.getContext();
			LayoutInflater layoutInflater = LayoutInflater.from(context);
			FrameLayout parent = new FrameLayout(context);
			m_Pagers.clear();
			for (int i = 0; i < m_PageSize; i++) {
				if (m_IsVertical) {
					m_Pagers.add(layoutInflater.inflate(R.layout.layout_preview_gallery_land_item, parent, false));
				} else {
					m_Pagers.add(layoutInflater.inflate(R.layout.layout_preview_gallery_item, parent, false));
				}
	        }
		}

		void addFile(File file) {
			m_Files.add(0, file);
			notifyDataSetChanged();
			resetCache();
		}

		void deleteFile(File file) {
			Iterator<File> it = m_Files.iterator();
			File fileItem;
			while (it.hasNext()) {
				fileItem = it.next();
				if (fileItem.getAbsoluteFile().equals(file.getAbsoluteFile())) {
					it.remove();
				}
			}
			notifyDataSetChanged();
		}
		// Returns total number of pages
		@Override
		public int getCount() {
			return m_Files.size() + 1;
		}

		// Returns the page title for the top indicator
		@Override
		public CharSequence getPageTitle(int position) {
			return "Page " + position;
		}

        public void destroyItem(View container, int position, Object object) {
            Log.d(TAG, "destroyItem:" + position);
//            ((ViewGroup) container).removeView((View) object);
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.d(TAG, "destroyItem:" + position);
//            container.removeView((View) object);
        }
 
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "instantiateItem:" + position);
            View ret = null;
            try {
            	if(position == 0){
            		ret = new View(m_PreviewGallery.getContext());
            	}else{
            		final int cacheIndex = (position-1)%m_PageSize;
            		ret = m_Pagers.get(cacheIndex);
            		container.removeView(ret);
            		container.addView(ret);
//            		if (!TextUtils.isEmpty(m_Map.get(cacheIndex)) && !m_Map.get(cacheIndex).equals(m_Files.get(position-1).getAbsolutePath())){
//            			setPageData(position);
//            		}
            	}
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return ret;
        }
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }
        
        private void resetCache(){
        	m_Map.clear();
        	int min = Math.max(1, 1-PAGE_OFFSET);
			int max = Math.min(getCount(), 1+TARGET);
			for(int i=min; i<max; i++){
				setPageData(i);
			}
        }
        
    	private void setPageData(final int position) {
    		final int cacheIndex = (position-1)%m_PageSize;
    		final String path = m_Files.get(position-1).getAbsolutePath();
    		Log.d(TAG, "cacheIndex" + cacheIndex);
    		
    		if (!TextUtils.isEmpty(m_Map.get(cacheIndex)) && m_Map.get(cacheIndex).equals(path)){
    			return;
    		}
			m_Map.put(cacheIndex, path);
    		View root = m_Pagers.get(cacheIndex);
    		Resources res = m_PreviewGallery.getContext().getResources();
    		if (m_IsVertical) {
    			m_ReqWidth = res.getDimensionPixelSize(R.dimen.preview_item_land_width);
    			m_ReqHeight = res.getDimensionPixelSize(R.dimen.preview_item_land_height);
    		} else {
    			m_ReqWidth = res.getDimensionPixelSize(R.dimen.preview_item_width);
    			m_ReqHeight = res.getDimensionPixelSize(R.dimen.preview_item_height);
    		}

    		final SoftReference<ImageView> softImage = new SoftReference<ImageView>((ImageView) root.findViewById(R.id.preview_image));
    		softImage.get().setScaleType(ImageView.ScaleType.CENTER);
    		softImage.get().setImageResource(R.drawable.loading);
    		final SoftReference<ImageView> softPlay = new SoftReference<ImageView>((ImageView) root.findViewById(R.id.play_icon));

    		final File file = m_Files.get(position-1);
    		m_FileManager.getBitmap(path, m_ReqWidth, m_ReqHeight, new PhotoCallback() {

    			@Override
    			public void onBitmapLoad(final Bitmap bitmap, final boolean isVideo) {
    				if (bitmap != null) {
    					new Handler(Looper.getMainLooper()).post(new Runnable() {

    						@Override
    						public void run() {
    							ImageView image = softImage.get();
    							if (image != null) {
    								image.setScaleType(ImageView.ScaleType.FIT_CENTER);
    								image.setImageBitmap(bitmap);

    								if (isVideo) {
    									ImageView play = softPlay.get();
    									play.setVisibility(View.VISIBLE);
    									image.setOnClickListener(new View.OnClickListener() {

    										@Override
    										public void onClick(View v) {
    											Intent intent = new Intent();
    											intent.setAction(Intent.ACTION_VIEW);
    											intent.setDataAndType(Uri.fromFile(file), "video/*");
    											m_PreviewGallery.getContext().startActivity(intent);
    										}
    									});
    								} else {
    									image.setOnClickListener(new View.OnClickListener() {

    										@Override
    										public void onClick(View v) {
    											Intent intent = new Intent();
    											intent.setAction(Intent.ACTION_VIEW);
    											intent.setDataAndType(Uri.fromFile(file), "image/*");
    											m_PreviewGallery.getContext().startActivity(intent);
    										}
    									});
    								}
    							}
    						}
    					});
    				} else {
    					HandlerUtils.sendMessage(m_PreviewGallery, MESSAGE_UPDATE_DELETED, 0, 0, file);
    				}
    			}
    		}, m_IsVertical, position);
        }
	}
}