package com.hyperionsoft.currencyconverter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

import com.google.gson.Gson;

public class ConverterGoogleHandler {
	
	private boolean errorOccured = false;

	static class Result {
		private String lhs;
		private String rhs;

		public String getLhs() {
			return lhs;
		}

		public String getRhs() {
			return rhs;
		}

		public void setLhs(String lhs) {
			this.lhs = lhs;
		}

		public void setRhs(String rhs) {
			this.rhs = rhs;
		}
	}

	public String returnRate(String baseAmount, String baseCurrency,  String termCurrency)  {

		String returnAmount = null;

		String google = "http://www.google.com/ig/calculator?hl=en&q=";
		String charset = "UTF-8";
	
		URL url = null;

		try {
			url = new URL(google + baseAmount + baseCurrency + "%3D%3F" + termCurrency);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Reader reader = null;
		try {
			reader = new InputStreamReader(url.openStream(), charset);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errorOccured=true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			errorOccured=true;
		}
		//Log.d("CRASH1","baseCurr = " + baseCurrency + " termCurr = " + termCurrency);
		
		if (errorOccured) { 
			return "Not Available";
		}
		
		Result result = null;
			try {
	  	     result = new Gson().fromJson(reader, Result.class);
			} catch (NullPointerException n) {
				n.printStackTrace();
			}
		

		if (result == null) 
			return "Not Available";
		 else {

			// Get the value without the term currency.
			returnAmount = result.getRhs().split("\\s+")[0];
			return returnAmount;

		}

	}

}
