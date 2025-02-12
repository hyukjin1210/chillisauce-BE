package com.example.chillisauce.users.service;

import com.example.chillisauce.reservations.entity.Reservation;
import com.example.chillisauce.reservations.repository.ReservationRepository;
import com.example.chillisauce.schedules.entity.Schedule;
import com.example.chillisauce.schedules.repository.ScheduleRepository;
import com.example.chillisauce.security.UserDetailsImpl;
import com.example.chillisauce.spaces.entity.UserLocation;
import com.example.chillisauce.spaces.repository.UserLocationRepository;
import com.example.chillisauce.users.dto.request.RoleDeptUpdateRequestDto;
import com.example.chillisauce.users.dto.response.UserDetailResponseDto;
import com.example.chillisauce.users.dto.response.UserListResponseDto;
import com.example.chillisauce.users.entity.Companies;
import com.example.chillisauce.users.entity.User;
import com.example.chillisauce.users.entity.UserRoleEnum;
import com.example.chillisauce.users.exception.UserException;
import com.example.chillisauce.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.example.chillisauce.fixture.FixtureFactory.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private UserLocationRepository userLocationRepository;
    @Mock
    CacheManager cacheManager;
    @Mock
    Cache userDetailsCache;

    private Companies company = Company_생성();
    private User user = User_USER권한_생성(company);
    private User admin = User_ADMIN권한_생성(company, "gurwlstm1210@gmail.com");
    private User user2 = User_USER권한_생성_아이디지정(3L, company);

    @Nested
    @DisplayName("성공 케이스")
    class SuccessCase {
        @DisplayName("사원 선택 조회")
        @Test
        void success1() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());
            when(userRepository.findByIdAndCompanies_CompanyName(admin.getId(), admin.getCompanies().getCompanyName())).thenReturn(Optional.of(admin));

            //when
            UserDetailResponseDto result = adminService.getUsers(admin.getId(), details);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testAdminUser");

        }

        @DisplayName("사원 목록 전체 조회")
        @Test
        void success2() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            List<User> allUsers = List.of(
                    User.builder().id(1L).email("123@123").build(),
                    User.builder().id(1L).email("123@123").build(),
                    User.builder().id(1L).email("123@123").build());
            when(userRepository.findAllByCompanies_CompanyName(admin.getCompanies().getCompanyName())).thenReturn(allUsers);

            //when
            UserListResponseDto result = adminService.getAllUsers(details);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getUserList().size()).isEqualTo(3);

        }

        @DisplayName("사원 권한 수정")
        @Test
        void success3() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            RoleDeptUpdateRequestDto requestDto = RoleDeptUpdateRequestDto.builder()
                    .updateRole(true)
                    .role(UserRoleEnum.MANAGER)
                    .build();

            //when
            when(userRepository.findByIdAndCompanies_CompanyName(2L, user.getCompanies().getCompanyName())).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenReturn(user);
            UserDetailResponseDto result = adminService.editUser(2L, details, requestDto);
            when(cacheManager.getCache("UserDetails")).thenReturn(userDetailsCache);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(UserRoleEnum.MANAGER);
            assertThat(user.getRole()).isEqualTo(UserRoleEnum.MANAGER);

            adminService.evictCacheByEmail(user.getEmail());
            verify(userDetailsCache, times(1)).evict(user.getEmail());

        }

        @DisplayName("사원 삭제")
        @Test
        void success4() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());


            Schedule schedule = Schedule.builder()
                    .user(user)
                    .build();

            Reservation reservation = Reservation.builder()
                    .user(user)
                    .build();

            UserLocation location = UserLocation.builder()
                    .userId(2L).
                    build();

            //when
            when(userRepository.findById(any())).thenReturn(Optional.of(user));
            when(scheduleRepository.findAllByUserId(any())).thenReturn(List.of(schedule));
            when(reservationRepository.findAllByUserId(any())).thenReturn(List.of(reservation));
            when(userLocationRepository.findByUserId(any())).thenReturn(Optional.of(location));
            when(cacheManager.getCache("UserDetails")).thenReturn(userDetailsCache);
            String result = adminService.deleteUser(2L, details);

            //then
            assertThat(result).isEqualTo("사원 삭제 성공");

            verify(scheduleRepository).deleteAll(List.of(schedule));
            verify(reservationRepository).deleteAll(List.of(reservation));
            verify(userLocationRepository).delete(location);
            verify(userRepository).delete(user);
            verify(userDetailsCache, times(1)).evict(user.getEmail());
        }

    }

    @Nested
    @DisplayName("실패 케이스")
    class FailCase {
        @DisplayName("사원 선택 조회 실패(관리자 권한 없음)")
        @Test
        void fail1() {
            //given

            UserDetailsImpl details = new UserDetailsImpl(user, user.getUsername());

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                adminService.getUsers(user.getId(), details);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("권한이 없습니다.");

        }

        @DisplayName("사원 선택 조회 실패(등록된 사원 없음)")
        @Test
        void fail2() {
            //given
            User admin = User.builder()
                    .id(1L)
                    .role(UserRoleEnum.ADMIN)
                    .username("뽀로로")
                    .companies(
                            Companies.builder()
                                    .companyName("뽀로로랜드")
                                    .build())
                    .build();
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            //when
            when(userRepository.findByIdAndCompanies_CompanyName(admin.getId(), admin.getCompanies().getCompanyName())).thenReturn(Optional.empty());

            UserException exception = assertThrows(UserException.class, () -> {
                adminService.getUsers(admin.getId(), details);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("등록된 사용자가 없습니다");

        }

        @DisplayName("사원 목록 조회 실패(관리자 권한 없음)")
        @Test
        void fail3() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(user, user.getUsername());

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                adminService.getAllUsers(details);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("권한이 없습니다.");
        }

        @DisplayName("사원 목록 조회 실패(사원 목록 없음)")
        @Test
        void fail4() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            //when
            when(userRepository.findAllByCompanies_CompanyName(admin.getCompanies().getCompanyName())).thenReturn(Collections.emptyList());

            UserListResponseDto result = adminService.getAllUsers(details);

            //then
            assertThat(result).isNotNull();
            assertThat(result.getUserList().size()).isEqualTo(0);
        }

        @DisplayName("사원 권한 수정 실패(관리자 권한 없음)")
        @Test
        void fail5() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(user, user.getUsername());

            RoleDeptUpdateRequestDto requestDto = RoleDeptUpdateRequestDto.builder()
                    .role(UserRoleEnum.MANAGER)
                    .build();
            //when
            UserException exception = assertThrows(UserException.class, () -> {
                adminService.editUser(2L, details, requestDto);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("권한이 없습니다.");
        }

        @DisplayName("사원 권한 수정 실패(등록된 사원 없음)")
        @Test
        void fail6() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            RoleDeptUpdateRequestDto requestDto = RoleDeptUpdateRequestDto.builder()
                    .role(UserRoleEnum.MANAGER)
                    .build();

            //when
            when(userRepository.findByIdAndCompanies_CompanyName(2L, admin.getCompanies().getCompanyName())).thenReturn(Optional.empty());

            UserException exception = assertThrows(UserException.class, () -> {
                adminService.editUser(2L, details, requestDto);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("등록된 사용자가 없습니다");

        }

        @DisplayName("사원 권한 수정 실패(관리자 권한으로 수정 불가)")
        @Test
        void fail7() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            RoleDeptUpdateRequestDto requestDto = RoleDeptUpdateRequestDto.builder()
                    .role(UserRoleEnum.ADMIN)
                    .build();

            //when
            when(userRepository.findByIdAndCompanies_CompanyName(user.getId(), user.getCompanies().getCompanyName())).thenReturn(Optional.of(user));

            UserException exception = assertThrows(UserException.class, () -> {
                adminService.editUser(user.getId(), details, requestDto);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("관리자 권한으로 수정할 수 없습니다.");

        }

        @DisplayName("사원 권한 수정 실패(관리자 권한은 수정 불가)")
        @Test
        void fail8() {
            //given

            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            RoleDeptUpdateRequestDto requestDto = RoleDeptUpdateRequestDto.builder()
                    .role(UserRoleEnum.MANAGER)
                    .updateRole(true)
                    .build();

            //when
            when(userRepository.findByIdAndCompanies_CompanyName(admin.getId(), admin.getCompanies().getCompanyName())).thenReturn(Optional.of(admin));

            UserException exception = assertThrows(UserException.class, () -> {
                adminService.editUser(admin.getId(), details, requestDto);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("관리자 권한은 변경할 수 없습니다.");

        }

        @DisplayName("사원 삭제 실패(등록된 사원 없음)")
        @Test
        void fail9() {
            //given

            UserDetailsImpl details = new UserDetailsImpl(admin, admin.getUsername());

            //when
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            UserException exception = assertThrows(UserException.class, () -> {
                adminService.deleteUser(2L, details);
            });

            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("등록된 사용자가 없습니다");

        }

        @DisplayName("사원 삭제 실패(권한 없음)")
        @Test
        void A() {
            //given
            UserDetailsImpl details = new UserDetailsImpl(user, user.getUsername());

            //when
            UserException exception = assertThrows(UserException.class, () -> {
                adminService.deleteUser(user2.getId(), details);
            });
            //then
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode().getMessage()).isEqualTo("권한이 없습니다.");
        }

    }
}