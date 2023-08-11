package com.example.demo.dto;

import java.util.UUID;

public class PingDTO extends UdpDTO {

    private String uuid = UUID.randomUUID().toString();

    public String getUuid() {
        return uuid;
    }

    public PingDTO() {
        super(UdpDTO.Ping);
    }
}
