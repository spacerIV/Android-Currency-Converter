package com.hyperionsoft.currencyconverter;

import java.net.URL;

/**
 * @author rob
 * 
 * This class is used to store all the information about a given article
 * that is returned from an RSS feed. It is based on the RSS2.0 definition and
 * contains the variables for select core attributes in the schema
 *
 */
public class RssArticle {
	
	private long articleId;
	private long feedId;
	private String title;
	private String description;
	private String pubDate;
	private URL url;
	private String encodedContent;
	/**
	 * @return the articleId
	 */
	public long getArticleId() {
		return articleId;
	}
	/**
	 * @param articleId the articleId to set
	 */
	public void setArticleId(long articleId) {
		this.articleId = articleId;
	}
	/**
	 * @return the feedId
	 */
	public long getFeedId() {
		return feedId;
	}
	/**
	 * @param feedId the feedId to set
	 */
	public void setFeedId(long feedId) {
		this.feedId = feedId;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the url
	 */
	public URL getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(URL url) {
		this.url = url;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param pubDate the pubDate to set
	 */
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	/**
	 * @return the pubDate
	 */
	public String getPubDate() {
		return pubDate;
	}
	/**
	 * @param encodedContent the encodedContent to set
	 */
	public void setEncodedContent(String encodedContent) {
		this.encodedContent = encodedContent;
	}
	/**
	 * @return the encodedContent
	 */
	public String getEncodedContent() {
		return encodedContent;
	}

}
