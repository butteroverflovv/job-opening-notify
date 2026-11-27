import urllib.request,pathlib,re,concurrent.futures,json
out=pathlib.Path('international-diagnostics');out.mkdir(exist_ok=True)
urls=[["ggg-list","http://m.ggg.hs.kr/html/sub04/sub04_1-1.asp?menu_no=168"],["ggg-secure","https://m.ggg.hs.kr/html/sub04/sub04_1-1.asp?menu_no=168"],["ggg-detail","http://m.ggg.hs.kr/html/sub04/sub04_1-1.asp?menu_no=168&bno=1685&board_mode=view"],["dtg-board","https://www.dtg.hs.kr/bbs/board.php?bo_table=030101"],["dtg-nowww","https://dtg.hs.kr/030101"],["dtg-http","http://www.dtg.hs.kr/030101"]]
def get(item):
 key,url=item;r={'id':key,'url':url}
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36'})
  with urllib.request.urlopen(req,timeout=25) as response:b=response.read(4000000);r.update(status=response.status,final=response.url);enc=response.headers.get_content_charset() or 'utf-8'
  m=re.search(br'charset\\s*=\\s*[\'"]?([A-Za-z0-9_-]+)',b[:4096],re.I);t=b.decode(m.group(1).decode() if m else enc,errors='replace');(out/(key+'.html')).write_text(t);r['bytes']=len(b)
 except Exception as e:r['error']=str(e)
 return r
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:r=list(pool.map(get,urls))
(out/'summary.json').write_text(json.dumps(r,ensure_ascii=False,indent=2))
for item in r:print(json.dumps(item,ensure_ascii=False))
