package com.example.demo.dto;

public class TransmitDTO extends UdpDTO {

    private String transmitHost;
    private Integer transmitPort;
    private String targetHost;
    private Integer targetPort;

    public TransmitDTO() {
        super(UdpDTO.Transmit);
    }

    public String getTransmitHost() {
        return transmitHost;
    }

    public void setTransmitHost(String transmitHost) {
        this.transmitHost = transmitHost;
    }

    public Integer getTransmitPort() {
        return transmitPort;
    }

    public void setTransmitPort(Integer transmitPort) {
        this.transmitPort = transmitPort;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public Integer getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(Integer targetPort) {
        this.targetPort = targetPort;
    }
}
