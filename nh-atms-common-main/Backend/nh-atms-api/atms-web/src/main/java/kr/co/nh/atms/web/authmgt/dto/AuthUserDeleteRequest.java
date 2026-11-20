package kr.co.nh.atms.web.authmgt.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 사용자 권한 다건 삭제 본문 {@code { keys: AuthUserKey[] }} (1~100건) — API 계약 §8 */
@Getter
@Setter
public class AuthUserDeleteRequest {
    private List<AuthUserKey> keys;
}
