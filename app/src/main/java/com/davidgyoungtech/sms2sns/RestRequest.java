package com.davidgyoungtech.sms2sns;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dyoung on 5/2/18.
 */

class RestRequest {
    private static final String TAG = RestRequest.class.getSimpleName();
    private static boolean mRequestActive = false;
    private AsyncCaller mAsyncCaller = new AsyncCaller();

    public HashMap<String,String> getHeadersForJsonRequestWithBody() {
        HashMap<String,String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Accept", "application/json");
        return map;
    }

    public void makeRequest(String url, String operation, String body, Map<String,String> headers, RestResponseHandler responseHandler) {
        if (mRequestActive) {
            if (responseHandler != null) {
                responseHandler.onFail(new IllegalStateException("Previous request in progress"));
            }
        }
        mAsyncCaller.mUrl = url;
        mAsyncCaller.mOperation = operation;
        mAsyncCaller.mRequestBody = body;
        mAsyncCaller.mRequestHeaders = headers;
        mAsyncCaller.mResponseHandler = responseHandler;
        mAsyncCaller.execute(null, null, null);
    }

    public boolean ismRequestActive() {
        return mRequestActive;
    }

    public interface RestResponseHandler {
        public void onFail(Exception e);
        public void onResponse(int httpStatus, Map<String,List<String>> headers, String body);
    }


    private class AsyncCaller extends AsyncTask<Void, Void, Void> {

        public RestResponseHandler mResponseHandler;
        public String mUrl;
        public String mOperation;
        public String mRequestBody;
        public Map<String,String> mRequestHeaders;
        Map<String,List<String>> mResponseHeaders;
        int mResponseCode;
        String mResponseBody;
        Exception mException;


        public AsyncCaller prepareCall(String url, String operation, String requestBody, Map<String,String> headers, RestResponseHandler responseHandler) {
            mResponseHandler = responseHandler;
            mOperation = operation;
            mRequestBody = requestBody;
            mUrl = url;
            mRequestHeaders = mRequestHeaders;
            return this;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRequestActive = true;
        }

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "start doInBackground");

            mException = null;
            try {
                sendRequest();
            }
            catch (Exception e) {
                Log.e(TAG, "Cannot send request", e);
                mException = new Exception("Cannot send request", e);
            }
            Log.d(TAG, "finish doInBackground");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "start onPostExecute");
            super.onPostExecute(result);
            mRequestActive = false;
            if (mResponseHandler != null) {
                if (mException != null) {
                    mResponseHandler.onFail(mException);
                }
                else {
                    mResponseHandler.onResponse(mResponseCode, mResponseHeaders, mResponseBody );
                }
            }
            Log.d(TAG, "finish onPostExecute");
        }

        public void sendRequest() throws Exception {
            StringBuilder responseBuilder = new StringBuilder();
            HttpURLConnection conn = null;
            URL url = new URL(mUrl);
            mResponseCode = -1;
            mResponseBody = null;
            mResponseHeaders = null;
            Log.d(TAG, "calling service at " + mUrl);
            conn = (HttpURLConnection) url.openConnection();
            for (String headerKey : mRequestHeaders.keySet()) {
                conn.addRequestProperty(headerKey, mRequestHeaders.get(headerKey));
            }
            conn.setRequestMethod(mOperation.toUpperCase());
            if (mRequestBody != null) {
                OutputStream out = conn.getOutputStream();
                try {
                    Writer writer = new OutputStreamWriter(out, "UTF-8");
                    Log.d(TAG, "posting: " + mRequestBody);
                    writer.write(mRequestBody);
                    writer.close();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
            mResponseCode = conn.getResponseCode();
            mResponseHeaders = conn.getHeaderFields();


            Log.d(TAG, "response code is " + conn.getResponseCode());
            BufferedReader in = null;
            try {
                if (mResponseCode >= 200 && mResponseCode <= 299) {
                    in = new BufferedReader(
                            new InputStreamReader(
                                    conn.getInputStream())
                    );

                }
                else {
                    in = new BufferedReader(
                            new InputStreamReader(
                                    conn.getErrorStream()
                            )
                    );

                }
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    responseBuilder.append(inputLine);
                in.close();
                Log.d(TAG, "response is " + responseBuilder.toString());
                mResponseBody = responseBuilder.toString();
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        }
    }
}

