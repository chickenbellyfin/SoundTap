package com.chickenbellyfinn.soundtap;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * Adapter to display ContentItems in the song list
 */
public class PlaylistAdapter extends ArrayAdapter<ContentItem>{

    private static final String TAG = PlaylistAdapter.class.getSimpleName();

    public PlaylistAdapter(Context ctx){
        super(ctx, R.layout.playlist_item);
    }

    public View getView(int position, View convertView, ViewGroup parent){
        View view = null;
        if(convertView == null){
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.playlist_item, null);
        } else {
            view = convertView;
        }

        ContentItem item = getItem(position);
        TextView title = (TextView)view.findViewById(R.id.title);
        TextView summary = (TextView)view.findViewById(R.id.artist);
        ImageView logo = (ImageView)view.findViewById(R.id.art);

        title.setText(item.track);
        summary.setText(item.artist);
        Picasso.with(getContext()).load(item.logo).placeholder(R.drawable.art).into(logo);

        return view;

    }

    /**
     * Add a ContentItem, ignore duplicates
     * @param item
     */
    @Override
    public void add(ContentItem item){
        for(int i = 0; i < getCount(); i++){
            if(item.track.equals(getItem(i).track)){
                Log.d(TAG, "duplicate "+item.track);
                return;
            }
        }
        super.add(item);
    }

}