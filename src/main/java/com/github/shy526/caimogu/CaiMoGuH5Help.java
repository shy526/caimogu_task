package com.github.shy526.caimogu;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.shy526.config.Config;
import com.github.shy526.factory.OkHttpClientFactory;
import com.github.shy526.vo.PageModel;
import com.github.shy526.vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;

@Slf4j
public class CaiMoGuH5Help {


    private final static Map<String, Integer> ReNumMap = new HashMap<>();

    public static boolean AcGameComment(String gameId) {

        int page = 1;
        boolean hasMore = true;
        do {
            PageModel pageModel = getGameCommentPage(gameId, "newly", "all", String.valueOf(page));
            hasMore = pageModel.getHasMore();
            page++;
            if (pageModel.getData() == null || pageModel.getData().isEmpty()) {
                break;
            }
            for (JSONObject item : pageModel.getData()) {
                String userId = item.getString("userId");
                if (userId.equals(Config.INSTANCE.userInfo.getUid())) {
                    continue;
                }
                String id = item.getString("id");
                int i = acGameCommentReply(id, "全对,神中神");
                return i == 0;
            }

        } while (hasMore);
        return false;

    }

    public static Set<String> scanGameIds() {
        int page = 1;
        boolean hasMore = false;
        Set<String> gameIds = new HashSet<>();
        do {
            PageModel pageModel = getGamePage("newly", String.valueOf(page));
            page++;
            if (pageModel.getData() == null || pageModel.getData().isEmpty()) {
                break;
            }
            for (JSONObject item : pageModel.getData()) {
                String id = item.getString("id");
                gameIds.add(id);
            }
            hasMore = pageModel.getHasMore();
        } while (hasMore);
        return gameIds;
    }


    /**
     * @param gameId id
     * @param order  hot newly
     * @param page
     * @param type   all  played clear
     * @return
     */
    public static PageModel getGameCommentPage(String gameId, String order, String type, String page) {

        String url = "https://api.caimogu.cc/v2/game/commentList";
        Map<String, String> params = new HashMap<>();
        params.put("order", order); //newly 最先 hot choice ""
        params.put("page", page);
        params.put("pid", "0");
        params.put("type", type);
        params.put("gameId", gameId);
        voEncrypt(params);
        url = buildBaseParams(url, params, "order", "page", "pid", "type", "gameId");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    PageModel pageModel = new PageModel();
                    pageModel.setHasMore(data.getBoolean("more"));
                    pageModel.setData(data.getList("list", JSONObject.class));
                    pageModel.setNextKey(data.getString("nextKey"));
                    return pageModel;
                } else if (code == 10002) {
                    return retry(url, "getGameCommentPage", () -> getGameCommentPage(gameId, order, type, page));
                }

            }
        } catch (Exception ignored) {

        }
        return new PageModel();
    }

    /**
     * @param msgId   游戏Id
     * @param content 评论内容
     * @return 99999 已经评论过了
     */
    public static int acGameCommentReply(String msgId, String content) {
        if (Config.INSTANCE.userInfo == null) {
            return -1;
        }
        String url = "https://api.caimogu.cc/v1/game/commentReply";
        Map<String, String> params = new HashMap<>();
        params.put("msgId", msgId);
        params.put("content", content);
        voEncrypt(params);
        url = buildBaseParams(url, params, "msgId", "content");
        JSONObject json = new JSONObject();
        String timeStr = params.get("time");
        json.put("msgId", vrEncrypt(params.get("msgId"), timeStr));
        json.put("content", vrEncrypt(params.get("content"), timeStr));
        String jsonStr = json.toJSONString();
        RequestBody requestBody = RequestBody.create(jsonStr, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("authorization", Config.INSTANCE.userInfo.getASuthorization())
                .post(requestBody)
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 10002) {

                    return retry(url, "acGameCommentReply", () -> acGameCommentReply(msgId, content));

                }

                return code;
            }
        } catch (Exception ignored) {

        }
        return -1;
    }


    private static <T> T retry(String url, String key, Supplier<T> supplier) {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        Integer reNum = ReNumMap.getOrDefault(key, 0);
        reNum++;
        ReNumMap.put(key, reNum);
        if (reNum <= 3) {
            log.error("签名错误 {}=>{} {}", key, reNum, url);
            T temp = supplier.get();
            ReNumMap.remove(key);
            return temp;
        } else {
            ReNumMap.remove(key);
        }
        return null;
    }


    /**
     * @param gameId  游戏Id
     * @param content 评论内容
     * @param score   2 4 6 8 10
     * @param process 1 玩过  2 通关
     * @return 99999 已经评论过了
     */
    public static int acGameScore(String gameId, String content, String score, String process) {
        if (Config.INSTANCE.userInfo == null) {
            return -1;
        }
        String url = "https://api.caimogu.cc/v2/game/score";
        Map<String, String> params = new HashMap<>();
        params.put("id", gameId);
        params.put("score", score);
        params.put("content", content);
        params.put("process", process);
        voEncrypt(params);
        url = buildBaseParams(url, params);
        JSONObject json = new JSONObject();
        String timeStr = params.get("time");
        json.put("id", vrEncrypt(params.get("id"), timeStr));
        json.put("content", vrEncrypt(params.get("content"), timeStr));
        json.put("score", vrEncrypt(params.get("score"), timeStr));
        json.put("process", vrEncrypt(params.get("process"), timeStr));
        String jsonStr = json.toJSONString();
        RequestBody requestBody = RequestBody.create(jsonStr, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("authorization", Config.INSTANCE.userInfo.getASuthorization())
                .post(requestBody)
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 10002) {
                    return retry(url, "acGameScore", () -> acGameScore(gameId, content, score, process));
                }
                return code;


            }
        } catch (Exception ignored) {

        }
        return -1;
    }

    public static int getRuleDetail(Set<String> detailIds) {
        List<String> circleIds = Arrays.asList("449", "329", "369", "383", "282", "466");
        LocalDateTime now = LocalDateTime.now();
        int acCommentNum = 0;
        int acCommentMax = Config.INSTANCE.userInfo.getMaxComment();
        for (String circleId : circleIds) {

            int page = 1;
            int pageSize = 0;
            int pageMax = 50;
            do {
                List<JSONObject> newly = getDetailPage(circleId, "newly", String.valueOf(page));
                page++;
                pageSize = newly.size();
                for (JSONObject detail : newly) {
                    String detaildId = detail.getString("id");
                    LocalDateTime createTime = detail.getLocalDateTime("createTime");
                    long between = ChronoUnit.DAYS.between(createTime, now);
                    int replyNumber = detail.getIntValue("replyNumber");
                    if (detailIds.contains(detaildId) || between > 20 || replyNumber <= 10) {
                        continue;
                    }
                    String acComment = findRuleComment(detaildId);
                    if (acComment == null) {
                        continue;
                    }
                    if (acComment(detaildId, acComment)) {
                        log.error("评论成功:{}-{}", circleId, detaildId);
                        detailIds.add(detaildId);
                        acCommentNum++;
                    } else {
                        log.error("评论失败:{}-{}", circleId, detaildId);
                    }
                    if (acCommentNum >= acCommentMax) {
                        return acCommentNum;
                    }
                }
            } while (pageSize > 0 && page < pageMax);
        }
        return acCommentNum;
    }

    private static String findRuleComment(String detaildId) {
        int page = 1;
        int pageSize = 0;
        List<String> commentStrList = new ArrayList<>();
        do {
            PageModel pageModel = getCommentPage(detaildId, "newly", String.valueOf(page));
            pageSize = pageModel.getData().size();
            page++;
            for (JSONObject comment : pageModel.getData()) {
                int praiseNumber = comment.getIntValue("praiseNumber");
                int replyCount = comment.getIntValue("replyCount");
                if (praiseNumber > 0 || replyCount > 0) {
                    continue;
                }
                commentStrList.add(comment.getString("content"));
            }
        } while (pageSize > 0 && commentStrList.isEmpty());
        if (commentStrList.isEmpty()) {
            return null;
        }
        int randomIndex = (int) (Math.random() * commentStrList.size());
        return commentStrList.get(randomIndex);

    }

    /**
     * @param detailId id
     * @param order    newly 最新  default 默认
     * @param page
     * @return
     */
    public static PageModel getCommentPage(String detailId, String order, String page) {

        String url = "https://api.caimogu.cc/v3/post/comment/list";
        Map<String, String> params = new HashMap<>();
        params.put("order", order); //newly 最先 hot choice ""
        params.put("page", page);
        params.put("id", detailId);
        voEncrypt(params);
        url = buildBaseParams(url, params, "order", "page", "id");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    PageModel pageModel = new PageModel();
                    pageModel.setHasMore(data.getIntValue("hasMore") != 0);
                    pageModel.setData(data.getList("list", JSONObject.class));
                    pageModel.setNextKey(data.getString("nextKey"));
                    return pageModel;
                } else if (code == 10002) {
                    return retry(url, "getCommentPage", () -> getCommentPage(detailId, order, page));

                }

            }
        } catch (Exception ignored) {

        }
        return new PageModel();
    }

    /**
     * @param sort // all 全部 newly 最新 score 最高评分  want_play 最想玩
     * @param page
     * @return
     */
    public static PageModel getGamePage(String sort, String page) {
        String url = "https://api.caimogu.cc/v2/game/list";
        Map<String, String> params = new HashMap<>();
        params.put("sort", sort);
        params.put("platform", "all"); //newly 最先 hot choice ""
        params.put("page", page);
        params.put("type", "all");
        voEncrypt(params);
        url = buildBaseParams(url, params, "sort", "platform", "page", "type");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    PageModel pageModel = new PageModel();
                    JSONObject data = result.getJSONObject("data");
                    pageModel.setHasMore(data.getBoolean("more"));
                    pageModel.setData(data.getList("list", JSONObject.class));
                    return pageModel;
                } else if (code == 10002) {
                    return retry(url, "getGamePage", () -> getGamePage(sort, page));

                }

            }
        } catch (Exception ignored) {
            log.error("游戏库获取错误");
        }
        return new PageModel();
    }

    /**
     * 获取莫个圈子的帖子
     *
     * @param id   圈子Id
     * @param type 查寻帖子类型 newly 最新  hot 热帖  choice 精华  "" 全部
     * @param page 页码
     */
    public static List<JSONObject> getDetailPage(String id, String type, String page) {
        String url = "https://api.caimogu.cc/v3/circle/detail/list";
        Map<String, String> params = new HashMap<>();
        params.put("topic", "0");
        params.put("type", type); //newly 最先 hot choice ""
        params.put("page", page);
        params.put("id", id);
        voEncrypt(params);
        url = buildBaseParams(url, params, "topic", "type", "page", "id");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                Integer code = result.getInteger("code");
                if (code == 0) {
                    return result.getList("data", JSONObject.class);
                } else if (code == 10002) {
                    return retry(url, "getDetailPage", () -> getDetailPage(id, type, page));

                }

            }
        } catch (Exception ignored) {

        }
        return new ArrayList<>();
    }

    /**
     * 获取所有已回复的帖子和游戏Id
     *
     * @return type=1 帖子Id type=2 游戏Id 3 评论过游戏库中的评论
     */
    public static Map<String, Set<String>> getReplyGroup(LocalDate now) {
        String nextKey = "";
        Map<String, Set<String>> group = new HashMap<>();
        Set<String> df = new HashSet<>();
        do {
            PageModel pageModel = getReplyList(nextKey);
            if (pageModel.getData().isEmpty()) {
                break;
            }
            List<JSONObject> data = pageModel.getData();
            for (JSONObject item : data) {
                String type = item.getString("type");
                String parentId = item.getString("parentId");
                LocalDate createTime = item.getLocalDateTime("createTime").toLocalDate();
                if (now != null && !now.equals(createTime)) {
                    return group;
                }

                if (now != null) {
                    Set<String> post = group.getOrDefault("1", df);
                    Set<String> game = group.getOrDefault("2", df);
                    Set<String> gamePost = group.getOrDefault("3", df);
                    if (post.size() >= 3 && game.size() >= 3 && !gamePost.isEmpty()) {
                        return group;
                    }
                }

                if (!"0".equals(parentId)) {
                    type = "3";
                }
                String targetId = item.getString("targetId");
                if (type == null || targetId == null) {
                    continue;
                }
                Set<String> set = group.get(type);
                if (set == null) {
                    set = new HashSet<>();
                    group.put(type, set);
                }
                set.add(targetId);
            }

            nextKey = pageModel.getHasMore() ? pageModel.getNextKey() : null;
        } while (nextKey != null && !nextKey.isEmpty());
        return group;
    }


    /*

     *直接评价
     */
    public static boolean acComment(String pId, String content) {
        if (Config.INSTANCE.userInfo == null) {
            return false;
        }
        String url = "https://api.caimogu.cc/v3/post/reply";
        Map<String, String> params = new HashMap<>();
        params.put("cid", "0");
        params.put("content", content);
        params.put("images", "");
        params.put("pid", pId);
        params.put("topic", "");
        voEncrypt(params);
        url = buildBaseParams(url, params);
        JSONObject json = new JSONObject();
        String timeStr = params.get("time");
        json.put("cid", vrEncrypt(params.get("cid"), timeStr));
        json.put("content", vrEncrypt(params.get("content"), timeStr));
        json.put("images", vrEncrypt(params.get("images"), timeStr));
        json.put("pid", vrEncrypt(params.get("pid"), timeStr));
        json.put("topic", vrEncrypt(params.get("topic"), timeStr));
        String jsonStr = json.toJSONString();
        RequestBody requestBody = RequestBody.create(jsonStr, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("authorization", Config.INSTANCE.userInfo.getASuthorization())
                .post(requestBody)
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    return true;
                } else if (code == 10002) {
                    return retry(url, "acComment", () -> acComment(pId, content));

                }
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    /*

     *
     */
    public static PageModel getReplyList(String nextKey) {
        if (Config.INSTANCE.userInfo == null) {
            return new PageModel();
        }
        String url = "https://api.caimogu.cc/v3/my/reply/list";
        Map<String, String> params = new HashMap<>();
        params.put("nextKey", nextKey);
        voEncrypt(params);
        url = buildBaseParams(url, params, "nextKey");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("authorization", Config.INSTANCE.userInfo.getASuthorization())
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    PageModel pageModel = new PageModel();
                    pageModel.setHasMore(data.getIntValue("hasMore") != 0);
                    pageModel.setData(data.getList("list", JSONObject.class));
                    pageModel.setNextKey(data.getString("nextKey"));
                    return pageModel;
                } else if (code == 10002) {
                    return retry(url, "getReplyList", () -> getReplyList(nextKey));

                }
            }

        } catch (Exception ignored) {

        }
        return new PageModel();
    }

    /**
     * 获取影响力和刷新UserInfo积分
     *
     * @return
     */
    public static int getPoint() {
        if (Config.INSTANCE.userInfo == null) {
            return -1;
        }
        String url = "https://api.caimogu.cc/v3/my/info";
        Map<String, String> params = new HashMap<>();
        voEncrypt(params);
        url = buildBaseParams(url, params);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("authorization", Config.INSTANCE.userInfo.getASuthorization())
                .build();
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    Integer point = data.getInteger("point");
                    Config.INSTANCE.userInfo.setPoint(point);
                    return point;
                } else if (code == 10002) {
                    return retry(url, "getPoint", () -> getPoint());

                }

            }
        } catch (Exception ignored) {

        }
        return -1;
    }

    /**
     * 登录
     *
     * @param username
     * @param password
     */
    public static void loginH5(String username, String password) {

        String url = "https://api.caimogu.cc/v3/login/account";
        String passwordEncrypt = DigestUtils.md5Hex(password);
        Map<String, String> params = new HashMap<>();
        params.put("password", passwordEncrypt);
        params.put("account", username);
        voEncrypt(params);

        params.remove("password");
        params.remove("account");

        url = buildBaseParams(url, params);
        JSONObject json = new JSONObject();
        String timeStr = params.get("time");
        json.put("account", vrEncrypt(username, timeStr));
        json.put("password", vrEncrypt(passwordEncrypt, timeStr));
        String jsonStr = json.toJSONString();
        RequestBody requestBody = RequestBody.create(jsonStr, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Host", "api.caimogu.cc")
                .addHeader("referer", "https://h5.caimogu.cc/")
                .addHeader("origin", "https://h5.caimogu.cc")
                .post(requestBody)
                .build();

        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject result = JSON.parseObject(response.body().string());
                int code = result.getIntValue("code");
                if (code == 0) {
                    JSONObject data = result.getJSONObject("data");
                    UserInfo userInfo = new UserInfo();
                    userInfo.setUid(data.getString("uid"));
                    userInfo.setASuthorization("Bearer " + data.getString("token"));
                    userInfo.setNickname(data.getString("nickname"));
                    Config.INSTANCE.userInfo = userInfo;
                } else if (code == 10002) {
                    retry(url, "loginH5", () -> {
                        loginH5(username, password);
                        return 1;
                    });

                }
            } else {
                log.error("登录失败");
            }
        } catch (Exception ignored) {
            log.error("登录失败");
        }
    }

    private static String buildBaseParams(String url, Map<String, String> params, String... selectKeyEx) {
        List<Object> selectKey = new ArrayList<>();
        selectKey.add("device");
        selectKey.add("ver");
        selectKey.add("time");
        selectKey.add("sign");
        if (selectKeyEx != null && selectKeyEx.length > 0) {
            selectKey.addAll(Arrays.asList(selectKeyEx));
        }
        StringBuilder urlBuild = new StringBuilder(url);
        String timeStr = params.get("time");
        boolean flag = false;
        for (String key : params.keySet()) {
            if (!flag) {
                urlBuild.append("?");
                flag = true;
            } else {
                urlBuild.append("&");
            }
            if (selectKey.contains(key)) {
                String temp = params.get(key);
                if (!key.equals("time") && !key.equals("sign")) {

                    temp = vrEncrypt(params.get(key), timeStr);
                }
                urlBuild.append(key).append("=").append(temp);
            }
        }
        return urlBuild.toString();
    }

    public static void voEncrypt(Map<String, String> paramMap) {

        if (!paramMap.containsKey("time")) {
            paramMap.put("time", (System.currentTimeMillis() + "").substring(0, 10));
        }

        paramMap.put("device", "h5");
        paramMap.put("ver", "1.0.0");
        Set<String> keySet = paramMap.keySet();
        List<String> validKeys = new ArrayList<>();
        for (String key : keySet) {
            if ("upimg".equals(key)) {
                continue;
            }
            String value = paramMap.get(key);
            if (value == null || "".equals(value.trim())) {
                continue;
            }
            validKeys.add(key);
        }
        Collections.sort(validKeys);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < validKeys.size(); i++) {
            String key = validKeys.get(i);
            String value = paramMap.get(key);
            sb.append(key).append("=").append(value);
            if (i < validKeys.size() - 1) {
                sb.append("&");
            }
        }

        String signSource = sb.toString().toLowerCase();
        String sign = DigestUtils.md5Hex(signSource);
        paramMap.put("sign", sign);
    }

    public static String vrEncrypt(String val, String time) {
        try {
            String keyStr = time + time.substring(0, 6);
            String md5E = DigestUtils.md5Hex(time).toLowerCase();
            String ivStr = md5E.substring(0, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(keyStr.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encryptBytes = cipher.doFinal(val.getBytes(StandardCharsets.UTF_8));
            String aesBase64 = Base64.encodeBase64String(encryptBytes);
            if (aesBase64.startsWith("+")) {
                aesBase64 = aesBase64.replace("+", "%2B");
            }
            return URLEncoder.encode(aesBase64, StandardCharsets.UTF_8.name());

        } catch (Exception ex) {
            log.error(ex.getMessage());
            return null;
        }
    }
}
