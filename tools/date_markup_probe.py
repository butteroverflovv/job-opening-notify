import concurrent.futures, urllib.request, urllib.error, http.cookiejar, pathlib, json
out=pathlib.Path("date-probe"); out.mkdir(exist_ok=True)
urls={"dtg-pretty":"https://www.dtg.hs.kr/030101","dtg-query":"https://www.dtg.hs.kr/bbs/board.php?bo_table=030101","dtg-no-www":"https://dtg.hs.kr/030101","dtg-http":"http://www.dtg.hs.kr/030101","sawl-www":"https://www.sawl.hs.kr/sys/bbs/board.php?bo_table=030601","sawl-no-www":"https://sawl.hs.kr/sys/bbs/board.php?bo_table=030601","sawl-http":"http://www.sawl.hs.kr/sys/bbs/board.php?bo_table=030601"}
agents={"mobile":"Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1","desktop":"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/131.0 Safari/537.36"}
def fetch(item):
 name,url,agent=item
 result={"name":name,"url":url}
 try:
  opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
  req=urllib.request.Request(url,headers={"User-Agent":agent,"Accept":"text/html,application/xhtml+xml","Referer":url})
  with opener.open(req,timeout=20) as res:
   data=res.read(4000000);(out/(name+".bin")).write_bytes(data);result.update(status=res.status,final=res.url,headers=dict(res.headers),size=len(data))
 except Exception as e: result["error"]=str(e)
 (out/(name+".json")).write_text(json.dumps(result,ensure_ascii=False))
 print(json.dumps({k:v for k,v in result.items() if k!="headers"},ensure_ascii=False),flush=True)
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
 list(pool.map(fetch,[(n+"-"+a,u,v) for n,u in urls.items() for a,v in agents.items()]))
