import urllib.request,urllib.parse,pathlib,re,json,concurrent.futures
out=pathlib.Path('school-catalog-diagnostics');out.mkdir(exist_ok=True)
sites=[["sejong-science","세종과학고등학교","https://sjsh.sen.hs.kr/80720/subMenu.do","서울","교사"],["hansung-science","한성과학고등학교","https://hansungsh.sen.hs.kr/1642/subMenu.do","서울","교사"],["gyeonggibuk-science","경기북과학고등학교","https://gbs-h.goeujb.kr/gbs-h/na/ntt/selectNttList.do?mi=4118&bbsId=2262","경기","교사"],["myungduk-fl","명덕외국어고등학교","http://mdfh.or.kr/?act=news.main&comm=com&mcode=1310","서울","교사"],["seoul-fl","서울외국어고등학교","https://sfl.sen.hs.kr/203923/subMenu.do","서울","교원"],["ewha-fl","이화여자외국어고등학교","https://www.ewha-gfh.hs.kr/0201","서울","교사 OR 교원"],["hanyoung-fl","한영외국어고등학교","https://hyfl.sen.hs.kr/145567/subMenu.do","서울","교사 OR 교원"],["goyang-fl","고양외국어고등학교","https://www.gyfl.hs.kr/main/main.php?categoryid=12&menuid=03&groupid=00","경기","교사"],["gwacheon-fl","과천외국어고등학교","https://www.gcfl.or.kr/","경기","채용"],["gimpo-fl","김포외국어고등학교","https://gfl.hs.kr/?act=board.list&code=2312","경기","교사"],["dongducheon-fl","동두천외국어고등학교","https://dfl-h.goedy.kr/dfl-h/na/ntt/selectNttList.do?mi=6398&bbsId=3642","경기","교사 AND 공고"],["seongnam-fl","성남외국어고등학교","https://snfl-h.goesn.kr/snfl-h/na/ntt/selectNttList.do?mi=27530&bbsId=17181","경기","교사 채용 OR 교원 선발"],["suwon-fl","수원외국어고등학교","https://www.sawl.hs.kr/sys/bbs/board.php?bo_table=030601","경기","교사 AND 공고"],["anyang-fl","안양외국어고등학교","https://anyangfl-h.goeay.kr/anyangfl-h/na/ntt/selectNttList.do?mi=9329&bbsId=4646","경기","교사 AND 채용"]]
def get(item):
 key,name,url,region,words=item
 result={'id':key,'url':url}
 try:
  req=urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/131.0 Mobile Safari/537.36'})
  with urllib.request.urlopen(req,timeout=25) as r:
   raw=r.read(4000000);ct=r.headers.get_content_charset();result.update(status=r.status,final=r.url,bytes=len(raw))
  enc=ct or ('euc-kr' if re.search(br'charset\\s*=\\s*[\'"]?(?:euc-kr|ks_c_5601-1987)',raw,re.I) else 'utf-8')
  text=raw.decode(enc,errors='replace');(out/(key+'.html')).write_text(text)
  result['links']=[(a,b[:250]) for a,b in re.findall(r'<a[^>]+href=[\'"]([^\'"]+)[\'"][^>]*>(.*?)</a>',text,re.S|re.I) if re.search(r'교원|교사|채용|공지|게시판|board|bbs|notice|write|news|page',a+b,re.I)][:70]
  result['frames']=re.findall(r'<(?:i?frame)[^>]+src=[\'"]([^\'"]+)',text,re.I)
  result['scripts']=re.findall(r'<script[^>]+src=[\'"]([^\'"]+)',text,re.I)[-12:]
  result['forms']=re.findall(r'<(?:form|input)[^>]+>',text,re.I)[-25:]
 except Exception as e:result['error']=str(e)
 return result
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool: results=list(pool.map(get,sites))
(out/'summary.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
for r in results:print(json.dumps(r,ensure_ascii=False))
