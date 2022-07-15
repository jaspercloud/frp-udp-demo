package com.example.demo.dto;

public class ConnectRespDTO extends UdpDTO {

    private String host;
    private Integer port;

    public ConnectRespDTO() {
        super(UdpDTO.ConnectResp);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
