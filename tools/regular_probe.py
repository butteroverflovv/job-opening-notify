import urllib.request,urllib.parse,pathlib,re,concurrent.futures
out=pathlib.Path('regular-diagnostics');out.mkdir(exist_ok=True)
base='https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancList.do'
sen='https://work.sen.go.kr/work/search/recInfo/BD_selectSrchRecInfo.do?jobType=jd001&jobCategory=jd001001&jobDuty=jd001001002&q_sortBy=regDt&q_recClosed=closed'
def get(key,url):
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'})
  with urllib.request.urlopen(req,timeout=25) as r: text=r.read(4000000).decode(r.headers.get_content_charset() or 'utf-8',errors='replace')
  (out/(key+'.html')).write_text(text)
  print('URL',key,url,flush=True)
  for line in text.splitlines():
   if re.search(r'pbanc|정규|직무|역사|rcJob|rcrt|jobDuty|jobType|goTab|tabChange|goSearch|srch|function|currPage|name=',line,re.I):print(key,line.strip()[:1800],flush=True)
  return text
 except Exception as e:print('ERROR',key,str(e),flush=True);return ''
g=get('goe',base);get('sen-regular',sen)
for i,src in enumerate(re.findall(r'<script[^>]*src=[\'"]([^\'"]+)',g,re.I)):
 if any(x in src.lower() for x in ['pbanc','hnfp','recruit']):get('script'+str(i),urllib.parse.urljoin(base,src))
