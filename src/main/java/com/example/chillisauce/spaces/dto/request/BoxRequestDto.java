package com.example.chillisauce.spaces.dto.request;

import com.example.chillisauce.spaces.dto.response.LocationDto;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class BoxRequestDto {

    private String boxName;
    private String x;
    private String y;




    public BoxRequestDto(String locationName, String x, String y) {
        this.boxName = locationName;
        this.x = x;
        this.y = y;
    }
}