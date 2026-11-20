package kr.co.nh.atms.web.authmgt.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 권한 등록·수정 본문 — API 계약 §8 {@code AuthUserSave}.
 * 등록: perNo = '1' 이면 부서권한(deptCode 필수, 사무소는 서버가 TB_OFC_RAW.ONLINE_CODE 로 채움),
 * 그 밖은 개인권한(perNo 필수, 부서·사무소는 서버가 그 사람의 TB_OFC_RAW 로 채움 — 본문 deptCode·ofcCode 무시).
 * 수정(PUT)은 키(deptCode·ofcCode·perNo)를 주소에서 받고 본문의 키는 무시한다. 검사는 AuthMgtService 한 곳(VAL_001 필드명).
 */
@Getter
@Setter
@NoArgsConstructor
public class AuthUserSave {
    /** 부서코드 1~6자리 숫자(부서권한 등록 때 필수) */
    private String deptCode;
    /** 사무소코드 — 등록 때는 서버가 채운다(보내도 무시) */
    private String ofcCode;
    /** 개인번호 — '1' = 부서권한, 그 밖 = 개인권한(TB_INSA 에 있어야 함) */
    private String perNo;
    /** 권한 1~2자(필수) */
    private String auth;
    /** 승인상태 Y/N(필수, 잠정 Q-18) */
    private String apvSts;
    /** 만료일시 14자리(비우면 무기한) */
    private String dueDate;
    /** 메모 100바이트 이내 */
    private String memo;
}
