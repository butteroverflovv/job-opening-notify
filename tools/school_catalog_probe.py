import urllib.request,urllib.parse,pathlib,re,json,concurrent.futures
out=pathlib.Path('school-catalog-diagnostics');out.mkdir(exist_ok=True)
sites=[["gc-notice","과천","https://www.gcfl.or.kr/notification/notice.asp","경기","채용"],["gc-read","과천","https://www.gcfl.or.kr/notification/notice_read.asp?idx=2416&lv1=2416&lv3=0&page=1","경기","채용"],["gy-simple","고양","https://www.gyfl.hs.kr/main/main.php","경기","교사"],["gy-default","고양","https://www.gyfl.hs.kr/main/main.php?categoryid=12","경기","교사"],["suwon-root","수원","https://www.sawl.hs.kr/","경기","교사"]]
def get(item):
 key,name,url,region,words=item
 result={'id':key,'url':url}
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36'})
  with urllib.request.urlopen(req,timeout=25) as r:
   raw=r.read(4000000);ct=r.headers.get_content_charset();result.update(status=r.status,final=r.url,bytes=len(raw))
  enc=('euc-kr' if re.search(br'charset\\s*=\\s*[\'"]?(?:euc-kr|ks_c_5601-1987)',raw,re.I) else (ct or 'utf-8'))
  (out/(key+'.bin')).write_bytes(raw);text=raw.decode(enc,errors='replace');(out/(key+'.html')).write_text(text)
  result['links']=[(a,b[:250]) for a,b in re.findall(r'<a[^>]+href=[\'"]([^\'"]+)[\'"][^>]*>(.*?)</a>',text,re.S|re.I) if re.search(r'교원|교사|채용|공지|게시판|board|bbs|notice|write|news|page',a+b,re.I)][:70]
  result['frames']=re.findall(r'<(?:i?frame)[^>]+src=[\'"]([^\'"]+)',text,re.I)
  result['scripts']=re.findall(r'<script[^>]+src=[\'"]([^\'"]+)',text,re.I)[-12:]
  result['forms']=re.findall(r'<(?:form|input)[^>]+>',text,re.I)[-25:]
 except Exception as e:
  result['error']=str(e)
  if isinstance(e,urllib.error.HTTPError):
   raw=e.read(100000);(out/(key+'-error.bin')).write_bytes(raw);result['errorBody']=raw.decode('utf-8',errors='replace')[:1000]
 return result
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool: results=list(pool.map(get,sites))
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
for r in results:print(json.dumps(r,ensure_ascii=False))
