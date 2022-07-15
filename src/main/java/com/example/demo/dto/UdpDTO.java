package com.example.demo.dto;

public class UdpDTO {

    public static final String ConnectReq = "connectReq";
    public static final String ConnectResp = "connectResp";
    public static final String Ping = "ping";
    public static final String Transmit = "transmit";
    public static final String Data = "data";

    private String type;

    public UdpDTO() {
    }

    public UdpDTO(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
