package com.github.shy526;


import com.github.shy526.caimogu.CaiMoGuH5Help;
import com.github.shy526.caimogu.CaiMoGuHelp;
import com.github.shy526.config.Config;
import com.github.shy526.github.GithubHelp;
import com.github.shy526.vo.GithubInfo;
import com.github.shy526.vo.UserInfo;
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


        String suffix = ".txt";
        String gameIdsFileName = "gameIds.txt";
        String acIdsFileName = "acIds-" + userInfo.getUid() + suffix;
        String postIdsFileName = "postIds-" + userInfo.getUid() + suffix;
        String gameCommentFileName = "gameComment-" + userInfo.getUid() + suffix;


        deleteGithubFile(gameIdsFileName);

        LocalDate current = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Set<String> df = new HashSet<>();
        //type=1 帖子Id type=2 游戏Id 3 评论过游戏库中的评论
        Map<String, Set<String>> nowReplyGroup = CaiMoGuH5Help.getReplyGroup(current);
        userInfo.setMaxGame(userInfo.getMaxGame() - nowReplyGroup.getOrDefault("2", df).size());
        userInfo.setMaxGameComment(userInfo.getMaxGameComment() - nowReplyGroup.getOrDefault("3", df).size());
        userInfo.setMaxComment(userInfo.getMaxComment() - nowReplyGroup.getOrDefault("2", df).size());
        log.error("{} 影响力获取渠道 剩余数量 帖子子回复数:{} 游戏评论回复:{} 游戏库评论:{}", current.format(formatter), userInfo.getMaxComment(), userInfo.getMaxGameComment(), userInfo.getMaxGame());
        if ((userInfo.getMaxComment()+userInfo.getMaxGame()+userInfo.getMaxGameComment()) <= 0) {
            log.error("{} 无可用渠道获取影响力", current.format(formatter));
            return;
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
        if (!gameIds.isEmpty()) {
            int trueFlag = 0;
            for (String gamId : gameIds) {
                if (trueFlag >= userInfo.getMaxGame()) {
                    break;
                }
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

            }
            log.error("成功评价游戏数量:{}", trueFlag);
        }

        String acGameIdsStr = String.join("\n", acGameIds);
        GithubHelp.createOrUpdateFile(acGameIdsStr, acIdsFileName, ownerRepo, githubApiToken);

        Set<String> postIds = checkAcFileName(postIdsFileName, replyGroup, "1");
        int acPostNum = CaiMoGuH5Help.getRuleDetail(postIds);
        GithubHelp.createOrUpdateFile(String.join("\n", postIds), postIdsFileName, ownerRepo, githubApiToken);
        log.error("成功评论帖子:{}", acPostNum);


        Set<String> gameCommentIds = checkAcFileName(gameCommentFileName, replyGroup, "3");

        int gameCommentNum = 0;
        for (String gameId : gameIds) {
            if (gameCommentIds.contains(gameId)) {
                continue;
            }
            if (gameCommentNum >= userInfo.getMaxGameComment()) {
                break;
            }
            gameCommentIds.add(gameId);
            int i = CaiMoGuH5Help.acGameCommentReply(gameId, "说的全对,确实很好玩");
            if (i == 0) {
                log.error("成功评论游戏库评论:{}", gameId);
                gameCommentNum++;
            }

        }
        GithubHelp.createOrUpdateFile(String.join("\n", gameCommentIds), gameCommentFileName, ownerRepo, githubApiToken);


        log.error("本次任务共获取影响力:{}", CaiMoGuH5Help.getPoint() - point);
    }

    /**
     * 清理非本用户的文件
     * @param gameIdsFileName
     */
    private static void deleteGithubFile(String gameIdsFileName) {
        GithubInfo githubInfo = Config.INSTANCE.GithubInfo;
        UserInfo userInfo = Config.INSTANCE.userInfo;
        String uid = userInfo.getUid();
        String ownerRepo = githubInfo.getOwnerRepo();
        String githubApiToken = githubInfo.getGithubApiToken();
        List<String> files = GithubHelp.getListFileName(ownerRepo, githubApiToken, "src/main/resources");
        for (String item : files) {
            if (!item.endsWith(".txt") || item.equals(gameIdsFileName)) {
                continue;
            }
            String[] split = item.split("-");
            if (split.length < 2) {
                GithubHelp.deleteFile(ownerRepo, githubApiToken, item);
                log.error("delete  {}", item);
                continue;
            }
            if (split[1].equals(uid)) {
                GithubHelp.deleteFile(ownerRepo, githubApiToken, item);
                log.error("delete  {}", item);
            }
        }
    }

    private static Set<String> checkAcFileName(String fileName, Map<String, Set<String>> replyGroup, String type) {
        GithubInfo githubInfo = Config.INSTANCE.GithubInfo;
        Set<String> checkIds = CaiMoGuHelp.readResources(fileName);
        if (checkIds.isEmpty()) {
            if (replyGroup.isEmpty()) {
                replyGroup = CaiMoGuH5Help.getReplyGroup(null);
            }
            checkIds = replyGroup.get(type);
            if (checkIds == null || checkIds.isEmpty()) {
                return new HashSet<>();
            }
            log.error("{}数据同步", fileName);
            GithubHelp.createOrUpdateFile(String.join("\n", checkIds), fileName, githubInfo.getOwnerRepo(), githubInfo.getGithubApiToken());
        }
        return checkIds;
    }


}
