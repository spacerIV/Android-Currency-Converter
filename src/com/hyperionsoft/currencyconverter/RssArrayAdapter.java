package com.hyperionsoft.currencyconverter;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class RssArrayAdapter extends ArrayAdapter<JSONObject> {

	public RssArrayAdapter(Activity activity, List<JSONObject> imageAndTexts) {
		super(activity, 0, imageAndTexts);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Activity activity = (Activity) getContext();
		LayoutInflater inflater = activity.getLayoutInflater();

		// Inflate the views from XML
		View rowView = inflater.inflate(R.layout.newsimagetext, null);
		JSONObject jsonImageText = getItem(position);
		
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		//The next section we update at runtime the text - as provided by the JSON from our REST call
		////////////////////////////////////////////////////////////////////////////////////////////////////
		TextView textView = (TextView) rowView.findViewById(R.id.job_text);
		
		try {
			Spanned text = (Spanned)jsonImageText.get("text");
			textView.setText(text);

		} catch (JSONException e) {
			textView.setText("JSON Exception");
		}

		return rowView;

	} 

}