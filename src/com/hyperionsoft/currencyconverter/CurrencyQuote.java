package com.hyperionsoft.currencyconverter;

/** 
 * This is used in the currencyActivity class. Each of the listView rows are one of these. 
 */

public class CurrencyQuote {
	private String currCode;
	private String amount;
	private static String baseCurr; 

	public CurrencyQuote(String currCode, String amount) {
		this.currCode = currCode;
		this.amount = amount;
	}

	public void setCurrCode(String currCode) {
		this.currCode = currCode;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getCurrCode() {
		return this.currCode;
	}

	public String getAmount() {
		return this.amount;
	}
	
	public void setbaseCurr(String baseCurr) {
		this.baseCurr = baseCurr;
	}
	
	public String getBaseCurr() {
		return this.baseCurr;
	}
}

