package com.github.shy526.caimogu;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.shy526.App;
import com.github.shy526.config.Config;
import com.github.shy526.factory.OkHttpClientFactory;
import com.github.shy526.vo.UserInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.Md5Crypt;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Slf4j
public class CaiMoGuHelp {
    public static Set<String> readResources(String fileName) {
        Set<String> ids = new HashSet<>();
        ClassLoader classLoader = App.class.getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            return ids;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream(fileName), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ids.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    /**
     * 获取参考 踩蘑菇中所有游戏Id
     *
     * @return
     */
    public static Set<String> ScanGameIds() {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();

        YearMonth current = YearMonth.now();
        ;

        YearMonth target = YearMonth.of(2021, 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        String urlFormat = "https://www.caimogu.cc/game/find.html?act=fetch&date=%s&sort=1&sort_desc=1&page=%s";
        Set<String> ids = new HashSet<>();
        while (!target.isAfter(current)) {
            String dateStr = formatter.format(target);
            parseGameId(urlFormat, dateStr, client, ids);
            target = target.plusMonths(1);
        }
        parseGameId(urlFormat, "2020%E5%89%8D", client, ids);
        return ids;
    }


    private static void parseGameId(String urlFormat, String dateStr, OkHttpClient client, Set<String> ids) {
        for (int page = 1; page < Integer.MAX_VALUE; page++) {
            Request request = buildCaimoguGameRequest(urlFormat, dateStr, page);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    continue;
                }
                JSONObject bodyJson = JSON.parseObject(response.body().string());
                JSONArray data = bodyJson.getJSONArray("data");
                if (data == null || data.isEmpty()) {
                    break;
                }
                for (Object item : data) {
                    JSONObject jsonObject = (JSONObject) item;
                    String id = jsonObject.getString("id");
                    String name = jsonObject.getString("name");
                    ids.add(id);
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static Request buildCaimoguGameRequest(String urlFormat, String dateStr, int page) {
        return new Request.Builder()
                .url(String.format(urlFormat, dateStr, page))
                .addHeader("Host", "www.caimogu.cc")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("referer", "https://www.caimogu.cc/game/find.html")
                .build();
    }


    /**
     * 踩蘑菇评分
     *
     * @param id           游戏Id
     * @param caiMoGuToken toKen
     * @return true 成功评分 没有评分
     */
    public static int actSore(String id, String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        FormBody formBody = new FormBody.Builder()
                .add("id", id.toString())
                .add("type", "2")
                .add("score", "10")
                .add("content", "神中神非常好玩")
                .build();
        Request request = new Request.Builder()
                .url("https://www.caimogu.cc/game/act/score") // 测试API，可替换为实际接口
                .post(formBody)
                .addHeader("Host", "www.caimogu.cc")
                .addHeader("Cookie", caiMoGuToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject jsonObject = JSON.parseObject(response.body().string());
                Integer status = jsonObject.getInteger("status");
                String info = jsonObject.getString("info");
                if ("请勿重复评分".equals(info)) {
                    return 0;
                }
                return status == 1 && info.isEmpty() ? 1 : 2;
            }
        } catch (Exception ignored) {

        }
        return 2;
    }

    /**
     * 获取踩蘑菇当前影响力
     *
     * @param caiMoGuToken
     * @return -1 影响力获取错误
     */
    public static int getClout(String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        Request request = new Request.Builder()
                .url("https://www.caimogu.cc/user/my.html")
                .addHeader("Host", "www.caimogu.cc")
                .addHeader("Cookie", caiMoGuToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String html = response.body().string();
                Document doc = Jsoup.parse(html);
                Elements select = doc.select("div.point-container > .number");
                if (select.size() != 1) {
                    return -1;
                }
                String str = select.first().html();
                return Integer.parseInt(str);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        return -1;
    }

    /**
     * 获取踩蘑菇当前影响力
     *
     * @param caiMoGuToken
     * @return -1 影响力获取错误
     */
    public static String getNickname(String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        Request request = new Request.Builder()
                .url("https://www.caimogu.cc/user/my.html")
                .addHeader("Host", "www.caimogu.cc")
                .addHeader("Cookie", caiMoGuToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String html = response.body().string();
                Document doc = Jsoup.parse(html);
                Elements select = doc.select("div.user-info  .nickname");
                if (select.size() != 1) {
                    return "";
                }
                return select.first().html();
            }
        } catch (Exception ignored) {

        }
        return "";
    }


    /**
     * 获取所有回复并归类
     *
     * @param caiMoGuToken
     * @return type=1 帖子Id type=2 游戏Id
     */
    public static Map<Integer, Set<String>> getReplyGroup(String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        int page = 1;
        String urlFormat = "https://www.caimogu.cc/user/act/my_list?act=reply&page=%s";
        Map<Integer, Set<String>> group = new HashMap<>();
        while (page != 0) {
            String url = String.format(urlFormat, page);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "www.caimogu.cc")
                    .addHeader("Cookie", caiMoGuToken)
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JSONObject reply = JSON.parseObject(response.body().string());
                    JSONArray jsonArray = reply.getJSONObject("data").getJSONArray("list");
                    if (jsonArray.isEmpty()) {
                        break;
                    }
                    for (Object item : jsonArray) {
                        JSONObject itemJson = (JSONObject) item;
                        Integer type = itemJson.getInteger("type");
                        String targetId = itemJson.getString("target_id");
                        Set<String> set = group.get(type);
                        if (set == null) {
                            set = new HashSet<>();
                            group.put(type, set);
                        }
                        set.add(targetId);
                    }
                }
            } catch (Exception ignored) {
            }
            page++;
        }
        return group;
    }


    /*
     * 获取 可以回复 并且 能获得 影响力的帖子
     * @param postId
     */
    public static int exeAcPost(List<String> qzIds, Set<String> acPostId, String caiMoGuToken) {
        String urlFormat = "https://www.caimogu.cc/circle/act/post_list?id=%s&kwType=post&kw=&type=all&topic=&tags=&page=%s";
        int ac = 0;
        for (String qzId : qzIds) {
            for (int page = 1; page <= 10; page++) {
                String url = String.format(urlFormat, qzId, page);
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Host", "www.caimogu.cc")
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build();

                OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        JSONObject reply = JSON.parseObject(response.body().string());
                        JSONArray jsonArray = reply.getJSONObject("data").getJSONArray("list");
                        for (Object item : jsonArray) {
                            JSONObject itemJson = (JSONObject) item;
                            String postId = itemJson.getString("id");
                            if (acPostId.contains(postId)) {
                                continue;
                            }
                            String uId = itemJson.getString("user_id");
                            String createTimeStr = itemJson.getString("create_time");
                            LocalDateTime date = LocalDateTime.parse(createTimeStr, formatter);
                            long between = ChronoUnit.DAYS.between(date, now);
                            Integer replyNumber = itemJson.getInteger("reply_number");
                            if (replyNumber <= 0 || between > 10) {
                                continue;
                            }
                            String comment = getPostComments(postId);
                            if (acPostComments(postId, uId, comment, caiMoGuToken)) {
                                log.error("评论{}成功", postId);
                                acPostId.add(postId);
                                ac++;
                            }
                            if (ac >= 3) {
                                return ac;
                            }
                        }
                        if (jsonArray.isEmpty()) {
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return ac;

    }

    private static boolean acPostComments(String postId, String uId, String msg, String caiMoGuToken) {
        OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
        String referer = "https://www.caimogu.cc/post/%s.html";
        FormBody formBody = new FormBody.Builder()
                .add("pid", postId)
                .add("ppid", "0")
                .add("cid", "0")
                .add("uid", uId)
                .add("msg", msg)
                .add("force", "0")
                .build();
        Request request = new Request.Builder()
                .url("https://www.caimogu.cc/post/act/comment") // 测试API，可替换为实际接口
                .post(formBody)
                .addHeader("Host", "www.caimogu.cc")
                .addHeader("referer", String.format(referer, postId))
                .addHeader("Cookie", caiMoGuToken)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                JSONObject jsonObject = JSON.parseObject(response.body().string());
                Integer status = jsonObject.getInteger("status");
                return status == 1;
            }
        } catch (Exception ignored) {

        }
        return false;
    }

    private static String getPostComments(String postId) {
        String urlFormat = "https://www.caimogu.cc/post/comments?id=%s&pid=0&order=default&page=%s";
        int page = 1;
        List<String> result = new ArrayList<>();
        while (page != 0) {
            String url = String.format(urlFormat, postId, page);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Host", "www.caimogu.cc")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                    .build();
            OkHttpClient client = OkHttpClientFactory.getInstance().getClient();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JSONObject reply = JSON.parseObject(response.body().string());
                    JSONArray jsonArray = reply.getJSONObject("data").getJSONArray("list");
                    for (Object item : jsonArray) {
                        JSONObject itemJson = (JSONObject) item;
                        Integer praiseNumber = itemJson.getInteger("praise_number");
                        Integer replyNumber = itemJson.getJSONArray("reply").size();
                        if (praiseNumber == 0 && replyNumber == 0) {
                            String content = itemJson.getString("content");
                            result.add(content);
                        }
                    }
                    if (result.size() > 10 || jsonArray.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
            page++;
        }
        if (result.isEmpty()) {
            result.add("让我看看是怎么个事");
        }
        Random random = new Random();
        int index = random.nextInt(result.size());
        return "<p>" + Jsoup.parse(result.get(index)).text() + "</p>";
    }




}
