package com.chickenbellyfinn.soundtap;


import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Akshay on 3/21/2015.
 */
public class SoundTouch {

    private static final String TAG = SoundTouch.class.getSimpleName();

    private static final int PORT = 8090;
    private static final String DEEZER_ACCOUNT = "Akshay_Thejaswi@bose.com";

    private String url = "";

    public SoundTouch(String address){
        url = String.format("http://%s:%d", address, PORT);
    }


    public ContentItem search(String searchTerm){
        ContentItem item = null;
        HttpClient client = new DefaultHttpClient();

        try {

            //send POST to /search on the SoundTouch
            HttpPost post = new HttpPost(url + "/search");

            //search XML
            String requestContent = "";
            requestContent += String.format("<search source=\"DEEZER\" sourceAccount=\"%s\" sortOrder=\"track\">", DEEZER_ACCOUNT);
            requestContent += String.format("<searchTerm filter=\"track\">%s</searchTerm></search>", searchTerm);
            post.setEntity(new StringEntity(requestContent));

            //execute HTTP request
            HttpResponse response = client.execute(post);
            Document document = getDocumentFromResponse(response);

            //Log.d(TAG, documentToString(document));

            //see if we got at least one <item> in the response
            NodeList itemList = document.getElementsByTagName("item");
            if(itemList.getLength() > 0){
                Element itemElem = (Element)itemList.item(0);

                String name = ((Element)itemElem.getElementsByTagName("itemName").item(0)).getTextContent();
                String artist = ((Element)itemElem.getElementsByTagName("artistName").item(0)).getTextContent();
                String logo = itemElem.getElementsByTagName("logo").item(0).getTextContent();
                Node contentItem = itemElem.getElementsByTagName("ContentItem").item(0);

                //Convert ContentItem element back to xml string
                String contentItemXML = documentToString(contentItem);

                //yay we have a ContentItem!
                item = new ContentItem();
                item.xml = contentItemXML;
                item.logo = logo;
                item.track = name;
                item.artist = artist;
            }

        } catch (Exception e){
            Log.e(TAG, "/search failed for "+searchTerm);
            e.printStackTrace();
            item = null;
        }

        return item;
    }

    public int getVolume(){
        int volume = 0;

        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url + "/volume");

        try {
            //execute HTTP request
            HttpResponse response = client.execute(get);
            Document document = getDocumentFromResponse(response);
            Element actualVolume = (Element)document.getElementsByTagName("actualvolume").item(0);

            volume = Integer.parseInt(actualVolume.getTextContent());

        } catch (Exception e){
            e.printStackTrace();
        }

        return volume;
    }

    public void setVolume(final int volume){
        new Thread(){
            @Override
            public void run(){
                HttpClient client = new DefaultHttpClient();
                //send POST to /volume on the SoundTouch
                HttpPost post = new HttpPost(url + "/volume");

                try {
                    //volume XML
                    String requestContent = "";
                    requestContent += String.format("<volume>%d</volume>", volume);
                    post.setEntity(new StringEntity(requestContent));

                    //execute HTTP request
                    HttpResponse response = client.execute(post);

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void play(final ContentItem item){
        new Thread(){
            @Override
            public void run(){
                HttpClient client = new DefaultHttpClient();
                //send POST to /volume on the SoundTouch
                HttpPost post = new HttpPost(url + "/select");

                try {
                    //send <ContentItem> xml
                    post.setEntity(new StringEntity(item.xml));

                    //execute HTTP request
                    HttpResponse response = client.execute(post);

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public boolean isPlaying(){
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url+"/now_playing");

        try {
            HttpResponse response = client.execute(get);
            Document document = getDocumentFromResponse(response);

            Node nowPlay = document.getElementsByTagName("nowPlaying").item(0);
            Element playStatus = (Element)document.getElementsByTagName("playStatus").item(0);
            if(playStatus.getTextContent().equals("PLAY_STATE")){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Document getDocumentFromResponse(HttpResponse response) throws Exception{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        return dBuilder.parse(response.getEntity().getContent());
    }

    private String documentToString(Node node) throws Exception{
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StreamResult result = new StreamResult(new StringWriter());
        DOMSource source = new DOMSource(node);
        transformer.transform(source, result);
        return result.getWriter().toString();
    }

}
