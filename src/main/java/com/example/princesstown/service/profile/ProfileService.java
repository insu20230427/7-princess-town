package com.example.princesstown.service.profile;

import com.example.princesstown.dto.request.ProfileEditRequestDto;
import com.example.princesstown.dto.response.ApiResponseDto;
import com.example.princesstown.dto.response.ProfileResponseDto;
import com.example.princesstown.entity.Location;
import com.example.princesstown.entity.User;
import com.example.princesstown.repository.user.UserRepository;
import com.example.princesstown.service.awsS3.S3Uploader;
import com.example.princesstown.service.location.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequiredArgsConstructor

public class ProfileService {
    private final UserRepository userRepository;
    private final S3Uploader s3Uploader;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationContext applicationContext;
    private final LocationService locationService;

    // 프로필 수정
    @Transactional
    public ResponseEntity<ApiResponseDto> updateUser(Long userId, ProfileEditRequestDto requestDto) {
        // 해당 유저를 DB에서 조회
        Optional<User> updateUser = userRepository.findById(userId);

        // 유저 존재 유무 판단
        if (!updateUser.isPresent()) {
            log.error("해당 유저를 조회하지 못했습니다");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDto(HttpStatus.NOT_FOUND.value(), "해당 유저를 조회하지 못했습니다"));
        }

        // 비밀번호 형식 검증
        if (!Pattern.matches("^[a-zA-Z0-9!@#$%^&*()_+{}:\"<>?,.\\\\/]{8,15}$", requestDto.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDto(400,"비밀번호는 최소 8자 이상, 15자 이하이며 알파벳 대소문자, 숫자로 구성되어야 합니다."));
        }

        // 위치 업데이트 로직
        if (requestDto.getLatitude() != null && requestDto.getLongitude() != null) {
            locationService.updateLocationAndRelatedEntities(userId, requestDto.getLatitude(), requestDto.getLongitude());
        }

        // DB에서 조회한 유저 정보를 가져옴
        User user = updateUser.get();

        // 프로필 정보를 업데이트
        updatePartialUserProfile(requestDto, user);

        try {
            // 프로필 이미지 업데이트 수행
            UpdateProfileImage(requestDto, user);
        } catch (IOException e) {
            log.error("이미지 업로드 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "이미지 업로드 중 오류가 발생했습니다."));
        }

        userRepository.save(user);

        // User를 ProfileResponseDto로 변환
        Location location = user.getLocation();
        ProfileResponseDto profileResponseDto = new ProfileResponseDto(user, location);

        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDto(HttpStatus.OK.value(), "프로필 수정 성공", profileResponseDto));
    }

    // 위치 정보가 포함되지 않은 부분적인 프로필 업데이트 메서드
    public void updatePartialUserProfile(ProfileEditRequestDto requestDto, User user) {
        if (!StringUtils.isEmpty(requestDto.getUsername())) {
            user.setUsername(requestDto.getUsername());
        }

        if (!StringUtils.isEmpty(requestDto.getPassword())) {
            String password = passwordEncoder.encode(requestDto.getPassword());
            user.setPassword(password);
        }

        if (!StringUtils.isEmpty(requestDto.getNickname())) {
            user.setNickname(requestDto.getNickname());
        }

        if (!StringUtils.isEmpty(requestDto.getEmail())) {
            user.setEmail(requestDto.getEmail());
        }

        if (!StringUtils.isEmpty(requestDto.getPhoneNumber())) {
            user.setPhoneNumber(requestDto.getPhoneNumber());
        }
    }

    // 프로필 이미지 업데이트
    public void UpdateProfileImage(ProfileEditRequestDto requestDto, User user) throws IOException {
        String imageUrl;

        if (requestDto.getProfileImage() != null) {
            imageUrl = s3Uploader.upload(requestDto.getProfileImage(), "profile-images");
        } else {
            imageUrl = s3Uploader.uploadDefaultImage(applicationContext);
        }

        user.setProfileImage(imageUrl);
    }
}