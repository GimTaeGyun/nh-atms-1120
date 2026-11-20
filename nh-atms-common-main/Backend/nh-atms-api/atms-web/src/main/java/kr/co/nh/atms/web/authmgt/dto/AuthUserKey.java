package kr.co.nh.atms.web.authmgt.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사용자 권한 키 — DEPT_CODE + OFC_CODE + PER_NO (삭제 본문의 한 줄) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserKey {
    private String deptCode;
    private String ofcCode;
    private String perNo;
}
