package com.example.demo.dto;

import java.util.UUID;

public class PongDTO extends UdpDTO {

    private String uuid = UUID.randomUUID().toString();

    public String getUuid() {
        return uuid;
    }

    public PongDTO() {
        super(UdpDTO.Pong);
    }
}
