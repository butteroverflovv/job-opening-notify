package kr.personal.historyjobs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import org.json.JSONTokener;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import kr.personal.historyjobs.core.Html;
import kr.personal.historyjobs.core.GoyangBoard;

/** Foreground-only browser for inspecting a public board and exporting rendered HTML. */
public class InspectActivity extends Activity {
    private WebView web;private String captured="";
    @Override public void onCreate(Bundle state){
        super.onCreate(state);boolean original=getIntent().getBooleanExtra("original",false);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Ui.WHITE);
        int pad=(int)(16*getResources().getDisplayMetrics().density);root.setPadding(pad,pad,pad,pad);
        if(android.os.Build.VERSION.SDK_INT>=30)root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets x=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(pad+x.left,pad+x.top,pad+x.right,pad+x.bottom);return insets;});
        LinearLayout top=new LinearLayout(this);top.setGravity(android.view.Gravity.CENTER_VERTICAL);top.addView(Ui.iconButton(this,"back","돌아가기",Ui.INK,this::finish));top.addView(Ui.text(this,original?"원문 보기":"게시판 연결 진단",18,Ui.INK,true));root.addView(top);
        TextView info=Ui.text(this,"게시판 원문을 확인하거나, 수집 오류를 분석할 수 있도록 현재 페이지를 HTML로 저장할 수 있어요.",13,Ui.SECONDARY,false);info.setPadding(0,pad,0,pad);if(!original)root.addView(info);
        Button capture=Ui.button(this,"현재 페이지 HTML 저장",true,()->{});if(!original)root.addView(capture);
        if(original){Button browser=Ui.button(this,"브라우저에서 게시판 열기",false,()->{String url=getIntent().getStringExtra("url");String target=GoyangBoard.supports(url)?GoyangBoard.endpoint(url)+"?categoryid=12&menuid=03&groupid=00":url;try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(target)));}catch(Exception e){Toast.makeText(this,"주소를 열 앱이 없어요",Toast.LENGTH_LONG).show();}});root.addView(browser);}
        web=new WebView(this);web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return Html.resolve(request.getUrl().toString(),request.getUrl().toString()).isEmpty();}});
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        String url=getIntent().getStringExtra("url");String safe=Html.resolve(url==null?"":url,url==null?"":url);if(!safe.isEmpty())try{if(GoyangBoard.supports(safe))web.postUrl(GoyangBoard.endpoint(safe),GoyangBoard.body(safe));else web.loadUrl(safe);}catch(IllegalArgumentException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
        capture.setOnClickListener(v->web.evaluateJavascript("document.documentElement.outerHTML",value->{try{
            Object parsed=new JSONTokener(value).nextValue();if(!(parsed instanceof String))throw new IllegalStateException();captured=(String)parsed;
            if(captured.length()>4_000_000){Toast.makeText(this,"페이지가 너무 커요",Toast.LENGTH_LONG).show();return;}
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/html").putExtra(Intent.EXTRA_TITLE,"history-board-diagnostic.html");startActivityForResult(i,301);
        }catch(Exception e){Toast.makeText(this,"페이지 저장 준비 실패",Toast.LENGTH_LONG).show();}}));
    }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=301||result!=RESULT_OK||data==null||data.getData()==null)return;
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IllegalStateException();out.write(captured.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"페이지를 저장했어요",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"저장 실패",Toast.LENGTH_LONG).show();}finally{captured="";}}
    @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
