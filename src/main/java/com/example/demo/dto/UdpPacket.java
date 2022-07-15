package com.example.demo.dto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UdpPacket {

    private DatagramPacket packet;

    public UdpPacket(byte[] bytes) {
        this.packet = new DatagramPacket(bytes, bytes.length);
    }

    private UdpPacket(DatagramPacket packet) {
        this.packet = packet;
    }

    public void send(DatagramSocket socket, String host, int port) throws IOException {
        packet.setAddress(InetAddress.getByName(host));
        packet.setPort(port);
        socket.send(packet);
    }

    public static UdpPacket receive(DatagramSocket socket, int size) throws IOException {
        byte[] bytes = new byte[size];
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);
        return new UdpPacket(packet);
    }

    public byte[] getData() {
        byte[] buf = Arrays.copyOf(packet.getData(), packet.getLength());
        return buf;
    }

    public InetAddress getAddress() {
        InetAddress address = packet.getAddress();
        return address;
    }

    public int getPort() {
        int port = packet.getPort();
        return port;
    }
}
