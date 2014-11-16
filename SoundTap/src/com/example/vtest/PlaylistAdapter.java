package com.example.vtest;

import com.squareup.picasso.Picasso;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PlaylistAdapter extends ArrayAdapter<ContentItem>{
	
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
		TextView name = (TextView)view.findViewById(R.id.name);
		ImageView logo = (ImageView)view.findViewById(R.id.logo);
		
		name.setText(item.getName());
		Picasso.with(getContext()).load(item.getLogo()).placeholder(R.drawable.ic_launcher).into(logo);
		
		return view;	
		
	}

}
