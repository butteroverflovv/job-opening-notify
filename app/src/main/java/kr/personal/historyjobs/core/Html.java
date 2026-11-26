package kr.personal.historyjobs.core;

import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Small bounded HTML reader. Does not execute remote scripts. */
public final class Html {
    private Html() {}
    private static final Pattern TOKEN=Pattern.compile("<!--(?s:.*?)-->|<![^>]*>|<(/?)([A-Za-z][\\w:-]*)([^>]*)>|([^<]+)");
    private static final Pattern ATTR=Pattern.compile("([\\w:-]+)(?:\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+)))?");
    private static final Set<String> VOID=Set.of("area","base","br","col","embed","hr","img","input","link","meta","param","source","track","wbr");
    public static class Node {
        public final String tag;
        public final Map<String,String> attrs=new LinkedHashMap<>();
        public final List<Node> children=new ArrayList<>();
        public final StringBuilder raw=new StringBuilder();
        public Node parent;
        Node(String tag) { this.tag=tag; }
        public String attr(String k) {return attrs.getOrDefault(k,"");}
        public String text() {
            StringBuilder b=new StringBuilder();append(b);return clean(b.toString());
        }
        private void append(StringBuilder b) {
            if(Set.of("script","style","noscript","nav","header","footer").contains(tag))return;
            if(tag.equals("#text")){b.append(raw);return;}
            boolean block=Set.of("p","div","tr","li","dt","dd","h1","h2","h3","h4","br","section","article","table").contains(tag);
            if(block)b.append('\n');
            for(Node n:children){n.append(b);if(n.tag.equals("td")||n.tag.equals("th"))b.append(" | ");}
            if(block)b.append('\n');
        }
        public List<Node> all() { List<Node> out=new ArrayList<>();walk(this,out);return out; }
        private static void walk(Node n,List<Node> out){out.add(n);for(Node c:n.children)walk(c,out);}
    }
    public static Node parse(String html) {
        if(html.length()>4_000_000)throw new IllegalArgumentException("페이지가 너무 큽니다");
        Node root=new Node("root");ArrayDeque<Node> stack=new ArrayDeque<>();stack.push(root);
        Matcher m=TOKEN.matcher(html);int count=0;
        while(m.find()){
            if(++count>120_000)throw new IllegalArgumentException("페이지 구조가 너무 큽니다");
            if(m.group(4)!=null){Node n=new Node("#text");n.raw.append(decode(m.group(4)));n.parent=stack.peek();stack.peek().children.add(n);continue;}
            if(m.group(2)==null)continue;
            String tag=m.group(2).toLowerCase(Locale.ROOT);
            if("/".equals(m.group(1))){
                if(stack.stream().anyMatch(n->n.tag.equals(tag))){while(stack.size()>1){if(stack.pop().tag.equals(tag))break;}}
                continue;
            }
            // Common omitted closing tags in board tables/lists.
            if((tag.equals("li")&&stack.peek().tag.equals("li"))||((tag.equals("td")||tag.equals("th"))&&(stack.peek().tag.equals("td")||stack.peek().tag.equals("th")))||tag.equals("tr")&&stack.peek().tag.equals("tr"))stack.pop();
            Node n=new Node(tag);Matcher a=ATTR.matcher(m.group(3));
            while(a.find()){String v=a.group(2)!=null?a.group(2):a.group(3)!=null?a.group(3):a.group(4)!=null?a.group(4):"";n.attrs.put(a.group(1).toLowerCase(Locale.ROOT),decode(v));}
            n.parent=stack.peek();stack.peek().children.add(n);
            if(!VOID.contains(tag)&&!m.group(3).trim().endsWith("/")&&stack.size()<180)stack.push(n);
        }
        return root;
    }
    public static String clean(String s){return s.replace('\u00a0',' ').replace('\u200b',' ').replaceAll("[\\t\\x0B\\f\\r ]+"," ").replaceAll(" *\\n *","\n").replaceAll("\\n{3,}","\n\n").trim();}
    public static String decode(String s){
        String v=s.replace("&nbsp;"," ").replace("&quot;","\"").replace("&#39;","'").replace("&apos;","'").replace("&lt;","<").replace("&gt;",">").replace("&amp;","&");
        Matcher m=Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(v);StringBuffer b=new StringBuffer();
        while(m.find()){String r=m.group();try{int cp=m.group(1).startsWith("x")?Integer.parseInt(m.group(1).substring(1),16):Integer.parseInt(m.group(1));if(Character.isValidCodePoint(cp))r=new String(Character.toChars(cp));}catch(Exception ignored){}m.appendReplacement(b,Matcher.quoteReplacement(r));}m.appendTail(b);return b.toString();
    }
    public static String resolve(String base,String raw){
        if(raw==null||raw.trim().isEmpty())return "";
        try{URI u=URI.create(base).resolve(decode(raw).trim().replace(" ","%20"));if(!Set.of("https","http").contains(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)return "";return u.toString().split("#",2)[0];}catch(Exception e){return "";}
    }
    public static boolean sameHost(String a,String b){try{return Objects.equals(URI.create(a).getHost(),URI.create(b).getHost());}catch(Exception e){return false;}}
}
