package com.example.demo.dto;

public class DataDTO extends UdpDTO {

    private byte[] data = new byte[0];

    public DataDTO() {
        super(UdpDTO.Data);
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
