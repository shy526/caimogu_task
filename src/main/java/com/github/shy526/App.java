package com.github.shy526;

import com.alibaba.fastjson2.util.StringUtils;
import com.github.shy526.caimogu.CaiMoGuHelp;
import com.github.shy526.github.GithubHelp;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

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
        String githubApiToken = System.getenv("MY_GITHUB_API_TOKEN");
        String ownerRepo = System.getenv("OWNER_REPO");
        String caiMoGuToken = System.getenv("CAI_MO_GU_TOKEN");
        if (githubApiToken==null|| githubApiToken.trim().isEmpty()){
            log.error("MY_GITHUB_API_TOKEN 参数没有配置(githubApiToken)");
            return;
        }
        if (ownerRepo==null|| ownerRepo.trim().isEmpty()){
            log.error("OWNER_REPO 参数没有配置(github用户名/github仓库名)");
            return;
        }
        if (caiMoGuToken==null|| caiMoGuToken.trim().isEmpty()){
            log.error("CAI_MO_GU_TOKEN 参数没有配置(浏览器中f12 应用程序 Cookie 中的 CAIMOGU )");
            return;
        }
        log.error(githubApiToken);
        log.error(ownerRepo);
        log.error(caiMoGuToken+"!!!");
        log.error("配置设置正常");

        String gameIdsFileName="gameIds.txt";
        String acIdsFileName="acIds.txt";
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
            GithubHelp.createOrUpdateFile(idsStr,gameIdsFileName,ownerRepo,githubApiToken);
        }
        Set<String> acIds = CaiMoGuHelp.readResources(acIdsFileName);
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
            if (CaiMoGuHelp.actSore(id,caiMoGuToken)){
                trueFlag++;
                acIds.add(id);
                 log.error("评价成功 "+id);
            }else{
                log.error("评价失败 "+id);
            }
            if (trueFlag == 3) {
                break;
            }

        }
        log.error("成功评价"+trueFlag);
        String acIdsStr = String.join("\n", acIds);
        GithubHelp.createOrUpdateFile(acIdsStr,acIdsFileName,ownerRepo,githubApiToken);
        GithubHelp.createOrUpdateFile(formatter.format(current),runFileName,ownerRepo,githubApiToken);
    }









}
