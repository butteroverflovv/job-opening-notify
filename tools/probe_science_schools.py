import urllib.request,urllib.parse,re,json,pathlib,time,http.cookiejar
opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
out=pathlib.Path('science-diagnostics');out.mkdir(exist_ok=True)
urls={
'gsh-list':'https://gs-h.goesw.kr/gs-h/na/ntt/selectNttList.do?mi=1275&bbsId=1088',
'sshs-list':'https://sshs.sen.hs.kr/75052/subMenu.do'}
results=[]
def fetch(name,url,data=None,referer=None):
 try:
  req=urllib.request.Request(url,data=data,headers={'User-Agent':'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1'})
  if referer:req.add_header('Referer',referer)
  with opener.open(req,timeout=20) as r:
   b=r.read(4000001);charset=r.headers.get_content_charset() or 'utf-8';s=b.decode(charset,errors='replace');final=r.url
  (out/(name+'.html')).write_text(s)
  row={'name':name,'url':url,'final_url':final,'bytes':len(b),'scripts':re.findall(r'<script[^>]+src=["\']([^"\']+)',s,re.I),'frames':re.findall(r'<iframe[^>]+src=["\']([^"\']+)',s,re.I)}
  results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True);return s,final,row
 except Exception as e:
  row={'name':name,'url':url,'error':str(e)};results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True);return '',url,{}
for name,url in urls.items():
 s,base,row=fetch(name,url)
 candidates=[]
 for raw in row.get('scripts',[]):
  u=urllib.parse.urljoin(base,raw)
  if urllib.parse.urlsplit(u).netloc==urllib.parse.urlsplit(base).netloc and re.search(r'ntt|board|menu|sub|bbs',u,re.I):candidates.append(u)
 for i,u in enumerate(list(dict.fromkeys(candidates))[:5]):time.sleep(.3);fetch(name+'-script-'+str(i),u)
 for i,raw in enumerate(row.get('frames',[])[:2]):
  u=urllib.parse.urljoin(base,raw)
  if urllib.parse.urlsplit(u).netloc==urllib.parse.urlsplit(base).netloc:fetch(name+'-frame-'+str(i),u)
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))

s=pathlib.Path(out/'sshs-list.html').read_text()
fields=dict(re.findall(r'<input[^>]*name="([^"]+)"[^>]*value="([^"]*)"',s,re.I))
fields={k:v for k,v in fields.items() if k in ['bbsId','bbsTyCode','customRecordCountPerPage','pageIndex','searchCondition','searchKeyword','checkNttId','mvmnReturnUrl']}
query=urllib.parse.urlencode(fields)
endpoint='https://sshs.sen.hs.kr/dggb/module/board/selectBoardListAjax.do'
fetch('sshs-ajax-get',endpoint+'?'+query,referer=urls['sshs-list'])
time.sleep(1)
fetch('sshs-ajax-post',endpoint,query.encode(),urls['sshs-list'])
fetch('gsh-page2',urls['gsh-list']+'&currPage=2')
fetch('gsh-detail','https://gs-h.goesw.kr/gs-h/na/ntt/selectNttInfo.do?mi=1275&bbsId=1088&nttSn=599447')
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))

fragment=(out/'sshs-ajax-get.html').read_text()
matches=re.findall(r"fnView\\('([^']+)',\\s*'([0-9]+)'",fragment)
if matches:
 bbs,ntt=matches[0]
 fetch('sshs-public-detail',urls['sshs-list']+'?nttId='+ntt)
 fetch('sshs-ajax-detail','https://sshs.sen.hs.kr/dggb/module/board/selectBoardDetailAjax.do?'+urllib.parse.urlencode({'bbsId':bbs,'nttId':ntt}),referer=urls['sshs-list'])
 fetch('sshs-sen-detail','https://sshs.sen.hs.kr/dggb/module/board/selectBoardSenDetailAjax.do?'+urllib.parse.urlencode({'bbsId':bbs,'nttId':ntt}),referer=urls['sshs-list'])
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
