package com.example.chillisauce.reservations.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationListResponseDto {
    List<ReservationDetailResponseDto> reservationList;
}
