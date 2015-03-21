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


public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ECHONEST_KEY = "XOBZGRFB04BFSTTQ9";

    private static final long MAX_TAP_INTERVAL_MS = 2000;
    private static final long TAP_COUNT = 8;
    private static final long POLL_INTERVAL = 5000;

    private ImageView artView;
    private TextView title;
    private TextView summary;
    private TextView bpmView;

    private SeekBar volumeBar;

    private ListView playlistView;
    private PlaylistAdapter playlist;
    private TextView emptyMessage;

    private Button tapButton;

    private SoundTouch soundTouch;

    private List<Long> tapIntervals = new ArrayList<Long>();
    private long lastTap = 0;
    private int currentBPM = 0;

    private Thread pollingThread = new Thread(){
        @Override
        public void run(){
            while(true){
                Log.v(TAG, "poll check!");
                if(!soundTouch.isPlaying()){
                    if(!playlist.isEmpty()){
                        ContentItem item = playlist.getItem(0);
                        playItem(item);
                    } else {
                        resetInfo();
                    }
                }

                synchronized (pollingThread){
                    try {
                        pollingThread.wait(POLL_INTERVAL);
                    } catch (Exception e){}
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("SoundTap");
        setContentView(R.layout.activity_main);

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

        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ContentItem item = playlist.getItem(position);
                playItem(item);
            }
        });

        soundTouch = new SoundTouch("10.0.0.27");

        tapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tap();
            }
        });

        resetInfo();

        volumeBar.setMax(100);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int volume = seekBar.getProgress();
                Log.d(TAG, "Set volume to "+volume);
                soundTouch.setVolume(volume);
            }
        });
        updateVolume();
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

    private void allowTapping(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tapButton.setEnabled(true);
                tapButton.setText("TAP TO SET BPM");
            }
        });
    }

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
            tapButton.setEnabled(false);
            tapButton.setText("Searching for "+bpm+" BPM!");
            bpmLookup(bpm);
        }

        lastTap = time;
    }

    private void updateVolume(){
        new Thread(){
            @Override
            public void run(){
                final int volume = soundTouch.getVolume();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        volumeBar.setProgress(volume);
                    }
                });
            }
        }.start();
    }

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

//    private void playNextItem(){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                if(playlist.getCount() > 0) {
//                    ContentItem nextItem = playlist.getItem(0);
//                    playlist.remove(nextItem);
//                    playlist.notifyDataSetChanged();
//
//                    title.setText(nextItem.track);
//                    summary.setText(nextItem.artist);
//                    Picasso.with(getBaseContext()).load(nextItem.logo).placeholder(R.drawable.art).into(artView);
//
//                    soundTouch.play(nextItem);
//                }
//            }
//        });
//    }

    private void playItem(final ContentItem item){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                playlist.remove(item);
                playlist.notifyDataSetChanged();

                if(playlist.isEmpty()) {
                    playlistView.setVisibility(View.GONE);
                    emptyMessage.setVisibility(View.VISIBLE);
                }

                title.setText(item.track);
                summary.setText(item.artist);
                bpmView.setText("~"+item.bpm+" BPM");
                Picasso.with(getBaseContext()).load(item.logo).placeholder(R.drawable.art).into(artView);

                soundTouch.play(item);
            }
        });
    }

    public void resetInfo(){
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

    private void bpmLookup(final int bpm){
        playlist.clear();
        playlistView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        new Thread(){
            @Override
            public void run(){
                HttpClient client = new DefaultHttpClient();
                String url = String.format("http://developer.echonest.com/api/v4/song/search?api_key=%s&min_tempo=%d&max_tempo=%d&results=20", ECHONEST_KEY, bpm-2, bpm+2);
                HttpGet get = new HttpGet(url);

                try {
                    HttpResponse response = client.execute(get);
                    String responseString = readStream(response.getEntity().getContent());

                    JSONObject responseObj = new JSONObject(responseString);
                    JSONArray songList = responseObj.getJSONObject("response").getJSONArray("songs");

                    getsongs:for(int i = 0; i < songList.length(); i++){
                        JSONObject song = songList.getJSONObject(i);
                        String artist = song.getString("artist_name");
                        String title = song.getString("title");
                        Log.d(TAG, "searching for "+artist + "/"+title);
                        ContentItem item = soundTouch.search(artist + " " + title);
                        if(item != null) {
                            item.bpm = bpm;
                            addToPlaylist(item);
                            Log.d(TAG, "adding "+item.xml);
                        }

                        if(playlist.getCount() == 10){
                            break getsongs;
                        }
                    }

                } catch(Exception e){
                    e.printStackTrace();
                }
                allowTapping();
            }
        }.start();
    }

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
