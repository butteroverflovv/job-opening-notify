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

get('goe-regular',"https://www.goe.go.kr/recruit/ad/func/pb/hnfpPbancList.do?mi=10502&srchOcptCd=D&srchOcptNm=%EC%82%AC%EB%A6%BD%ED%95%99%EA%B5%90%20%EA%B5%90%EC%9B%90%EC%A0%95%EA%B7%9C%EC%B1%84%EC%9A%A9&srchEcptDl=N&currPage=1")
get('sen-regular-full',"https://work.sen.go.kr/work/search/recInfo/BD_selectSrchRecInfo.do?jobType=jd001&jobCategory=jd001001&jobDuty=jd001001002&q_srchType=rcrtTtl&q_srchText=&q_currPage=1&q_rowPerPage=15&q_srchArea=&q_srchSchl=&q_tabId=&q_sortBy=regDt&q_mySrchArea=&q_srchJob=jd001%2C%EC%A0%95%EA%B7%9C%EA%B5%90%EC%9B%90%2Cjd001001%2C%EB%B3%B4%ED%86%B5%EA%B5%90%EA%B3%BC%2Cjd001001002%2C%EB%8F%84%EB%8D%95%EC%82%AC%ED%9A%8C%EA%B3%BC&q_recClosed=closed&q_today=&q_jobCategory=")
