package com.oneplus.camera.ui;

import java.io.File;
import java.util.List;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.view.ViewPager.PageTransformer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.Log;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.io.FileManager;
import com.oneplus.camera.io.FileManager.PhotoCallback;

final class PreviewGallery extends UIComponent
{
	// Constants
	static private final int MESSAGE_UPDATE_RESET = 1000;
	static private final int MESSAGE_UPDATE_ADDED = 1001;
	
	// Private fields
	private View m_PreviewGallery;
	private ViewPager m_ViewPager;
	
	private FileManager m_FileManager;
	private PagerAdapter m_Adapter;

	// Constructor
	PreviewGallery(CameraActivity cameraActivity) {
		super("Preview Gallery", cameraActivity, true);
	}

	// Handle message.
	@Override
	protected void handleMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_UPDATE_RESET:{
			m_Adapter = new PagerAdapter(this.getCameraActivity().getFragmentManager());
			m_Adapter.initialize(m_FileManager);
			m_ViewPager.setAdapter(m_Adapter);
			m_PreviewGallery.setBackgroundDrawable(null);
			bringToBack();
			break;
		}
		case MESSAGE_UPDATE_ADDED:{
			int current = m_ViewPager.getCurrentItem();
			m_Adapter = new PagerAdapter(this.getCameraActivity().getFragmentManager());
			m_Adapter.initialize(m_FileManager);
			m_ViewPager.setAdapter(m_Adapter);
			if(current != 0){
				m_ViewPager.setCurrentItem(current+1);
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
	protected void onInitialize()
	{
		// call super
		super.onInitialize();
		
		// setup UI
		final CameraActivity cameraActivity = this.getCameraActivity();
		m_PreviewGallery = cameraActivity.findViewById(R.id.preview_gallery);
		
		m_ViewPager = (ViewPager) m_PreviewGallery.findViewById(R.id.preview_gallery_pager);
		m_ViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
		m_ViewPager.setOffscreenPageLimit(3);
		m_Adapter = new PagerAdapter(cameraActivity.getFragmentManager());

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
						
						m_FileManager.addHandler(FileManager.EVENT_MEDIA_FILES_ADDED, new EventHandler<EventArgs>() {

							@Override
							public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e) {
								HandlerUtils.sendMessage(PreviewGallery.this, MESSAGE_UPDATE_ADDED);
								
							}

						});

					}
				});

				m_Adapter.initialize(m_FileManager);
			}
		});
		m_ViewPager.setAdapter(m_Adapter);
		m_ViewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageScrollStateChanged(int state) {
				
			}

			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				
			}

			@Override
			public void onPageSelected(int position) {
				if(position == 0){
					m_PreviewGallery.setBackgroundDrawable(null);
					bringToBack();
				}else{
					m_PreviewGallery.setBackgroundColor(cameraActivity.getResources().getColor(R.color.Previerw_gallery_background));
					m_PreviewGallery.bringToFront();
				}
			}});

		m_ViewPager.setPageTransformer(false, new PageTransformer(){

			@Override
			public void transformPage(View view, float position) {
			    final float MIN_SCALE = 0.85f;
			    final float MIN_ALPHA = 0.5f;
			      int pageWidth = view.getWidth();
			        int pageHeight = view.getHeight();

			        if (position < -1) { // [-Infinity,-1)
			            // This page is way off-screen to the left.
			            view.setAlpha(0);

			        } else if (position <= 1) { // [-1,1]
			            // Modify the default slide transition to shrink the page as well
			            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
			            float vertMargin = pageHeight * (1 - scaleFactor) / 2;
			            float horzMargin = pageWidth * (1 - scaleFactor) / 2;
			            if (position < 0) {
			                view.setTranslationX(horzMargin - vertMargin / 2);
			            } else {
			            	if(m_ViewPager.getCurrentItem()>0){
			            		horzMargin *= 3.5;
			            	}
			                view.setTranslationX(-horzMargin + vertMargin / 2);
			            }

			            // Scale the page down (between MIN_SCALE and 1)
			            view.setScaleX(scaleFactor);
			            view.setScaleY(scaleFactor);

			            // Fade the page relative to its size.
			            view.setAlpha(MIN_ALPHA +
			                    (scaleFactor - MIN_SCALE) /
			                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

			        } else { // (1,+Infinity]
			            // This page is way off-screen to the right.
			            view.setAlpha(0);
			        }
				
			}});
		
		m_ViewPager.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				cameraActivity.onTouchEvent(event);
				return false;
			}});
	}
	
	void bringToBack(){
		ViewGroup parent = ((ViewGroup) m_PreviewGallery.getParent());
		parent.removeView(m_PreviewGallery);
		parent.addView(m_PreviewGallery, 0);
	}

	private static class ImageFragment extends Fragment {

		private File m_File;
		private FileManager m_FileManager;
		static private final String TAG = ImageFragment.class.getSimpleName();

		public ImageFragment(File file, FileManager fileManager) {
			m_File = file;
			m_FileManager = fileManager;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			Log.d(TAG, "onCreate");
			super.onCreate(savedInstanceState);

		}

		@Override
		public void onDestroyView() {
			Log.d(TAG, "onDestroyView");
			super.onDestroyView();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.layout_preview_gallery_item, container, false);
			final ImageView image = (ImageView) (view.findViewById(R.id.preview_image));
			
			int reqWidth = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.preview_item_width);
			int reqHeight = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.preview_item_height);

			m_FileManager.getBitmap(m_File.getAbsolutePath(), reqWidth, reqHeight, new PhotoCallback() {

				@Override
				public void onBitmapLoad(final Bitmap bitmap) {
					if (bitmap != null) {
						new Handler(Looper.getMainLooper()).post(new Runnable() {

							@Override
							public void run() {
								image.setImageBitmap(bitmap);
								image.setOnClickListener(new View.OnClickListener() {

									@Override
									public void onClick(View v) {
										Intent intent = new Intent();
										intent.setAction(Intent.ACTION_VIEW);
										intent.setDataAndType(Uri.fromFile(m_File), "image/*");
										startActivity(intent);
									}
								});
							}
						});
					}

				}
			});
			return view;
		}
	}
	
    private static class PagerAdapter extends FragmentStatePagerAdapter {
    	private List<File> m_Files;
    	private FileManager m_FileManager;

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }
        
        void initialize(FileManager fileManager){
        	m_FileManager = fileManager;
        	m_Files = fileManager.getMediaFiles();
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return m_Files.size() + 1;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case 0: // Fragment # 0 - This will show FirstFragment
                return new Fragment();
            default:
                return new ImageFragment(m_Files.get(position - 1), m_FileManager);
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }
}
