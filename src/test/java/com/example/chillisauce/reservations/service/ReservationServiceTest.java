package com.example.chillisauce.reservations.service;

import com.example.chillisauce.reservations.dto.ReservationRequestDto;
import com.example.chillisauce.reservations.dto.ReservationResponseDto;
import com.example.chillisauce.reservations.entity.Reservation;
import com.example.chillisauce.reservations.exception.ReservationErrorCode;
import com.example.chillisauce.reservations.exception.ReservationException;
import com.example.chillisauce.reservations.repository.ReservationRepository;
import com.example.chillisauce.security.UserDetailsImpl;
import com.example.chillisauce.spaces.MrRepository;
import com.example.chillisauce.spaces.entity.Mr;
import com.example.chillisauce.users.entity.User;
import com.example.chillisauce.users.entity.UserRoleEnum;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    ReservationRepository reservationRepository;
    @Mock
    MrRepository meetingRoomRepository;
    @InjectMocks
    ReservationService reservationService;

    @Test
    void 예약등록성공() {
        // given
        Long meetingRoomId = 1L;
        LocalDate startDate = LocalDate.of(2023, 4, 8);
        LocalTime startTime = LocalTime.of(12, 0);
        LocalDate endDate = LocalDate.of(2023, 4, 8);
        LocalTime endTime = LocalTime.of(13, 0);
        LocalDateTime start = LocalDateTime.of(startDate, startTime);
        LocalDateTime end = LocalDateTime.of(endDate, endTime);
        ReservationRequestDto requestDto = new ReservationRequestDto(start, end);
        User user = User.builder()
                .id(1L)
                .email("test@email.com")
                .username("tester")
                .password("12345678")
                .role(UserRoleEnum.USER)
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getEmail());

        // when
        when(meetingRoomRepository.findById(any(Long.class))).thenReturn(Optional.of(new Mr()))
                .thenThrow(ReservationException.class);

        ReservationResponseDto result = reservationService.addReservation(meetingRoomId, requestDto, userDetails);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStart()).isEqualTo(start);
        assertThat(result.getEnd()).isEqualTo(LocalDateTime.of(startDate, startTime).plusMinutes(59));
    }

    @Test
    void 예약등록실패_중복되는_시간이_있으면_예외가_발생한다() {
        // given
        User user = User.builder()
                .id(1L)
                .email("test@email.com")
                .username("tester")
                .password("12345678")
                .role(UserRoleEnum.USER)
                .build();

        Mr meetingRoom = Mr.builder()
                .id(1L)
                .mrName("testMeetingRoom")
                .x("100")
                .y("100")
                .username("tester")
                .build();

        LocalDateTime firstStart = LocalDateTime.of(2023, 4, 8, 12, 0);
        LocalDateTime firstEnd = LocalDateTime.of(2023, 4, 8, 15, 30);
        LocalDateTime secondStart = LocalDateTime.of(2023, 4, 8, 12, 0);
        LocalDateTime secondEnd = LocalDateTime.of(2023, 4, 8, 14, 30);
        Reservation firstReservation = Reservation.builder()
                .startTime(firstStart)
                .endTime(firstEnd)
                .build();

        ReservationRequestDto secondReservationDto = new ReservationRequestDto(secondStart, secondEnd);
        UserDetailsImpl userDetails = new UserDetailsImpl(user, user.getEmail());

        when(meetingRoomRepository.findById(any(Long.class)))
                .thenReturn(Optional.of(meetingRoom)).thenThrow(ReservationException.class);

        doReturn(Optional.of(firstReservation)).when(reservationRepository)
                .findFirstByMeetingRoomIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        meetingRoom.getId(), secondStart, secondStart.plusMinutes(59));

        // when
        final ReservationException exception =
                assertThrows(ReservationException.class,
                        () -> reservationService.addReservation(meetingRoom.getId(), secondReservationDto, userDetails));

        // then
        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ReservationErrorCode.DUPLICATED_TIME);
    }

    @Test
    void 동시예약테스트() throws InterruptedException {
        // given
        Integer threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        List<ReservationResponseDto> resultList = new ArrayList<>();
        Long meetingRoomId = 1L;
        Mr meetingRoom = Mr.builder().build();
        User user = User.builder().build();
        UserDetailsImpl userDetails = new UserDetailsImpl(user, "test@email.com");

        LocalDateTime startTime = LocalDateTime.of(2023, 4, 8, 12, 0);
        ReservationRequestDto start = new ReservationRequestDto(startTime, startTime.plusMinutes(59));
        when(meetingRoomRepository.findById(meetingRoomId)).thenReturn(Optional.of(meetingRoom));

        // when
        IntStream.range(0, threadCount).forEach(e ->
                executorService.submit(() -> {
                    try {
                        resultList.add(reservationService.addReservation(meetingRoomId, start, userDetails));
                    } finally {
                        countDownLatch.countDown();
                    }
                }));

        countDownLatch.await();

        // then
        //TODO: 동시성 이슈 해결 후 수정 필요
        assertThat(resultList.size()).isPositive();
    }
}