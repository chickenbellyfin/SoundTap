package com.example.vtest;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class SoundTouch {
	
	private static final String TAG = SoundTouch.class.getSimpleName();
	private static final int PORT = 8090;
	private static final String ADDRESS = "http://192.168.1.15";
	
	private static final String ACCOUNT = "isaiah_smith@bose.com";
	public static ContentItem getContentItem(String search){
		ContentItem item = null;
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(ADDRESS+":"+PORT+"/search");
		String reqContent = "";
		reqContent += String.format("<search source=\"DEEZER\" sourceAccount=\"%s\" sortOrder=\"track\">", ACCOUNT);
		reqContent += String.format("<searchTerm filter=\"track\">%s</searchTerm></search>", search);
		try {
			post.setEntity(new StringEntity(reqContent));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			HttpResponse res = client.execute(post);
			//String responseString = Util.readStream(res.getEntity().getContent());
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			String str = Util.readStream(res.getEntity().getContent());
			//Log.d(TAG, str);
			Document doc = dBuilder.parse(new StringBufferInputStream(str));
			NodeList itemList = doc.getElementsByTagName("item");
			if(itemList.getLength() > 0){
				Element itemElem = (Element)itemList.item(0);
				
				Node cnode = itemElem.getElementsByTagName("ContentItem").item(0);
				
				String name = ((Element)itemElem.getElementsByTagName("name").item(0)).getTextContent();
				String logo = itemElem.getElementsByTagName("logo").item(0).getTextContent();
				
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");

				StreamResult result = new StreamResult(new StringWriter());
				DOMSource source = new DOMSource(cnode);
				transformer.transform(source, result);

				String cxml = result.getWriter().toString();

				item = new ContentItem(name, cxml, logo);
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		return item;		
	}
	
	public static void play(ContentItem item){
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(ADDRESS+":"+PORT+"/select");
		String reqContent = item.getXml();
		//Log.d(TAG, "SoundTouch: "+reqContent);
		try {
			post.setEntity(new StringEntity(reqContent));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		try {
			HttpResponse res = client.execute(post);
			Log.d(TAG, Util.readStream(res.getEntity().getContent()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public static boolean isPlaying(){
		HttpClient client = new DefaultHttpClient();
		HttpGet req = new HttpGet(ADDRESS+":"+PORT+"/now_playing");
		try {
			HttpResponse res = client.execute(req);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			String str = Util.readStream(res.getEntity().getContent());
			Log.d(TAG, str);
			Document doc = dBuilder.parse(new StringBufferInputStream(str));
			
			Node nowPlay = doc.getElementsByTagName("nowPlaying").item(0);
			
			Log.d(TAG, "nowPlaying nodes "+nowPlay.getChildNodes().getLength());
			if(nowPlay.getChildNodes().getLength() > 1){
				return true;
			}
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return false;
	}

}
