package com.example.demo.dto;

import java.util.UUID;

public class BeatDTO extends UdpDTO {

    private String uuid = UUID.randomUUID().toString();

    public String getUuid() {
        return uuid;
    }

    public BeatDTO() {
        super(UdpDTO.Beat);
    }

    public BeatDTO(String uuid) {
        super(UdpDTO.Beat);
        this.uuid = uuid;
    }

}
