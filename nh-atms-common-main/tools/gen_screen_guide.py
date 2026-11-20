#!/usr/bin/env python3
"""
화면 개발 가이드(docs/09_권한관리_폐쇄망_적용가이드.md) 생성기 — 표준 라이브러리만.

쓰는 법 (저장소 nh-atms-common-main 맨 위에서)
  python3 -I tools/gen_screen_guide.py          # docs/09 를 다시 만든다
  python3 -I tools/gen_screen_guide.py --check  # 지금 파일과 같은지만 본다(다르면 종료코드 1)

입력 (tools/screen-guides/)
  _header.md            문서 머리(제목·작성법 요약). {{INDEX}} 자리에 화면 목록 표가 들어간다
  <화면키>.json         화면 하나의 명세: order·screenKey·title·body·base·groups·steps
  <화면키>.md           사람이 쓰는 본문(§0 한눈에 ~ §6 검사). 부록은 쓰지 않는다 — 이 도구가 만든다

steps 한 줄: {"no":"B1","kind":"new|replace|diff|delete","path":"<저장소 기준 경로>","desc":"이 파일: 한 줄",
              "drop":"<정규식, 선택>","base":"<커밋, 선택>","undo":[["<정규식>","<바꿀 글>"], …](선택),"at":"<커밋, 선택>"}
  - new    : 부록 A 에 파일 전체
  - replace: 기존 파일을 전체본으로 통째로 덮어쓴다(부록 A 에 파일 전체 — 파일/모듈 단위 반입용, 2026-11-13)
  - delete : 그 파일을 지운다(부록 A 에 '파일 삭제' 만, 부록 B 에는 없음)
  - diff : 이전 상태 → 지금 파일의 unified diff(문맥 3줄). 이전 상태는
           drop 이 있으면 "지금 파일에서 drop 에 맞는 줄을 뺀 것"(그 화면 관련 줄만 diff 로 보이게),
           없으면 git 의 base 커밋(step.base > 화면 json base) 내용.
           undo 가 있으면 "지금 파일" 대신 거기에 re.sub(MULTILINE)을 차례로 적용한 것을 쓴다 —
           뒤에 만든 화면(제N+1부 …)이 같은 파일에 넣은 줄을 앞 부의 diff 에서 빼는 용도.
           at 이 있으면 "지금 파일" 대신 그 커밋의 내용을 쓴다(뒤 부에서 그 파일을 지웠거나 크게 바꿨을 때 앞 부를 그 시점으로 고정).
           부록 B 의 SHA-512 는 지금 파일(모든 부를 적용한 뒤) 기준, at 이 있으면 그 커밋 내용 기준.
작성법 전체: docs/92_화면개발가이드_작성법.md
"""
import difflib, glob, hashlib, json, os, re, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC_DIR = os.path.join(ROOT, 'tools', 'screen-guides')
OUT = os.path.join(ROOT, 'docs', '09_권한관리_폐쇄망_적용가이드.md')
LANG = {'java': 'java', 'xml': 'xml', 'ts': 'ts', 'tsx': 'tsx', 'tsv': 'text', 'sql': 'sql', 'yml': 'yaml', 'css': 'css', 'md': 'markdown'}


def read(p):
    with open(os.path.join(ROOT, p), encoding='utf-8') as f:
        return f.read()


def git_prefix():
    return subprocess.run(['git', 'rev-parse', '--show-prefix'], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()


def at_commit(commit, path):
    r = subprocess.run(['git', 'show', f'{commit}:{git_prefix()}{path}'], cwd=ROOT, capture_output=True, text=True)
    if r.returncode != 0:
        raise SystemExit(f'[gen_screen_guide] {commit} 에 {path} 가 없습니다: {r.stderr.strip()}')
    return r.stdout


def lang(p):
    return LANG.get(p.rsplit('.', 1)[-1], 'text')


def content(s):
    """step 의 '적용 뒤' 내용 — at 이 있으면 그 커밋, 없으면 지금 파일"""
    return at_commit(s['at'], s['path']) if s.get('at') else read(s['path'])


def sha(s):
    return hashlib.sha512(content(s).encode('utf-8')).hexdigest()


def appendix(spec):
    title = spec['title']
    out = [f"\n## 부록 A. {title} — 순서대로 새 파일 전체 · 기존 파일 diff\n",
           "> §2 의 번호 순서 그대로입니다. 경로는 `nh-atms-common-main/` 기준. "
           "**새** = 전체 소스로 새 파일, **diff** = `-` 줄 지우고 `+` 줄 넣기(이 화면 관련 줄만).\n"]
    seen = set()
    for s in spec['steps']:
        g = s['no'][0]
        if g not in seen:
            out.append(f"\n### {spec['groups'].get(g, g)}\n")
            seen.add(g)
        if s['kind'] == 'delete':
            out.append(f"\n#### {s['no']}. 파일 삭제 `{s['path']}`\n\n> 이 파일: {s['desc']}\n\n이 파일을 지웁니다(이 파일을 부르던 곳은 앞 단계에서 이미 고쳤습니다).")
            continue
        cur = content(s)
        if s['kind'] in ('new', 'replace'):
            head = '새 파일' if s['kind'] == 'new' else '전체 교체'
            out.append(f"\n#### {s['no']}. {head} `{s['path']}`\n\n> 이 파일: {s['desc']}\n\n```{lang(s['path'])}\n{cur.rstrip()}\n```")
            continue
        for pat, rep in s.get('undo', []):
            cur = re.sub(pat, rep, cur, flags=re.M)
        if s.get('drop'):
            before = ''.join(l for l in cur.splitlines(True) if not re.search(s['drop'], l))
        else:
            before = at_commit(s.get('base') or spec['base'], s['path'])
        d = ''.join(difflib.unified_diff(before.splitlines(True), cur.splitlines(True), n=3))
        d = d[d.index('@@'):] if '@@' in d else '(변경 없음)'
        out.append(f"\n#### {s['no']}. diff `{s['path']}`\n\n> 이 파일: {s['desc']}\n\n```diff\n{d.rstrip()}\n```")
    out.append(f"\n## 부록 B. {title} — 파일별 SHA-512 (적용을 마친 뒤, nh-atms-common-main 맨 위에서 `sha512sum -c`)\n\n```text")
    out += [f"{sha(s)}  {s['path']}" for s in spec['steps'] if s['kind'] != 'delete']
    out.append('```\n')
    return '\n'.join(out)


def build():
    specs = []
    for p in sorted(glob.glob(os.path.join(SPEC_DIR, '*.json'))):
        with open(p, encoding='utf-8') as f:
            specs.append(json.load(f))
    specs.sort(key=lambda s: s['order'])
    index = ['| 부 | 화면 | 화면키 | 파일 수(새·전체 교체 / diff) |', '|---|---|---|---|']
    for s in specs:
        n_new = sum(1 for x in s['steps'] if x['kind'] in ('new', 'replace'))
        n_del = sum(1 for x in s['steps'] if x['kind'] == 'delete')
        index.append(f"| 제{s['order']}부 | {s['title']} | `{s['screenKey']}` | {n_new} / {len(s['steps']) - n_new - n_del}"
                     + (f" / 삭제 {n_del}" if n_del else '') + " |")
    parts = [read('tools/screen-guides/_header.md').replace('{{INDEX}}', '\n'.join(index)).rstrip() + '\n']
    for s in specs:
        body = read(f"tools/screen-guides/{s['body']}").rstrip()
        parts.append(f"\n<!-- 생성물: tools/gen_screen_guide.py — {s['screenKey']} 시작 (손으로 고치지 말고 tools/screen-guides/ 를 고친 뒤 다시 만든다) -->\n\n"
                     f"# 제{s['order']}부. {s['title']} (`{s['screenKey']}`)\n\n{body}\n{appendix(s)}\n<!-- {s['screenKey']} 끝 -->\n")
    return '\n'.join(parts)


def main():
    text = build()
    if '--check' in sys.argv:
        with open(OUT, encoding='utf-8') as f:
            same = f.read() == text
        print('[gen_screen_guide] ' + ('같습니다.' if same else '다릅니다 — python3 -I tools/gen_screen_guide.py 로 다시 만드세요.'))
        sys.exit(0 if same else 1)
    with open(OUT, 'w', encoding='utf-8') as f:
        f.write(text)
    print(f'[gen_screen_guide] {os.path.relpath(OUT, ROOT)} 를 만들었습니다.')


if __name__ == '__main__':
    main()
