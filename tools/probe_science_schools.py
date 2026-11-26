import urllib.request,urllib.parse,re,json,pathlib,http.cookiejar,time
out=pathlib.Path('science-diagnostics');out.mkdir(exist_ok=True)
opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
results=[]
ua='Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1'
def fetch(name,url,ref=''):
 try:
  req=urllib.request.Request(url,headers={'User-Agent':ua,'Accept':'text/html,application/xhtml+xml'})
  if ref:req.add_header('Referer',ref)
  with opener.open(req,timeout=20) as r:
   b=r.read(4000001);s=b.decode(r.headers.get_content_charset() or 'utf-8',errors='replace');final=r.url
  (out/(name+'.html')).write_text(s)
  row={'name':name,'url':url,'final_url':final,'bytes':len(b)};results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True);time.sleep(.6);return s
 except Exception as e:
  row={'name':name,'url':url,'error':str(e)};results.append(row);print(json.dumps(row),flush=True);return ''
g='https://gs-h.goesw.kr/gs-h/na/ntt/selectNttList.do?mi=1275&bbsId=1088'
s='https://sshs.sen.hs.kr/75052/subMenu.do'
fetch('gsh-list',g)
fetch('gsh-detail','https://gs-h.goesw.kr/gs-h/na/ntt/selectNttInfo.do?mi=1275&bbsId=1088&nttSn=599447',g)
root=fetch('sshs-list',s)
fields=dict(re.findall(r'<input[^>]*name="([^"]+)"[^>]*value="([^"]*)"',root,re.I))
fields={k:v for k,v in fields.items() if k in ['bbsId','bbsTyCode','customRecordCountPerPage','pageIndex','searchCondition','searchKeyword']}
fragment=fetch('sshs-ajax-get','https://sshs.sen.hs.kr/dggb/module/board/selectBoardListAjax.do?'+urllib.parse.urlencode(fields),s)
matches=re.findall(r"fnView[(]'([^']+)',[ ]*'([0-9]+)'",fragment)
if matches:
 bbs,ntt=matches[0]
 fetch('sshs-public-detail',s+'?nttId='+ntt,s)
 for name,endpoint in [('sshs-ajax-detail','selectBoardDetailAjax.do'),('sshs-sen-detail','selectBoardSenDetailAjax.do')]:
  fetch(name,'https://sshs.sen.hs.kr/dggb/module/board/'+endpoint+'?'+urllib.parse.urlencode({'bbsId':bbs,'nttId':ntt}),s)
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
