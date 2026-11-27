import urllib.request,urllib.parse,pathlib,re,json,concurrent.futures
out=pathlib.Path('international-diagnostics');out.mkdir(exist_ok=True)
sites=[["seoul-international","서울국제고등학교","https://sghs.sen.hs.kr/79021/subMenu.do","서울","교사 OR 기간제 교원"],["sejong-international","세종국제고등학교","https://sjgl.sjeduhs.kr/sjgl-h/na/ntt/selectNttList.do?mi=6656&bbsId=3814","기타 지역","교사 AND 공고"],["goyang-international","고양국제고등학교","https://ggg.hs.kr/?menu_no=168","경기","교사"],["dongtan-international","동탄국제고등학교","https://www.dtg.hs.kr/030101","경기","교사"],["cheongshim-international","청심국제중고등학교","https://csia.hs.kr/?act=board.list&code=1029","경기","교사 AND 공고 OR 기간제 교원"],["daewon-international","대원국제중학교","http://daewon.ms.kr/?act=board.list&code=1310","서울","기간제교원 OR 기간제 교원 OR 교사"]]
def get(item):
 key,name,url,region,words=item;result={'id':key,'url':url}
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36'})
  with urllib.request.urlopen(req,timeout=25) as r:raw=r.read(4000000);ct=r.headers.get_content_charset();result.update(status=r.status,final=r.url,bytes=len(raw))
  meta=re.search(br'charset\\s*=\\s*[\'"]?([a-zA-Z0-9_-]+)',raw[:4096],re.I);enc=meta.group(1).decode() if meta else ct or 'utf-8';text=raw.decode(enc,errors='replace');(out/(key+'.html')).write_text(text);(out/(key+'.bin')).write_bytes(raw)
  result['links']=[(a,re.sub('<[^>]+>',' ',b)[:150]) for a,b in re.findall(r'<a[^>]+href=[\'"]([^\'"]+)[\'"][^>]*>(.*?)</a>',text,re.S|re.I) if re.search(r'교원|교사|채용|공지|board|bbs|notice|page|menu_no',a+b,re.I)][-65:]
  result['frames']=re.findall(r'<(?:i?frame)[^>]+src=[\'"]([^\'"]+)',text,re.I);result['forms']=re.findall(r'<(?:form|input)[^>]+>',text,re.I)[-20:]
 except Exception as e:result['error']=str(e)
 return result
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:results=list(pool.map(get,sites))
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
for r in results:print(json.dumps(r,ensure_ascii=False))
