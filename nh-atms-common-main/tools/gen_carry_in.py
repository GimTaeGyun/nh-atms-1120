#!/usr/bin/env python3
"""
다음 반입 목록(docs/93_반입목록.md) 생성기 — 표준 라이브러리만.

쓰는 법 (저장소 nh-atms-common-main 맨 위에서)
  python3 -I tools/gen_carry_in.py                 # docs/93_반입목록.md 를 다시 만든다
  python3 -I tools/gen_carry_in.py --check         # 문서가 최신인지 + 바뀐 파일이 모두 모듈에 들어 있는지(아니면 종료코드 1)
  python3 -I tools/gen_carry_in.py --json <파일>   # 같은 내용을 JSON 으로(보기용 페이지를 만들 때)

입력
  tools/carry-in.json        base(지난 반입 기준 커밋) · 모듈(번호·이름·요약·파일[경로, 변경점]) · 삭제 묶음 · 적용 순서
  tools/screen-guides/*.json 화면 개발 가이드 명세 — 파일마다 어느 '부' 에서 바뀌었는지 표시용
  git                        base..HEAD 사이 바뀐 파일(새·교체·삭제 판단) — 반입은 언제나 '지금 main 의 최종본' 한 번(2026-11-13 사용자 결정)
"""
import glob, hashlib, json, os, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'docs', '93_반입목록.md')
SCOPE = ['Frontend', 'Backend', 'db']
SHORT = [
    ('Frontend/nh-atms-web/src/', 'src/'),
    ('Frontend/nh-atms-web/', 'web-front/'),
    ('Backend/nh-atms-api/atms-web/src/main/java/kr/co/nh/atms/web/', 'web/'),
    ('Backend/nh-atms-api/atms-web/src/test/java/kr/co/nh/atms/web/', 'test/'),
    ('Backend/nh-atms-api/atms-web/src/main/resources/mapper/', 'mapper/'),
    ('Backend/nh-atms-api/atms-common/src/main/java/kr/co/nh/atms/common/', 'common/'),
    ('Backend/nh-atms-api/atms-common/src/main/resources/', 'common-res/'),
]


def git(*args):
    return subprocess.run(['git', *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout


def prefix():
    return git('rev-parse', '--show-prefix').strip()


def existed(commit, path):
    return subprocess.run(['git', 'cat-file', '-e', f'{commit}:{prefix()}{path}'], cwd=ROOT, capture_output=True).returncode == 0


def short(p):
    for a, b in SHORT:
        if p.startswith(a):
            return b + p[len(a):]
    return p


def sha(p):
    with open(os.path.join(ROOT, p), 'rb') as f:
        return hashlib.sha512(f.read()).hexdigest()


def collect():
    cfg = json.load(open(os.path.join(ROOT, 'tools', 'carry-in.json'), encoding='utf-8'))
    base = cfg['base']
    parts = {}
    for sp in glob.glob(os.path.join(ROOT, 'tools', 'screen-guides', '*.json')):
        s = json.load(open(sp, encoding='utf-8'))
        for st in s['steps']:
            parts.setdefault(st['path'], set()).add(s['order'])
    changed = {}
    for line in git('diff', '--name-status', '--relative', base, 'HEAD', '--', *SCOPE).splitlines():
        cols = line.split('\t')
        changed[cols[-1]] = cols[0][0]
    listed, problems = set(), []
    modules = []
    for m in cfg['modules']:
        files = []
        for path, note in m['files']:
            listed.add(path)
            now = os.path.exists(os.path.join(ROOT, path))
            kind = '삭제' if not now else ('교체' if existed(base, path) else '새')
            if path not in changed:
                problems.append(f'목록에 있지만 {base} 이후 바뀌지 않음: {path}')
            files.append({'path': path, 'short': short(path), 'kind': kind, 'note': note,
                          'parts': sorted(parts.get(path, [])), 'sha': sha(path) if now else None})
        groups = []
        for g, what in m.get('deleteGroups', []):
            hit = sorted(p for p, st in changed.items() if st == 'D' and p not in listed and (p.startswith(g) or g in p))
            for p in hit:
                listed.add(p)
            groups.append({'group': short(g), 'what': what, 'files': [short(p) for p in hit]})
        modules.append({**{k: m[k] for k in ('no', 'name', 'part', 'summary')}, 'files': files, 'deleteGroups': groups})
    for p, st in sorted(changed.items()):
        if p not in listed:
            problems.append(f'바뀐 파일인데 어느 모듈에도 없음({st}): {p}')
    return {'base': base, 'head': git('rev-parse', '--short', 'HEAD').strip(), 'title': cfg['title'], 'order': cfg['order'], 'verify': cfg.get('verify', []), 'modules': modules}, problems


def count(m, kind):
    n = sum(1 for f in m['files'] if f['kind'] == kind)
    return n + (sum(len(g['files']) for g in m['deleteGroups']) if kind == '삭제' else 0)


def render(d):
    o = [f"# 93. {d['title']} — 모듈별 파일 · 변경점 (생성물)", '',
         f"> 기준: 지난 반입 이후 = `{d['base']}` → 지금 `main`. **반입은 파일 단위, 모든 파일은 지금 main 의 최종본 한 번만**(여러 번 바뀐 파일도 한 줄, 변경점 칸에 모두 적음 — 2026-11-13 사용자 결정).",
         '> - 이 파일은 생성물입니다. 고칠 때는 `tools/carry-in.json` 을 고치고 `python3 -I tools/gen_carry_in.py` (확인 `--check`).',
         '> - 소스 본문·한 줄씩 설명은 [`09_권한관리_폐쇄망_적용가이드.md`](09_권한관리_폐쇄망_적용가이드.md) 의 해당 부(제1~7부)를 봅니다. 여기는 **무엇을 넣는지 한눈에** 보는 목록입니다.',
         '> - 구분: **새** = 새 파일, **교체** = 있는 파일을 통째로 덮어쓰기, **삭제** = 지우기. 부 = 화면 개발 가이드의 제N부.',
         '> - 경로 줄임: `src/` = Frontend/nh-atms-web/src/, `web-front/` = Frontend/nh-atms-web/, `web/` = atms-web …/kr/co/nh/atms/web/, `test/` = atms-web …/src/test/java/kr/co/nh/atms/web/, `mapper/` = atms-web …/resources/mapper/, `common/` = atms-common …/kr/co/nh/atms/common/, `common-res/` = atms-common …/src/main/resources/',
         '', '## 0. 한눈에 보기', '',
         '| # | 모듈 | 범위 | 새 | 교체 | 삭제 | 무엇이 달라지나 |', '|---|---|---|---|---|---|---|']
    tot = {'새': 0, '교체': 0, '삭제': 0}
    for m in d['modules']:
        c = {k: count(m, k) for k in tot}
        for k in tot:
            tot[k] += c[k]
        o.append(f"| {m['no']} | **{m['name']}** | {m['part']} | {c['새']} | {c['교체']} | {c['삭제']} | {m['summary'][0]} |")
    o.append(f"| | **합계** | | **{tot['새']}** | **{tot['교체']}** | **{tot['삭제']}** | |")
    for m in d['modules']:
        o += ['', f"## {m['no']}. {m['name']} — {m['part']}", '']
        o += [f'- {s}' for s in m['summary']]
        if m['files']:
            o += ['', '| 구분 | 파일 | 부 | 변경점 |', '|---|---|---|---|']
            for f in m['files']:
                p = '·'.join(f'제{x}부' for x in f['parts']) or '-'
                o.append(f"| {f['kind']} | `{f['short']}` | {p} | {f['note']} |")
        if m['deleteGroups']:
            o += ['', '**삭제 묶음** (폐쇄망에서 이미 지웠으면 확인만)', '', '| 묶음 | 파일 수 | 무엇 |', '|---|---|---|']
            for g in m['deleteGroups']:
                o.append(f"| `{g['group']}` | {len(g['files'])} | {g['what']} |")
            o += ['', '<details><summary>삭제 파일 전체 목록</summary>', '', '```text']
            for g in m['deleteGroups']:
                o += g['files']
            o += ['```', '', '</details>']
    n = len(d['modules'])
    o += ['', f'## {n + 1}. 적용 순서 (빨간줄 최소)', '']
    o += [f'{i}. {s}' for i, s in enumerate(d['order'], 1)]
    if d.get('verify'):
        o += ['', '**순서 검증**', ''] + [f'- {v}' for v in d['verify']]
    o += ['', '- 모든 파일이 최종본이라 순서를 바꿔도 최종 결과는 같습니다. 위 순서대로 넣으면 **단계마다 빨간줄 0** 입니다(검증함).',
          '- 넣기 전에 지금 파일을 따로 복사해 두면 되돌리기 = 그 파일을 다시 덮어쓰기 + 새 파일 지우기.',
          '', f'## {n + 2}. DB', '',
          '- `db/change/menu_three_level.sql` — 견본 메뉴 `SMP000`·`SMP001~` → `SMP0000`(대)·`SMP0100` 견본 화면(중)·`SMP0101~`(소), 권한별 메뉴 행 메뉴ID 변경, 메뉴관리 메뉴 행 추가. **한 문장씩, 마지막 `COMMIT;`**, `<내 개인번호>` 를 바꿔서. 폐쇄망 개발 DB 에서 처음 확인.',
          '- 메뉴는 이제 DB 로만 그립니다(제3부) → 이 SQL 을 안 돌리면 좌측에 견본 화면 폴더가 안 생기고 메뉴관리 메뉴가 없습니다.',
          '- 권한관리는 테이블 변경 없음(`TB_SCR_AUTH`·`TB_SCR_AUTH_MENU`·`TB_OFC_RAW` 읽기·쓰기). 메뉴관리 메뉴의 권한은 권한관리 → 메뉴 권한 탭에서 체크.',
          '- 공지사항도 테이블 변경 없음(`TB_NOTICE` 그대로). 사무소 빈 공지 = 전체 공지(잠정 Q-24) — 새 SQL(보이는 범위·앞뒤 글)은 폐쇄망 개발 DB 에서 처음 확인.',
          '', f'## {n + 3}. 확인 체크리스트', '',
          '- [ ] 백엔드 `gradlew.bat build --offline` 통과',
          '- [ ] 프론트 `npm run check` 통과',
          '- [ ] (SQL 뒤) 상단 메뉴에 DB 대메뉴, 좌측에 견본 › 견본 화면 › 권한관리·메뉴관리',
          '- [ ] 권한관리: 부서권한·개인권한 조회·등록(권한 콤보)·수정·삭제, 메뉴 권한 탭 체크 → 저장',
          '- [ ] 메뉴관리: 소메뉴 등록(메뉴ID 미리보기 → 저장), 하위 있는 메뉴 삭제 막힘, 저장 뒤 좌측 메뉴 바로 바뀜',
          '- [ ] 공지사항: 범위 칸(전체·사무소)·N, 공지 범위 \'사무소 공지\' 때만 사무소 칸, 상세 이전·다음 글, 관리자가 아니면 남의 글 수정·삭제 흐림·등록 팝업 전체 공지 흐림',
          '- [ ] 검색필터: 권한관리·메뉴관리·기기모델코드·기기정보가 그대로 조회되고, 메뉴 권한 탭 권한 없이 [조회] → \'권한을 선택하세요.\' 한 줄',
          '- [ ] 화면 비율: 크롬 100% 에서 예전 80% 화면과 같은 크기(글자 11.2px), 표 바깥 테두리·페이징 없는 표 아래 \'총 n건\'',
          '- [ ] 공통 UI: 진한 글자색, 검색필터 `• 라벨`, 헤더 ☆ 즐겨찾기, 흰 바탕, 화면 스크롤 없이 표가 아래 끝까지',
          '- [ ] 부록 SHA-512: 넣은 파일이 이 목록과 같은지(`sha512sum -c`)',
          '', '## 부록. SHA-512 (넣은 뒤 nh-atms-common-main 맨 위에서 `sha512sum -c`)', '', '```text']
    for m in d['modules']:
        for f in m['files']:
            if f['sha']:
                o.append(f"{f['sha']}  {f['path']}")
    o += ['```', '']
    return '\n'.join(o)


def main():
    d, problems = collect()
    if '--json' in sys.argv:
        with open(sys.argv[sys.argv.index('--json') + 1], 'w', encoding='utf-8') as f:
            json.dump(d, f, ensure_ascii=False)
    text = render(d)
    if '--check' in sys.argv:
        same = os.path.exists(OUT) and open(OUT, encoding='utf-8').read() == text
        for p in problems:
            print('[gen_carry_in] ' + p)
        print('[gen_carry_in] ' + ('같습니다.' if same else '다릅니다 — python3 -I tools/gen_carry_in.py 로 다시 만드세요.'))
        sys.exit(0 if same and not problems else 1)
    for p in problems:
        print('[gen_carry_in] 주의: ' + p)
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write(text)
    print(f'[gen_carry_in] {os.path.relpath(OUT, ROOT)} 를 만들었습니다.')


if __name__ == '__main__':
    main()
