package com.wangxingxing.socketheartbeat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.wangxingxing.socketheartbeat.databinding.ActivityMainBinding;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ActivityMainBinding mBinding;

    private MessageBackReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private LocalBroadcastManager mLocalBroadcastManager;
    private Intent mServiceIntent;

    private IBackService mBackService;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBackService = IBackService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        init();
    }

    private void init() {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mReceiver = new MessageBackReceiver(mBinding.tvResult);
        mServiceIntent = new Intent(this, BackService.class);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BackService.ACTION_HEART_BEAT);
        mIntentFilter.addAction(BackService.ACTION_MESSAGE);

        mBinding.btnSendMsg.setOnClickListener(v -> {
            String msg = mBinding.etMsg.getText().toString();
            if (TextUtils.isEmpty(msg)) {
                msg = "hello test socket";
            }
            try {
                boolean isSend = mBackService.sendMessage(msg);
                Log.d(TAG, "init: isSend=" + isSend);
                mBinding.etMsg.setText("");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    class MessageBackReceiver extends BroadcastReceiver {
        private WeakReference<TextView> mTextView;

        public MessageBackReceiver(TextView textView) {
            mTextView = new WeakReference<>(textView);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            TextView tv = mTextView.get();
            if (TextUtils.equals(action, BackService.ACTION_HEART_BEAT)) {
                if (tv != null) {
                    tv.setText("Get a heart heat");
                }
            } else {
                String message = intent.getStringExtra("message");
                tv.setText(message);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocalBroadcastManager.registerReceiver(mReceiver, mIntentFilter);
        bindService(mServiceIntent, conn, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(conn);
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }
}