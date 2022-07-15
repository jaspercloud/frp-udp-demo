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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@ConditionalOnProperty(value = "app.type", havingValue = "client")
@Configuration
public class ClientApp implements InitializingBean {

    @Value("${udp.connect.host}")
    private String host;

    @Value("${udp.server.port}")
    private int port;

    private ExecutorService executorService;
    private DatagramSocket server;

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService = Executors.newCachedThreadPool();
        server = new DatagramSocket();
        {
            byte[] bytes = "hello".getBytes();
            InetSocketAddress remote = new InetSocketAddress(host, port);
            server.send(new DatagramPacket(bytes, bytes.length, remote));
        }
        {
            byte[] bytes = new byte[1450];
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
            server.receive(packet);
            String text = new String(packet.getData(), 0, packet.getLength());
            System.out.println("remote: " + text);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] bytes = new byte[1450];
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                        server.receive(packet);
                        server.send(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @PostMapping("/connect")
    public void connect(@RequestParam("host") String host,
                        @RequestParam("port") int port,
                        @RequestParam("retry") int retry) throws Exception {
        new Thread(() -> {
            try {
                for (int i = 0; i < retry; i++) {
                    DatagramSocket client = new DatagramSocket();
                    byte[] bytes = "hello".getBytes();
                    InetSocketAddress remote = new InetSocketAddress(host, port);
                    client.send(new DatagramPacket(bytes, bytes.length, remote));
                    Future<DatagramPacket> future = executorService.submit(new Callable<DatagramPacket>() {
                        @Override
                        public DatagramPacket call() throws Exception {
                            byte[] bytes = new byte[1450];
                            DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                            client.receive(packet);
                            return packet;
                        }
                    });
                    try {
                        future.get(3000, TimeUnit.MILLISECONDS);
                        loop(client, remote);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loop(DatagramSocket client, InetSocketAddress remote) throws Exception {
        while (true) {
            {
                byte[] bytes = "hello".getBytes();
                client.send(new DatagramPacket(bytes, bytes.length, remote));
            }
            {
                byte[] bytes = new byte[1450];
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
                client.receive(packet);
                String text = new String(packet.getData(), 0, packet.getLength());
                System.out.println("rev: " + text);
            }
            Thread.sleep(1000L);
        }
    }
}
