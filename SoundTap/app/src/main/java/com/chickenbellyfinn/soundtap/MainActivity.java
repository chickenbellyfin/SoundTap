package com.chickenbellyfinn.soundtap;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application activity. Does all of the things.
 */
public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ECHONEST_KEY = "XOBZGRFB04BFSTTQ9";

    private static final long MAX_TAP_INTERVAL_MS = 2000;
    private static final long TAP_COUNT = 8;
    private static final long POLL_INTERVAL = 5000;

    //Views
    private ImageView artView;
    private TextView title;
    private TextView summary;
    private TextView bpmView;

    private SeekBar volumeBar;

    private ListView playlistView;
    private PlaylistAdapter playlist;
    private TextView emptyMessage; //shown when the playlist is empty

    private Button tapButton;

    //SoundTouch speaker client
    private SoundTouch soundTouch;

    //keeping track of taps to get BPM
    private List<Long> tapIntervals = new ArrayList<Long>();
    private long lastTap = 0;
    private int currentBPM = 0;

    //A thread of check the playing status of the speaker
    private Thread pollingThread = new Thread(){
        @Override
        public void run(){
            while(true){
                Log.v(TAG, "poll check!");

                //if the speaker isn't playing, load the next playlist item
                if(!soundTouch.isPlaying()){
                    if(!playlist.isEmpty()){
                        ContentItem item = playlist.getItem(0);
                        playItem(item);
                    } else {
                        //there are no playlist items either so clean up the interface
                        resetInfo();
                    }
                }

                try {
                    Thread.sleep(POLL_INTERVAL); //don't poll continuously
                } catch (Exception e){}

            }
        }
    };

    /**
     * Called when the app first starts
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SoundTap");
        setContentView(R.layout.activity_main);

        //get all of the views from the layout
        artView = (ImageView)findViewById(R.id.art);
        title = (TextView)findViewById(R.id.title);
        summary = (TextView)findViewById(R.id.summary);
        volumeBar = (SeekBar)findViewById(R.id.volume);
        playlistView = (ListView)findViewById(R.id.listView);
        emptyMessage = (TextView)findViewById(R.id.empty);
        tapButton = (Button)findViewById(R.id.tap);
        bpmView = (TextView)findViewById(R.id.bpm);

        playlistView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);

        playlist = new PlaylistAdapter(this);
        playlistView.setAdapter(playlist);

        //when a playlist item gets clicked, we should play it immediately
        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentItem item = playlist.getItem(position);
                playItem(item);
            }
        });

        soundTouch = new SoundTouch("10.0.0.27");

        //register taps on the tap button
        tapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tap();
            }
        });

        //reset the now playing interface
        resetInfo();

        //set up the volume bar
        volumeBar.setMax(100);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            /**
             * set the volume of the speaker only when the seekbar is let go
             * @param seekBar
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int volume = seekBar.getProgress();
                Log.d(TAG, "Set volume to "+volume);
                soundTouch.setVolume(volume);
            }
        });

        //get the initial volume of the speaker
        updateVolume();

        //start polling the play state of the speaker
        pollingThread.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a ContentItem to the playlist. Starts playing if this is the first result in a search
     * @param item
     */
    private void addToPlaylist(final ContentItem item){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean playlistWasEmpty = playlist.isEmpty();

                playlist.add(item);
                playlist.notifyDataSetChanged();

                playlistView.setVisibility(View.VISIBLE);
                emptyMessage.setVisibility(View.GONE);

                if(!playlist.isEmpty() && playlistWasEmpty && currentBPM != item.bpm){
                    currentBPM = item.bpm;
                    playItem(playlist.getItem(0));
                }
            }
        });
    }

    /**
     * Re-enables the tapping button
     */
    private void allowTapping(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tapButton.setEnabled(true);
                tapButton.setText("TAP TO SET BPM");
            }
        });
    }

    /**
     * Register a tap on the BPM tap button
     * After TAP_COUNT consecutive taps, start looking up songs with the resulting tempo
     */
    private void tap(){
        long time = System.currentTimeMillis();
        //if it has been a while since the last tap
        //start measuring BPM again
        if(time - lastTap > MAX_TAP_INTERVAL_MS){
            tapIntervals.clear();
        } else {
            tapIntervals.add(time - lastTap);
        }

        int bpm = getBPM();
        tapButton.setText(bpm + " BPM");

        //if there are enough taps to set a BPM
        if(tapIntervals.size() > TAP_COUNT){
            //disable the tap button until the search is complete
            tapButton.setEnabled(false);
            tapButton.setText("Searching for "+bpm+" BPM!");
            bpmLookup(bpm); //start searching
        }

        lastTap = time;
    }

    /**
     * Get the volume from the SoundTouch speaker
     */
    private void updateVolume(){
        new Thread(){
            @Override
            public void run(){
                //SoundTouch's getVolume nees to run on a separate thread because it performs an HTTP request
                final int volume = soundTouch.getVolume();

                //but we need to go back to the UI thread to set the volume bar
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        volumeBar.setProgress(volume);
                    }
                });
            }
        }.start();
    }

    /**
     * Compute the average BPM of the recorded taps so far
     * @return beats per minute, 0 if there are no taps
     */
    private int getBPM(){
        int bpm = 0;
        //average tap intervals
        if(tapIntervals.size() > 0) {
            for (Long interval : tapIntervals) {
                bpm += interval / tapIntervals.size();
            }
            bpm = (60000) / bpm; //intervals in ms -> beats per minute
        }
        return bpm;
    }

    /**
     * Play the given ContentItem on SoundTouch
     * @param item
     */
    private void playItem(final ContentItem item){
        //ensure that this runs on the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //remove the item from the playlist
                playlist.remove(item);
                playlist.notifyDataSetChanged();

                //update playlist visibility
                if(playlist.isEmpty()) {
                    playlistView.setVisibility(View.GONE);
                    emptyMessage.setVisibility(View.VISIBLE);
                }

                //show the item in the now playing interface
                title.setText(item.track);
                summary.setText(item.artist);
                bpmView.setText("~"+item.bpm+" BPM");
                Picasso.with(getBaseContext()).load(item.logo).placeholder(R.drawable.art).into(artView); //load album art

                //finally, send the content to SoundTouch to start playing
                soundTouch.play(item);
            }
        });
    }

    /**
     * Resets the now playing section of the interface for when nothing is playing
     */
    public void resetInfo(){
        //ensure that this runs on the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                artView.setImageResource(R.drawable.art);
                title.setText("Not playing");
                summary.setText("Tap to find some music!");
                bpmView.setText("");
            }
        });
    }

    /**
     * Lookup songs that have the given tempo in BPM(+ or - 2 bpm) from EchoNest,
     * then find the corresponding ContentItems for SoundTouch.
     * The found ContentItems are added to the playlist as they are found.
     * @param bpm tempo in beats per minute to search for
     */
    private void bpmLookup(final int bpm){
        //clear the playlist
        playlist.clear();
        playlistView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);

        //Network operation, so start a new thread
        new Thread(){
            @Override
            public void run(){

                HttpClient client = new DefaultHttpClient();

                //EchoNest api url for finding 20 songs with the given BPM
                String url = String.format("http://developer.echonest.com/api/v4/song/search?api_key=%s&min_tempo=%d&max_tempo=%d&results=20", ECHONEST_KEY, bpm-2, bpm+2);
                HttpGet get = new HttpGet(url);

                try {
                    //execute the EchoNest request
                    HttpResponse response = client.execute(get);
                    String responseString = readStream(response.getEntity().getContent());

                    //EchoNest returns JSON, so parse it
                    JSONObject responseObj = new JSONObject(responseString);
                    //get the list of songs from the JSON response
                    JSONArray songList = responseObj.getJSONObject("response").getJSONArray("songs");

                    //For each song in the EchoNest reponse, lookup the content on SoundTouch
                    getsongs:for(int i = 0; i < songList.length(); i++){
                        JSONObject song = songList.getJSONObject(i);
                        String artist = song.getString("artist_name");
                        String title = song.getString("title");
                        Log.d(TAG, "searching for "+artist + "/"+title);

                        //Call SoundTouch.search(), which will call /search with the artist & title as the search term
                        //The speaker will find the exact item (if possible) in the Deezer music service
                        ContentItem item = soundTouch.search(artist + " " + title);

                        //if SoundTouch found the track, add it to the playlist
                        if(item != null) {
                            item.bpm = bpm;
                            addToPlaylist(item);
                            Log.d(TAG, "adding "+item.xml);
                        }

                        //Sometimes songs are not found by SoundTouch, so we request 20 from EchoNest,
                        //but cap our playlist to 10 so that we don't search for too long
                        if(playlist.getCount() == 10){
                            break getsongs;
                        }
                    }

                } catch(Exception e){
                    e.printStackTrace();
                }

                //re-enable the BPM tapping button
                allowTapping();
            }
        }.start();
    }

    /**
     * Read an entire InputStream into a String
     * @param is InputStream
     * @return string, not null
     */
    public static String readStream(InputStream is){
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String response = "";
        String line;
        try {
            while((line = reader.readLine()) != null){
                response += line;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}
