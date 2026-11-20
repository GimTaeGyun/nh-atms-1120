package kr.co.nh.atms.web.menumgt.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 메뉴 다건 삭제 본문 {@code { menuIds: string[] }} (1~100건) — API 계약 §9 */
@Getter
@Setter
public class MenuMgtDeleteRequest {
    private List<String> menuIds;
}
