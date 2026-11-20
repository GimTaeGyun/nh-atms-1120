package kr.co.nh.atms.web.menumgt.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 메뉴 등록·수정 본문 — API 계약 §9 {@code MenuMgtSave}.
 * 등록: menuLevel(1 대·2 중·3 소) + 상위(대메뉴는 prefix 영문 3자, 중메뉴는 대메뉴ID, 소메뉴는 중메뉴ID) — 메뉴ID 는 서버가 MAX+1 로 정한다.
 * 수정(PUT): menuNm·menuPath(화면만)·sortNo·useYn 만 쓴다. 상위·레벨은 메뉴ID(PK)에 들어 있어 바꿀 수 없다(보내도 무시).
 * 검사는 MenuMgtService 한 곳(VAL_001 필드명).
 */
@Getter
@Setter
@NoArgsConstructor
public class MenuMgtSave {
    /** 레벨 1 대·2 중·3 소 (등록 때 필수) */
    private Integer menuLevel;
    /** 상위메뉴ID — 중메뉴 등록 = 대메뉴ID, 소메뉴 등록 = 중메뉴ID */
    private String parentMenuId;
    /** 대메뉴 접두어 영문 대문자 3자 (대메뉴 등록 때만) */
    private String prefix;
    /** 메뉴명 100바이트 이내(필수) */
    private String menuNm;
    /** 메뉴경로 — 소메뉴(화면)만, '/{대분류}/{kebab 이름}' */
    private String menuPath;
    /** 순서 0~99999(필수) */
    private Integer sortNo;
    /** 사용여부 Y/N(필수) */
    private String useYn;
}
