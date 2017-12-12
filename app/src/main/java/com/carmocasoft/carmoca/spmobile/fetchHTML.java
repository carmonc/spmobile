package com.carmocasoft.carmoca.spmobile;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by carl on 12/11/17.
 */


class fetchHTML extends AsyncTask<String, Void, String>
{
    String HTML_response= "";

    OnTaskFinished onOurTaskFinished;


    public fetchHTML(OnTaskFinished onTaskFinished)
    {
        onOurTaskFinished = onTaskFinished;
    }
    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... urls)
    {
        try
        {
            URL url = new URL(urls[0]); // enter your url here which to download

            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String inputLine;

            while ((inputLine = br.readLine()) != null)
            {
                // System.out.println(inputLine);
                HTML_response += inputLine;
            }
            br.close();

            System.out.println("Done");

        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return HTML_response;
    }

    @Override
    protected void onPostExecute(String feed)
    {
        onOurTaskFinished.onHtmlRetrieved(feed);
    }
}
