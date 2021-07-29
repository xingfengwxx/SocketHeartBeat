package com.wangxingxing.socketheartbeat;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Arrays;

/**
 * author : 王星星
 * date : 2021/7/29 10:04
 * email : 1099420259@qq.com
 * description :
 */
public class BackService extends Service {

    private static final String TAG = "BackService";

    public static final long HEART_BEAT_RATE = 3 * 1000;

    public static final String HOST = "192.168.0.3";
    public static final int PORT = 9800;

    public static final String ACTION_MESSAGE = "com.wangxingxing.socketheartbeat.ACTION_MESSAGE";
    public static final String ACTION_HEART_BEAT = "com.wangxingxing.socketheartbeat.ACTION_HEART_BEAT";

    private ReadThread mReadThread;

    private LocalBroadcastManager mLocalBroadcastManager;

    private WeakReference<Socket> mSocket;

    private long sendTime = 0L;
    private IBackService.Stub mBackService = new IBackService.Stub() {
        @Override
        public boolean sendMessage(String message) throws RemoteException {
            return sendMsg(message);
        }
    };

    // For heart Beat
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                boolean isSuccess = sendMsg("");//就发送一个\r\n过去 如果发送失败，就重新初始化一个socket
                if (!isSuccess) {
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBackService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new InitSocketThread().start();
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    public boolean sendMsg(String msg) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (mSocket == null || mSocket.get() == null) {
                    Log.e(TAG, "mSocket is null");
                }

                Socket socket = mSocket.get();
                try {
                    if (!socket.isClosed() && !socket.isOutputShutdown()) {
                        OutputStream os = socket.getOutputStream();
                        String message = msg + "\r\n";
                        os.write(message.getBytes());
                        os.flush();
                        sendTime = System.currentTimeMillis();//每次发送成数据，就改一下最后成功发送的时间，节省心跳间隔时间
                    } else {

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        return true;
    }

    private void initSocket() {
        //初始化Socket
        try {
            Socket socket = new Socket(HOST, PORT);
            mSocket = new WeakReference<>(socket);
            mReadThread = new ReadThread(socket);
            mReadThread.start();
            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//初始化成功后，就准备发送心跳包
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseLastSocket(WeakReference<Socket> socket) {
        try {
            if (socket != null) {
                Socket sk = socket.get();
                if (!sk.isClosed()) {
                    sk.close();
                }
                sk = null;
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            initSocket();
        }
    }

    class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @Override
        public void run() {
            super.run();
            Socket socket = mWeakSocket.get();
            if (socket != null) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length = 0;
                    while (!socket.isClosed() && !socket.isInputShutdown()
                            && isStart && ((length = is.read(buffer)) != -1)) {
                        if (length > 0) {
                            String message = new String(Arrays.copyOf(buffer, length)).trim();
                            Log.d(TAG, message);
                            //收到服务器过来的消息，就通过Broadcast发送出去
                            if (message.equals("ok")) {
                                //处理心跳回复
                                Intent intent = new Intent(BackService.ACTION_HEART_BEAT);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            } else {
                                //其他消息回复
                                Intent intent = new Intent(BackService.ACTION_MESSAGE);
                                intent.putExtra("message", message);
                                mLocalBroadcastManager.sendBroadcast(intent);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
