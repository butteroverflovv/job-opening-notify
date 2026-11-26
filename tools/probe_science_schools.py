import urllib.request,urllib.parse,re,json,pathlib,time
out=pathlib.Path('science-diagnostics');out.mkdir(exist_ok=True)
urls={
'gsh-list':'https://gs-h.goesw.kr/gs-h/na/ntt/selectNttList.do?mi=1275&bbsId=1088',
'sshs-list':'https://sshs.sen.hs.kr/75052/subMenu.do'}
results=[]
def fetch(name,url):
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36 HistoryJobs/0.1'})
  with urllib.request.urlopen(req,timeout=20) as r:
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
