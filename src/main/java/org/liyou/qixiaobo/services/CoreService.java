package org.liyou.qixiaobo.services;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.liyou.qixiaobo.controllers.CardController;
import org.liyou.qixiaobo.daos.AuntiesDao;
import org.liyou.qixiaobo.daos.StageDao;
import org.liyou.qixiaobo.daos.UserDao;
import org.liyou.qixiaobo.entities.hibernate.*;
import org.liyou.qixiaobo.entities.weichat.request.*;
import org.liyou.qixiaobo.entities.weichat.response.Article;
import org.liyou.qixiaobo.entities.weichat.response.BaseResponseMessage;
import org.liyou.qixiaobo.entities.weichat.response.NewsResponseMessage;
import org.liyou.qixiaobo.entities.weichat.response.TextResponseMessage;
import org.liyou.qixiaobo.utils.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.liyou.qixiaobo.utils.MessageUtil.*;

/**
 * Created by Administrator on 14-3-1.
 */
@Component
public class CoreService {
    /**
     * 处理微信发来的请求
     *
     * @param request
     * @return
     */

    private static Article sArticle;
    private static Logger logger = Logger.getLogger(CoreService.class);
    @Resource
    private DotaService dotaService;
    @Resource
    private StageDao stageDao;
    @Resource
    private UserDao userDao;
    @Resource
    private AuntiesDao auntiesDao;
    private final Random random = new Random();
    private static final int isAuthed = 1;
    private Calendar calendar = Calendar.getInstance();
    private final String WEATHER_INDEX = "http://m.weather.com.cn/data/";
    private final String FORTURE_URL = "http://apix.sinaapp.com/fortune/?appkey=trailuser&name=%name%";
    private final String LIYOU_FORTURE = "得分：89分\\n 天格：大吉 人格：大吉 地格：大吉 外格：大吉 总格：" +
            "大吉\\n概述：李尤的姓名三才配置为:金木土（吉） 它具有如下数理诱导力，据此会对人生产生一定的影响。" +
            " 过于焦虑导致成功运被压抑,不能成功,易生身心过劳等疾病,惟境遇尚可得安定。留意身边年龄较小异性，可得佳缘。" +
            "\\n总论：这是一种较平凡的配置,若努力向上且忍耐力强,可得正比率的发展,但大成功的机会不多,易生身心过劳,是辛勤" +
            "得财类型。天运五行属水之人,事业顺利,财源甚丰。\\n性格：为人仁慈,喜欢助他人,有进取心,对家庭有责任感,一生辛勤奋发" +
            ",但处事较主观固执,容易陷入一意孤行,这一点应改进。\\n意志：意志过于坚定,计划不够周到,处事也有冲动之倾向。\\n事业：" +
            "属于一种稳定性收入的行业,如薪水阶级、农牧业,成本不大的生意均很适合,也能顺利。\\n家庭：男命者家庭大致圆满,夫妻共" +
            "同创业。女命者家庭多和谐、美满。\\n婚姻：男娶努力节俭之妻,婚后大致和顺;女嫁朴实善良之夫,婚后平淡是福。\\n子女：" +
            "女孩较多,有责任感又能孝顺。但身体较为虚弱。\\n社交：性格较直率,有话直话,少言而多行,外缘及社交能力均不错。\\n精神：" +
            "表面似乎无事,为家庭及工作多操劳,精神不安定。\\n财运：劳碌之财,如能节俭,积少成多,仍可小富。\\n健康：易患胃肠" +
            "、神经衰弱、呼吸器官、头痛等症。\\n老运：一生劳碌,老景尚佳,但仍闲不住,继续辛勤不休。";
    private final String QIXIAOBO_FORTURE = "\"得分：86分  \\n天格：吉 人格：大吉 地格：大吉 外格：凶 总格：大吉\\n概述：" +
            "祁晓波的姓名三才配置为:水火土（凶） 它具有如下数理诱导力，据此会对人生产生一定的影响。 成功运被压抑,不能伸张,多" +
            "有不平不满。\\n总论：辛勤劳苦而收获少,心情常在苦闷中,做事冲动而积极,成功失败常在一瞬间,如能涵养忍耐力,中年后可成功" +
            "发展,慎防意外灾病。天运五行属木者,事业能成功发展。\\n性格：表面乐观豪爽,喜出风头爱面子,乐于解决别人的困难,不耐独处" +
            ",容易和异性接近,内心烦闷不安,一心想自当老板,容易造成不平不满的人生过程。\\n意志：意志坚定,而独断独行,但耐心不足,做事" +
            "有虎头蛇尾之倾向,常有误打误撞而获财利者。\\n事业：适合投机性或爆发性的行业,常有出人意料的绝招,得到名利和地位。\\n家庭" +
            "：男命者家庭大致圆满,应注意桃花的后果,女命者则夫妻多争执,家庭难和睦。\\n婚姻：男娶温厚贤惠之妻,婚后夫妻大致和睦;女嫁好胜" +
            "好强之夫,婚后家庭不美满。\\n子女：子女谦恭又有责任感,长大后在社会上能成功发展。\\n社交：外表乐观又喜出风头,做事急躁,在" +
            "社会上能得朋友之助,社交能力佳。\\n精神：表面坚忍,一生辛苦劳碌,心情不开朗。\\n财运：早年劳苦,中年后得财,财运尚佳,天运" +
            "五行属木者大成功,属水者常受失败打击。\\n健康：易患头痛、脑疾、呼吸系统、高血压、神经痛等。\\n老运：晚景转佳运,但仍性急" +
            "固执,心神忧烦,天运五行属木火,晚景安然无忧。\"";
    private final String DELETE_FORTURE_STRING = "（评分由数理文化得出，仅供娱乐参考）";
    private final String JOKE_URL = "http://apix.sinaapp.com/joke/?appkey=trialuser";
    private final String DELETE_JOKE_STRING = "\\n\\n技术支持 方倍工作室";
    private final String FACE_URL = "http://api2.sinaapp.com/recognize/picture/?appkey=0020120430&appsecert=fa6095e123cd28fd&reqtype=text&keyword=%KEYWORD%";
    private static int 德惠 = 101060103;
    private static int 南京 = 101190101;
    private static int 太原 = 101100101;
    private final String BAIDU_PLACE_API_URL = "http://api.map.baidu.com/place/v2/search?ak=FuDNRht4COuEW5fNg0cGwbU1&output=json&query=%query%&page_size=10&page_num=0&scope=2&location=%location%&radius=500";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy年MM月dd日");
    private final String respContent = "呀，尤，我错了……网络问题么 \ue411";
    private IChat iChat = XiaoI.getInstance();
    private static HttpClient client = new HttpClient();

    static {
        client.getHttpConnectionManager().getParams()
                .setConnectionTimeout(5000);
        sArticle = new Article();
        sArticle.setTitle("我的微博");
        sArticle.setDescription("我从星星来，要到星星去！");
        sArticle.setPicUrl("http://tp4.sinaimg.cn/2791610843/180/40043803902/1");
        sArticle.setUrl("http://weibo.com/2791610843/profile?rightmod=1&wvr=5&mod=personinfo");
    }

    public String processRequest (HttpServletRequest request) {
        String respMessage = null;
        try {
            // 默认返回的文本消息内容
            BaseResponseMessage responseMessage = null;
            // xml请求解析
            Map<String, String> requestMap = MessageUtil.parseXml(request);
            BaseEvent baseEvent = processMessage(requestMap);
            WeiChatUser weiChatUser = userDao.getWeiChatUserByFromUserName(baseEvent.getFromUserName());
            if (weiChatUser == null) {
                weiChatUser = new WeiChatUser();
                weiChatUser.setFromUserName(baseEvent.getFromUserName());
                weiChatUser.setFlag(0);
                Stage stage = stageDao.query(Stage.class, 0);
                weiChatUser.setStage(stage);
                weiChatUser = userDao.insert(weiChatUser);
            }
            logger.info(baseEvent.toString());
            if (baseEvent instanceof BaseRequestMessage) {
                if (baseEvent instanceof TextRequestMessage) {
                    TextRequestMessage textRequestMessage = (TextRequestMessage) baseEvent;
                    if (weiChatUser.getFlag() == isAuthed && isGodness(textRequestMessage.getContent())) {
                        responseMessage = new NewsResponseMessage();
                        List<Article> articles = new ArrayList<Article>();
                        NewsResponseMessage newsResponseMessage = (NewsResponseMessage) responseMessage;
                        Article article = new Article();
                        article.setTitle("我的小尤哦");
                        article.setPicUrl(YoYoUtil.PIC_PAGE_FACE);
                        article.setUrl(YoYoUtil.PIC_GODNESS);
                        Date now = new Date();
                        calendar.set(2013, Calendar.APRIL, 22);
                        Date lastDate = calendar.getTime();
                        long expire = (now.getTime() - lastDate.getTime()) / 24 / 60 / 60 / 1000;
                        article.setDescription("相识于2013年4月22日，初见惊艳，再见沉迷。至今已有" + expire + "日！");
                        articles.add(article);
                        newsResponseMessage.setArticles(articles);
                    } else if (weiChatUser.getFlag() == isAuthed && (responseMessage = processMenu(textRequestMessage.getContent(), weiChatUser, baseEvent)) != null) {
                        //donothing
                    } else {
                        responseMessage = new NewsResponseMessage();
                        List<Article> articles = new ArrayList<Article>();
                        NewsResponseMessage newsResponseMessage = (NewsResponseMessage) responseMessage;
                        List<Hero> models = dotaService.searchHeros(textRequestMessage.getContent());
                        if (DotaService.complete && models != null && models.size() != 0) {
                            if (models.size() == 1) {
                                Hero model = models.get(0);
                                Article article = new Article();
                                article.setTitle(model.getName());
                                article.setPicUrl(model.getImgUrl());
                                article.setUrl(YoYoUtil.WEBSITE_URL + "dota/heros/" + model.getId());
                                article.setDescription(model.getDes());
                                articles.add(article);
                                for (Skill skill : model.getSkills()) {
                                    Article art = new Article();
                                    art.setTitle(skill.getSkillName());
                                    art.setUrl(YoYoUtil.WEBSITE_URL + "dota/heros/" + model.getId());
                                    art.setPicUrl(skill.getSkillImgUrl());
                                    art.setDescription(skill.getSkillDesc());
                                    articles.add(art);
                                }
                            } else {
                                for (Hero model : models) {
                                    Article article = new Article();
                                    article.setTitle(model.getName());
                                    article.setPicUrl(model.getImgUrl());
                                    article.setUrl(model.getUrl());
                                    article.setDescription(model.getDes());
                                    articles.add(article);
                                }
                            }
                            newsResponseMessage.setArticles(articles);
                        } else {
                            String content = textRequestMessage.getContent();
                            content = content.trim();
                            if (isLiYou(content)) {
                                weiChatUser = userDao.getWeiChatUserByFromUserName(baseEvent.getFromUserName());
                                if (weiChatUser != null && weiChatUser.getFlag() != isAuthed) {
                                    weiChatUser.setFlag(isAuthed);
                                    userDao.update(weiChatUser);
                                } else if (weiChatUser == null) {
                                    //impossible
                                    weiChatUser = new WeiChatUser();
                                    weiChatUser.setFromUserName(baseEvent.getFromUserName());
                                    weiChatUser.setFlag(isAuthed);
                                    Stage stage = stageDao.query(Stage.class, 0);
                                    weiChatUser.setStage(stage);
                                    weiChatUser = userDao.insert(weiChatUser);
                                }
                                int size = CardController.cards.size();
                                int randomNum = random.nextInt();
                                randomNum = Math.abs(randomNum) % size;
                                String card = CardController.cards.get(randomNum);
                                Article article = new Article();
                                article.setTitle(textRequestMessage.getContent() + "の专属" + card + "卡");
                                article.setPicUrl(YoYoUtil.WEBSITE_URL + "cards/" + card);
                                article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/" + card + "/" + textRequestMessage.getContent() + "/" + System.currentTimeMillis());
                                article.setDescription("出示本卡可令祁麻麻" + getTitle(randomNum) + "一次。仅限" + content + "使用。有效期forever~");
                                articles.add(article);
                                newsResponseMessage.setArticles(articles);
                            } else {
                                TextResponseMessage textResponseMessage = new TextResponseMessage();
                                if (weiChatUser.getFlag() == isAuthed) {
                                    textResponseMessage.setContent(iChat.chat(textRequestMessage.getFromUserName(), content, XiaoI.ASK, null));
                                } else if (content.equals("小尤")) {
                                    textResponseMessage.setContent("木哈哈哈，你当我傻么，我不如直接告诉你得了……回覆我小尤的名字哦！！！");
                                } else {
                                    textResponseMessage.setContent("拜託，你又不是我家小尤，我才不給你回照片呢~回覆我小尤的名字哦！！！");
                                }
                                responseMessage = textResponseMessage;
                            }
                        }
                    }


                } else if (baseEvent instanceof VoiceRequestMessage) {
                    responseMessage = new TextResponseMessage();
                    VoiceRequestMessage voiceRequestMessage = (VoiceRequestMessage) baseEvent;
                    String result = voiceRequestMessage.getRecognition();
                    String talk = null;
                    if (result == null || result.trim().equals("")) {
                        talk = "Sorry,还没有开通语音消息识别功能哦，请尝试在退出模式下进行文字交流哦~";
                    } else {
                        talk = iChat.chat(voiceRequestMessage.getFromUserName(), result, XiaoI.ASK, null);
                    }

                    ((TextResponseMessage) responseMessage).setContent(talk);
                } else if (baseEvent instanceof ImageRequestMessage) {
                    responseMessage = new TextResponseMessage();
                    TextResponseMessage textResponseMessage = (TextResponseMessage) responseMessage;
                    ImageRequestMessage imageRequestMessage = (ImageRequestMessage) baseEvent;
                    HttpMethod getMethod = null;
                    try {
                        final String url = FACE_URL.replace("%KEYWORD%", imageRequestMessage.getPicUrl());
                        logger.info("getPicUrl;" + imageRequestMessage.getPicUrl());
                        getMethod = new GetMethod(url);
                        int ret_code = client.executeMethod(getMethod);
                        if (ret_code == 200) {
                            JSONObject jsonObject = JSONObject.fromObject(getMethod.getResponseBodyAsString());
                            JSONObject text = jsonObject.getJSONObject("text");
                            String result = text.getString("content");
                            if (result.equals("我有权保持沉默！")) {
                                result += "Tips:尤尤换张图片呗，我评分很艰难哎~";
                            } else if (result.equals("不支持的图片类型！")) {
                                result = "这张图片真心看不懂哎~尤尤要不直接拍一张怎么样啊……";
                            } else if (result.equals("图片大小超过最大值！")) {
                                result = "哎哟，不要为难我了哎，人家只是波波为尤尤设置的机器人而已嘛,你说要是传一张稍小一点的图片肿么样……";
                            } else if (!result.contains("察颜")) {
                                result = "哎唷，不要为难我了哎，伦家只是波波为尤尤设置的小机器人而已嘛……";
                            }
                            textResponseMessage.setContent(result);
                        } else {
                            textResponseMessage.setContent(respContent);
                        }

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                    } catch (HttpException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                    } catch (IOException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                    } finally {
                        if (getMethod != null) {
                            getMethod.releaseConnection();
                        }
                    }
                } else if (baseEvent instanceof LinkRequestMessage) {
                    responseMessage = new TextResponseMessage();
                } else if (baseEvent instanceof LocationRequestMessage) {
                    NewsResponseMessage newsResponseMessage = new NewsResponseMessage();
                    TextResponseMessage textResponseMessage = new TextResponseMessage();
                    LocationRequestMessage locationRequestMessage = (LocationRequestMessage) baseEvent;
                    HttpMethod getMethod = null;
                    try {
                        String query = java.net.URLEncoder.encode("美食", "UTF-8");
                        final String url = BAIDU_PLACE_API_URL.replace("%location%", locationRequestMessage.getLocation_X() +
                                "," + locationRequestMessage.getLocation_Y()).replace("%query%", query);
                        getMethod = new GetMethod(url);
                        int ret_code = client.executeMethod(getMethod);
                        if (ret_code == 200) {
                            String result = getMethod.getResponseBodyAsString();
                            JSONObject jsonObject = JSONObject.fromObject(result);
                            JSONArray jsonArray = jsonObject.getJSONArray("results");
                            List<Article> articles = new ArrayList<Article>(11);
                            Article articleFirst = new Article();
                            articleFirst.setTitle("美食");
                            articleFirst.setDescription("附近500米美食");
                            articleFirst.setPicUrl("");
                            articleFirst.setUrl("");
                            articles.add(articleFirst);
                            for (int i = 0; jsonArray != null && i < jsonArray.size(); i++) {
                                String name = null;
                                String telephone = null;
                                String address = null;
                                int distance = 0;
                                String detail_url = "";
                                String price = null;
                                double overall_rating = 0;
                                try {
                                    JSONObject object = jsonArray.getJSONObject(i);
                                    name = object.getString("name");
                                    telephone = object.getString("telephone");
                                    address = object.getString("address");
                                    String uid = object.getString("uid");
                                    JSONObject detail_info = object.getJSONObject("detail_info");
                                    distance = detail_info.getInt("distance");
                                    detail_url = detail_info.getString("detail_url");
                                    price = detail_info.getString("price");
                                    overall_rating = detail_info.getDouble("overall_rating");
                                    double service_rating = detail_info.getDouble("service_rating");
                                    double environment_rating = detail_info.getDouble("environment_rating");
                                } catch (JSONException ex) {
                                    ex.printStackTrace();
                                }
                                Article article = new Article();
                                article.setDescription("电话:" + telephone + "\r\n地址:" + address + "\r\n距离:" + distance + "价格:" + price + "评分:" + overall_rating);
                                article.setTitle(name + "\r\n" + article.getDescription());
                                article.setPicUrl("");
                                article.setUrl(detail_url);
                                articles.add(article);
                            }
                            newsResponseMessage.setArticles(articles);
                            responseMessage = newsResponseMessage;
                        } else {
                            textResponseMessage.setContent(respContent);
                            responseMessage = newsResponseMessage;
                        }

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                        responseMessage = newsResponseMessage;
                    } catch (HttpException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                        responseMessage = newsResponseMessage;
                    } catch (IOException e) {
                        e.printStackTrace();
                        textResponseMessage.setContent(respContent);
                        responseMessage = newsResponseMessage;
                    } finally {
                        if (getMethod != null) {
                            getMethod.releaseConnection();
                        }
                    }
                } else {
                    System.out.println("**********************");
                    System.out.println(baseEvent.toString());
                    System.out.println("**********************");
                    responseMessage = new TextResponseMessage();
                }
            } else if (baseEvent instanceof PushEvent) {
                responseMessage = processPushEvent((PushEvent) baseEvent, weiChatUser);
            } else {
                responseMessage = new TextResponseMessage();
            }
            responseMessage.setToUserName(baseEvent.getFromUserName());
            responseMessage.setFromUserName(baseEvent.getToUserName());
            responseMessage.setCreateTime(new Date().getTime());
            responseMessage.setFuncFlag(0);
            respMessage = MessageUtil.messageToXml(responseMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return respMessage;
    }

    private BaseResponseMessage processPushEvent (PushEvent pushEvent, WeiChatUser weiChatUser) {
        String event = pushEvent.getEvent();
        if (event.equals("subscribe")) {
            TextResponseMessage textResponseMessage = new TextResponseMessage();
            textResponseMessage.setContent(getMainMenu(weiChatUser));
            return textResponseMessage;
        } else {
            //TODO
            TextResponseMessage textResponseMessage = new TextResponseMessage();
            return textResponseMessage;
        }
    }

    public BaseResponseMessage processTextMessage (Map<String, String> requestMap) {
        BaseResponseMessage message = null;
        return message;
    }

    public static BaseEvent processMessage (Map<String, String> requestMap) {
        BaseEvent message = null;
        String msgType = requestMap.get("MsgType");
        if (msgType.equals(REQ_MESSAGE_TYPE_TEXT)) {
            message = new TextRequestMessage();
            message = reflectMessage(requestMap, message);
        } else if (msgType.equals(REQ_MESSAGE_TYPE_IMAGE)) {
            message = new ImageRequestMessage();
            message = reflectMessage(requestMap, message);
        } else if (msgType.equals(REQ_MESSAGE_TYPE_LINK)) {
            message = new LinkRequestMessage();
            message = reflectMessage(requestMap, message);
        } else if (msgType.equals(REQ_MESSAGE_TYPE_LOCATION)) {
            message = new LocationRequestMessage();
            message = reflectMessage(requestMap, message);
        } else if (msgType.equals(REQ_MESSAGE_TYPE_VOICE)) {
            message = new VoiceRequestMessage();
            message = reflectMessage(requestMap, message);
        } else if (msgType.equals(REQ_MESSAGE_TYPE_EVENT)) {
            String event = requestMap.get("Event");
            if (event.equals(EVENT_TYPE_SUBSCRIBE)) {
                if (requestMap.get("EventKey") == null) {
                    //关注事件
                    message = new BaseEvent();
                } else {
                    //扫描二维码事件
                    message = new QREvent();
                }
            } else if (event.equals(EVENT_TYPE_UNSUBSCRIBE)) {
                message = new BaseEvent();
            } else if (event.equals(EVENT_TYPE_SCAN)) {
                message = new QREvent();
            } else if (event.equals(EVENT_TYPE_LOCATION)) {
                message = new LocationEvent();
            } else if (event.equals(EVENT_TYPE_CLICK)) {
                message = new ClickEvent();
            } else {
                message = new BaseEvent();
            }
            message = reflectMessage(requestMap, message);
        } else {
            message = new BaseRequestMessage();
            message = reflectMessage(requestMap, message);
        }
        return message;
    }

    public static BaseEvent reflectMessage (Map<String, String> requestMap, BaseEvent message) {
        Class clazz = message.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = null;
                    Class cla = field.getType();
                    if (cla == long.class || cla == Long.class) {
                        value = Long.parseLong(requestMap.get(field.getName()));
                    } else if (cla == byte.class || cla == Byte.class) {
                        value = Byte.parseByte(requestMap.get(field.getName()));
                    } else if (cla == int.class || cla == Integer.class) {
                        value = Integer.parseInt(requestMap.get(field.getName()));
                    } else if (cla == float.class || cla == Float.class) {
                        value = Float.parseFloat(requestMap.get(field.getName()));
                    } else if (cla == double.class || cla == Double.class) {
                        value = Double.parseDouble(requestMap.get(field.getName()));
                    } else {
                        value = requestMap.get(field.getName());
                    }
                    field.set(message, value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        }
        return message;
    }

    private boolean isGodness (String text) {
        if (text == null) {
            return false;
        }
        text = text.trim().toLowerCase();
        return (text.equals("godness") || text.equals("女神") || text.equals("nvshen"));
    }

    private String getMainMenu (WeiChatUser user) {
        if (user == null || user.getFlag() != isAuthed) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("こんにちは、僕は祁です。番号を選択してください。回復の女神がサプライズよ。回復?このメニュー表示。").append("\n\r\n");
        List<Stage> stages = stageDao.getStagesByCategory(1);
        for (Stage stage : stages) {
            buffer.append("【");
            buffer.append(stage.getKey());
            buffer.append("】 ");
            String des = stage.getDes();
            des = des.trim();
            buffer.append(des);
            buffer.append("\ue417");
            buffer.append("\n");
        }
        buffer.append("\r\n");
        return buffer.toString();
    }

    /**
     * emoji表情转换(hex -> utf-16)
     *
     * @param hexEmoji
     * @return
     */
    public static String emoji (int hexEmoji) {
        return String.valueOf(Character.toChars(hexEmoji));
    }

    private boolean isLiYou (String name) {
        if (name == null) {
            return false;
        }
        if ((name = name.toLowerCase()).equals("liyou")) {
            return true;
        } else if (name.equals("李尤")) {
            return true;
        } else if (name.equals("小尤尤")) {
            return true;
        } else if (name.equals("yoyo")) {
            return true;
        } else if (name.equals("yoyo_littlepig")) {
            return true;
        } else if (name.equals("尤尤")) {
            return true;
        } else if (name.equals("lu")) {
            return true;
        }
        return false;
    }

    private BaseResponseMessage processMenu (String content, WeiChatUser weiChatUser, BaseEvent baseEvent) {
        if (content == null) {
            return null;
        }
        content = content.trim();
        System.out.println(content);
        TextResponseMessage textResponseMessage = new TextResponseMessage();
        if (content.equals("?") || content.equals("？")) {
            textResponseMessage.setContent(getMainMenu(weiChatUser));
            return textResponseMessage;
        }
        Stage stage = weiChatUser.getStage();
        if ((stage == null || stage.getId() == 1) && content.equals("0")) {
            //we know we are in normal mode
            textResponseMessage.setContent("哇哦，我们还没有选择模式哦，尤尤调皮啦……");
        } else {
            //we should handle the menu
            if (content.equals("0")) {
                textResponseMessage.setContent("啊,退出模式【" + stage.getDes() + "】你可以和来自星星的Yo交流了哦\r\n" + getMainMenu(weiChatUser));
                stage = stageDao.getStagesByCategoryAndKey(1, "0");
                weiChatUser.setStage(stage);
                userDao.update(weiChatUser);
            } else {
                stage = stageDao.getStagesByCategoryAndKey(1, content);
                if (stage != null) {
                    textResponseMessage.setContent("哇哦 尤尤选择了【" + stage.getDes() + "】，输入0退出?显示菜单。");
                    weiChatUser.setStage(stage);
                    userDao.update(weiChatUser);
                    String key = stage.getKey();
                    if (key.equals("1")) {
                        textResponseMessage.setContent("小尤的亲戚……输入任意非命令字符显示周期哦");
                        //生理周期
                    } else if (key.equals("2")) {
                        //运程
                        try {
                            Constellation constellation = new Constellation(2);
                            StringBuilder stringBuilder = new StringBuilder();
                            List<Constellation.Item> items = constellation.getItems();
                            if (items == null || items.size() == 0) {

                            } else {
                                stringBuilder.append(constellation.getConstellation());
                                stringBuilder.append("\r\n");
                                stringBuilder.append(constellation.getDate());
                                stringBuilder.append("\r\n");
                                for (Constellation.Item item : items) {
                                    stringBuilder.append(item.getTitle());
                                    stringBuilder.append("\r\n");
                                    if (item.getRank() == 0) {
                                        stringBuilder.append(item.getValue());
                                    } else {
                                        stringBuilder.append(item.getRankString(item.getRank()));
                                    }
                                    stringBuilder.append("\r\n");
                                }
                            }
                            textResponseMessage.setContent(stringBuilder.toString());
                        } catch (IOException e) {
                            System.err.println(e.toString());
                            textResponseMessage.setContent(respContent);
                        }
                    } else if (key.equals("3")) {
                        try {
                            List<Article> articles = new ArrayList<Article>(8);
                            Weather nanjing = new Weather("南京");
                            List<Weather.WeatherInfo> infos = nanjing.getWeatherInfos();
                            if (infos == null) {
                                Article article = new Article();
                                article.setTitle(" 小尤の天气--南京   " + nanjing.getDate());
                                article.setPicUrl(YoYoUtil.WEBSITE_URL + "cards/travel");
                                article.setDescription("oops!");
                                article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                articles.add(article);
                            } else {
                                Article article = new Article();
                                article.setTitle(" 小尤の天气--南京   ");
                                article.setPicUrl("");
                                article.setDescription("");
                                article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                articles.add(article);
                                for (Weather.WeatherInfo info : infos) {
                                    article = new Article();
                                    article.setTitle(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setPicUrl(info.getDayPictureUrl());
                                    article.setDescription(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                    articles.add(article);
                                }
                                Weather dehui = new Weather("德惠");
                                infos = dehui.getWeatherInfos();
                                article = new Article();
                                article.setTitle(" 小尤の天气--德惠   ");
                                article.setPicUrl("");
                                article.setDescription("");
                                article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                articles.add(article);
                                for (Weather.WeatherInfo info : infos) {
                                    article = new Article();
                                    article.setTitle(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setPicUrl(info.getDayPictureUrl());
                                    article.setDescription(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                    articles.add(article);
                                }
                            }
                            NewsResponseMessage newsResponseMessage = new NewsResponseMessage();
                            newsResponseMessage.setArticles(articles);
                            return newsResponseMessage;
                        } catch (IOException e) {
                            textResponseMessage.setContent(respContent);
                        }
                    } else if (key.equals("4")) {
                        textResponseMessage.setContent("输入任何人姓名获得各种卡哦！如李尤……");
                        return textResponseMessage;
                        //图片
                    } else if (key.equals("5")) {
                        textResponseMessage.setContent("哦哦，输入任何人的姓名获得对应的姓名评分哦！如李尤……");
                        return textResponseMessage;
                        //姓名评分
                    } else if (key.equals("6")) {
                        textResponseMessage.setContent("小尤木哈哈哈，笑话哦！回复任意非命令字符……");
                        return textResponseMessage;
                        //笑话
                    } else if (key.equals("7")) {
                        textResponseMessage.setContent("上传一张图片哦，查看评分呢……偷偷告诉你其实任意模式下上传图片即可哦~");
                        return textResponseMessage;
                        //喜欢
                    } else if (key.equals("8")) {
                        textResponseMessage.setContent("小尤日记……回复任意非命令字符……");
                        return textResponseMessage;
                        //
                    } else {
                        return null;
                    }
                } else {
                    stage = weiChatUser.getStage();
                    if (stage == null || stage.getId() == 1) {
                        return null;
                    }
                    //handle the menu actually
                    String key = stage.getKey();
                    if (key.equals("1")) {
                        Date now = new Date();
                        List<Aunties> auntiesList = auntiesDao.queryByTime(now, 3);
                        Date lastDate = null;
                        StringBuilder stringBuilder = new StringBuilder();
                        int avag = 0;
                        int used = 0;
                        stringBuilder.append("小尤近" + auntiesList.size() + "次亲戚来的时间哦：\r\n\r");
                        for (Aunties aunties : auntiesList) {
                            lastDate = aunties.getAuntDate();
                            String dateStr = DATE_FORMAT.format(lastDate);
                            stringBuilder.append(dateStr);
                            if (aunties.getIntervalDate() != 0) {
                                stringBuilder.append("----距离上次时间" + aunties.getIntervalDate() + "天");
                                used++;
                            }

                            avag += aunties.getIntervalDate();
                            stringBuilder.append("\r\n\r");
                        }
                        avag /= used;
                        long expire = (now.getTime() - lastDate.getTime()) / 24 / 60 / 60 / 1000;
                        stringBuilder.append("**********************\r\n\r");
                        stringBuilder.append("距离上次已过去").append(expire).append("天,距离下一次大概还有").append(avag - expire).append("天\r\n");
                        if (expire >= avag - 5) {
                            stringBuilder.append("小尤注意哦，亲戚要来了哦，注意身体哦！！！\r\n");
                        }
                        stringBuilder.append("祁麻麻のTips:\r\n（1）给自己的身体加温，可以用热水袋暖自己的肚子，让肚子不受寒\r\n（2）可以用热水加红糖\r\n（3）把生姜、大枣放在锅中加水煮沸，然后加入鸡蛋喝下去也可以减缓疼痛\r\n\r");
                        stringBuilder.append("<a href=\"http://www.dayima.com/?var=mobile\">大姨吗</a>");
                        textResponseMessage.setContent(stringBuilder.toString());
                        return textResponseMessage;
                        //生理周期
                    } else if (key.equals("2")) {
                        //运程
                        try {
                            int value = 2;
                            if (content.startsWith("xz")) {
                                content = content.replace("xz", "0");
                                try {
                                    value = Integer.parseInt(content);
                                } catch (Exception ex) {
                                    String response = Constellation.getConstellationString();
                                    response = "请输入序号选择星座如xz2为我家小尤的双子座\r\n" + response;
                                    textResponseMessage.setContent(response);
                                    return textResponseMessage;
                                }
                                if (value < 0 || value > 11) {
                                    String response = Constellation.getConstellationString();
                                    response = "请输入序号选择星座如xz2为我家小尤的双子座\r\n" + response;
                                    textResponseMessage.setContent(response);
                                    return textResponseMessage;
                                }
                            } else {
                                String response = Constellation.getConstellationString();
                                response = "请输入序号选择星座如xz2为我家小尤的双子座\r\n" + response;
                                textResponseMessage.setContent(response);
                                return textResponseMessage;
                            }
                            Constellation constellation = new Constellation(value);
                            StringBuilder stringBuilder = new StringBuilder();
                            List<Constellation.Item> items = constellation.getItems();
                            if (items == null || items.size() == 0) {
                                String response = Constellation.getConstellationString();
                                response = "请输入序号选择星座如xz2为我家小尤的双子座\r\n" + response;
                                textResponseMessage.setContent(response);
                                return textResponseMessage;
                            } else {
                                stringBuilder.append(constellation.getConstellation());
                                stringBuilder.append("\n");
                                stringBuilder.append(constellation.getDate());
                                stringBuilder.append("\n");
                                for (Constellation.Item item : items) {
                                    stringBuilder.append(item.getTitle());
                                    stringBuilder.append("\n");
                                    if (item.getRank() == 0) {
                                        stringBuilder.append(item.getValue());
                                    } else {
                                        stringBuilder.append(item.getRankString(item.getRank()));
                                    }
                                    stringBuilder.append("\n");
                                }
                            }
                            textResponseMessage.setContent(stringBuilder.toString());
                            return textResponseMessage;
                        } catch (IOException e) {
                            textResponseMessage.setContent(respContent);
                        }
                    } else if (key.equals("3")) {
                        //天气
                        try {
                            List<Article> articles = new ArrayList<Article>(4);
                            Weather weather = new Weather(content);
                            List<Weather.WeatherInfo> infos = weather.getWeatherInfos();
                            if (infos == null) {
                                textResponseMessage.setContent("Sorry 哦，没找到尤要的城市哎！");
                                return textResponseMessage;
                            } else {
                                Article article = new Article();
                                article.setTitle(" 小尤の天气--" + weather.getCity());
                                article.setPicUrl("");
                                article.setDescription("");
                                article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                articles.add(article);
                                for (Weather.WeatherInfo info : infos) {
                                    article = new Article();
                                    article.setTitle(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setPicUrl(info.getDayPictureUrl());
                                    article.setDescription(info.getDate() + "\r\n" + info.getWeather() + " " + info.getTemperature());
                                    article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/travel/李尤/");
                                    articles.add(article);
                                }
                            }
                            NewsResponseMessage newsResponseMessage = new NewsResponseMessage();
                            newsResponseMessage.setArticles(articles);
                            return newsResponseMessage;
                        } catch (IOException e) {
                            textResponseMessage.setContent(respContent);

                        }
                    } else if (key.equals("4")) {
                        //图片
                        int size = CardController.cards.size();
                        int randomNum = random.nextInt();
                        randomNum = Math.abs(randomNum) % size;
                        String card = CardController.cards.get(randomNum);
                        List<Article> articles = new ArrayList<Article>(1);
                        Article article = new Article();
                        article.setTitle(content + "の专属" + card + "卡");
                        article.setPicUrl(YoYoUtil.WEBSITE_URL + "cards/" + card);
                        article.setUrl(YoYoUtil.WEBSITE_URL + "cards/loveuu/" + card + "/" + content + "/" + System.currentTimeMillis());
                        article.setDescription("出示本卡可令祁麻麻" + getTitle(randomNum) + "一次。仅限" + content + "使用。有效期forever~");
                        articles.add(article);
                        NewsResponseMessage newsResponseMessage = new NewsResponseMessage();
                        newsResponseMessage.setArticles(articles);
                        return newsResponseMessage;
                    } else if (key.equals("5")) {
                        //姓名评价
                        content = content.trim();
                        if (content.equals("李尤")) {
                            String result = LIYOU_FORTURE.replace("\\n", "\r\n");
                            textResponseMessage.setContent(result);
                        } else if (content.equals("祁晓波")) {
                            String result = QIXIAOBO_FORTURE.replace("\\n", "\r\n");
                            textResponseMessage.setContent(result);
                        } else {
                            HttpMethod getMethod = null;
                            try {
                                content = java.net.URLEncoder.encode(content, "UTF-8");
                                final String url = FORTURE_URL.replace("%name%", content);
                                getMethod = new GetMethod(url);
                                int ret_code = client.executeMethod(getMethod);
                                if (ret_code == 200) {
                                    String result = getMethod.getResponseBodyAsString();
                                    result = result.replace(DELETE_FORTURE_STRING, "");
                                    result = result.replace("\\n", "\r\n");
                                    textResponseMessage.setContent(result);
                                } else {
                                    textResponseMessage.setContent(respContent);
                                }

                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                textResponseMessage.setContent(respContent);
                            } catch (HttpException e) {
                                e.printStackTrace();
                                textResponseMessage.setContent(respContent);
                            } catch (IOException e) {
                                e.printStackTrace();
                                textResponseMessage.setContent(respContent);
                            } finally {
                                if (getMethod != null) {
                                    getMethod.releaseConnection();
                                }
                            }
                        }
                        return textResponseMessage;
                    } else if (key.equals("6")) {
                        HttpMethod getMethod = null;
                        try {
                            getMethod = new GetMethod(JOKE_URL);
                            int ret_code = client.executeMethod(getMethod);
                            if (ret_code == 200) {
                                String result = getMethod.getResponseBodyAsString();
                                result = result.replace(DELETE_JOKE_STRING, "");
                                result = result.replace("\\n", "\r\n");
                                textResponseMessage.setContent(result);
                            } else {
                                textResponseMessage.setContent(respContent);
                            }

                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            textResponseMessage.setContent(respContent);
                        } catch (HttpException e) {
                            e.printStackTrace();
                            textResponseMessage.setContent(respContent);
                        } catch (IOException e) {
                            e.printStackTrace();
                            textResponseMessage.setContent(respContent);
                        } finally {
                            if (getMethod != null) {
                                getMethod.releaseConnection();
                            }
                        }
                        return textResponseMessage;
                        //笑话
                    } else if (key.equals("7")) {
                        textResponseMessage.setContent("小尤尤调皮了哎，明明叫你上传图片的哈……");
                        return textResponseMessage;
                        //娱乐，面相评分
                    } else if (key.equals("8")) {
                        //小尤日记
                        textResponseMessage.setContent("<a href=\"" + YoYoUtil.WEBSITE_URL + "evernote\">少年和她</a>");
                        return textResponseMessage;
                    } else {

                    }
                }
            }
        }
        return textResponseMessage;
    }

    public static String getTitle (int kind) {
        String title;
        switch (kind) {
            case 0:
                title = "打扫房间";
                break;
            case 1:
                title = "洗衣服";
                break;
            case 2:
                title = "做饭刷碗";
                break;
            case 3:
                title = "讲笑话";
                break;
            case 4:
                title = "陪逛街";
                break;
            case 5:
                title = "一起旅行";
                break;
            case 6:
                title = "不生气";
                break;
            default:
                title = "各种玩";
                break;
        }
        return title;
    }

}
