package com.oneplus.camera.ui;

import java.io.File;
import java.util.List;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.oneplus.base.EventArgs;
import com.oneplus.base.EventHandler;
import com.oneplus.base.EventKey;
import com.oneplus.base.EventSource;
import com.oneplus.base.HandlerUtils;
import com.oneplus.base.component.ComponentSearchCallback;
import com.oneplus.base.component.ComponentUtils;
import com.oneplus.camera.CameraActivity;
import com.oneplus.camera.R;
import com.oneplus.camera.UIComponent;
import com.oneplus.camera.io.FileManager;

final class PreviewGallery extends UIComponent
{
	// Constants
	static private final int MESSAGE_UPDATE_FILES = 1000;
	
	// Private fields
	private View m_PreviewGallery;
	private ViewPager m_ViewPager;
	
	private FileManager m_FileManager;
	private PagerAdapter m_Adapter;
	private View m_OrignalTop;

	// Constructor
	PreviewGallery(CameraActivity cameraActivity) {
		super("Preview Gallery", cameraActivity, true);
	}

	// Handle message.
	@Override
	protected void handleMessage(Message msg) {
		switch (msg.what) {
		case MESSAGE_UPDATE_FILES:{
			m_Adapter = new PagerAdapter(this.getCameraActivity().getFragmentManager());
			m_Adapter.initialize(m_FileManager.getMediaFiles());
			m_ViewPager.setAdapter(m_Adapter);
			m_ViewPager.requestLayout();
			m_PreviewGallery.setBackgroundDrawable(null);
			m_OrignalTop.bringToFront();
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
		ViewGroup parent = ((ViewGroup) m_PreviewGallery.getParent());
		m_OrignalTop = parent.getChildAt(parent.getChildCount() - 1);
		
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
						m_FileManager.addHandler(FileManager.EVENT_MEDIA_FILES_UPDATED, new EventHandler<EventArgs>() {

							@Override
							public void onEventReceived(EventSource source, EventKey<EventArgs> key, EventArgs e) {
								HandlerUtils.sendMessage(PreviewGallery.this, MESSAGE_UPDATE_FILES);
								
							}

						});

					}
				});

				m_Adapter.initialize(m_FileManager.getMediaFiles());
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
					m_OrignalTop.bringToFront();
				}else{
					m_PreviewGallery.setBackgroundColor(cameraActivity.getResources().getColor(R.color.Previerw_gallery_background));
					m_PreviewGallery.bringToFront();
				}
			}});
		m_ViewPager.setOnTouchListener(new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				cameraActivity.onTouchEvent(event);
				return false;
			}
		});
	}

	private static class ImageFragment extends Fragment {
		
		private	File	m_File;
		public ImageFragment(File file){
			m_File = file;
		}

		@Override
		public void onCreate(Bundle savedInstanceState)  
		{
		    super.onCreate(savedInstanceState);

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.layout_preview_gallery_item, container, false);
			ImageView image = (ImageView) (view.findViewById(R.id.preview_image));

			int width = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.preview_item_width);
			int height = inflater.getContext().getResources().getDimensionPixelSize(R.dimen.preview_item_height);

			BitmapFactory.Options bmOptions = new BitmapFactory.Options();
			bmOptions.inSampleSize = calculateInSampleSize(bmOptions, width, height);
			Bitmap bitmap = BitmapFactory.decodeFile(m_File.getAbsolutePath(), bmOptions);

			if (bitmap != null) {
				bitmap = scaleCenterCrop(bitmap, width, height);
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

			return view;
		}
		
		public Bitmap scaleCenterCrop(Bitmap source, int newWidth, int newHeight) {
			int sourceWidth = source.getWidth();
			int sourceHeight = source.getHeight();

			// Compute the scaling factors to fit the new height and width,
			// respectively.
			// To cover the final image, the final scaling will be the bigger
			// of these two.
			float xScale = (float) newWidth / sourceWidth;
			float yScale = (float) newHeight / sourceHeight;
			float scale = Math.max(xScale, yScale);

			// Now get the size of the source bitmap when scaled
			float scaledWidth = scale * sourceWidth;
			float scaledHeight = scale * sourceHeight;

			// Let's find out the upper left coordinates if the scaled bitmap
			// should be centered in the new size give by the parameters
			float left = (newWidth - scaledWidth) / 2;
			float top = (newHeight - scaledHeight) / 2;

			// The target rectangle for the new, scaled version of the source
			// bitmap will now
			// be
			RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

			// Finally, we create a new bitmap of the specified size and draw
			// our new,
			// scaled bitmap onto it.
			Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
			Canvas canvas = new Canvas(dest);
			canvas.drawBitmap(source, null, targetRect, null);

			return dest;
		}
		
		public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
			// Raw height and width of image
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if (height > reqHeight || width > reqWidth) {

				final int halfHeight = height / 2;
				final int halfWidth = width / 2;

				// Calculate the largest inSampleSize value that is a power of 2
				// and keeps both
				// height and width larger than the requested height and width.
				while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
					inSampleSize *= 2;
				}
			}

			return inSampleSize;
		}
	}
	
    private static class PagerAdapter extends FragmentStatePagerAdapter {
    	private List<File> m_Files;

        public PagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }
        
        void initialize(List<File> fm){
        	m_Files = fm;
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
                return new ImageFragment(m_Files.get(position - 1));
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return "Page " + position;
        }

    }
}
