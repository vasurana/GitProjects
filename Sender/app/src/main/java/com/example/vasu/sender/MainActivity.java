package com.example.vasu.sender;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.opentok.android.Connection;
import com.opentok.android.Session;
import com.opentok.android.Subscriber;
import com.opentok.android.SubscriberKit;
import com.opentok.android.Stream;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.OpentokError;
import com.opentok.android.SubscriberKit;


public class MainActivity extends ActionBarActivity implements WebServiceCoordinator.Listener,
        Session.SessionListener, SubscriberKit.SubscriberListener,Session.SignalListener, View.OnClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String SIGNAL_TYPE_CHAT = "chat";

    private WebServiceCoordinator mWebServiceCoordinator;

    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private Session mSession;
    private Subscriber mSubscriber;
    private FrameLayout mSubscriberViewContainer;

    private Button mSendButton;
    private EditText mMessageEditText;

    private ChatMessageAdapter mMessageHistory;
    private ListView mMessageHistoryListView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSubscriberViewContainer = (FrameLayout)findViewById(R.id.subscriber_container);
        mSendButton = (Button) findViewById(R.id.send_button);
        mMessageEditText = (EditText) findViewById(R.id.message_edit_text);

        // Attach handlers to UI
        mSendButton.setOnClickListener(this);


        // initialize WebServiceCoordinator and kick off request for necessary data
        mWebServiceCoordinator = new WebServiceCoordinator(this, this);
        mWebServiceCoordinator.fetchSessionConnectionData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeSession() {
        mSession = new Session(this, mApiKey, mSessionId);
        mSession.setSessionListener(this);
        mSession.setSignalListener(this);
        mSession.connect(mToken);
    }

    private void logOpenTokError(OpentokError opentokError) {
        Log.e(LOG_TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(LOG_TAG, "Error Code: " + opentokError.getErrorCode().name());
    }

    private void sendMessage() {
        disableMessageViews();
        mSession.sendSignal(SIGNAL_TYPE_CHAT, mMessageEditText.getText().toString());
        mMessageEditText.setText("");
        enableMessageViews();
    }

    private void disableMessageViews() {
        mMessageEditText.setEnabled(false);
        mSendButton.setEnabled(false);
    }

    private void enableMessageViews() {
        mMessageEditText.setEnabled(true);
        mSendButton.setEnabled(true);
    }

    private void showMessage(String message) {
        Toast toast = Toast.makeText(this, getString(R.string.message_toast_label, message), Toast.LENGTH_SHORT);
        toast.show();
    }

    /* Web Service Coordinator delegate methods */

    @Override
    public void onSessionConnectionDataReady(String apiKey, String sessionId, String token) {
        mApiKey = apiKey;
        mSessionId = sessionId;
        mToken = token;

        initializeSession();
    }

    @Override
    public void onWebServiceCoordinatorError(Exception error) {
        Log.e(LOG_TAG, "Web Service error: " + error.getMessage());
    }

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {
        Log.i(LOG_TAG, "Session Connected");
      /*  if (mSubscriber != null) {
                      mSession.subscribe(mSubscriber);
                    }*/

    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(LOG_TAG, "Session Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Received");
        if (mSubscriber == null) {
                      mSubscriber = new Subscriber(this, stream);
                      mSubscriber.setSubscriberListener(this);
                        mSubscriber.getRenderer().setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                                BaseVideoRenderer.STYLE_VIDEO_FILL);
                        mSession.subscribe(mSubscriber);
                   }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(LOG_TAG, "Stream Dropped");
        if (mSubscriber != null) {
                        mSubscriber = null;
                        mSubscriberViewContainer.removeAllViews();
                    }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }

    /* Subscriber Listener methods */

    @Override
    public void onConnected(SubscriberKit subscriberKit) {
               Log.i(LOG_TAG, "Subscriber Connected");
               mSubscriberViewContainer.addView(mSubscriber.getView());
            }

    @Override
        public void onDisconnected(SubscriberKit subscriberKit) {
                Log.i(LOG_TAG, "Subscriber Disconnected");
            }

    @Override
        public void onError(SubscriberKit subscriberKit, OpentokError opentokError) {
                logOpenTokError(opentokError);
            }

    @Override
    public void onClick(View v) {
        if (v.equals(mSendButton)) {
            sendMessage();
        }
    }

    @Override
    public void onSignalReceived(Session session, String type, String data, Connection connection) {

        switch (type) {
            case SIGNAL_TYPE_CHAT:
                showMessage(data);
                break;
        }

    }
}
