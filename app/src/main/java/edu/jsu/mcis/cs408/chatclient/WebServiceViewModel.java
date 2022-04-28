package edu.jsu.mcis.cs408.chatclient;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WebServiceViewModel extends ViewModel {
    private static final String TAG = "WebServiceDemoViewModel";

    private static final String CLIENT_URL = "http://ec2-3-143-211-101.us-east-2.compute.amazonaws.com/CS408_SimpleChat/Chat";
    private static final String USERNAME = "Star lord";
    private final ExecutorService requestThreadExecutor;
    private final Runnable httpGetRequestThread, httpPostRequestThread, httpDeleteRequestThread;
    private Future<?> pending;

    private MutableLiveData<String> output;

    private String message;

    public WebServiceViewModel() {
        requestThreadExecutor = Executors.newSingleThreadExecutor();

        httpGetRequestThread = new Runnable() {
            @Override
            public void run() {
                if (pending != null) {
                    pending.cancel(true);
                }

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("GET", CLIENT_URL));
                } catch (Exception e) {
                    Log.e(TAG, " Exception: ", e);
                }
            }
        };

        httpPostRequestThread = new Runnable() {
            @Override
            public void run() {
                if (pending != null) { pending.cancel(true); }

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("POST", CLIENT_URL));
                }
                catch (Exception e) { Log.e(TAG, " Exception: ", e); }
            }
        };

        httpDeleteRequestThread = new Runnable() {
            @Override
            public void run() {
                if (pending != null) { pending.cancel(true); }

                try {
                    pending = requestThreadExecutor.submit(new HTTPRequestTask("DELETE", CLIENT_URL));
                }
                catch (Exception e) { Log.e(TAG, " Exception: ", e); }
            }
        };
    }

    public void generateOutput() {
        httpGetRequestThread.run();
    }

    private void setOutput(String s) {
        this.getOutput().postValue(s);
    }

    public void sendMessage(String message) {
        this.message = message;
        httpPostRequestThread.run();
    }

    public void clearChat() {
        httpDeleteRequestThread.run();
    }

    public MutableLiveData<String> getOutput() {
        if (output == null) {
            output = new MutableLiveData<>();
        }
        return output;
    }



    final class HTTPRequestTask implements Runnable {
        private static final String TAG = "HTTPRequestTask";
        private final String method, urlString;

        HTTPRequestTask(String method, String urlString) {
            this.method = method;
            this.urlString = urlString;
        }

        @Override
        public void run() {
            setOutput(doRequest());
        }


        private String doRequest() {
            StringBuilder r = new StringBuilder();
            String line;

            HttpURLConnection conn = null;
            String input = null;

            try {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                URL url = new URL(CLIENT_URL);

                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod(method);
                conn.setDoInput(true);

                if (method.equals("POST")) {
                    JSONObject outputJSON = new JSONObject();
                    outputJSON.put("message", message);
                    outputJSON.put("name", USERNAME);

                    conn.setDoOutput(true);
                    OutputStream out = conn.getOutputStream();
                    out.write(outputJSON.toString().getBytes());
                    out.flush();
                    out.close();
                }

                conn.connect();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                int code = conn.getResponseCode();

                if (code == HTTP_OK || code == HTTP_CREATED) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    do {
                        line = br.readLine();
                        if (line != null) {
                            r.append(line);
                        }
                    } while(line != null);
                }

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                JSONObject jo = new JSONObject(r.toString());
                input =  (String)jo.get("messages");

            } catch (Exception e) {
                Log.e(TAG, " Exception: ", e);
            } finally {
                if (conn != null) { conn.disconnect(); }
            }

            return input;
        }
    }
}

