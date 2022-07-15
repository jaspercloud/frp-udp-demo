package com.example.demo.app;

import com.example.demo.dto.ConnectRespDTO;
import com.example.demo.dto.TransmitDTO;
import com.example.demo.dto.UdpDTO;
import com.example.demo.dto.UdpPacket;
import com.google.gson.Gson;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import java.net.DatagramSocket;

@ConditionalOnProperty(value = "app.type", havingValue = "server")
@Configuration
public class ServerApp implements InitializingBean {

    @Value("${udp.server.port}")
    private int port;

    private Gson gson = new Gson();

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
        while (true) {
            UdpPacket revUdpPacket = UdpPacket.receive(server, 1450);
            String host = revUdpPacket.getAddress().getHostAddress();
            int port = revUdpPacket.getPort();
            byte[] data = revUdpPacket.getData();
            String json = new String(data);
            UdpDTO udpDTO = gson.fromJson(json, UdpDTO.class);
            switch (udpDTO.getType()) {
                case UdpDTO.ConnectReq: {
                    System.out.println(String.format("ConnectReq: host=%s, port=%s", revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort()));
                    ConnectRespDTO respDTO = new ConnectRespDTO();
                    respDTO.setHost(host);
                    respDTO.setPort(port);
                    UdpPacket udpPacket = new UdpPacket(gson.toJson(respDTO).getBytes());
                    udpPacket.send(server, host, port);
                    break;
                }
                case UdpDTO.Transmit: {
                    TransmitDTO transmitDTO = gson.fromJson(json, TransmitDTO.class);
                    transmitDTO.setTargetHost(revUdpPacket.getAddress().getHostAddress());
                    transmitDTO.setTargetPort(revUdpPacket.getPort());
                    System.out.println(String.format("Transmit: req=%s:%d, %s:%d->%s:%d",
                            revUdpPacket.getAddress().getHostAddress(), revUdpPacket.getPort(),
                            transmitDTO.getTransmitHost(), transmitDTO.getTransmitPort(),
                            transmitDTO.getTargetHost(), transmitDTO.getTargetPort()));
                    UdpPacket udpPacket = new UdpPacket(gson.toJson(transmitDTO).getBytes());
                    udpPacket.send(server, transmitDTO.getTransmitHost(), transmitDTO.getTransmitPort());
                    break;
                }
            }
        }
    }
}
