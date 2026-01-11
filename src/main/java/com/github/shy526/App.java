package com.github.shy526;


import com.github.shy526.caimogu.CaiMoGuH5Help;
import com.github.shy526.caimogu.CaiMoGuHelp;
import com.github.shy526.config.Config;
import com.github.shy526.github.GithubHelp;
import com.github.shy526.vo.GithubInfo;
import com.github.shy526.vo.UserInfo;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Hello world!
 */
@Slf4j
public class App {
    public static void main(String[] args) {
        log.error("启动踩蘑菇获取影响力任务");
        String githubApiToken = System.getenv("MY_GITHUB_API_TOKEN");
        String ownerRepo = System.getenv("OWNER_REPO");
        String userName = System.getenv("CMG_NAME");
        String password = System.getenv("CMG_PASSWORD");
        GithubInfo githubInfo = new GithubInfo();
        githubInfo.setGithubApiToken(githubApiToken);
        githubInfo.setOwnerRepo(ownerRepo);
        Config.INSTANCE.GithubInfo = githubInfo;
        if (ownerRepo == null || ownerRepo.trim().isEmpty()) {
            log.error("OWNER_REPO 未设置");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            log.error("CMG_PASSWORD 未设置");
            return;
        }
        if (userName == null || userName.trim().isEmpty()) {
            log.error("CMG_NAME 未设置");
            return;
        }

        if (githubApiToken == null || githubApiToken.trim().isEmpty()) {
            log.error("MY_GITHUB_API_TOKEN 未设置");
            return;
        }
        log.error("配置设置未缺失");

        CaiMoGuH5Help.loginH5(userName, password);
        UserInfo userInfo = Config.INSTANCE.userInfo;
        if (userInfo == null) {
            log.error("踩蘑菇 用户名/密码错误,或者踩蘑菇接口失效");
            return;
        }

        int point = CaiMoGuH5Help.getPoint();
        log.error("当前用户:{},影响力:{}", userInfo.getNickname(), userInfo.getPoint());


        String gameIdsFileName = "gameIds.txt";
        String acIdsFileName = "acIds.txt";
        String postIdsFileName = "postIds.txt";
        String runFileName = "run.txt";
        String gameCommentFileName = "gameComment.txt";

        //检查运行文件
        Set<String> run = CaiMoGuHelp.readResources(runFileName);
        LocalDate current = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Iterator<String> iterator = run.iterator();
        String dateStr = iterator.hasNext() ? iterator.next() : null;
        if (dateStr != null) {
            LocalDate date = LocalDate.parse(dateStr, formatter);
            if (current.isEqual(date) || current.isBefore(date)) {
                log.error("今日任务已经执行,若要重复执行请删run.txt");
                return;
            }
        }

        //检查游戏库Id是否存在
        Set<String> gameIds = CaiMoGuHelp.readResources(gameIdsFileName);
        if (gameIds.isEmpty()) {
            log.error("生成gameId");
            gameIds = CaiMoGuHelp.ScanGameIds();
            String idsStr = String.join("\n", gameIds);
            GithubHelp.createOrUpdateFile(idsStr, gameIdsFileName, ownerRepo, githubApiToken);
        }

        Map<String, Set<String>> replyGroup = new HashMap<>();
        Set<String> acGameIds = checkAcFileName(acIdsFileName, replyGroup, "2");

        //去掉交集
        if (!acGameIds.isEmpty()) {
            gameIds.removeAll(acGameIds);
        }
        // 如果已经评论完了 本地文件尝试扫描远程
        if (gameIds.isEmpty()) {
            //无可用id时重新扫描
            gameIds = CaiMoGuHelp.ScanGameIds();
        }
        if (!acGameIds.isEmpty()) {
            gameIds.removeAll(acGameIds);
        }
/*        if (!gameIds.isEmpty()) {
            int trueFlag = 0;
            for (String gamId : gameIds) {
                int code = CaiMoGuH5Help.acGameScore(gamId, "神中神非常好玩", "10", "1");
                if (code == 99999) {
                    acGameIds.add(gamId);
                    log.error("重复评价 " + gamId);
                } else if (code == 0) {
                    trueFlag++;
                    acGameIds.add(gamId);
                    log.error("评价成功 " + gamId);
                } else {
                    log.error("无法正常评论游戏");
                    break;
                }
                if (trueFlag>=3){
                    break;
                }
            }
            log.error("成功评价游戏数量:{}", trueFlag);
        }*/

        String acGameIdsStr = String.join("\n", acGameIds);
        GithubHelp.createOrUpdateFile(acGameIdsStr, acIdsFileName, ownerRepo, githubApiToken);

        Set<String> postIds = checkAcFileName(postIdsFileName, replyGroup, "1");
        int acPostNum = CaiMoGuH5Help.getRuleDetail(postIds);
        GithubHelp.createOrUpdateFile(String.join("\n", postIds), postIdsFileName, ownerRepo, githubApiToken);
        log.error("成功评论帖子数量:{}", acPostNum);


/*        Set<String> gameCommentIds = checkAcFileName(gameCommentFileName, replyGroup, "3");
        for (String gameId : gameIds) {
            if (gameCommentIds.contains(gameId)) {
                continue;
            }
            gameCommentIds.add(gameId);
            int i = CaiMoGuH5Help.acGameCommentReply(gameId, "说的全对,确实很好玩");
            if (i==0){
                log.error("成功恢复游戏库评论:{}", gameId);
                break;
            }
        }
        GithubHelp.createOrUpdateFile(String.join("\n", gameCommentIds), gameCommentFileName, ownerRepo, githubApiToken);
*/

        log.error("本次任务共获取影响力:{}", CaiMoGuH5Help.getPoint() - point);
        HashSet<String> temp = new HashSet<>();
        temp.add(formatter.format(current));
        GithubHelp.createOrUpdateFile(String.join("\n", temp), runFileName, ownerRepo, githubApiToken);
    }

    private static Set<String> checkAcFileName(String fileName, Map<String, Set<String>> replyGroup, String type) {
        GithubInfo githubInfo = Config.INSTANCE.GithubInfo;
        Set<String> checkIds = CaiMoGuHelp.readResources(fileName);
        if (checkIds.isEmpty()) {
            if (replyGroup.isEmpty()) {
                replyGroup = CaiMoGuH5Help.getReplyGroup();
            }
            checkIds = replyGroup.get(type);
            if (checkIds==null||checkIds.isEmpty()) {
                return new HashSet<>();
            }
            log.error("{}数据同步",fileName);
            GithubHelp.createOrUpdateFile(String.join("\n", checkIds), fileName, githubInfo.getOwnerRepo(), githubInfo.getGithubApiToken());
        }
        return checkIds;
    }


}
