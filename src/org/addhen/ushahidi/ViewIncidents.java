package org.addhen.ushahidi;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ViewIncidents extends Activity {
	private ImageView mImageView;
    private TextView title;
    private TextView body;
    private TextView date;
    private TextView location;
    private TextView category;
    private TextView status;
    
    private Bundle extras = new Bundle();
    private String URL;
    private final String PREFS_NAME = "Ushahidi";
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_incidents);
        
        mImageView = (ImageView) findViewById(R.id.img);
        
        Bundle incidents = getIntent().getExtras();
        
        extras = incidents.getBundle("incidents");
        
        String iStatus = Util.toInt(extras.getString("status") ) == 0 ? "Unverified" : "Verified";
        title = (TextView) findViewById(R.id.title);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(144, 80, 62));
		title.setText(extras.getString("title"));
        
        category = (TextView) findViewById(R.id.category);
        category.setTextColor(Color.BLACK);
        category.setText(extras.getString("category"));
        
        
        date = (TextView) findViewById(R.id.date);
        date.setTextColor(Color.BLACK);
        date.setText( Util.joinString("Date:",extras.getString("date")));
        
        
        location = (TextView) findViewById(R.id.location);
        location.setTextColor(Color.BLACK);
        location.setText(extras.getString("location"));
        
        body = (TextView) findViewById(R.id.webview);
        body.setTextColor(Color.BLACK);
        body.setText(extras.getString("desc"));
        
        status = (TextView) findViewById( R.id.status);
        status.setTextColor(Color.rgb(41, 142, 40));
        status.setText(iStatus);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	this.setURL( settings.getString("Domain", "") );
        
        mImageView = (ImageView) findViewById(R.id.img);
        
        Drawable drawable = getResources().getDrawable(R.drawable.ushahidi_globe); 
        
        mImageView.setImageDrawable(
        	drawable
        );
       
          
        Gallery g = (Gallery) findViewById(R.id.gallery);
        
        g.setAdapter(new ImageAdapter(this) );
        
        
        
    }
    
    public void setURL( String URL ) {
		// set the directory where ushahidi photos are stored
		String photoDir = "/media/uploads/";
		this.URL = URL+photoDir;
	}
	
	public String getURL() {
		return this.URL;
	}
    
 // As drawable.  
	public static Drawable imageOperations(String url, String saveFilename) {
		try {
			InputStream is = (InputStream) fetch(url);
			Drawable d = Drawable.createFromStream(is, saveFilename);
			return d;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	// Fetch image from the given URL
	private static Object fetch(String address) throws MalformedURLException,IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content; 
	}
    
    public class ImageAdapter extends BaseAdapter {
        
    	public ImageAdapter( Context context ){
    		mContext = context;
    	}
    	public int getCount() {
    		return mImageIds.length;
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView i = new ImageView(mContext);
			i.setImageDrawable( 
					getResources().getDrawable( mImageIds[position] ) );
			
			i.setScaleType(ImageView.ScaleType.FIT_XY);
            
			i.setLayoutParams(new Gallery.LayoutParams(180, 88));

			return i;
		}
		
		private Context mContext;

        private Integer[] mImageIds = {
        		R.drawable.ushahidi_icon,
        		R.drawable.ushahidi_icon,
        		R.drawable.ushahidi_icon,
        		R.drawable.ushahidi_icon,
        };
    }
        
}

