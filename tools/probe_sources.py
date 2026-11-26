#!/usr/bin/env python3
"""Optional public-board connection diagnostics. Does not decide build success."""
import concurrent.futures, json, pathlib, re, urllib.request
out = pathlib.Path('diagnostics')
out.mkdir(exist_ok=True)
urls = {
    'seoul': 'https://work.sen.go.kr/work/search/recInfo/BD_selectSrchRecInfo.do?jobType=jd002&jobCategory=jd002001&jobDuty=all0&q_tabId=jobTab&q_srchJob=jd002,%EA%B3%84%EC%95%BD%EC%A0%9C%EA%B5%90%EC%9B%90,jd002001,%EA%B8%B0%EA%B0%84%EC%A0%9C%EA%B5%90%EC%9B%90,jd002001-all,%EC%A0%84%EC%B2%B4',
    'gyeonggi': 'https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancList.do?mi=10502',
    'seoul-detail': 'https://work.sen.go.kr/work/search/recInfo/BD_selectRecDetail.do?q_rcrtSn=82380',
    'gyeonggi-detail': 'https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancInfoView.do?mi=10502&pbancSn=67178',
    'tokyo-detail': 'http://www.tokos.ed.jp/front_notice.brd/_656.405.2dd1fd3d/?shell=/index.shell:225',
    'tokyo-menu': 'http://www.tokos.ed.jp/menu_cache.js',
    'tokyo': 'http://www.tokos.ed.jp/front_notice.brd?shell=/index.shell:225',
}
def probe(item):
    key, url = item
    result = {'source': key, 'url': url}
    try:
        request = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1'})
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read(4_000_001)
            if len(raw) > 4_000_000:
                raise ValueError('page too large')
            charset = response.headers.get_content_charset() or 'utf-8'
            text = raw.decode(charset, errors='replace')
            (out / (key + '.html')).write_text(text, encoding='utf-8')
            result.update(status=response.status, bytes=len(raw), final_url=response.url)
            result['handlers'] = re.findall(r'(?:onclick|href|data-url)\s*=\s*([\'"])(.*?)\1', text, re.I)
            result['handlers'] = [value for _, value in result['handlers'] if re.search(r'rcrtSn|pbanc|front_notice|num=|mode=|fn.*(?:View|Detail|Page)|pageIndex|currPage', value, re.I)][:35]
            result['scripts'] = re.findall(r'<script[^>]*src=[\'"]([^\'"]+)', text, re.I)[:20]
            result['relevant_lines'] = [line.strip()[:1200] for line in text.splitlines() if re.search(r'function.*(?:view|detail|page)|pbancSn|q_rcrtSn|fn_pbanc|fnView|goView|viewForm|goPage|front_notice|채용|nttFileDownload|opDownload', line, re.I)][:25]
    except Exception as exc:
        result['error'] = str(exc)
    return result
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
    results = list(pool.map(probe, urls.items()))
(out / 'summary.json').write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding='utf-8')
for result in results:
    print(json.dumps(result, ensure_ascii=False))
