package com.chickenbellyfinn.soundtouch;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.squareup.picasso.Picasso;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
	private final static String TAG = MainActivity.class.getSimpleName();
	ImageButton tap;
	ListView list;
	TextView text;
	TextView info;
	
	ArrayList<Long> intervals = new ArrayList<Long>();
	PlaylistAdapter playlist;
	
	long last;
	boolean first = true;
	int bpm = 0;
	
	boolean vibrating = false;
	
	ArrayList<String> searchTerms = new ArrayList<String>();
		
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tap = (ImageButton)findViewById(R.id.tap);
        list = (ListView)findViewById(R.id.playlist);
        playlist = new PlaylistAdapter(this);
        text = (TextView)findViewById(R.id.text);
        info = (TextView)findViewById(R.id.info);
        list.setAdapter(playlist);
        
        list.setOnItemClickListener(new ListView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				playSong(playlist.getItem(position));
				
			}
		});
        
        new Thread(){
        	public void run(){
        		while(true){
        			try {
        				if(!SoundTouch.isPlaying() && playlist.getCount() > 0){
        					Log.d(TAG, "polling");
        					final ContentItem item = playlist.getItem(0);
        					runOnUiThread(new Runnable() {								
								@Override
								public void run() {
									playSong(item);
								}
							});
        				}
        				
        				Thread.sleep(5000);
        				
        			} catch(Exception e){
        				e.printStackTrace();
        			}
        		}
        	}
        }.start();
        
        final Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        tap.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(first || System.currentTimeMillis() - last > 2000){
					intervals.clear();
					first = false;
					last = System.currentTimeMillis();
					vibrating = false;
				} else {
					long now = System.currentTimeMillis();
					intervals.add(0,now - last);
					last = now;
				}
				
				bpm = 0;
				if(intervals.size() > 0){
				for(Long l:intervals){
					bpm += l/intervals.size();
				}
				Log.d(TAG, intervals.toString());
				bpm = (60000)/bpm;
				
				if(intervals.size() > 10){
					final int newBpm = bpm;
					bpm = 0;
					intervals.clear();
					tap.setEnabled(false);
					vibrating = true;
					new Thread(){
						public void run(){
							while(vibrating){
								vib.vibrate(50);
								try{
									Thread.sleep(60000/newBpm);
								}catch(Exception e){}
							}
						}
					}.start();
					new Thread(){
						public void run(){
							final ArrayList<String> results = getSongByBeat(newBpm);
							final ArrayList<ContentItem> itemResults = new ArrayList<ContentItem>();
							for(String s:results){
								ContentItem item = SoundTouch.getContentItem(s);
								if(item != null){
									itemResults.add(item);
								}							
							}
							
							try{
							//	Thread.sleep(3000);
							}catch(Exception e){}
							
							Log.d(TAG, "CONTENT "+itemResults.toString());
							vibrating = false;
							runOnUiThread(new Runnable(){
								public void run(){
									playlist.clear();								
									playlist.addAll(itemResults);
									playlist.notifyDataSetChanged();
									list.setVisibility(View.VISIBLE);
									text.setVisibility(View.GONE);
									tap.setEnabled(true);
									if(playlist.getCount() > 0){
										playSong(playlist.getItem(0));
									}

		        		
								}
							});
						}
					}.start();
				}
				
				}
				
			}
		});
        

        // Vibrate for 500 milliseconds
        //v.vibrate(500);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
      //  getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.info) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public ArrayList<String> getSongByBeat(int bpm){
    	String key = "XOBZGRFB04BFSTTQ9";
    	HttpClient client = new DefaultHttpClient();
    	HttpGet hg = new HttpGet(String.format("http://developer.echonest.com/api/v4/song/search?api_key=%s&min_tempo=%d&max_tempo=%d&results=10&sort=artist_familiarity-desc", key, bpm-2, bpm+2));
    	ArrayList<String> results = new ArrayList<String>();
    	try {
			HttpResponse res = client.execute(hg);
			String responseString = Util.readStream(res.getEntity().getContent());
			Log.d(TAG, "echonest says: "+responseString);
			JSONObject obj = new JSONObject(responseString);
			JSONArray songs = obj.getJSONObject("response").getJSONArray("songs"); 
			for(int i = 0; i < songs.length(); i++){
				JSONObject song = songs.getJSONObject(i);
				String artist = song.getString("artist_name");
				String title = song.getString("title");
				results.add(artist + " "+title);
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
    	Log.d(TAG, results.toString());
    	return results;
    }
    
    public void playSong(final ContentItem item){
    	new Thread(){
			public void run(){
				SoundTouch.play(item);
			}
		}.start();
		info.setText(item.getName());
		Picasso.with(getBaseContext()).load(item.getLogo()).placeholder(R.drawable.album_placeholder).resize(tap.getWidth(), tap.getHeight()).centerCrop().into(tap);
		playlist.remove(item);
    	
    }
    
}