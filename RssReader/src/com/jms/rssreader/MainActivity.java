package com.jms.rssreader;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;
import com.jms.dragtorefresh.RefreshableInterface;
import com.jms.dragtorefresh.RefreshableListView;
import com.jms.rssreader.adapter.PostItemAdapter;
import com.jms.rssreader.vo.PostData;

public class MainActivity extends Activity implements RefreshableInterface {
	private enum RSSXMLTag {
		TITLE, DATE, LINK, CONTENT, GUID, IGNORETAG;
	}

	private ArrayList<PostData> listData;
	private String urlString = "http://jmsliu.com/feed?paged=";
	private RefreshableListView postListView;
	private PostItemAdapter postAdapter;
	private int pagnation = 1; // start from 1
	private boolean isRefreshLoading = true;
	private boolean isLoading = false;
	private ArrayList<String> guidList;

	private AdView adView;
	private Tracker googleTracker;
	private GoogleAnalytics googleAnalytics;
	private final static String PREFERENCE_FILENAME = "JMSRssReader";
	private Intent postviewIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_postlist);

		// check connectivity state
		/*
		 * ConnectivityManager cm = (ConnectivityManager)
		 * getSystemService(Context.CONNECTIVITY_SERVICE); NetworkInfo ni =
		 * cm.getActiveNetworkInfo(); if (ni != null && ni.isConnected()) { new
		 * RssDataController().execute(urlString + 0); } else {
		 * 
		 * }
		 */
		googleAnalytics = GoogleAnalytics.getInstance(this);
		googleTracker = googleAnalytics.getTracker("UA-23293636-5");

		// check installation
		SharedPreferences settings = getSharedPreferences(PREFERENCE_FILENAME,
				0);
		boolean isFirstRun = settings.getBoolean("isFirstRun", false);
		if (!isFirstRun) {
			// google analytics
			googleTracker.sendEvent("installation", "install", null, null);

			isFirstRun = true;
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean("isFirstRun", isFirstRun);

			// Commit the edits!
			editor.commit();
		}

		// add google admob
		adView = new AdView(this, AdSize.SMART_BANNER, "a151cfacdc9a91e");
		LinearLayout adContainer = (LinearLayout) this
				.findViewById(R.id.adsContainer);
		adContainer.addView(adView);

		AdRequest adRequest = new AdRequest();
		Set<String> keywordsSet = new HashSet<String>();
		keywordsSet.add("game");
		keywordsSet.add("dating");
		keywordsSet.add("money");
		keywordsSet.add("girl");
		adRequest.addKeywords(keywordsSet);
		adView.loadAd(adRequest);

		guidList = new ArrayList<String>();
		listData = new ArrayList<PostData>();
		postListView = (RefreshableListView) this
				.findViewById(R.id.postListView);
		postAdapter = new PostItemAdapter(this, R.layout.postitem, listData);
		postListView.setAdapter(postAdapter);
		postListView.setOnRefresh(this);
		postListView.onRefreshStart();
		postListView.setOnItemClickListener(onItemClickListener);
	}

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			PostData data = listData.get(arg2 - 1);

			Bundle postInfo = new Bundle();
			postInfo.putString("content", data.postContent);

			if (postviewIntent == null) {
				postviewIntent = new Intent(MainActivity.this,
						PostViewActivity.class);
			}

			postviewIntent.putExtras(postInfo);
			startActivity(postviewIntent);
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.actionAbout:
			String appString = null;
			try {
				appString = this.getPackageManager().getPackageInfo(
						this.getPackageName(), 0).versionName;
				appString = "JMS Rss Reader Version " + appString;
			} catch (NameNotFoundException e) {
				Toast.makeText(this, "Get Version Name Error", Toast.LENGTH_SHORT).show();
			}
			Toast.makeText(this, appString, Toast.LENGTH_SHORT).show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private class RssDataController extends
			AsyncTask<String, Integer, ArrayList<PostData>> {
		private RSSXMLTag currentTag;

		@Override
		protected ArrayList<PostData> doInBackground(String... params) {
			// TODO Auto-generated method stub
			String urlStr = params[0];
			InputStream is = null;
			ArrayList<PostData> postDataList = new ArrayList<PostData>();

			URL url;
			try {
				url = new URL(urlStr);

				HttpURLConnection connection = (HttpURLConnection) url
						.openConnection();
				//connection.setReadTimeout(10 * 1000);
				//connection.setConnectTimeout(10 * 1000);
				connection.setRequestMethod("GET");
				connection.setDoInput(true);
				connection.connect();
				int response = connection.getResponseCode();
				Log.d("debug", "The response is: " + response);
				is = connection.getInputStream();

				// parse xml
				XmlPullParserFactory factory = XmlPullParserFactory
						.newInstance();
				factory.setNamespaceAware(true);
				XmlPullParser xpp = factory.newPullParser();
				xpp.setInput(is, null);

				int eventType = xpp.getEventType();
				PostData pdData = null;
				SimpleDateFormat dateFormat = new SimpleDateFormat(
						"EEE, DD MMM yyyy HH:mm:ss", Locale.US);
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_DOCUMENT) {

					} else if (eventType == XmlPullParser.START_TAG) {
						if (xpp.getName().equals("item")) {
							pdData = new PostData();
							currentTag = RSSXMLTag.IGNORETAG;
						} else if (xpp.getName().equals("title")) {
							currentTag = RSSXMLTag.TITLE;
						} else if (xpp.getName().equals("link")) {
							currentTag = RSSXMLTag.LINK;
						} else if (xpp.getName().equals("pubDate")) {
							currentTag = RSSXMLTag.DATE;
						} else if (xpp.getName().equals("encoded")) {
							currentTag = RSSXMLTag.CONTENT;
						} else if (xpp.getName().equals("guid")) {
							currentTag = RSSXMLTag.GUID;
						}
					} else if (eventType == XmlPullParser.END_TAG) {
						if (xpp.getName().equals("item")) {
							// format the data here, otherwise format data in
							// Adapter
							Date postDate = dateFormat.parse(pdData.postDate);
							pdData.postDate = dateFormat.format(postDate);
							postDataList.add(pdData);
						} else {
							currentTag = RSSXMLTag.IGNORETAG;
						}
					} else if (eventType == XmlPullParser.TEXT) {
						String content = xpp.getText();
						content = content.trim();
						if (pdData != null) {
							switch (currentTag) {
							case TITLE:
								if (content.length() != 0) {
									if (pdData.postTitle != null) {
										pdData.postTitle += content;
									} else {
										pdData.postTitle = content;
									}
								}
								break;
							case LINK:
								if (content.length() != 0) {
									if (pdData.postLink != null) {
										pdData.postLink += content;
									} else {
										pdData.postLink = content;
									}
								}
								break;
							case DATE:
								if (content.length() != 0) {
									if (pdData.postDate != null) {
										pdData.postDate += content;
									} else {
										pdData.postDate = content;
									}
								}
								break;
							case CONTENT:
								if (content.length() != 0) {
									if (pdData.postContent != null) {
										pdData.postContent += content;
									} else {
										pdData.postContent = content;
									}
								}
								break;
							case GUID:
								if (content.length() != 0) {
									if (pdData.postGuid != null) {
										pdData.postGuid += content;
									} else {
										pdData.postGuid = content;
									}
								}
								break;
							default:
								break;
							}
						}
					}

					eventType = xpp.next();
				}
				Log.v("tst", String.valueOf(postDataList.size()));
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				// new URL exception
				googleTracker.sendEvent("debug", "MalformedURLException",
						e.toString(), null);
				e.printStackTrace();
			} catch (ProtocolException e) {
				// TODO Auto-generated catch block
				// setRequestMethod exception
				googleTracker.sendEvent("debug", "ProtocolException",
						e.toString(), null);
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				// XmlPullParserFactory.newInstance()
				googleTracker.sendEvent("debug", "XmlPullParserException",
						e.toString(), null);
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				// dateFormat.parse(pdData.postDate);
				googleTracker.sendEvent("debug", "ParseException",
						e.toString(), null);
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// openConnection()
				// connection.getResponseCode()
				// connection.connect();
				// connection.getInputStream()
				// xpp.next()
				googleTracker.sendEvent("debug", "IOException", e.toString(),
						null);
				e.printStackTrace();
			}
			return postDataList;
		}

		@Override
		protected void onPostExecute(ArrayList<PostData> result) {
			// TODO Auto-generated method stub
			boolean isupdated = false;
			for (int i = 0; i < result.size(); i++) {
				// check if the post is already in the list
				if (guidList.contains(result.get(i).postGuid)) {
					continue;
				} else {
					isupdated = true;
					guidList.add(result.get(i).postGuid);
				}

				if (isRefreshLoading) {
					listData.add(i, result.get(i));
				} else {
					listData.add(result.get(i));
				}
			}

			if (isupdated) {
				postAdapter.notifyDataSetChanged();
			}

			isLoading = false;

			if (isRefreshLoading) {
				postListView.onRefreshComplete();
			} else {
				postListView.onLoadingMoreComplete();
			}

			super.onPostExecute(result);
		}
	}

	@Override
	public void startFresh() {
		// TODO Auto-generated method stub
		if (!isLoading) {
			isRefreshLoading = true;
			isLoading = true;
			new RssDataController().execute(urlString + 1);
		} else {
			postListView.onRefreshComplete();
		}
	}

	@Override
	public void startLoadMore() {
		// TODO Auto-generated method stub
		if (!isLoading) {
			isRefreshLoading = false;
			isLoading = true;
			new RssDataController().execute(urlString + (++pagnation));
		} else {
			postListView.onLoadingMoreComplete();
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (adView != null) {
			adView.destroy();
		}

		super.onDestroy();
	}
}
