package com.example.demo.app;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

@RestController
@ConditionalOnProperty(value = "app.type", havingValue = "client")
@Configuration
public class ClientApp implements InitializingBean {

    @Value("${udp.client.host}")
    private String host;

    @Value("${udp.client.port}")
    private int port;

    private DatagramSocket client;

    @Override
    public void afterPropertiesSet() throws Exception {
        client = new DatagramSocket();
        new Thread(() -> {
            try {
                runUdpClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runUdpClient() throws Exception {
        {
            byte[] bytes = "test".getBytes();
            InetSocketAddress remote = new InetSocketAddress(host, port);
            client.send(new DatagramPacket(bytes, bytes.length, remote));
        }
        {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            while (true) {
                client.receive(packet);
                String host = packet.getAddress().getHostAddress();
                int port = packet.getPort();
                String text = new String(packet.getData(), 0, packet.getLength());
                System.out.println(String.format("host=%s, port=%s, text=%s", host, port, text));
            }
        }
    }

    @PostMapping("/connect")
    public void connect(@RequestParam("host") String host,
                        @RequestParam("port") int port) throws Exception {
        byte[] bytes = "test".getBytes();
        InetSocketAddress remote = new InetSocketAddress(host, port);
        client.send(new DatagramPacket(bytes, bytes.length, remote));
    }
}
