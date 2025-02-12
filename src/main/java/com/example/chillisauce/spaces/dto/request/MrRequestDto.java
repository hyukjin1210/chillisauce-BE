package com.example.chillisauce.spaces.dto.request;

import com.example.chillisauce.spaces.dto.response.LocationDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MrRequestDto {
    private String mrName;
    private String x;
    private String y;


    public MrRequestDto(String locationName, String x, String y) {
        this.mrName = locationName;
        this.x = x;
        this.y = y;
    }
}

