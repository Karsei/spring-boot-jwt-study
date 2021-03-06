package kr.pe.karsei.springbootjwtstudy.controllers;

import kr.pe.karsei.springbootjwtstudy.models.TokenResponse;
import kr.pe.karsei.springbootjwtstudy.providers.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(value = "auth")
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 인증 요청을 담당합니다.
     * @param params 필요 파라미터
     * @return Token 정보 객체
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> params) {
        // 인증 및 토큰 생성
        TokenResponse token = jwtTokenProvider.authorize(params.get("username"));

        return ResponseEntity.ok(token);
    }
}
