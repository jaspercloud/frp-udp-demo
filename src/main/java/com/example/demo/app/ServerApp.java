package com.example.demo.app;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

@ConditionalOnProperty(value = "app.type", havingValue = "server")
@Configuration
public class ServerApp implements InitializingBean {

    @Value("${udp.server.port}")
    private int port;

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            try {
                runUdpServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runUdpServer() throws Exception {
        DatagramSocket server = new DatagramSocket(port);
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        while (true) {
            server.receive(packet);
            String host = packet.getAddress().getHostAddress();
            int port = packet.getPort();
            String text = new String(packet.getData(), 0, packet.getLength());
            System.out.println(String.format("host=%s, port=%s, text=%s", host, port, text));

            //response
            byte[] bytes = text.getBytes();
            InetSocketAddress remote = new InetSocketAddress(host, port);
            server.send(new DatagramPacket(bytes, bytes.length, remote));
        }
    }
}
