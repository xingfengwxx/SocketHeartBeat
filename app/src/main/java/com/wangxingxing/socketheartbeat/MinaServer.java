package com.wangxingxing.socketheartbeat;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * author : 王星星
 * date : 2021/7/29 11:40
 * email : 1099420259@qq.com
 * description : 心跳机制服务器代码
 */
public class MinaServer {
    public static void main(String[] args) {
        SocketAcceptor acceptor = new NioSocketAcceptor();
        acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new TextLineCodecFactory(Charset.forName("UTF-8"))));
        acceptor.setHandler(new ServerHandler());
        acceptor.getSessionConfig().setReadBufferSize(2048);
        acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);
        int bindPort = 9800;
        try {
            acceptor.bind(new InetSocketAddress(bindPort));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static class ServerHandler extends IoHandlerAdapter {

        @Override
        public void messageReceived(IoSession session, Object message)
                throws Exception {
            super.messageReceived(session, message);
            System.out.println("Received:" + message);
            if (message.toString().length() > 0) {
                session.write(message.toString().trim() + " reveived at: " + System.currentTimeMillis());
            } else {
                session.write("ok");
            }
        }

        @Override
        public void messageSent(IoSession session, Object message) throws Exception {
            super.messageSent(session, message);
            System.out.println("messageSent:" + message);
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            super.sessionClosed(session);
            System.out.println("sessionClosed:" + session);
        }

        @Override
        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            System.out.println("sessionCreated:" + session);
            session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 3 * 1000);
        }

        @Override
        public void sessionIdle(IoSession session, IdleStatus status)
                throws Exception {
            super.sessionIdle(session, status);
            System.out.println("sessionIdle:" + session);
            if (status == IdleStatus.BOTH_IDLE) {
                session.write("heartBeat");
            }
        }

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            super.sessionOpened(session);
            System.out.println("sessionOpened:" + session);
            session.write("Hello");
        }
    }
}
