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

    private DatagramSocket client;
    private ExecutorService executorService;
    private Gson gson = new Gson();

    private ConnectRespDTO connectRespDTO;

    @Override
    public void afterPropertiesSet() throws Exception {
        executorService = Executors.newCachedThreadPool();
        client = new DatagramSocket();
        client.setReuseAddress(true);
        ConnectReqDTO udpDTO = new ConnectReqDTO();
        UdpPacket udpPacket = new UdpPacket(gson.toJson(udpDTO).getBytes());
        udpPacket.send(client, serverHost, serverPort);
        UdpPacket revUdpPacket = UdpPacket.receive(client, 1450);
        byte[] data = revUdpPacket.getData();
        String json = new String(data);
        connectRespDTO = gson.fromJson(json, ConnectRespDTO.class);
        System.out.println(String.format("remote: localPort=%s, host=%s, port=%s",
                client.getLocalPort(), connectRespDTO.getHost(), connectRespDTO.getPort()));
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        BeatDTO reqHeart = new BeatDTO();
                        UdpPacket udpPacket = new UdpPacket(gson.toJson(reqHeart).getBytes());
                        udpPacket.send(client, serverHost, serverPort);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        UdpPacket revUdpPacket = UdpPacket.receive(client, 1450);
                        byte[] data = revUdpPacket.getData();
                        String json = new String(data);
                        UdpDTO udpDTO = gson.fromJson(json, UdpDTO.class);
                        switch (udpDTO.getType()) {
                            case UdpDTO.Ping: {
                                PingDTO pingDTO = gson.fromJson(json, PingDTO.class);
                                System.out.println(String.format("revPing: host=%s, port=%s uuid=%s",
                                        revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort(), pingDTO.getUuid()));
                                UdpPacket udpPacket = new UdpPacket(gson.toJson(new PongDTO()).getBytes());
                                udpPacket.send(client, revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort());
                                break;
                            }
                            case UdpDTO.Pong: {
                                PongDTO pongDTO = gson.fromJson(json, PongDTO.class);
                                System.out.println(String.format("revPong: host=%s, port=%s uuid=%s",
                                        revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort(), pongDTO.getUuid()));
                                break;
                            }
                            case UdpDTO.Data: {
                                System.out.println(String.format("revData: host=%s, port=%s", revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort()));
                                UdpPacket udpPacket = new UdpPacket(data);
                                udpPacket.send(client, revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort());
                                break;
                            }
                            case UdpDTO.Transmit: {
                                TransmitDTO transmitDTO = gson.fromJson(json, TransmitDTO.class);
                                System.out.println(String.format("transmit: host=%s, port=%s", transmitDTO.getTargetHost(), transmitDTO.getTargetPort()));
                                PongDTO pongDTO = new PongDTO();
                                UdpPacket udpPacket = new UdpPacket(gson.toJson(pongDTO).getBytes());
                                udpPacket.send(client, transmitDTO.getTargetHost(), transmitDTO.getTargetPort());
                                break;
                            }
                            case UdpDTO.Beat: {
                                BeatDTO beatDTO = gson.fromJson(json, BeatDTO.class);
                                String uuid = beatDTO.getUuid();
//                                System.out.println(String.format("recv: heart=%s", uuid));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @PostMapping("/once")
    public void once(@RequestParam("host") String host,
                     @RequestParam("port") int port) throws Exception {
        System.out.println("localPort=" + client.getLocalPort());
        //sendTarget
        {
            PingDTO pingDTO = new PingDTO();
            UdpPacket udpPacket = new UdpPacket(gson.toJson(pingDTO).getBytes());
            udpPacket.send(client, host, port);
            System.out.println(String.format("sendConnectPing: %s:%d uuid=%s", host, port, pingDTO.getUuid()));
        }
        //sendTransmit
        {
            TransmitDTO transmitDTO = new TransmitDTO();
            transmitDTO.setTransmitHost(host);
            transmitDTO.setTransmitPort(port);
            UdpPacket udpPacket = new UdpPacket(gson.toJson(transmitDTO).getBytes());
            udpPacket.send(client, serverHost, serverPort);
        }
        Future<UdpPacket> future = executorService.submit(new Callable<UdpPacket>() {
            @Override
            public UdpPacket call() throws Exception {
                UdpPacket udpPacket = UdpPacket.receive(client, 1450);
                return udpPacket;
            }
        });
        try {
            future.get(1000L, TimeUnit.MILLISECONDS);
            System.out.println(String.format("Connected: %s:%d", host, port));
        } catch (TimeoutException e) {
            System.out.println(String.format("ConnectTimeout: %s:%d", host, port));
        }
    }

    @PostMapping("/connect")
    public void connect(@RequestParam("host") String host,
                        @RequestParam("port") int port) throws Exception {
        System.out.println(String.format("local: localPort=%s, host=%s, port=%s",
                client.getLocalPort(), connectRespDTO.getHost(), connectRespDTO.getPort()));
        //sendTarget
        {
            PingDTO pingDTO = new PingDTO();
            UdpPacket udpPacket = new UdpPacket(gson.toJson(pingDTO).getBytes());
            System.out.println(String.format("sendTarget: %s:%d", host, port));
            udpPacket.send(client, host, port);
        }
        //sendTransmit
        {
            TransmitDTO transmitDTO = new TransmitDTO();
            transmitDTO.setTransmitHost(host);
            transmitDTO.setTransmitPort(port);
            transmitDTO.setTargetHost(connectRespDTO.getHost());
            transmitDTO.setTargetPort(connectRespDTO.getPort());
            UdpPacket udpPacket = new UdpPacket(gson.toJson(transmitDTO).getBytes());
            System.out.println(String.format("sendTransmit: %s:%d", serverHost, serverPort));
            udpPacket.send(client, serverHost, serverPort);
        }
        Thread.sleep(1000L);
    }

    @PostMapping("/sendTarget")
    public void sendTarget(@RequestParam("host") String host,
                           @RequestParam("port") int port) throws Exception {
        System.out.println(String.format("local: localPort=%s, host=%s, port=%s",
                client.getLocalPort(), connectRespDTO.getHost(), connectRespDTO.getPort()));
        //sendTarget
        PingDTO pingDTO = new PingDTO();
        UdpPacket udpPacket = new UdpPacket(gson.toJson(pingDTO).getBytes());
        System.out.println(String.format("sendTarget: %s:%d", host, port));
        udpPacket.send(client, host, port);
        Thread.sleep(1000L);
    }

    @PostMapping("/sendTransmit")
    public void sendTransmit(@RequestParam("host") String host,
                             @RequestParam("port") int port) throws Exception {
        System.out.println(String.format("local: localPort=%s, host=%s, port=%s",
                client.getLocalPort(), connectRespDTO.getHost(), connectRespDTO.getPort()));
        TransmitDTO transmitDTO = new TransmitDTO();
        transmitDTO.setTransmitHost(host);
        transmitDTO.setTransmitPort(port);
        transmitDTO.setTargetHost(connectRespDTO.getHost());
        transmitDTO.setTargetPort(connectRespDTO.getPort());
        UdpPacket udpPacket = new UdpPacket(gson.toJson(transmitDTO).getBytes());
        System.out.println(String.format("sendTransmit: %s:%d", serverHost, serverPort));
        udpPacket.send(client, serverHost, serverPort);
        Thread.sleep(1000L);
    }
}
