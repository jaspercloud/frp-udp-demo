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
        byte[] data = new byte[1450];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        while (true) {
            server.receive(packet);
            String host = packet.getAddress().getHostAddress();
            int port = packet.getPort();
            System.out.println(String.format("host=%s, port=%s", host, port));

            byte[] bytes = String.format("%s:%d", host, port).getBytes();
            InetSocketAddress remote = new InetSocketAddress(host, port);
            server.send(new DatagramPacket(bytes, bytes.length, remote));
        }
    }
}
