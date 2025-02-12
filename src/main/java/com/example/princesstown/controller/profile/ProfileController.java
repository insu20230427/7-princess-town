package com.example.princesstown.controller.profile;

import com.example.princesstown.dto.request.ProfileEditRequestDto;
import com.example.princesstown.dto.response.ApiResponseDto;
import com.example.princesstown.dto.response.ProfileResponseDto;
import com.example.princesstown.security.user.UserDetailsImpl;
import com.example.princesstown.service.profile.ProfileService;
import com.example.princesstown.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;

    // 프로필 조회 메서드
    @GetMapping("/api/auth/profile")
    @ResponseBody
    public ProfileResponseDto lookupUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Long userId = userDetails.getUser().getUserId();
        ProfileResponseDto userResponse = userService.lookupUser(userId);

        return userResponse;
    }

    // 프로필 수정
    @PutMapping("/api/auth/profile")
    @ResponseBody
    public ResponseEntity<ApiResponseDto> updateUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails, @ModelAttribute ProfileEditRequestDto editRequestDto) {
        Long userId = userDetails.getUser().getUserId();

        return profileService.updateUser(userId, editRequestDto);
    }


    // view.html
    @GetMapping("/auth/profile")
    public String getPost(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        model.addAttribute("user", userDetails.getUser());

        return "profile";
    }

    @GetMapping("/update")
    public String updatePage(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        model.addAttribute("username",userDetails.getUser().getUsername());
        model.addAttribute("user",userDetails.getUser());

        return "profileUpdate";
    }
}
