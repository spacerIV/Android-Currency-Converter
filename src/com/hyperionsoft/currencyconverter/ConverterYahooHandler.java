package com.hyperionsoft.currencyconverter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.util.Log;


public class ConverterYahooHandler {

	public String returnRate(String Amount, String fromCurrency, String toCurrency) throws IOException  {
		
		
		URL yahooUrl = new URL("http://quote.yahoo.com/d/quotes.csv?f=l1&s=" + fromCurrency + toCurrency + "=X");
		yahooUrl.getFile();
		InputStream inputStream = yahooUrl.openStream();									
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        
		/** Seems slower, but works.
		HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet("http://quote.yahoo.com/d/quotes.csv?f=l1&s=ZARUSD=X");
        HttpResponse response = httpClient.execute(httpGet, localContext);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		**/
		String line;
		String returnAmount = null;
	
		while ((line = bufferedReader.readLine()) != null) returnAmount = line;
		inputStream.close();
		return returnAmount;
	}
	
	public List<CurrencyYahoo> returnRate(List<CurrencyYahoo> currenciesYahoo) throws IOException {
		
		//Build the URL from the given currencies in currenciesYahoo
		StringBuilder yahooSb = new StringBuilder();
		yahooSb.append("http://quote.yahoo.com/d/quotes.csv?f=l1&s=");
		
		for (CurrencyYahoo y : currenciesYahoo) {
			yahooSb.append(y.getFromCurr());
			yahooSb.append(y.getToCurr());
			yahooSb.append("=X");	
			yahooSb.append("&s=");
		}

		Log.d("yahoo",yahooSb.toString());
		URL yahooUrl = new URL(yahooSb.substring(0, yahooSb.length()-3));
		yahooUrl.getFile();
		InputStream inputStream = yahooUrl.openStream();									
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		
		String line;
		String returnAmount = null;
		int lineNo=0;
	
		while ((line = bufferedReader.readLine()) != null) {
			returnAmount = line;
			CurrencyYahoo y = currenciesYahoo.get(lineNo);
			y.setToAmount(returnAmount);
			lineNo = lineNo + 1;
		}
		inputStream.close();
		
		//Parse the response and set the toAmount for each currency pair. Here im just putting in a random value...
		//for (CurrencyYahoo y : currenciesYahoo) {
	//		Integer rand =  (int)(Math.random() * 100);
	//		y.setToAmount(rand.toString());
//		}
		
		return currenciesYahoo;
		
	}

public String returnRate() throws IOException  {
		
	String returnAmount = null;
	    /**
		URL yahooUrl = new URL("http://quote.yahoo.com/d/quotes.csv?f=l1&s=" + fromCurrency + toCurrency + "=X");
		yahooUrl.getFile();
		InputStream inputStream = yahooUrl.openStream();									
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        
		/** Seems slower, but works.
		HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet("http://quote.yahoo.com/d/quotes.csv?f=l1&s=ZARUSD=X");
        HttpResponse response = httpClient.execute(httpGet, localContext);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		
		String line;
		
	
		while ((line = bufferedReader.readLine()) != null) {
			returnAmount = line;
		}
		
		inputStream.close();
		**/
		
		return returnAmount;
	}
}
