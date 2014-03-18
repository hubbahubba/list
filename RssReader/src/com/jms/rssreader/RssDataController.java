//package com.jms.rssreader;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
//import java.net.ProtocolException;
//import java.net.URL;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.Locale;
//
//import org.xmlpull.v1.XmlPullParser;
//import org.xmlpull.v1.XmlPullParserException;
//import org.xmlpull.v1.XmlPullParserFactory;
//
//import android.os.AsyncTask;
//import android.util.Log;
//
//import com.jms.rssreader.vo.PostData;
//
//
//
//public class RssDataController extends
//AsyncTask<String, Integer, ArrayList<PostData>> {
//
//	private enum RSSXMLTag {
//		TITLE, DATE, LINK, CONTENT, GUID, IGNORETAG;
//	}
//
//	
//	private ArrayList<String> guidList;
//
//
//	private RSSXMLTag currentTag;
//
//	@Override
//	protected ArrayList<PostData> doInBackground(String... params) {
//		guidList = new ArrayList<String>();
//		// TODO Auto-generated method stub
//		String urlStr = params[0];
//		InputStream is = null;
//		ArrayList<PostData> postDataList = new ArrayList<PostData>();
//
//		URL url;
//		try {
//			url = new URL(urlStr);
//
//			HttpURLConnection connection = (HttpURLConnection) url
//					.openConnection();
//			//connection.setReadTimeout(10 * 1000);
//			//connection.setConnectTimeout(10 * 1000);
//			connection.setRequestMethod("GET");
//			connection.setDoInput(true);
//			connection.connect();
//			int response = connection.getResponseCode();
//			Log.d("debug", "The response is: " + response);
//			is = connection.getInputStream();
//
//			// parse xml
//			XmlPullParserFactory factory = XmlPullParserFactory
//					.newInstance();
//			factory.setNamespaceAware(true);
//			XmlPullParser xpp = factory.newPullParser();
//			xpp.setInput(is, null);
//
//			int eventType = xpp.getEventType();
//			PostData pdData = null;
//			SimpleDateFormat dateFormat = new SimpleDateFormat(
//					"EEE, DD MMM yyyy HH:mm:ss", Locale.US);
//			while (eventType != XmlPullParser.END_DOCUMENT) {
//				if (eventType == XmlPullParser.START_DOCUMENT) {
//
//				} else if (eventType == XmlPullParser.START_TAG) {
//					if (xpp.getName().equals("item")) {
//						pdData = new PostData();
//						currentTag = RSSXMLTag.IGNORETAG;
//					} else if (xpp.getName().equals("title")) {
//						currentTag = RSSXMLTag.TITLE;
//					} else if (xpp.getName().equals("link")) {
//						currentTag = RSSXMLTag.LINK;
//					} else if (xpp.getName().equals("pubDate")) {
//						currentTag = RSSXMLTag.DATE;
//					} else if (xpp.getName().equals("encoded")) {
//						currentTag = RSSXMLTag.CONTENT;
//					} else if (xpp.getName().equals("guid")) {
//						currentTag = RSSXMLTag.GUID;
//					}
//				} else if (eventType == XmlPullParser.END_TAG) {
//					if (xpp.getName().equals("item")) {
//						// format the data here, otherwise format data in
//						// Adapter
//						Date postDate = dateFormat.parse(pdData.postDate);
//						pdData.postDate = dateFormat.format(postDate);
//						postDataList.add(pdData);
//					} else {
//						currentTag = RSSXMLTag.IGNORETAG;
//					}
//				} else if (eventType == XmlPullParser.TEXT) {
//					String content = xpp.getText();
//					content = content.trim();
//					if (pdData != null) {
//						switch (currentTag) {
//						case TITLE:
//							if (content.length() != 0) {
//								if (pdData.postTitle != null) {
//									pdData.postTitle += content;
//								} else {
//									pdData.postTitle = content;
//								}
//							}
//							break;
//						case LINK:
//							if (content.length() != 0) {
//								if (pdData.postLink != null) {
//									pdData.postLink += content;
//								} else {
//									pdData.postLink = content;
//								}
//							}
//							break;
//						case DATE:
//							if (content.length() != 0) {
//								if (pdData.postDate != null) {
//									pdData.postDate += content;
//								} else {
//									pdData.postDate = content;
//								}
//							}
//							break;
//						case CONTENT:
//							if (content.length() != 0) {
//								if (pdData.postContent != null) {
//									pdData.postContent += content;
//								} else {
//									pdData.postContent = content;
//								}
//							}
//							break;
//						case GUID:
//							if (content.length() != 0) {
//								if (pdData.postGuid != null) {
//									pdData.postGuid += content;
//								} else {
//									pdData.postGuid = content;
//								}
//							}
//							break;
//						default:
//							break;
//						}
//					}
//				}
//
//				eventType = xpp.next();
//			}
//			Log.v("tst", String.valueOf(postDataList.size()));
//		} catch (MalformedURLException e) {
//			// TODO Auto-generated catch block
//			// new URL exception
//			
//			//googleTracker.sendEvent("debug", "MalformedURLException",
//			//		e.toString(), null);
//			e.printStackTrace();
//		} catch (ProtocolException e) {
//			// TODO Auto-generated catch block
//			// setRequestMethod exception
//			
//			//googleTracker.sendEvent("debug", "ProtocolException",
//			//		e.toString(), null);
//			e.printStackTrace();
//		} catch (XmlPullParserException e) {
//			// TODO Auto-generated catch block
//			// XmlPullParserFactory.newInstance()
//			
//			//googleTracker.sendEvent("debug", "XmlPullParserException",
//			//		e.toString(), null);
//			e.printStackTrace();
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			// dateFormat.parse(pdData.postDate);
//			
//			//googleTracker.sendEvent("debug", "ParseException",
//			//		e.toString(), null);
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			// openConnection()
//			// connection.getResponseCode()
//			// connection.connect();
//			// connection.getInputStream()
//			// xpp.next()
//			
//			//googleTracker.sendEvent("debug", "IOException", e.toString(),
//			//		null);
//			e.printStackTrace();
//		}
//		return postDataList;
//	}
//
//	@Override
//	protected void onPostExecute(ArrayList<PostData> result) {
//		// TODO Auto-generated method stub
//		boolean isupdated = false;
//		for (int i = 0; i < result.size(); i++) {
//			// check if the post is already in the list
//			if (guidList.contains(result.get(i).postGuid)) {
//				continue;
//			} else {
//				isupdated = true;
//				guidList.add(result.get(i).postGuid);
//			}
//
//			if (isRefreshLoading) {
//				listData.add(i, result.get(i));
//			} else {
//				listData.add(result.get(i));
//			}
//		}
//
//		if (isupdated) {
//			postAdapter.notifyDataSetChanged();
//		}
//
//		isLoading = false;
//
//		if (isRefreshLoading) {
//			postListView.onRefreshComplete();
//		} else {
//			postListView.onLoadingMoreComplete();
//		}
//
//		super.onPostExecute(result);
//	}
//}