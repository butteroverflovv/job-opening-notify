package kr.personal.historyjobs.core;

import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/** Bounded text extraction for HWP 5 and HWPX. No original app code is copied. */
public final class HanText {
    private HanText() {}
    private static final int LIMIT=16*1024*1024;
    public static String read(byte[] bytes)throws Exception{
        if(bytes.length>12*1024*1024)throw new IOException("한글 파일이 12MB보다 큽니다");
        if(bytes.length>4&&bytes[0]=='P'&&bytes[1]=='K')return hwpx(bytes);
        if(bytes.length>512&&(bytes[0]&255)==0xd0&&(bytes[1]&255)==0xcf)return hwp(bytes);
        throw new IOException("지원하지 않는 한글 문서 형식");
    }
    private static String hwpx(byte[] b)throws Exception{
        TreeMap<Integer,String> sections=new TreeMap<>();int total=0;
        try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(b))){ZipEntry e;int entries=0;
            while((e=z.getNextEntry())!=null){
                if(++entries>2500)throw new IOException("문서 항목 수 제한 초과");
                if(!e.getName().matches("(?i)Contents/section\\d+\\.xml"))continue;
                byte[] xml=Net.read(z,4*1024*1024);total+=xml.length;if(total>LIMIT)throw new IOException("문서 해제 크기 제한 초과");
                SAXParserFactory f=SAXParserFactory.newInstance();f.setNamespaceAware(true);
                f.setFeature("http://xml.org/sax/features/external-general-entities",false);
                f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
                StringBuilder text=new StringBuilder();
                f.newSAXParser().parse(new ByteArrayInputStream(xml),new DefaultHandler(){
                    boolean inText=false;
                    @Override public void startElement(String uri,String local,String name,Attributes a){if(local.equals("t"))inText=true;if(local.equals("lineBreak"))text.append('\n');}
                    @Override public void characters(char[] c,int s,int n){if(inText)text.append(c,s,n);}
                    @Override public void endElement(String uri,String local,String name){if(local.equals("t"))inText=false;if(local.equals("p")||local.equals("tr"))text.append('\n');if(local.equals("tc"))text.append(" | ");}
                });
                int n=Integer.parseInt(e.getName().replaceAll("\\D",""));sections.put(n,text.toString());
            }
        }
        if(sections.isEmpty())throw new IOException("HWPX 본문 없음");return Html.clean(String.join("\n",sections.values()));
    }
    private static String hwp(byte[] bytes)throws Exception{
        Compound c=new Compound(bytes);byte[] header=c.stream("FileHeader");
        if(header.length<40)throw new IOException("HWP 헤더 없음");int flags=le(header,36);
        if((flags&6)!=0)throw new IOException("암호화·배포용 HWP는 원문 확인 필요");
        List<String> names=new ArrayList<>();for(String s:c.entries.keySet())if(s.matches("Section\\d+"))names.add(s);
        names.sort(Comparator.comparingInt(s->Integer.parseInt(s.substring(7))));
        if(names.isEmpty())throw new IOException("HWP 본문 없음");
        StringBuilder out=new StringBuilder();int total=0;
        for(String name:names){
            byte[] data=c.stream(name);
            if((flags&1)!=0){Inflater inflater=new Inflater(true);try(InputStream in=new InflaterInputStream(new ByteArrayInputStream(data),inflater)){data=Net.read(in,LIMIT);}finally{inflater.end();}}
            total+=data.length;if(total>LIMIT)throw new IOException("HWP 해제 크기 제한 초과");
            for(int p=0;p+4<=data.length;){
                int h=le(data,p);p+=4;int tag=h&1023,size=(h>>>20)&4095;
                if(size==4095){if(p+4>data.length)throw new IOException("HWP 레코드 손상");size=le(data,p);p+=4;}
                if(size<0||size>data.length-p)throw new IOException("HWP 레코드 크기 오류");
                if(tag==67){String raw=new String(data,p,size,StandardCharsets.UTF_16LE);
                    for(int i=0;i<raw.length();i++){char ch=raw.charAt(i);if(ch=='\n'||ch=='\r'){out.append('\n');continue;}if(ch=='\t'){out.append(' ');i+=7;continue;}if(ch<32){if(Set.of(1,2,3,11,12,14,15,16,17,18,21,22,23).contains((int)ch))i+=7;continue;}out.append(ch);}out.append('\n');}
                p+=size;
            }
        }
        return Html.clean(out.toString());
    }
    private static int le(byte[] b,int p)throws IOException{if(p<0||p+4>b.length)throw new IOException("문서 범위 오류");return ByteBuffer.wrap(b,p,4).order(ByteOrder.LITTLE_ENDIAN).getInt();}
    private static int u16(byte[] b,int p)throws IOException{if(p<0||p+2>b.length)throw new IOException("문서 범위 오류");return (b[p]&255)|((b[p+1]&255)<<8);}
    private static final class Entry {String name;int start;long size;Entry(String n,int s,long z){name=n;start=s;size=z;}}
    private static final class Compound {
        final byte[] bytes;final int sector,mini,cutoff;final int[] fat,miniFat;final Map<String,Entry> entries=new LinkedHashMap<>();final byte[] miniStream;
        Compound(byte[] bytes)throws IOException{
            this.bytes=bytes;int shift=u16(bytes,30),mshift=u16(bytes,32);if((shift!=9&&shift!=12)||mshift!=6)throw new IOException("지원하지 않는 HWP 저장 구조");
            sector=1<<shift;mini=1<<mshift;cutoff=le(bytes,56);if(cutoff!=4096)throw new IOException("HWP 미니 스트림 오류");
            List<Integer> ids=new ArrayList<>();for(int i=0;i<109;i++){int s=le(bytes,76+4*i);if(s>=0)ids.add(s);}
            int dif=le(bytes,68),n=le(bytes,72);if(n<0||n>1024)throw new IOException("HWP DIFAT 오류");Set<Integer> seen=new HashSet<>();
            for(int i=0;i<n&&dif>=0;i++){if(!seen.add(dif))throw new IOException("문서 체인 순환");byte[] sec=sector(dif);for(int p=0;p<sector-4;p+=4){int s=le(sec,p);if(s>=0)ids.add(s);}dif=le(sec,sector-4);}
            if(ids.size()>8192)throw new IOException("HWP FAT 크기 제한");
            int expected=le(bytes,44);if(expected<1||expected>ids.size())throw new IOException("HWP FAT 손상");
            fat=new int[expected*(sector/4)];int cursor=0;for(int i=0;i<expected;i++){byte[] sec=sector(ids.get(i));for(int p=0;p<sector;p+=4)fat[cursor++]=le(sec,p);}
            byte[] directory=chain(le(bytes,48),fat,bytes,false,LIMIT);
            Entry root=null;
            for(int p=0;p+128<=directory.length;p+=128){int len=u16(directory,p+64),type=directory[p+66]&255;if(len<2||len>64||type==0)continue;
                String name=new String(directory,p,len-2,StandardCharsets.UTF_16LE);long size=le(directory,p+120)&0xffffffffL;if(shift==12)size|=(le(directory,p+124)&0xffffffffL)<<32;
                if(size<0||size>LIMIT)throw new IOException("HWP 스트림 크기 제한");Entry e=new Entry(name,le(directory,p+116),size);if(type==5)root=e;else if(type==2)entries.put(name,e);
            }
            int miniCount=le(bytes,64);if(miniCount<0||miniCount>8192)throw new IOException("HWP mini FAT 오류");
            byte[] mf=miniCount==0?new byte[0]:chain(le(bytes,60),fat,bytes,false,Math.min(LIMIT,miniCount*sector));
            miniFat=new int[mf.length/4];for(int i=0;i<miniFat.length;i++)miniFat[i]=le(mf,4*i);
            miniStream=root==null||root.size==0?new byte[0]:trim(chain(root.start,fat,bytes,false,LIMIT),(int)root.size);
        }
        byte[] sector(int id)throws IOException{long pos=(long)(id+1)*sector;if(id<0||pos<0||pos+sector>bytes.length)throw new IOException("HWP 섹터 범위 오류");return Arrays.copyOfRange(bytes,(int)pos,(int)pos+sector);}
        byte[] chain(int start,int[] table,byte[] source,boolean small,int max)throws IOException{
            int s=start,unit=small?mini:sector;ByteArrayOutputStream out=new ByteArrayOutputStream();Set<Integer> visited=new HashSet<>();
            while(s>=0){if(s>=table.length||!visited.add(s))throw new IOException("HWP 체인 손상");long p=(long)s*unit+(small?0:sector);if(p<0||p+unit>source.length)throw new IOException("HWP 데이터 범위 오류");if(out.size()+unit>max)throw new IOException("HWP 스트림 제한");out.write(source,(int)p,unit);s=table[s];}return out.toByteArray();
        }
        byte[] stream(String name)throws IOException{Entry e=entries.get(name);if(e==null)return new byte[0];return trim(chain(e.start,e.size<cutoff?miniFat:fat,e.size<cutoff?miniStream:bytes,e.size<cutoff,LIMIT),(int)e.size);}
        byte[] trim(byte[] b,int size)throws IOException{if(size>b.length)throw new IOException("HWP 데이터 잘림");return Arrays.copyOf(b,size);}
    }
}
