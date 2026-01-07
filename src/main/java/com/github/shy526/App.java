package com.github.shy526;

import com.github.shy526.caimogu.CaiMoGuHelp;
import com.github.shy526.github.GithubHelp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base16;
import org.apache.commons.codec.binary.Base64;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Hello world!
 *
 */
@Slf4j
public class App 
{
    public static void main( String[] args )
    {
        log.error("启动踩蘑菇获取影响力任务");
      //  String githubApiToken = System.getenv("MY_GITHUB_API_TOKEN");
     //   String ownerRepo = System.getenv("OWNER_REPO");
        String caiMoGuToken =  System.getenv("CAI_MO_GU_TOKEN");
        int clout = CaiMoGuHelp.getClout(caiMoGuToken);
        String nickname = CaiMoGuHelp.getNickname(caiMoGuToken);
        log.error("当前用户:{},影响力:{}",nickname,clout);
        if (clout==-1){
            log.error(Base64.encodeBase64String(caiMoGuToken.getBytes()));
            log.error("CAI_MO_GU_TOKEN 已经失效 重新获取(浏览器中f12 应用程序 Cookie 中的 cmg_token )");
            return;
        }
/*        if (githubApiToken==null|| githubApiToken.trim().isEmpty()){
            log.error("MY_GITHUB_API_TOKEN 参数没有配置(githubApiToken)");
            return;
        }
        if (ownerRepo==null|| ownerRepo.trim().isEmpty()){
            log.error("OWNER_REPO 参数没有配置(github用户名/github仓库名)");
            return;
        }
        if (caiMoGuToken==null|| caiMoGuToken.trim().isEmpty()){
            log.error("CAI_MO_GU_TOKEN 参数没有配置(浏览器中f12 应用程序 Cookie 中的 cmg_token )");
            return;
        }*/
        log.error("配置设置正常");



        String gameIdsFileName="gameIds.txt";
        String acIdsFileName="acIds.txt";
        String postIdsFileName="postIds.txt";
        String runFileName="run.txt";


        Set<String> run = CaiMoGuHelp.readResources(runFileName);
        LocalDate current = LocalDate.now();;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Iterator<String> iterator = run.iterator();
        String dateStr = iterator.hasNext() ? iterator.next() : null;
        if (dateStr!=null){
            LocalDate date = LocalDate.parse(dateStr, formatter);
            if (current.isEqual(date)||current.isBefore(date)) {
                log.error("今日任务已经执行,若要重复执行请删run.txt");
                return;
            }
        }

        Set<String> ids = CaiMoGuHelp.readResources(gameIdsFileName);
        if(ids.isEmpty()){
            ids = CaiMoGuHelp.ScanGameIds();
            String idsStr = String.join("\n", ids);
           // GithubHelp.createOrUpdateFile(idsStr,gameIdsFileName,ownerRepo,githubApiToken);
        }

        Set<String> acIds = CaiMoGuHelp.readResources(acIdsFileName);
        Map<Integer, Set<String>> replyGroup =new HashMap<>();
        if(acIds.isEmpty()){
            //文件不存在时主动查寻回复中所有已经回复过的GameId
             replyGroup = CaiMoGuHelp.getReplyGroup(caiMoGuToken);
            Set<String> acIdSource = replyGroup.get(2);
            acIds=acIdSource==null?acIds:acIdSource;
            String idsStr = String.join("\n", acIds);
          //  GithubHelp.createOrUpdateFile(idsStr,acIdsFileName,ownerRepo,githubApiToken);
        }

        //去掉交集
        if (!acIds.isEmpty()) {
            ids.removeAll(acIds);
        }
        if (ids.isEmpty()) {
            //无可用id时重新扫描
            ids = CaiMoGuHelp.ScanGameIds();
        }
        if (!acIds.isEmpty()) {
            ids.removeAll(acIds);
        }
        if (ids.isEmpty()) {
            log.error("无可供评分的游戏");
            return;
        }

        int trueFlag = 0;
       for (String id : ids) {
           int s = CaiMoGuHelp.actSore(id, caiMoGuToken);
           if (s==1){
                trueFlag++;
                acIds.add(id);
                log.error("评价成功 "+id);
            }else if (s==0){

               acIds.add(id);
               log.error("重复评价 "+id);
            }
            if (trueFlag == 3) {
                break;
            }

        }
        log.error("成功评价游戏数量:{}",trueFlag);
        String acIdsStr = String.join("\n", acIds);
      //  GithubHelp.createOrUpdateFile(acIdsStr,acIdsFileName,ownerRepo,githubApiToken);

        //这里开始回复帖子
        Set<String> postIds = CaiMoGuHelp.readResources(postIdsFileName);
        if(postIds.isEmpty()){
            //文件不存在时主动查寻回复中所有已经回复过的GameId
            if (replyGroup.isEmpty()){
                replyGroup = CaiMoGuHelp.getReplyGroup(caiMoGuToken);
            }
            Set<String> postIdS = replyGroup.get(1);
            postIds=postIdS==null?postIds:postIdS;
            String idsStr = String.join("\n", postIds);
          //  GithubHelp.createOrUpdateFile(idsStr,postIdsFileName,ownerRepo,githubApiToken);
        }

        List<String> qzIds = Arrays.asList("449", "329", "369", "383", "282", "466");
        int acPostNum = CaiMoGuHelp.exeAcPost(qzIds, postIds,caiMoGuToken);
        log.error("成功评论帖子数量:{}",acPostNum);
        int clout2 = CaiMoGuHelp.getClout(caiMoGuToken);
        log.error("本次任务共获取影响力:{}",clout2-clout);
       // GithubHelp.createOrUpdateFile(String.join("\n", postIds),postIdsFileName,ownerRepo,githubApiToken);
      //  GithubHelp.createOrUpdateFile(formatter.format(current),runFileName,ownerRepo,githubApiToken);
    }









}
