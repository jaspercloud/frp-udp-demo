package com.example.demo.app;

import com.example.demo.dto.*;
import com.google.gson.Gson;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.concurrent.*;

@RestController
@ConditionalOnProperty(value = "app.type", havingValue = "client")
@Configuration
public class ClientApp implements InitializingBean {

    @Value("${udp.connect.host}")
    private String serverHost;

    @Value("${udp.server.port}")
    private int serverPort;

    private ExecutorService executorService;
    private DatagramSocket server;
    private ConnectRespDTO connectRespDTO;
    private Gson gson = new Gson();

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService = Executors.newCachedThreadPool();
        server = new DatagramSocket();
        {
            ConnectReqDTO udpDTO = new ConnectReqDTO();
            UdpPacket udpPacket = new UdpPacket(gson.toJson(udpDTO).getBytes());
            udpPacket.send(server, serverHost, serverPort);
        }
        {
            UdpPacket revUdpPacket = UdpPacket.receive(server, 1450);
            byte[] data = revUdpPacket.getData();
            String json = new String(data);
            connectRespDTO = gson.fromJson(json, ConnectRespDTO.class);
            System.out.println(String.format("remote: host=%s, port=%s", connectRespDTO.getHost(), connectRespDTO.getPort()));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        UdpPacket revUdpPacket = UdpPacket.receive(server, 1450);
                        byte[] data = revUdpPacket.getData();
                        String json = new String(data);
                        UdpDTO udpDTO = gson.fromJson(json, UdpDTO.class);
                        switch (udpDTO.getType()) {
                            case UdpDTO.Ping: {
                                System.out.println(String.format("revPing: host=%s, port=%s", revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort()));
                                UdpPacket udpPacket = new UdpPacket(data);
                                udpPacket.send(server, revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort());
                                break;
                            }
                            case UdpDTO.Data: {
                                System.out.println(String.format("revData: host=%s, port=%s", revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort()));
                                UdpPacket udpPacket = new UdpPacket(data);
                                udpPacket.send(server, revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort());
                                break;
                            }
                            case UdpDTO.Transmit: {
                                TransmitDTO transmitDTO = gson.fromJson(json, TransmitDTO.class);
                                UdpPacket udpPacket = new UdpPacket(data);
                                udpPacket.send(server, transmitDTO.getTargetHost(), transmitDTO.getTargetPort());
                                break;
                            }
                        }
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
                DatagramSocket client = new DatagramSocket();
                for (int i = 0; i < retry; i++) {
                    //sendTransmit
                    {
                        TransmitDTO transmitDTO = new TransmitDTO();
                        transmitDTO.setTransmitHost(host);
                        transmitDTO.setTransmitPort(port);
                        UdpPacket udpPacket = new UdpPacket(gson.toJson(transmitDTO).getBytes());
                        udpPacket.send(client, serverHost, serverPort);
                    }
                    Thread.sleep(50L);
                    //sendTarget
                    {
                        UdpPacket udpPacket = new UdpPacket(gson.toJson(new PingDTO()).getBytes());
                        udpPacket.send(client, host, port);
                        System.out.println(String.format("sendConnectPing: i=%s %s:%d", i, host, port));
                    }
                    Future<UdpPacket> future = executorService.submit(new Callable<UdpPacket>() {
                        @Override
                        public UdpPacket call() throws Exception {
                            UdpPacket udpPacket = UdpPacket.receive(client, 1450);
                            return udpPacket;
                        }
                    });
                    try {
                        future.get(200, TimeUnit.MILLISECONDS);
                        System.out.println(String.format("Connected: %s:%d", host, port));
                        loop(client, host, port);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loop(DatagramSocket client, String host, int port) throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        UdpPacket udpPacket = UdpPacket.receive(client, 1450);
                        String json = new String(udpPacket.getData());
                        DataDTO dataDTO = gson.fromJson(json, DataDTO.class);
                        System.out.println(String.format("revData: host=%s, port=%s, data=%s",
                                udpPacket.getAddress().getHostAddress(), udpPacket.getPort(),
                                new String(dataDTO.getData())));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        int i = 0;
        while (true) {
            String data = "data" + i++;
            System.out.println(String.format("sendData: %s:%d data=%s", host, port, data));
            DataDTO dataDTO = new DataDTO();
            dataDTO.setData(data.getBytes());
            UdpPacket udpPacket = new UdpPacket(gson.toJson(dataDTO).getBytes());
            udpPacket.send(client, host, port);
            Thread.sleep(1000L);
        }
    }
}
