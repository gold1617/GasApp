package com.example.gasapp;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class URLHelper  {

    private static CookieStore cookieStore;
    private static HttpContext localContext;

    public  static JSONObject myAPIGet(String url, String AuthKey)
    {
        JSONObject JSONResponse = null;

        if(cookieStore == null || localContext == null)
        {
            cookieStore = new BasicCookieStore();
            localContext = new BasicHttpContext();
            localContext.setAttribute(ClientContext.COOKIE_STORE,cookieStore);
        }



        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        try
        {
            httpGet.addHeader("X-Api-Key", AuthKey);


            HttpResponse response = client.execute(httpGet,localContext);
            int status = response.getStatusLine().getStatusCode();
            Log.d("NETWORK",response.getStatusLine().getReasonPhrase());
            Log.d("NETWORK",String.valueOf(status));
            final String responseBody = EntityUtils.toString(response.getEntity());
            JSONResponse = new JSONObject(responseBody);
        }
        catch (UnsupportedEncodingException e)
        {
            Log.e("AUTH", "Error sending to server", e);
        }
        catch (IOException e)
        {
            Log.e("AUTH", "Error sending to server", e);
        }
        catch (JSONException e)
        {
            Log.e("AUTH", "Error sending to server", e);
        }
        catch (Exception e)
        {
            Log.e("AUTH", "Error sending to server", e);
        }
        finally
        {
            return JSONResponse;
        }
    }
}
