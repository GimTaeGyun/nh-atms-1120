"""개발납기 일정안 생성 스크립트 (2026-11-12).

    python3 docs/schedule/make_schedule.py      # -> docs/schedule/개발납기_일정안.xlsx / .md / .html

기준(사용자 결정 2026-11-12)은 README.md 참고. 조건을 바꿀 때는 아래 상수만 고치고 다시 실행한다.
수식 값까지 채우려면 LibreOffice로 재계산이 필요하다(엑셀에서 열면 자동 계산).
"""
import datetime as dt
import math
import os
import sys

sys.dont_write_bytecode = True

from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter as CL
from openpyxl.workbook.defined_name import DefinedName

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from schedule_data import ROWS  # noqa: E402

D = dt.date
PEOPLE = ["최성민", "김태균", "이태성"]
WEEK = "월화수목금토일"
# 주말 외 제외일 (설 연휴/대체공휴일은 추정). 12/31은 근무일(사용자 결정 2026-11-12)
HOLIDAYS = [
    (D(2026, 12, 25), "공휴일", "성탄절"),
    (D(2027, 1, 1), "공휴일", "신정"),
    (D(2027, 2, 5), "공휴일", "설 연휴(추정)"),
    (D(2027, 2, 8), "공휴일", "설 대체공휴일(추정)"),
    (D(2027, 3, 1), "공휴일", "삼일절"),
]
HSET = {h for h, _, _ in HOLIDAYS}
START = {"김태균": D(2026, 12, 1), "최성민": D(2026, 12, 7), "이태성": D(2026, 12, 1)}
VACATION = {"이태성": (D(2026, 12, 14), D(2026, 12, 25))}  # 휴가는 이태성만 반영
END = D(2027, 3, 31)  # 3월 말일(수) - 2026-11-12 기간 연장(2/28 → 3/31)
DAY_TABLE = [(1, 2, 1), (3, 4, 2), (5, 7, 3), (8, 10, 5)]  # 난이도 하한, 상한, 개발일수
AVG = {"김태균": 4, "이태성": 4, "최성민": 6}  # 담당자 평균 난이도(반올림) - 미정/빈칸에 적용
FIXED = {"260000": (D(2026, 11, 13), D(2026, 11, 20), D(2026, 11, 27)), "264000": None}  # 기존 납기 유지
MOVE = {  # 화면ID: (새 담당자, 이유) - 원 배정 시 이태성 84일 > 가용 73일 → 관련 업무 묶음 단위 이관(종료일 평준화)
    "266000": ("김태균", "기기상태·출동 묶음(김태균 장애관리/기기관리와 연관)"),
    "266001": ("김태균", "기기상태·출동 묶음"),
    "266002": ("김태균", "기기상태·출동 묶음"),
    "266003": ("김태균", "기기상태·출동 묶음"),
    "266004": ("김태균", "기기상태·출동 묶음"),
    "269311": ("김태균", "장애통보 현황(263103)의 상세 팝업"),
    "269322": ("김태균", "전국운영현황(265130) 관련 상세 팝업"),
    "269601": ("김태균", "출동 현황 관련 팝업(도착시간)"),
    "266107": ("최성민", "현송관리 묶음(266105·266106과 함께)"),
    "269631": ("최성민", "설치코너 관리(263000) 팝업 묶음"),
    "269632": ("최성민", "설치코너 관리(263000) 팝업 묶음"),
    "269633": ("최성민", "설치코너 관리(263000) 팝업 묶음"),
    "269634": ("최성민", "설치코너 관리(263000) 팝업 묶음"),
}
OUT = os.path.join(HERE, "개발납기_일정안.xlsx")


def dev_days(d):
    for lo, hi, n in DAY_TABLE:
        if lo <= d <= hi:
            return n
    raise ValueError(d)


def workdays(p):
    d, out = START[p], []
    vac = VACATION.get(p)
    while len(out) < 120:
        if d.weekday() < 5 and d not in HSET and not (vac and vac[0] <= d <= vac[1]):
            out.append(d)
        d += dt.timedelta(1)
    return out


# ---------------------------------------------------------------- 일정 계산
rows = []
for i, (g, name, sid, diff, dev) in enumerate(ROWS):
    r = dict(no=i + 1, g=g, name=name, id=sid, orig=diff, dev0=dev, dev=dev, note=[], days=None, applied=None)
    rows.append(r)
    if dev == "-":
        r["dev"] = None
        r["note"].append("중복화면 - 일정 제외")
        continue
    if diff is None:
        r["applied"] = AVG[dev]
        r["note"].append(f"난이도 미정 → 담당자 평균 {AVG[dev]}로 가정")
    else:
        r["applied"] = diff
    if sid == "260000":
        r["note"].append("기존 납기 유지")
        continue
    r["days"] = 1 if sid == "264000" else dev_days(r["applied"])
    if sid in MOVE:
        r["dev"] = MOVE[sid][0]
        r["note"].append(f"날짜는 {r['dev']} 일정(이관안)으로 산정: {MOVE[sid][1]}")

seq = {}
for p in PEOPLE:
    mine = [r for r in rows if r["dev"] == p and r["id"] != "260000"]
    fixed = [r for r in mine if r["id"] == "264000"]
    pri = [r for r in mine if r not in fixed and r["g"] != "팝업"
           and (r["id"][:3] in ("264", "266") or r["name"] == "SLA 소명 자료 자동화")]
    pri.sort(key=lambda r: (r["applied"], r["no"]))  # 264·266 안에서도 쉬운 것 먼저
    rest = [r for r in mine if r not in fixed and r not in pri]  # 나머지는 표 순서
    W = workdays(p)
    c = 0
    order = fixed + pri + rest
    for k, r in enumerate(order, 1):
        s = c
        c += r["days"]
        r.update(seq=k, start=W[math.floor(s + 1e-9)], due=W[math.ceil(c - 1e-9) - 1], cum=c,
                 pri="우선(264·266)" if r in pri or r in fixed else "일반")
        if r["id"] == "264000":
            r["note"].append("기존 납기 12/01 유지(1일 소요로 계산)")
        if r["due"] > END:
            r["note"].append(f"※ {END.month}/{END.day} 초과")
    seq[p] = order


# 260000: 기존 납기 11/27, 난이도 2 → 1일 역산(시작 = 납기)
for r in rows:
    if r["id"] == "260000":
        r.update(start=FIXED["260000"][2], due=FIXED["260000"][2], seq="기존", pri="", cum=None)


# ---------------------------------------------------------------- 요약값 (엑셀 하단·md·html 공용)
# 표시 담당자 = 사진 원본(dev0, 사용자 결정 2026-11-12: 담당자는 변경 없이, 추후 담당자만 수정)
# 날짜 산정 담당자 = dev (이관안 MOVE 반영) - 납기는 이관안 기준 값을 유지
def avail(p):
    return sum(1 for d in workdays(p) if d <= END)


def span_days(a, b):  # 시작~납기, 주말만 제외(양끝 포함)
    return sum(1 for x in range((b - a).days + 1) if (a + dt.timedelta(x)).weekday() < 5)


def work_days(r):  # 시작~납기, 주말·공휴일·(산정 담당자) 휴가 제외
    vac = VACATION.get(r["dev"])
    n = 0
    for x in range((r["due"] - r["start"]).days + 1):
        d = r["start"] + dt.timedelta(x)
        if d.weekday() < 5 and d not in HSET and not (vac and vac[0] <= d <= vac[1]):
            n += 1
    return n


for r in rows:
    if r.get("due"):
        r["span"], r["work"] = span_days(r["start"], r["due"]), work_days(r)


def disp(r):
    return r["dev0"] if r["dev0"] != "-" else None


over = [x for x in rows if x.get("due") and x["due"] > END]
moved = {}
for sid, (to, why) in MOVE.items():
    moved.setdefault(to, []).append(sid)
orig_days = {p: sum(r["days"] for r in rows if r["dev0"] == p and r["days"]) for p in PEOPLE}
MOVE_NOTE = ("개발 담당자는 사진 원본 그대로(추후 담당자만 수정 예정). 납기 날짜는 이관안 기준으로 산정한 값을 유지: "
             f"원 배정대로면 이태성 {orig_days['이태성']:g}일 > 가용 {avail('이태성')}일이라 "
             + ", ".join(f"{len(ids)}개({', '.join(ids)})를 {to}" for to, ids in moved.items())
             + " 일정에 넣어 계산함(표의 노란 칸·'일정 산정 담당자' 열). 원본 담당자 기준으로 보면 이 화면들은 "
               "이태성 본인 일정과 기간이 겹칩니다.")
if over:
    OVER_NOTE = (f"아래 화면이 {END.month}/{END.day}을 넘습니다(분홍 칸): "
                 + ", ".join(f"{x['dev0']} {x['id']} {x['name']} → {x['due']:%m/%d}" for x in over) + ".")
else:
    OVER_NOTE = (f"모든 화면이 {END.month}/{END.day} 안에 끝납니다(이관안 기준 여유: "
                 + ", ".join(f"{p} {avail(p) - seq[p][-1]['cum']:g}일" for p in PEOPLE) + ").")
SPAN_NOTE = ("개발 시작일 = 산정 담당자가 그 화면을 시작하는 날. 소요일 = 시작일~개발납기 평일 수(주말만 제외, 양끝 포함), "
             "실작업일 = 같은 기간에서 공휴일·휴가까지 뺀 날 수. 260000은 난이도 2(1일)로 역산해 시작일 = 납기일.")

# ---------------------------------------------------------------- 스타일
FN = "Arial"
f = Font(name=FN, size=10)
fb = Font(name=FN, size=10, bold=True)
ftitle = Font(name=FN, size=12, bold=True)
fblue = Font(name=FN, size=10, color="0000FF")
fred = Font(name=FN, size=10, color="C00000", bold=True)
fdup = Font(name=FN, size=10, color="C00000")
HDR = PatternFill("solid", fgColor="D9E1F2")
YEL = PatternFill("solid", fgColor="FFF2CC")
GREY = PatternFill("solid", fgColor="EDEDED")
PINK = PatternFill("solid", fgColor="FCE4D6")
SUB = PatternFill("solid", fgColor="E2EFDA")
thin = Side(style="thin", color="A6A6A6")
B = Border(left=thin, right=thin, top=thin, bottom=thin)
CEN = Alignment(horizontal="center", vertical="center", wrap_text=True)
LEFT = Alignment(vertical="center")


def put(ws, r, c, v, font=f, fmt=None, fill=None, align=CEN, border=True):
    cell = ws.cell(r, c, v)
    cell.font = font
    cell.alignment = align
    if border:
        cell.border = B
    if fmt:
        cell.number_format = fmt
    if fill:
        cell.fill = fill
    return cell


def header(ws, r, c0, labels):
    for j, v in enumerate(labels):
        put(ws, r, c0 + j, v, fb, fill=HDR)


wb = Workbook()
ws0 = wb.active
ws0.title = "기준"
ws1 = wb.create_sheet("개발일정(표순서)", 0)
ws2 = wb.create_sheet("담당자별 순서", 1)
N = len(rows)
LAST = N + 1  # 표 마지막 행
S1 = "'개발일정(표순서)'"


def rng(col):
    return f"{S1}!${col}$2:${col}${LAST}"


# ---------------------------------------------------------------- 기준 시트
ws0["A1"] = "개발 일수표 (난이도 구간 → 개발일수, 사용자 결정 2026-11-12)"
ws0["A1"].font = ftitle
header(ws0, 2, 1, ["난이도 하한", "난이도 상한", "개발일수(일)"])
for i, (lo, hi, n) in enumerate(DAY_TABLE, 3):
    for j, v in enumerate((lo, hi, n), 1):
        put(ws0, i, j, v, fblue)
ws0["E2"] = "휴일 (주말 외 제외일)"
ws0["E2"].font = fb
header(ws0, 3, 5, ["날짜", "구분", "내용"])
for i, (h, kind, t) in enumerate(HOLIDAYS, 4):
    put(ws0, i, 5, h, fblue, "yyyy-mm-dd")
    put(ws0, i, 6, kind)
    put(ws0, i, 7, t, align=LEFT)
HOL_END = 3 + len(HOLIDAYS)
wb.defined_names["휴일"] = DefinedName("휴일", attr_text=f"'기준'!$E$4:$E${HOL_END}")
ws0["A9"] = "※ 파란 글씨 = 입력값. 일수표를 바꾸면 개발일수·요약은 다시 계산되지만, 개발 납기 날짜는 make_schedule.py를 다시 실행해야 바뀝니다."
ws0["A9"].font = f
for j, w in enumerate([12, 12, 12, 3, 12, 10, 26], 1):
    ws0.column_dimensions[CL(j)].width = w

# ---------------------------------------------------------------- 개발일정(표순서)
cols = ["순번", "메뉴(Level2)", "화면명", "화면ID", "개발 난이도(원본)", "적용 난이도", "개발일수", "개발 담당자",
        "일정 산정 담당자(이관안)", "디자인 납기", "퍼블 납기", "개발 납기", "개발 시작일", "소요일(주말 제외)",
        "실작업일(주말·공휴일·휴가 제외)", "순서(산정 담당자 기준)", "구분", "비고"]
header(ws1, 1, 1, cols)


def vac_formula(i):
    parts = []
    for p, (a, b) in VACATION.items():
        parts.append(f'IF(I{i}="{p}",MAX(0,NETWORKDAYS(MAX(M{i},DATE({a.year},{a.month},{a.day})),'
                     f'MIN(L{i},DATE({b.year},{b.month},{b.day})),휴일)),0)')
    return "-" + "-".join(parts) if parts else ""


for i, r in enumerate(rows, 2):
    vals = [r["no"], r["g"], r["name"], r["id"], "미정" if r["orig"] is None else r["orig"], r["applied"], r["days"],
            r["dev0"], r["dev"] or "-"]
    for j, v in enumerate(vals, 1):
        put(ws1, i, j, v, align=LEFT if j in (2, 3) else CEN)
    for j in range(10, 19):
        put(ws1, i, j, None)
    if r["id"] == "260000":
        put(ws1, i, 10, FIXED["260000"][0])
        put(ws1, i, 11, FIXED["260000"][1])
    elif r.get("due"):
        put(ws1, i, 11, f'=IF(M{i}="","",WORKDAY(M{i}-7+1,-1,휴일))')
        put(ws1, i, 10, f'=IF(M{i}="","",WORKDAY(M{i}-14+1,-1,휴일))')
    if r.get("due"):
        put(ws1, i, 12, r["due"])
        put(ws1, i, 13, r["start"])
        put(ws1, i, 14, f'=IF(M{i}="","",NETWORKDAYS(M{i},L{i}))')
        put(ws1, i, 15, f'=IF(M{i}="","",NETWORKDAYS(M{i},L{i},휴일){vac_formula(i)})')
        put(ws1, i, 16, r["seq"])
        put(ws1, i, 17, r["pri"])
    if r["days"] is not None and r["id"] != "264000":
        put(ws1, i, 7, f'=IF(F{i}="","",INDEX(기준!$C$3:$C$6,MATCH(F{i},기준!$A$3:$A$6,1)))')
    put(ws1, i, 18, " / ".join(r["note"]), align=LEFT)
    for j in (10, 11):
        ws1.cell(i, j).number_format = "m/d"
    for j in (12, 13):
        ws1.cell(i, j).number_format = "yyyy-mm-dd"
    ws1.cell(i, 7).number_format = "0.0"
    if r["dev"] is None:
        for j in range(1, 19):
            ws1.cell(i, j).font = fdup
    if r["dev"] and r["dev"] != r["dev0"]:
        ws1.cell(i, 9).fill = YEL
        ws1.cell(i, 9).font = fb
    if r.get("due") and r["due"] > END:
        ws1.cell(i, 12).fill = PINK
        ws1.cell(i, 12).font = fred
    if r["orig"] is None:
        ws1.cell(i, 6).font = fblue
    if r["id"] in ("264000",):
        ws1.cell(i, 7).font = fblue
    ws1.cell(i, 8).font = fb if r["dev"] else fdup
for j, w in enumerate([6, 12, 34, 10, 10, 9, 9, 11, 13, 10, 10, 12, 12, 10, 14, 10, 13, 60], 1):
    ws1.column_dimensions[CL(j)].width = w
ws1.freeze_panes = "D2"
ws1.auto_filter.ref = f"A1:R{LAST}"
ws1.row_dimensions[1].height = 42

# ---------------- 표 아래 요약 (원본에 붙여넣는 1~LAST행은 그대로)
R = LAST + 3
put(ws1, R, 2, "■ 요약 (아래는 위 표를 수식으로 집계한 내용입니다. 원본 엑셀에는 1~%d행만 옮기면 됩니다.)" % LAST, ftitle,
    align=LEFT, border=False)
R += 2
ENDF = f"DATE({END.year},{END.month},{END.day})"

# 1. 개발자별 정리
put(ws1, R, 2, "1. 개발자별 정리 (개발 담당자 = 사진 원본 기준, 오른쪽은 날짜 산정에 쓴 이관안 기준)", fb, align=LEFT, border=False)
R += 1
header(ws1, R, 2, ["개발 담당자", "투입 시작", "휴가", "가용 근무일", "화면 수", "개발일수 합계", "여유(+)/초과(-)",
                   "이관안 화면 수", "이관안 개발일수", "이관안 여유", "첫 개발납기", "마지막 개발납기",
                   f"{END.month}/{END.day} 초과 화면 수"])
top = R + 1
for k, p in enumerate(PEOPLE):
    r = top + k
    vac = VACATION.get(p)
    put(ws1, r, 2, p, fb)
    put(ws1, r, 3, START[p], fblue, "yyyy-mm-dd")
    put(ws1, r, 4, f"{vac[0]:%m/%d}~{vac[1]:%m/%d}" if vac else "-", fblue)
    vf = (f"-NETWORKDAYS(DATE({vac[0].year},{vac[0].month},{vac[0].day}),"
          f"DATE({vac[1].year},{vac[1].month},{vac[1].day}),휴일)") if vac else ""
    put(ws1, r, 5, f"=NETWORKDAYS(C{r},{ENDF},휴일){vf}")
    put(ws1, r, 6, f'=COUNTIF({rng("H")},B{r})')
    put(ws1, r, 7, f'=SUMIF({rng("H")},B{r},{rng("G")})', fmt="0.0")
    put(ws1, r, 8, f"=E{r}-G{r}", fmt="0.0;[Red]-0.0")
    put(ws1, r, 9, f'=COUNTIF({rng("I")},B{r})')
    put(ws1, r, 10, f'=SUMIF({rng("I")},B{r},{rng("G")})', fmt="0.0")
    put(ws1, r, 11, f"=E{r}-J{r}", fmt="0.0;[Red]-0.0")
    put(ws1, r, 12, f'=_xlfn.MINIFS({rng("L")},{rng("H")},B{r})', fmt="yyyy-mm-dd")
    put(ws1, r, 13, f'=_xlfn.MAXIFS({rng("L")},{rng("H")},B{r})', fmt="yyyy-mm-dd")
    put(ws1, r, 14, f'=COUNTIFS({rng("H")},B{r},{rng("L")},">"&{ENDF})')
r = top + len(PEOPLE)
put(ws1, r, 2, "합계", fb, fill=SUB)
put(ws1, r, 3, "", fill=SUB)
put(ws1, r, 4, "", fill=SUB)
for c in (5, 6, 7, 8, 9, 10, 11, 14):
    col = CL(c)
    put(ws1, r, c, f"=SUM({col}{top}:{col}{r - 1})", fb, fmt="0.0;[Red]-0.0" if c in (7, 8, 10, 11) else None, fill=SUB)
put(ws1, r, 12, f"=MIN(L{top}:L{r - 1})", fb, "yyyy-mm-dd", fill=SUB)
put(ws1, r, 13, f"=MAX(M{top}:M{r - 1})", fb, "yyyy-mm-dd", fill=SUB)
put(ws1, r + 1, 2, "※ 260000(11/27, 기간 전 완료)은 화면 수에만 포함, 개발일수 없음. 중복화면 5개(담당자 '-')는 제외. "
    "원본 담당자 기준 여유가 음수면 이관(또는 담당자 조정)이 필요하다는 뜻.", f, align=LEFT, border=False)
R = r + 4

# 2. 난이도별 개발일수
put(ws1, R, 2, "2. 난이도별 개발일수 (기준 시트 일수표, 화면 수는 원본 담당자 기준)", fb, align=LEFT, border=False)
R += 1
header(ws1, R, 2, ["난이도 구간", "개발일수(일)"] + [f"{p} 화면 수" for p in PEOPLE] + ["화면 수 합계", "개발일수 합계"])
top = R + 1
for k in range(len(DAY_TABLE)):
    r = top + k
    src = 3 + k
    put(ws1, r, 2, f'=기준!A{src}&"~"&기준!B{src}&"점"', fb)
    put(ws1, r, 3, f"=기준!C{src}", fmt="0.0")
    for j, p in enumerate(PEOPLE):
        put(ws1, r, 4 + j, f'=COUNTIFS({rng("H")},"{p}",{rng("F")},">="&기준!A{src},'
                           f'{rng("F")},"<="&기준!B{src},{rng("G")},">0")')
    put(ws1, r, 7, f"=SUM(D{r}:F{r})")
    put(ws1, r, 8, f'=SUMIFS({rng("G")},{rng("F")},">="&기준!A{src},{rng("F")},"<="&기준!B{src})', fmt="0.0")
r = top + len(DAY_TABLE)
put(ws1, r, 2, "합계", fb, fill=SUB)
put(ws1, r, 3, "", fill=SUB)
for c in range(4, 9):
    col = CL(c)
    put(ws1, r, c, f"=SUM({col}{top}:{col}{r - 1})", fb, fmt="0.0" if c == 8 else None, fill=SUB)
put(ws1, r + 1, 2, "※ 미정 난이도는 담당자 평균(김태균 4, 이태성 4)으로 넣은 값 기준. 264000은 기존 납기 유지로 1일 계산.", f,
    align=LEFT, border=False)
R = r + 4

# 3. 휴일·휴가·미투입
put(ws1, R, 2, "3. 휴일·휴가·미투입 (근무일에서 뺀 날)", fb, align=LEFT, border=False)
R += 1
header(ws1, R, 2, ["날짜", "요일", "구분", "대상", "내용"])
items = [(h, None, kind, "전원", t) for h, kind, t in HOLIDAYS]
for p, (a, b) in VACATION.items():
    items.append((a, b, "휴가", p, f"휴가 {a:%m/%d}~{b:%m/%d} (근무일 기준 %d일)"
                  % sum(1 for x in range((b - a).days + 1)
                        if (a + dt.timedelta(x)).weekday() < 5 and a + dt.timedelta(x) not in HSET)))
for p in PEOPLE:
    if START[p] > D(2026, 12, 1):
        items.append((D(2026, 12, 1), START[p] - dt.timedelta(1), "미투입", p, f"12월 2째 주 {START[p]:%m/%d}부터 투입"))
items.sort(key=lambda x: x[0])
for k, (a, b, kind, who, t) in enumerate(items):
    r = R + 1 + k
    put(ws1, r, 2, a, fmt="yyyy-mm-dd")
    put(ws1, r, 3, WEEK[a.weekday()] + (f"~{WEEK[b.weekday()]}" if b else ""))
    put(ws1, r, 4, kind, fill=PINK if kind == "휴가" else (GREY if kind == "미투입" else None))
    put(ws1, r, 5, who)
    put(ws1, r, 6, t + (f" (~{b:%Y-%m-%d})" if b else ""), align=LEFT)
r = R + 1 + len(items)
put(ws1, r, 2, f"※ 기간 12/1(화)~{END:%m/%d}({WEEK[END.weekday()]}), 주말 제외. 12/31은 근무일. 설 연휴(2/5~2/7)와 "
    "대체공휴일 2/8은 추정 → 확정 달력으로 확인. 휴일 날짜는 기준 시트 E열에서 고칩니다.", f, align=LEFT, border=False)
R = r + 3

# 4. 월별·담당자별 개발납기 건수
put(ws1, R, 2, "4. 월별·담당자별 개발납기 건수 (원본 담당자 기준)", fb, align=LEFT, border=False)
R += 1
header(ws1, R, 2, ["월", "시작", "끝"] + PEOPLE + ["합계"])
months = [("2026-11 (기간 전)", D(2026, 11, 1), D(2026, 11, 30)), ("2026-12", D(2026, 12, 1), D(2026, 12, 31)),
          ("2027-01", D(2027, 1, 1), D(2027, 1, 31)), ("2027-02", D(2027, 2, 1), D(2027, 2, 28)),
          ("2027-03", D(2027, 3, 1), D(2027, 3, 31)), ("기간 초과(4월~)", D(2027, 4, 1), D(2027, 6, 30))]
top = R + 1
for k, (lab, a, b) in enumerate(months):
    r = top + k
    put(ws1, r, 2, lab, fb, fill=PINK if "초과" in lab else None)
    put(ws1, r, 3, a, fmt="m/d")
    put(ws1, r, 4, b, fmt="m/d")
    for j, p in enumerate(PEOPLE):
        put(ws1, r, 5 + j, f'=COUNTIFS({rng("H")},"{p}",{rng("L")},">="&C{r},{rng("L")},"<="&D{r})')
    put(ws1, r, 8, f"=SUM(E{r}:G{r})", fb)
r = top + len(months)
put(ws1, r, 2, "합계", fb, fill=SUB)
put(ws1, r, 3, "", fill=SUB)
put(ws1, r, 4, "", fill=SUB)
for c in range(5, 9):
    col = CL(c)
    put(ws1, r, c, f"=SUM({col}{top}:{col}{r - 1})", fb, fill=SUB)
R = r + 3

# 5. 작성 기준·가정
put(ws1, R, 2, "5. 작성 기준·가정·초과 안내", fb, align=LEFT, border=False)
notes = [
    "① 개발납기 = 화면 작업이 끝나는 근무일 (산정 담당자별로 화면을 순서대로 이어서 개발).",
    "② 퍼블 납기 = 개발 시작일 7일 전, 디자인 납기 = 개발 시작일 14일 전 (주말·휴일이면 직전 근무일). 수식 =WORKDAY(시작일-7+1,-1,휴일).",
    "③ 순서: 산정 담당자별로 264xxx·266xxx 화면 먼저(난이도 낮은 순, 같으면 표 순서) → 나머지 표 순서. "
    "'SLA 소명 자료 자동화(신규)'는 SLA관리(2662xx) 묶음으로 우선 그룹에 포함.",
    "④ 난이도 '미정'/빈칸 7개는 담당자 평균 난이도(반올림)로 가정 → 난이도 확정 시 재산정.",
    "⑤ 260000(11/27)·264000(12/01)은 기존 표의 납기 유지. 중복화면 269300~269304는 일정 제외.",
    "⑥ " + MOVE_NOTE,
    "⑦ " + OVER_NOTE,
    "⑧ " + SPAN_NOTE,
    "⑨ 사진 판독 결과임. 266107(반환금액상세조회) 원 담당자는 사진 정렬상 이태성으로 판독 → 원본과 대조 필요.",
    "⑩ 개발 납기·시작일은 고정값(make_schedule.py 산출). 조건 변경 시 docs/schedule/make_schedule.py 상수를 고쳐 재생성.",
]
for k, t in enumerate(notes):
    put(ws1, R + 1 + k, 2, t, f, align=LEFT, border=False)

# ---------------------------------------------------------------- 담당자별 순서 (날짜 산정 기준 = 이관안 담당자)
header(ws2, 1, 1, ["일정 산정 담당자(이관안)", "순서", "구분", "화면ID", "화면명", "메뉴(Level2)", "적용 난이도", "개발일수",
                   "누적일수", "개발 시작일", "개발 납기", "소요일(주말 제외)", "실작업일", "퍼블 납기", "디자인 납기",
                   "개발 담당자(원본)", "비고"])
i = 2
for p in PEOPLE:
    for r in seq[p]:
        vals = [p, r["seq"], r["pri"], r["id"], r["name"], r["g"], r["applied"], r["days"], r["cum"], r["start"], r["due"],
                f"=NETWORKDAYS(J{i},K{i})", r["work"], f"=WORKDAY(J{i}-7+1,-1,휴일)", f"=WORKDAY(J{i}-14+1,-1,휴일)",
                r["dev0"], " / ".join(r["note"])]
        for j, v in enumerate(vals, 1):
            put(ws2, i, j, v, align=LEFT if j in (5, 6, 17) else CEN)
        for j in (10, 11):
            ws2.cell(i, j).number_format = "yyyy-mm-dd"
        for j in (14, 15):
            ws2.cell(i, j).number_format = "m/d"
        for j in (8, 9):
            ws2.cell(i, j).number_format = "0.0"
        if r["dev0"] != p:
            ws2.cell(i, 16).fill = YEL
        if r["due"] > END:
            ws2.cell(i, 11).fill = PINK
            ws2.cell(i, 11).font = fred
        if r["pri"] == "일반":
            ws2.cell(i, 2).fill = GREY
            ws2.cell(i, 3).fill = GREY
        i += 1
for j, w in enumerate([13, 6, 13, 10, 34, 12, 9, 9, 9, 12, 12, 10, 9, 10, 10, 13, 60], 1):
    ws2.column_dimensions[CL(j)].width = w
ws2.freeze_panes = "E2"
ws2.auto_filter.ref = f"A1:Q{i - 1}"
ws2.row_dimensions[1].height = 30

wb.save(OUT)


# ---------------------------------------------------------------- 모바일용 md / html (엑셀과 같은 계산 결과)
def prev_workday(d):
    while d.weekday() >= 5 or d in HSET:
        d -= dt.timedelta(1)
    return d


def md_(d):
    return f"{d.month}/{d.day}"


def deadlines(r):
    if r["id"] == "260000":
        return FIXED["260000"]
    return prev_workday(r["start"] - dt.timedelta(14)), prev_workday(r["start"] - dt.timedelta(7)), r["due"]


def person_rows(p):  # 원본 담당자 기준, 개발납기 순
    out = []
    for r in sorted((x for x in rows if x["dev0"] == p and x.get("due")), key=lambda x: (x["due"], x["start"], x["no"])):
        dz, pb, dv = deadlines(r)
        out.append(dict(id=r["id"], name=r["name"], g=r["g"], diff=r["applied"], days=r["days"] if r["days"] else "-",
                        start=r["start"], design=dz, publ=pb, due=dv, span=r["span"], work=r["work"],
                        pri=r["pri"].startswith("우선"), plan=r["dev"] if r["dev"] != p else "",
                        over=dv > END, note=" / ".join(r["note"])))
    return out


def summary():
    res = []
    for p in PEOPLE:
        pr = person_rows(p)
        days_sum = sum(x["days"] for x in pr if x["days"] != "-")
        plan = sum(r["days"] for r in seq[p])
        vac = VACATION.get(p)
        res.append(dict(p=p, start=START[p], vac=f"{md_(vac[0])}~{md_(vac[1])}" if vac else "-", avail=avail(p),
                        cnt=len(pr), days=days_sum, slack=avail(p) - days_sum, plan=plan, pslack=avail(p) - plan,
                        pcnt=len(seq[p]) + (1 if p == "최성민" else 0), first=min(x["due"] for x in pr),
                        last=max(x["due"] for x in pr), over=sum(1 for x in pr if x["over"]),
                        out=sum(1 for x in pr if x["plan"])))
    return res


def diff_table():
    out = []
    for lo, hi, n in DAY_TABLE:
        sel = [r for r in rows if r["days"] and lo <= r["applied"] <= hi]
        cnt = [sum(1 for r in sel if r["dev0"] == p) for p in PEOPLE]
        out.append((f"{lo}~{hi}점", n, cnt, sum(cnt), sum(r["days"] for r in sel)))
    return out


def off_days():
    out = [(h, None, kind, "전원", t) for h, kind, t in HOLIDAYS]
    for p, (a, b) in VACATION.items():
        n = sum(1 for x in range((b - a).days + 1)
                if (a + dt.timedelta(x)).weekday() < 5 and a + dt.timedelta(x) not in HSET)
        out.append((a, b, "휴가", p, f"휴가 (근무일 {n}일)"))
    for p in PEOPLE:
        if START[p] > D(2026, 12, 1):
            out.append((D(2026, 12, 1), START[p] - dt.timedelta(1), "미투입", p, f"{md_(START[p])}부터 투입"))
    return sorted(out, key=lambda x: x[0])


def month_counts():
    out = []
    for lab, a, b in months:
        c = [sum(1 for x in person_rows(p) if a <= x["due"] <= b) for p in PEOPLE]
        if lab.startswith("기간 초과") and not sum(c):
            continue
        out.append((lab, c, sum(c)))
    return out


def day_label(a, b):
    return f"{md_(a)}({WEEK[a.weekday()]})" + (f"~{md_(b)}({WEEK[b.weekday()]})" if b else "")


RULES = [
    f"기간 12/1(화) ~ {md_(END)}({WEEK[END.weekday()]}), 주말·공휴일 제외, 12/31은 근무일",
    "개발일수: " + ", ".join(f"난이도 {lo}~{hi} → {n}일" for lo, hi, n in DAY_TABLE)
    + " (미정은 담당자 평균 난이도)",
    "순서: 264xxx·266xxx 먼저(쉬운 것 먼저) → 나머지 표 순서",
    "퍼블 납기 = 개발 시작일 7일 전, 디자인 납기 = 개발 시작일 14일 전 (주말·휴일이면 직전 근무일)",
    "260000(11/27)·264000(12/01)은 기존 납기 유지, 중복화면 269300~269304 제외",
    MOVE_NOTE,
    OVER_NOTE,
    SPAN_NOTE,
    "사진 판독 결과 → 원본과 대조 필요 (266107 원 담당자 = 이태성으로 판독)",
]
SUM = summary()

# ---- Markdown
L_ = ["# 개발납기 일정안 (모바일용)", "",
      f"> 생성 {dt.date.today():%Y-%m-%d} · `make_schedule.py` 산출 · 엑셀(`개발납기_일정안.xlsx`)과 같은 내용  ",
      "> 개발 담당자 = 사진 원본(추후 수정 예정). 표시: ★ = 우선(264·266xxx), ↪이름 = 날짜를 그 담당자 일정(이관안)으로 산정, "
      "⚠ = 기간 초과  ",
      "> 소요 = 시작~납기 평일 수(주말만 제외), 실작업 = 공휴일·휴가까지 뺀 날 수", "",
      "## 1. 개발자별 정리 (원본 담당자 기준)", "",
      "| 담당자 | 투입 | 가용일 | 화면 수 | 개발일수 | 여유 | 이관안 개발일수 | 이관안 여유 | 마지막 납기 |",
      "|---|---|---|---|---|---|---|---|---|"]
for x in SUM:
    L_.append(f"| {x['p']} | {md_(x['start'])}" + (f" (휴가 {x['vac']})" if x['vac'] != '-' else "")
              + f" | {x['avail']} | {x['cnt']} | {x['days']:g} | {x['slack']:+g} | {x['plan']:g} | {x['pslack']:+g}"
              f" | {md_(x['last'])} |")
L_ += ["", "## 2. 난이도별 개발일수", "", "| 난이도 | 일수 | " + " | ".join(PEOPLE) + " | 합계 | 일수 합계 |",
       "|---|---|" + "---|" * len(PEOPLE) + "---|---|"]
for lab, n, cnt, tot, dsum in diff_table():
    L_.append(f"| {lab} | {n:g}일 | " + " | ".join(map(str, cnt)) + f" | {tot} | {dsum:g} |")
L_ += ["", "## 3. 휴일·휴가·미투입", "", "| 날짜 | 구분 | 대상 | 내용 |", "|---|---|---|---|"]
for a, b, kind, who, t in off_days():
    L_.append(f"| {day_label(a, b)} | {kind} | {who} | {t} |")
L_ += ["", "## 4. 월별 개발납기 건수 (원본 담당자 기준)", "", "| 월 | " + " | ".join(PEOPLE) + " | 합계 |",
       "|---|" + "---|" * len(PEOPLE) + "---|"]
for lab, c, tot in month_counts():
    L_.append(f"| {lab} | " + " | ".join(map(str, c)) + f" | {tot} |")
L_ += ["", "## 5. 작성 기준", ""] + [f"- {t}" for t in RULES]
for p in PEOPLE:
    x = next(v for v in SUM if v["p"] == p)
    L_ += ["", f"## {p} ({x['cnt']}개 · {md_(x['first'])}~{md_(x['last'])}, 개발납기 순)", "",
           "| 화면 | 시작 | 개발 | 소요 | 퍼블 | 디자인 |", "|---|---|---|---|---|---|"]
    for r in person_rows(p):
        mark = ("★" if r["pri"] else "") + ("⚠" if r["over"] else "")
        plan = f" ↪{r['plan']}" if r["plan"] else ""
        L_.append(f"| {mark}{r['id']} {r['name'].replace('|', '/')}{plan} | {md_(r['start'])} | **{md_(r['due'])}** | "
                  f"{r['span']}일 | {md_(r['publ'])} | {md_(r['design'])} |")

L_ += ["", "## 원본 엑셀 기준 표 (I~Q열 + 개발 시작일·소요일, 사진 순서 94행 · 붙여넣기용)", "",
       "> 원본 시트의 I열(화면명)~Q열(개발납기) 순서, 그 오른쪽에 개발 시작일·소요일(주말 제외)·실작업일. "
       "개발 담당자는 사진 원본, 디자인·퍼블 담당자는 빈칸, 중복화면(269300~269304)은 개발 담당자 '-'.", "",
       "| 화면명 | 화면ID | 개발 난이도 1~10 | 디자인 담당자 | 디자인 납기 | 퍼블 담당자 | 퍼블 납기 | 개발 담당자 | 개발납기 "
       "| 개발 시작일 | 소요일(주말 제외) | 실작업일(공휴일·휴가 제외) |",
       "|---|---|---|---|---|---|---|---|---|---|---|---|"]
for r in rows:
    dz = pb = dv = None
    if r.get("due"):
        dz, pb, dv = deadlines(r)
    iso = lambda d: d.isoformat() if d else ""  # noqa: E731
    diff = "미정" if r["orig"] is None else r["orig"]
    L_.append(f"| {r['name'].replace('|', '/')} | {r['id']} | {diff} |  | {iso(dz)} |  | {iso(pb)} | "
              f"{r['dev0']} | {iso(dv)} | {iso(r.get('start'))} | {r.get('span', '')} | {r.get('work', '')} |")
open(os.path.join(HERE, "개발납기_일정안.md"), "w").write("\n".join(L_) + "\n")

# ---- HTML (오프라인 단일 파일, 휴대폰 화면용 카드)
import html as _h  # noqa: E402
import json as _j  # noqa: E402

data = []
for p in PEOPLE:
    for k, r in enumerate(person_rows(p), 1):
        data.append(dict(p=p, seq=str(k), id=r["id"], name=r["name"], g=r["g"], diff=r["diff"], days=f"{r['days']:g}"
                         if r["days"] != "-" else "-", due=r["due"].isoformat(),
                         dueL=f"{md_(r['due'])}({WEEK[r['due'].weekday()]})", startL=md_(r["start"]),
                         span=r["span"], work=r["work"], publ=md_(r["publ"]), design=md_(r["design"]), pri=r["pri"],
                         plan=r["plan"], over=r["over"], note=r["note"]))


def tbl(head, body):
    return ("<div class=tw><table><thead><tr>" + "".join(f"<th>{_h.escape(str(h))}</th>" for h in head)
            + "</tr></thead><tbody>" + "".join("<tr>" + "".join(f"<td>{_h.escape(str(c))}</td>" for c in row)
                                               + "</tr>" for row in body) + "</tbody></table></div>")


sum_cards = "".join(
    f"<div class=card><div class=nm>{x['p']}</div><div class=big>{md_(x['last'])}<small>마지막 납기</small></div>"
    f"<div class=kv><span>투입</span><b>{md_(x['start'])}</b><span>휴가</span><b>{x['vac']}</b>"
    f"<span>가용일</span><b>{x['avail']}일</b><span>화면 수</span><b>{x['cnt']}개</b>"
    f"<span>개발일수</span><b>{x['days']:g}일 (여유 {x['slack']:+g})</b>"
    f"<span>이관안</span><b>{x['plan']:g}일 (여유 {x['pslack']:+g})</b>"
    f"<span>타 담당자 일정 산정</span><b>{x['out']}개</b></div></div>" for x in SUM)
sections = (
    "<h2>난이도별 개발일수</h2>" + tbl(["난이도", "일수"] + PEOPLE + ["합계", "일수합"],
                                     [[a, f"{n:g}"] + c + [t, f"{d:g}"] for a, n, c, t, d in diff_table()])
    + "<h2>휴일·휴가·미투입</h2>" + tbl(["날짜", "구분", "대상", "내용"],
                                       [[day_label(a, b), k, w, t] for a, b, k, w, t in off_days()])
    + "<h2>월별 개발납기 건수</h2>" + tbl(["월"] + PEOPLE + ["합계"], [[l] + c + [t] for l, c, t in month_counts()])
    + "<h2>작성 기준</h2><ul>" + "".join(f"<li>{_h.escape(t)}</li>" for t in RULES) + "</ul>")
HTML = """<!doctype html><html lang=ko><head><meta charset=utf-8>
<meta name=viewport content="width=device-width,initial-scale=1">
<title>개발납기 일정안</title>
<style>
:root{--bg:#f6f7f9;--fg:#1d2330;--mut:#667085;--card:#fff;--line:#e3e6ea;--acc:#1f6feb;--warn:#c4320a;--warnbg:#fde8e1;--mv:#8a6d00;--mvbg:#fff4cc;--pri:#0b7a3e}
@media (prefers-color-scheme:dark){:root{--bg:#111418;--fg:#e6e8eb;--mut:#9aa3ae;--card:#1b2027;--line:#2c333c;--acc:#5aa2ff;--warn:#ff8a65;--warnbg:#3a1f17;--mv:#ffd75e;--mvbg:#3a3214;--pri:#4cc38a}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 -apple-system,"Malgun Gothic","Apple SD Gothic Neo",sans-serif}
header{position:sticky;top:0;z-index:2;background:var(--bg);padding:12px 16px 8px;border-bottom:1px solid var(--line)}
h1{font-size:18px;margin:0 0 8px}h2{font-size:16px;margin:22px 0 8px}main{padding:0 16px 40px;max-width:760px;margin:auto}
.chips{display:flex;gap:6px;overflow-x:auto;padding-bottom:4px}.chips button{flex:none;border:1px solid var(--line);background:var(--card);color:var(--fg);border-radius:999px;padding:6px 12px;font-size:14px}
.chips button.on{background:var(--acc);border-color:var(--acc);color:#fff}
input{width:100%;margin-top:6px;padding:8px 10px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--fg);font-size:15px}
.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(210px,1fr));gap:10px}
.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:12px}
.nm{font-weight:700}.big{font-size:22px;font-weight:700;margin:2px 0 6px}.big small{font-size:12px;color:var(--mut);font-weight:400;margin-left:6px}
.kv{display:grid;grid-template-columns:auto 1fr;gap:2px 10px;font-size:13px}.kv span{color:var(--mut)}
.tw{overflow-x:auto}table{border-collapse:collapse;width:100%;font-size:13px;background:var(--card)}th,td{border:1px solid var(--line);padding:5px 7px;text-align:center;white-space:nowrap}th{background:var(--bg)}
ul{padding-left:18px;font-size:13px;color:var(--mut)}details{margin-top:6px}summary{cursor:pointer;color:var(--acc);font-size:14px}
.item{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px 12px;margin:8px 0}
.item.over{border-color:var(--warn);background:var(--warnbg)}
.top{display:flex;justify-content:space-between;gap:8px;align-items:baseline}.t{font-weight:600}.id{color:var(--mut);font-size:12px;margin-right:4px}
.due{font-weight:700;color:var(--acc);white-space:nowrap}.meta{font-size:12px;color:var(--mut);margin-top:3px}
.tag{display:inline-block;font-size:11px;border-radius:4px;padding:0 5px;margin-left:4px}.tag.p{color:var(--pri);border:1px solid var(--pri)}.tag.m{color:var(--mv);background:var(--mvbg)}.tag.o{color:var(--warn);border:1px solid var(--warn)}
.who{font-size:12px;font-weight:600;margin-right:6px}.cnt{font-size:13px;color:var(--mut);margin:10px 0 0}
</style></head><body>
<header><h1>개발납기 일정안 <small style="font-size:12px;color:var(--mut);font-weight:400">__GEN__</small></h1>
<div class=chips id=who></div><div class=chips id=mon style="margin-top:6px"></div>
<input id=q type=search placeholder="화면명·화면ID 검색"></header>
<main>
<h2>개발자별 정리 <small style="font-size:12px;color:var(--mut);font-weight:400">담당자 = 사진 원본</small></h2><div class=cards>__CARDS__</div>
<details><summary>난이도별 일수 · 휴일/휴가 · 월별 건수 · 작성 기준</summary>__SECTIONS__</details>
<h2>담당자별 일정</h2><div class=cnt id=cnt></div><div id=list></div>
</main>
<script>
const DATA=__DATA__;const PEOPLE=__PEOPLE__;
const MONTHS=[["전체 월",""],["12월","2026-12"],["1월","2027-01"],["2월","2027-02"],["3월","2027-03"]];
let who="전체",mon="",q="";
function chips(el,items,cur,set){el.innerHTML="";items.forEach(([l,v])=>{const b=document.createElement("button");b.textContent=l;if(v===cur)b.className="on";b.onclick=()=>{set(v);render()};el.appendChild(b)})}
function esc(s){return String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]))}
function render(){
 chips(document.getElementById("who"),[["전체","전체"],...PEOPLE.map(p=>[p,p])],who,v=>who=v);
 chips(document.getElementById("mon"),MONTHS,mon,v=>mon=v);
 const rows=DATA.filter(d=>(who==="전체"||d.p===who)&&(!mon||d.due.startsWith(mon))&&(!q||(d.name+d.id).toLowerCase().includes(q)));
 rows.sort((a,b)=>a.due.localeCompare(b.due));
 document.getElementById("cnt").textContent=rows.length+"개 화면 · 개발납기 순";
 document.getElementById("list").innerHTML=rows.map(d=>`<div class="item${d.over?" over":""}"><div class=top><div class=t>${who==="전체"?`<span class=who>${esc(d.p)}</span>`:""}<span class=id>${esc(d.id)}</span>${esc(d.name)}${d.pri?'<span class="tag p">우선</span>':""}${d.plan?`<span class="tag m">↪${esc(d.plan)} 일정</span>`:""}${d.over?'<span class="tag o">초과</span>':""}</div><div class=due>${esc(d.dueL)}</div></div><div class=meta>시작 ${esc(d.startL)} · 소요 ${esc(d.span)}일(실작업 ${esc(d.work)}일) · 퍼블 ${esc(d.publ)} · 디자인 ${esc(d.design)}</div><div class=meta>${esc(d.g)} · 난이도 ${esc(d.diff)} · ${esc(d.days)}일${d.note?" · "+esc(d.note):""}</div></div>`).join("");
}
document.getElementById("q").addEventListener("input",e=>{q=e.target.value.trim().toLowerCase();render()});
render();
</script></body></html>
"""
HTML = (HTML.replace("__GEN__", f"{dt.date.today():%Y-%m-%d} 생성").replace("__CARDS__", sum_cards)
        .replace("__SECTIONS__", sections).replace("__DATA__", _j.dumps(data, ensure_ascii=False))
        .replace("__PEOPLE__", _j.dumps(PEOPLE, ensure_ascii=False)))
open(os.path.join(HERE, "개발납기_일정안.html"), "w").write(HTML)

for p in PEOPLE:
    print(p, "이관안 개발일수", seq[p][-1]["cum"], "마지막 납기", seq[p][-1]["due"])
print("saved", OUT)
