package kr.personal.historyjobs.core;
import java.util.*;
/** Reads bounded data literals from the legacy board. Never evaluates JavaScript. */
public final class ArrayLiteral {
    private final String text;private int p,nodes;
    private ArrayLiteral(String text,int start){this.text=text;this.p=start;}
    public static List<?> posts(String html){return named(html,"Posts");}
    public static List<?> named(String html,String name){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\bvar\\s+"+java.util.regex.Pattern.quote(name)+"\\s*=\\s*(\\[)").matcher(html);
        if(!m.find())return List.of();
        try{Object v=new ArrayLiteral(html,m.start(1)).value(0);return v instanceof List?(List<?>)v:List.of();}catch(IllegalArgumentException e){return List.of();}
    }
    private void ws(){while(p<text.length()&&Character.isWhitespace(text.charAt(p)))p++;}
    private Object value(int depth){
        if(depth>40||++nodes>100000)throw new IllegalArgumentException("data limits");ws();if(p>=text.length())throw new IllegalArgumentException("truncated data");
        char ch=text.charAt(p);
        if(ch=='['){p++;List<Object> a=new ArrayList<>();ws();if(p<text.length()&&text.charAt(p)==']'){p++;return a;}while(true){a.add(value(depth+1));ws();if(p>=text.length())throw new IllegalArgumentException();char sep=text.charAt(p++);if(sep==']')return a;if(sep!=',')throw new IllegalArgumentException();}}
        if(ch=='\"'||ch=='\'')return string();
        int start=p;while(p<text.length()&&",] \r\n\t".indexOf(text.charAt(p))<0)p++;
        String s=text.substring(start,p);if(s.equals("null"))return null;if(s.equals("true"))return Boolean.TRUE;if(s.equals("false"))return Boolean.FALSE;
        try{return Double.parseDouble(s);}catch(Exception e){throw new IllegalArgumentException("not a data literal");}
    }
    private String string(){
        char quote=text.charAt(p++);StringBuilder b=new StringBuilder();
        while(p<text.length()){
            char c=text.charAt(p++);if(c==quote)return b.toString();if(c!='\\'){b.append(c);continue;}
            if(p>=text.length())throw new IllegalArgumentException();char esc=text.charAt(p++);
            switch(esc){case 'n':b.append('\n');break;case 'r':b.append('\r');break;case 't':b.append('\t');break;case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'u':if(p+4>text.length())throw new IllegalArgumentException();b.append((char)Integer.parseInt(text.substring(p,p+4),16));p+=4;break;case 'x':if(p+2>text.length())throw new IllegalArgumentException();b.append((char)Integer.parseInt(text.substring(p,p+2),16));p+=2;break;default:b.append(esc);}
            if(b.length()>1_000_000)throw new IllegalArgumentException("string limit");
        }
        throw new IllegalArgumentException("unterminated string");
    }
}
