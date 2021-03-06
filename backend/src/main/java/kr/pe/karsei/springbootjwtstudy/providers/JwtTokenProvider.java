package kr.pe.karsei.springbootjwtstudy.providers;

import io.jsonwebtoken.*;
import kr.pe.karsei.springbootjwtstudy.models.AuthUser;
import kr.pe.karsei.springbootjwtstudy.models.TokenResponse;
import kr.pe.karsei.springbootjwtstudy.services.AuthUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

/**
 * JWT Token 을 생성, 인증, 권한 부여, 유효성 검사 등의 기능을 담당한다.
 */
@Slf4j
@Component
public class JwtTokenProvider {
    // JWT Secret Key
    @Value("${jwt.secret-key}")
    private String secretKey;

    // Access Token 유효시간 (10분)
    private static final long TIME_VALID_ACCEES_TOKEN = Duration.ofMinutes(10).toMillis();

    // Refresh Token 유효시간 (30분)
    private static final long TIME_VALID_REFRESH_TOKEN = Duration.ofMinutes(30).toMillis();

    private final AuthUserService authUserService;
    public JwtTokenProvider(AuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    /**
     * 생성자를 만들면서 미리 Secret Key 를 Base64 로 변환합니다.
     */
    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    /**
     * JWT Access Token 을 생성합니다.
     * @param user 유저 정보
     * @return Access Token 문자열
     */
    public String createAccessToken(AuthUser user) {
        Date now = new Date();

        // Payload
        Claims claims = Jwts.claims()
                // sub - Subject (whom the token refers to)
                .setSubject(user.getUsername())
                // iss - Issuer (who created and signed this token)
                .setIssuer("SampleApi")
                // jti - JWT ID (unique identifier for this token)
                .setId(user.getUsername())
                // iat - issued at (seconds since Unix epoch)
                .setIssuedAt(now)
                // exp - Expiration time (seconds since Unix epoch)
                .setExpiration(new Date(now.getTime() + TIME_VALID_ACCEES_TOKEN));
        claims.put("roles", user.getAuthorities());

        return Jwts.builder()
                // [Header] typ - Type of token
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                // [Payload]
                .setClaims(claims)
                // [Signature] alg - Signature or encryption algorithm
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    /**
     * JWT Refresh Token 을 생성합니다.
     * @return Refresh Token 문자열
     */
    public String createRefreshToken() {
        Date now = new Date();
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + TIME_VALID_REFRESH_TOKEN))
                .compact();
    }

    /**
     * JWT Token 정보를 조회합니다.
     * @param token JWT Token
     * @return JWT Token 정보 객체
     */
    public Claims getClaims(String token) {
        /*
         * {
         *   "sub": "developer",
         *   "iss": "SampleApi",
         *   "jti": "developer",
         *   "roles": [],
         *   "iat": 1650171240,
         *   "exp": 1650173040
         * }
         */
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    }

    /**
     * JWT Token 에서 인증 정보를 조회합니다.
     * @param token JWT 토큰
     * @return 인증 확인된 인증 객체 (setAuthenticated = true)
     */
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = authUserService.loadUserByUsername(getClaims(token).getSubject());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    /**
     * 유저를 인증하고 JWT Token 을 생성합니다.
     * @param username 유저 이름
     * @return JWT Token 객체
     */
    public TokenResponse authorize(String username) {
        // 조회
        AuthUser member = authUserService.loadUserByUsername(username);
        if (member == null) throw new IllegalArgumentException("가입되지 않은 이름입니다.");

        return TokenResponse.builder()
                .accessToken(createAccessToken(member))
                .refreshToken(createRefreshToken())
                .build();
    }

    /**
     * HTTP Request 의 Header 에서 JWT Token 값을 가져옵니다.
     * @param request HTTP 요청 객체
     * @return JWT Token 문자열
     * @throws IllegalArgumentException 인증 헤더가 올바르지 않을 경우 예외 발생
     */
    public String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new IllegalArgumentException("인증 헤더가 올바르지 않습니다.");

        return authHeader.substring("Bearer ".length());
        // return request.getHeader("X-AUTH-TOKEN");
    }

    /**
     * JWT Token 을 검증하여 Token 이 유효한지 판단합니다.
     * @param token JWT 토큰
     * @return 유효하다면 {@code true}; 그렇지 않다면 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        }
        catch (SignatureException e) {
            log.error("Invalid JWT signatrue: {}", e.getMessage());
        }
        catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }
        catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        }
        catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        }
        catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
