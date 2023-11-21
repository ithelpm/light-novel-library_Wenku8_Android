package org.mewx.wenku8.global.api

import android.content.ContentValues

@SuppressWarnings("unused")
object Wenku8API {
    /*
     * Basic definitions
     */
    var NoticeString: String? = ""
    val REGISTER_URL: String? = "http://www.wenku8.com/register.php"
    val BASE_URL: String? = "http://app.wenku8.com/android.php"
    val RELAY_URL: String? = "https://wenku8-relay.mewx.org/"
    private val NovelFinishedSC: String? = "已完成"
    private val NovelFinishedTC: String? = "已完成"
    private val NovelNotFinishedSC: String? = "连载中"
    private val NovelNotFinishedTC: String? = "連載中"
    fun getCoverURL(aid: Int): String? {
        return "http://img.wenku8.com/image/" + aid / 1000 + "/" + aid + "/" + aid + "s.jpg"
    }

    // this only prevents good boys from doing bad things, and it's what we all know :-)
    // and I know the mixed use for both simplified and traditional do not work in this case
    const val MIN_REPLY_TEXT = 7
    private val badWords: HashSet<String?>? = HashSet(Arrays.asList( // Simplified
            "共产党", "政府", "毛泽东", "邓小平", "江泽民", "胡锦涛", "温家宝", "习近平",
            "李克强", "台独", "藏独", "反日", "反共", "反中", "达赖", "刘晓波", "毛主席", "愤青",
            "反华", "右翼", "游行", "示威", "静坐", "公安", "李洪志", "法轮功", "刷分", "路过路过",
            ".......", "。。。。", "色情", "吃屎", "你妈", "他妈", "她妈", "操你", "垃圾", "去死",
            "迷魂药", "催情药", "毒品",  // Traditional
            "共產黨", "政府", "毛澤東", "鄧小平", "江澤民", "胡錦濤", "溫家寶", "習近平",
            "李克強", "臺獨", "藏獨", "反日", "反共", "反中", "達賴", "劉曉波", "毛主席", "憤青",
            "反華", "右翼", "遊行", "示威", "靜坐", "公安", "李洪誌", "法輪功", "刷分", "路過路過",
            ".......", "。。。。", "色情", "吃屎", "你媽", "他媽", "她媽", "操你", "垃圾", "去死",
            "迷魂藥", "催情藥", "毒品",  // Testing
            "blablabla"
    ))

    private fun getLANG(l: LANG?): Int {
        return when (l) {
            LANG.SC -> 0
            LANG.TC -> 1
            else -> 0 // for extended language
        }
    }

    fun getSTATUSByInt(i: Int): STATUS? {
        return if (i == 0) STATUS.NOT_FINISHED else STATUS.FINISHED
    }

    fun getSTATUSByString(s: String?): STATUS? {
        return if (s.equals(NovelNotFinishedSC) || s.equals(NovelNotFinishedTC)) STATUS.NOT_FINISHED else STATUS.FINISHED
    }

    fun getStatusBySTATUS(s: STATUS?): String? {
        return when (GlobalConfig.getCurrentLang()) {
            TC -> if (s == STATUS.FINISHED) NovelFinishedTC else NovelNotFinishedTC
            SC ->                 // the default one
                if (s == STATUS.FINISHED) NovelFinishedSC else NovelNotFinishedSC

            else -> if (s == STATUS.FINISHED) NovelFinishedSC else NovelNotFinishedSC
        }
    }

    fun getNOVELSORTBY(n: String?): NOVELSORTBY? {
        return when (n) {
            "allvisit" -> NOVELSORTBY.allVisit
            "monthvisit" -> NOVELSORTBY.monthVisit
            "monthvote" -> NOVELSORTBY.monthVote
            "weekvisit" -> NOVELSORTBY.weekVisit
            "weekvote" -> NOVELSORTBY.weekVote
            "dayvisit" -> NOVELSORTBY.dayVisit
            "dayvote" -> NOVELSORTBY.dayVote
            "postdate" -> NOVELSORTBY.postDate
            "lastupdate" -> NOVELSORTBY.lastUpdate
            "goodnum" -> NOVELSORTBY.goodNum
            "size" -> NOVELSORTBY.size
            "fullflag" -> NOVELSORTBY.fullFlag
            "allvote" -> NOVELSORTBY.allVote // default
            else -> NOVELSORTBY.allVote
        }
    }

    fun getNOVELSORTBY(n: NOVELSORTBY?): String? {
        return when (n) {
            NOVELSORTBY.allVisit -> "allvisit"
            NOVELSORTBY.allVote -> "allvote"
            NOVELSORTBY.monthVisit -> "monthvisit"
            NOVELSORTBY.monthVote -> "monthvote"
            NOVELSORTBY.weekVisit -> "weekvisit"
            NOVELSORTBY.weekVote -> "weekvote"
            NOVELSORTBY.dayVisit -> "dayvisit"
            NOVELSORTBY.dayVote -> "dayvote"
            NOVELSORTBY.postDate -> "postdate"
            NOVELSORTBY.lastUpdate -> "lastupdate"
            NOVELSORTBY.goodNum -> "goodnum"
            NOVELSORTBY.size -> "size"
            NOVELSORTBY.fullFlag -> "fullflag"
            else -> "allvote" // default
        }
    }

    fun getNOVELSORTBY_ChsId(n: NOVELSORTBY?): Int {
        return when (n) {
            NOVELSORTBY.allVisit -> R.string.tab_allvisit
            NOVELSORTBY.monthVisit -> R.string.tab_monthvisit
            NOVELSORTBY.monthVote -> R.string.tab_monthvote
            NOVELSORTBY.weekVisit -> R.string.tab_weekvisit
            NOVELSORTBY.weekVote -> R.string.tab_weekvote
            NOVELSORTBY.dayVisit -> R.string.tab_dayvisit
            NOVELSORTBY.dayVote -> R.string.tab_dayvote
            NOVELSORTBY.postDate -> R.string.tab_postdate
            NOVELSORTBY.lastUpdate -> R.string.tab_lastupdate
            NOVELSORTBY.goodNum -> R.string.tab_goodnum
            NOVELSORTBY.size -> R.string.tab_size
            NOVELSORTBY.fullFlag -> R.string.tab_fullflag
            NOVELSORTBY.allVote -> R.string.tab_allvote // default
            else -> R.string.tab_allvote
        }
    }

    fun getErrorInfo_ResId(errNo: Int): Int {
        return when (errNo) {
            0 ->                 // 请求发生错误
                R.string.error_00

            1 ->                 // 成功(登陆、添加、删除、发帖)
                R.string.error_01

            2 ->                 // 用户名错误
                R.string.error_02

            3 ->                 // 密码错误
                R.string.error_03

            4 ->                 // 请先登陆
                R.string.error_04

            5 ->                 // 已经在书架
                R.string.error_05

            6 ->                 // 书架已满
                R.string.error_06

            7 ->                 // 小说不在书架
                R.string.error_07

            8 ->                 // 回复帖子主题不存在
                R.string.error_08

            9 ->                 // 签到失败
                R.string.error_09

            10 ->                 // 推荐失败
                R.string.error_10

            11 ->                 // 帖子发送失败
                R.string.error_11

            22 ->                 // refer page 0
                R.string.error_22

            else ->                 // unknown
                R.string.error_unknown
        }
    }

    /**
     * This part are the old API writing ways.
     * It's not efficient enough, and maybe bug-hidden.
     */
    @VisibleForTesting
    fun getEncryptedMAP(str: String?): Map<String?, String?>? {
        val params: Map<String?, String?> = HashMap()
        params.put("appver", BuildConfig.VERSION_NAME)
        params.put("request", LightBase64.EncodeBase64(str))
        params.put("timetoken", "" + System.currentTimeMillis())
        return params
    }

    private fun getEncryptedCV(str: String?): ContentValues? {
        val cv = ContentValues()
        val map = getEncryptedMAP(str)
        for (key in map.keySet()) {
            // better than running encryption again
            cv.put(key, map.get(key))
        }
        return cv
    }

    fun getNovelCover(aid: Int): ContentValues? {
        // get the aid, and return a "jpg" file or other, in binary
        // not using this because UIL does not support post to get image
        return getEncryptedCV("action=book&do=cover&aid=$aid")
    }

    fun getNovelShortInfo(aid: Int, l: LANG?): ContentValues? {
        // get short XML info of a novel, here is an example:
        // --------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <metadata>
        // <data name="Title" aid="1305"><![CDATA[绝对双刃absolute duo]]></data>
        // <data name="Author" value="柊★巧"/>
        // <data name="BookStatus" value="0"/>
        // <data name="LastUpdate" value="2014-10-01"/>
        // <data
        // name="IntroPreview"><![CDATA[　　「焰牙」——那是藉由超化之后的精神力将自身灵...]]></data>
        // </metadata>
        return getEncryptedCV("action=book&do=info&aid=" + aid + "&t=" + getLANG(l))
    }

    fun getNovelShortInfoUpdate_CV(aid: Int, l: LANG?): ContentValues? {
        // action=book&do=bookinfo&aid=3&t=1 //小说信息（升级版）
        return getEncryptedCV("action=book&do=bookinfo&aid=" + aid + "&t=" + getLANG(l))
    }

    fun getNovelFullIntro(aid: Int, l: LANG?): ContentValues? {
        // get full XML intro of a novel, here is an example:
        // --------------------------------------------------
        // 　　在劍與魔法作為一股強大力量的世界裡，克雷歐過著只有繪畫是唯一生存意義的孤獨生活。
        // 　　不過生於名門的他，為了取得繼承人資格必須踏上試煉之旅。
        // 　　踏入禁忌森林的他，遇見一名半人半植物的魔物。
        // 　　輕易被抓的克雷歐設法勾起少女的興趣得到幫助，卻又被她當成寵物一般囚禁起來。
        // 　　兩人從此展開不可思議的同居時光，這樣的生活令他感到很安心。
        // 　　但平靜的日子沒有持續太久……
        // 　　描繪人與魔物的戀情，溫暖人心的奇幻故事。
        return getEncryptedCV("action=book&do=intro&aid=" + aid + "&t="
                + getLANG(l))
    }

    fun getNovelFullMeta(aid: Int, l: LANG?): ContentValues? {
        // get full XML metadata of a novel, here is an example:
        // -----------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <metadata>
        // <data name="Title"
        // aid="1306"><![CDATA[向森之魔物献上花束(向森林的魔兽少女献花)]]></data>
        // <data name="Author" value="小木君人"/>
        // <data name="DayHitsCount" value="26"/>
        // <data name="TotalHitsCount" value="43984"/>
        // <data name="PushCount" value="1735"/>
        // <data name="FavCount" value="848"/>
        // <data name="PressId" value="小学馆" sid="10"/>
        // <data name="BookStatus" value="已完成"/>
        // <data name="BookLength" value="105985"/>
        // <data name="LastUpdate" value="2012-11-02"/>
        // <data name="LatestSection" cid="41897"><![CDATA[第一卷 插图]]></data>
        // </metadata>
        return getEncryptedCV("action=book&do=meta&aid=" + aid + "&t="
                + getLANG(l))
    }

    fun getNovelIndex(aid: Int, l: LANG?): ContentValues? {
        // get full XML index of a novel, here is an example:
        // --------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <package>
        // <volume vid="41748"><![CDATA[第一卷 告白于苍刻之夜]]>
        // <chapter cid="41749"><![CDATA[序章]]></chapter>
        // <chapter cid="41750"><![CDATA[第一章「去对我的『楯』说吧——」]]></chapter>
        // <chapter cid="41751"><![CDATA[第二章「我真的对你非常感兴趣」]]></chapter>
        // <chapter cid="41752"><![CDATA[第三章「揍我吧！」]]></chapter>
        // <chapter cid="41753"><![CDATA[第四章「下次，再来喝苹果茶」]]></chapter>
        // <chapter cid="41754"><![CDATA[第五章「这是约定」]]></chapter>
        // <chapter cid="41755"><![CDATA[第六章「你的背后——由我来守护！」]]></chapter>
        // <chapter cid="41756"><![CDATA[第七章「茱莉——爱交给你！」]]></chapter>
        // <chapter cid="41757"><![CDATA[尾声]]></chapter>
        // <chapter cid="41758"><![CDATA[后记]]></chapter>
        // <chapter cid="41759"><![CDATA[插图]]></chapter>
        // </volume>
        // <volume vid="45090"><![CDATA[第二卷 谎言、真相与赤红]]>
        // <chapter cid="45091"><![CDATA[序章]]></chapter>
        // <chapter cid="45092"><![CDATA[第一章「莉莉丝·布里斯托」]]></chapter>
        // <chapter cid="45093"><![CDATA[第二章「借你的话来说就是……」]]></chapter>
        // <chapter cid="45094"><![CDATA[第三章「这真是个好提议」]]></chapter>
        // <chapter cid="45095"><![CDATA[第四章「如守护骑士一般」]]></chapter>
        // <chapter cid="45096"><![CDATA[第五章「『咬龙战』，开始！」]]></chapter>
        // <chapter cid="45097"><![CDATA[第六章「超越人类的存在」]]></chapter>
        // <chapter cid="45098"><![CDATA[第七章「『灵魂』」]]></chapter>
        // <chapter cid="45099"><![CDATA[尾声]]></chapter>
        // <chapter cid="45100"><![CDATA[后记]]></chapter>
        // <chapter cid="45105"><![CDATA[插图]]></chapter>
        // </volume>
        // ...... ......
        // </package>
        return getEncryptedCV("action=book&do=list&aid=" + aid + "&t="
                + getLANG(l))
    }

    fun getNovelContent(aid: Int, cid: Int, l: LANG?): ContentValues? {
        // get full content of an article of a novel,
        // the images should be processed then, here is an example:
        // --------------------------------------------------------
        // 第一卷 告白于苍刻之夜 插图
        // ...... ......
        // <!--image-->http://pic.wenku8.cn/pictures/1/1305/41759/50471.jpg<!--image-->
        // <!--image-->http://pic.wenku8.cn/pictures/1/1305/41759/50472.jpg<!--image-->
        // <!--image-->http://pic.wenku8.cn/pictures/1/1305/41759/50473.jpg<!--image-->
        // ...... ......
        return getEncryptedCV("action=book&do=text&aid=" + aid + "&cid=" + cid
                + "&t=" + getLANG(l))
    }

    // ##########
    // # Here test: action=book&do=vote&aid=1239 //推荐小说
    // # (就是网站上面那个喜欢小说 就推一下那个，app日限制5次/需要登录账号)
    // ##########
    // ReqTest07 = ''
    // #return getResult( ReqTest07, True );
    fun searchNovelByNovelName(novelName: String?, l: LANG?): ContentValues? {
        // get a list of search results, here is an example:
        // Note: there are extra line-break.
        // -------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <result>
        // <item aid='1699'/>
        // <item aid='1638'/>
        // <item aid='1293'/>
        // <item aid='977'/>
        // <item aid='693'/>
        // <item aid='993'/>
        // <item aid='333'/>
        // <item aid='499'/>
        // <item aid='826'/>
        // </result>
        return getEncryptedCV(("action=search&searchtype=articlename&searchkey="
                + LightNetwork.encodeToHttp(novelName)) + "&t=" + getLANG(l))
    }

    fun searchNovelByAuthorName(authorName: String?,
                                l: LANG?): ContentValues? {
        // get a list of search results.
        // Note: there are extra line-break.
        return getEncryptedCV(("action=search&searchtype=author&searchkey="
                + LightNetwork.encodeToHttp(authorName)) + "&t=" + getLANG(l))
    }

    fun getNovelList(n: NOVELSORTBY?, page: Int): ContentValues? {
        // here get a specific list of novels, sorted by NOVELSORTBY
        // ---------------------------------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <result>
        // <page num='166'/>
        // <item aid='1143'/>
        // <item aid='1034'/>
        // <item aid='1213'/>
        // <item aid='1'/>
        // <item aid='1011'/>
        // <item aid='1192'/>
        // <item aid='433'/>
        // <item aid='47'/>
        // <item aid='7'/>
        // <item aid='374'/>
        // </result>
        return getEncryptedCV("action=articlelist&sort=" + getNOVELSORTBY(n)
                + "&page=" + page)
    }

    fun getNovelListWithInfo(n: NOVELSORTBY?, page: Int, l: LANG?): ContentValues? {
        // get novel list with info digest
        // -------------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <result>
        // <page num='166'/>
        //
        // <item aid='1034'>
        // <data name='Title'><![CDATA[恶魔高校DxD(High School DxD)]]></data>
        // <data name='TotalHitsCount' value='2316361'/>
        // <data name='PushCount' value='153422'/>
        // <data name='FavCount' value='14416'/>
        // <data name='Author' value='xxx'/>
        // <data name='BookStatus' value='xxx'/>
        // <data name='LastUpdate' value='xxx'/>
        // <data name='IntroPreview' value='xxx'/>
        // </item>
        // ...... ......
        // </result>
        return getEncryptedCV("action=novellist&sort=" + getNOVELSORTBY(n)
                + "&page=" + page + "&t=" + getLANG(l))
    }

    fun getLibraryList(): ContentValues? {
        // return an XML file, once get the "sort id",
        // call getNovelListByLibrary
        // --------------------------
        // <?xml version="1.0" encoding="utf-8"?>
        // <metadata>
        // <item sort="1">电击文库</item>
        // <item sort="2">富士见文库</item>
        // <item sort="3">角川文库</item>
        // <item sort="4">MF文库J</item>
        // <item sort="5">Fami通文库</item>
        // <item sort="6">GA文库</item>
        // <item sort="7">HJ文库</item>
        // <item sort="8">一迅社</item>
        // <item sort="9">集英社</item>
        // <item sort="10">小学馆</item>
        // <item sort="11">讲谈社</item>
        // <item sort="12">少女文库</item>
        // <item sort="13">其他文库</item>
        // <item sort="14">游戏剧本</item>
        // </metadata> '''; # action=xml&item=sort&t=0
        return getEncryptedCV("action=xml&item=sort&t=0")
    }

    fun getNovelListByLibrary(sortId: Int, page: Int): ContentValues? {
        // sortId is from "getLibraryList" above
        return getEncryptedCV("action=articlelist&sort=" + sortId + "&page="
                + page)
    }

    fun getNovelListByLibraryWithInfo(sortId: Int,
                                      page: Int, l: LANG?): ContentValues? {
        // sortId is from "getLibraryList" above
        return getEncryptedCV("action=novellist&sort=" + sortId + "&page="
                + page + "&t=" + getLANG(l))
    }

    /*
     * I rewrite part of the APIs to get the best performance.
     * The old APIs are above, and use HttpRequest.
     * This part uses AFinal and that's more efficient.
     */
    fun getUserLoginParams(username: String?, password: String?): ContentValues? {
        // By username.
        val temp = "action=login&username=" + LightNetwork.encodeToHttp(username) + "&password=" + LightNetwork.encodeToHttp(password)
        return getEncryptedCV(temp)
    }

    fun getUserLoginEmailParams(email: String?, password: String?): ContentValues? {
        // By email.
        val temp = "action=loginemail&username=" + LightNetwork.encodeToHttp(email) + "&password=" + LightNetwork.encodeToHttp(password)
        return getEncryptedCV(temp)
    }

    fun getUserAvatar(): ContentValues? {
        // return jpeg raw data
        return getEncryptedCV("action=avatar")
    }

    fun getUserLogoutParams(): ContentValues? {
        return getEncryptedCV("action=logout")
    }

    fun getUserInfoParams(): ContentValues? {
        /*
         * <?xml version="1.0" encoding="utf-8"?>
         * <metadata>
         * <item name="uname"><![CDATA[apptest]]></item>
         * <item name="nickname"><![CDATA[apptest]]></item>
         * <item name="score">10</item>
         * <item name="experience">10</item>
         * <item name="rank"><![CDATA[新手上路]]></item>
         * </metadata>
         */
        return getEncryptedCV("action=userinfo")
    }

    fun getUserSignParams(): ContentValues? {
        /*
         * _cb({"ret":0});
         */
        return getEncryptedCV("action=block&do=sign") // 增加一个积分/天
    }

    fun getVoteNovelParams(aid: Int): ContentValues? {
        // 推荐小说  (就是网站上面那个喜欢小说 就推一下那个，app日限制5次/需要登录账号)
        return getEncryptedCV("action=book&do=vote&aid=$aid")
    }

    fun getBookshelfListAid(l: LANG?): ContentValues? {
        // 查询书架列表，只含有aid

        /*
         * <?xml version="1.0" encoding="utf-8"?>
         * <metadata>
         *     <book aid="1499" />
         *     <book aid="1754" />
         *     <book aid="1605" />
         *     <book aid="1483" />
         *     <book aid="1469" />
         *     <book aid="1087" />
         * </metadata>
         */
        return getEncryptedCV("action=bookcase&do=list&t=" + getLANG(l))
    }

    fun getBookshelfListParams(l: LANG?): ContentValues? {
        // 查询书架列表

        // find "aid", find first \" to second \"
        /*
         * <?xml version="1.0" encoding="utf-8"?>
         * <metadata>
         *
         * <book aid="1499" date="2015-04-19">
         * <name><![CDATA[時鐘機關之星Clockwork Planet]]></name>
         * <chapter cid="64896"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * <book aid="1754" date="2014-12-05">
         * <name><![CDATA[貓耳天使與戀愛蘋果]]></name>
         * <chapter cid="60552"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * <book aid="1605" date="2014-05-06">
         * <name><![CDATA[驚悚文集]]></name>
         * <chapter cid="54722"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * <book aid="1483" date="2013-08-24">
         * <name><![CDATA[茉建寺埃莉諾的非主流科學研究室]]></name>
         * <chapter cid="49057"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * <book aid="1469" date="2013-08-05">
         * <name><![CDATA[塔京靈魂術士]]></name>
         * <chapter cid="48537"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * <book aid="1087" date="2013-05-15">
         * <name><![CDATA[我的她是戰爭妖精]]></name>
         * <chapter cid="46779"><![CDATA[插圖]]></chapter>
         * </book>
         *
         * </metadata>
         */
        return getEncryptedCV("action=bookcase&t=" + getLANG(l))
    }

    fun getAddToBookshelfParams(aid: Int): ContentValues? {
        // 新增书架 aid为文章ID
        return getEncryptedCV("action=bookcase&do=add&aid=$aid")
    }

    fun getDelFromBookshelfParams(aid: Int): ContentValues? {
        // 删除书架 aid为文章ID
        return getEncryptedCV("action=bookcase&do=del&aid=$aid")
    }

    /**
     * Search bad words from the input string
     * @param source input string
     * @return null if not found; otherwise the bad word is returned
     */
    @Nullable
    fun searchBadWords(source: String?): String? {
        // remove all space
        var source = source
        source = source.replaceAll("\\s", "")

        // traverse bad words
        for (badWord in badWords) {
            if (source.contains(badWord)) {
                return badWord
            }
        }
        return null
    }

    fun getCommentListParams(aid: Int, page: Int): ContentValues? {
        // 书评列表, aid为文章ID, page不得为空（从1开始）
        var page = page
        if (page < 1) page = 1
        return getEncryptedCV("action=review&do=list&aid=" + aid + "&page=" + page + "&t=" + getLANG(GlobalConfig.getCurrentLang()))
    }

    fun getCommentContentParams(rid: Int, page: Int): ContentValues? {
        // 书评内容, rid为主题ID（不是aid）, page不得为空
        var page = page
        if (page < 1) page = 1
        return getEncryptedCV("action=review&do=show&rid=" + rid + "&page=" + page + "&t=" + getLANG(GlobalConfig.getCurrentLang()))
    }

    fun getCommentNewThreadParams(aid: Int, title: String?, content: String?): ContentValues? {
        // 书评发帖 aid为文章ID
        // 书评那边限制不少于7个中文字符才可发送，每次发送间隔10s以上
        // 采用简体！

        // 需要敏感词过滤，特殊符号处理
        return getEncryptedCV(("action=review&do=post&aid=" + aid
                + "&title=" + LightBase64.EncodeBase64(LightNetwork.encodeToHttp(title, "GBK"))
                ) + "&content=" + LightBase64.EncodeBase64(LightNetwork.encodeToHttp(content, "GBK")))
    }

    fun getCommentReplyParams(rid: Int, content: String?): ContentValues? {
        // 书评回帖 rid为主题ID
        // 书评那边限制不少于7个中文字符才可发送，每次发送间隔10s以上
        // 采用简体！

        // 需要敏感词过滤，特殊符号处理

        // Server-side bug, only GBK worked.
        return getEncryptedCV("action=review&do=reply&rid=" + rid
                + "&content=" + LightBase64.EncodeBase64(LightNetwork.encodeToHttp(content, "GBK")))
    }

    /*
     * Basic converter functions
     */
    enum class LANG {
        SC,  // simplified Chinese
        TC // traditional Chinese
    }

    enum class STATUS {
        FINISHED,  // novel's publishing finished
        NOT_FINISHED // novel's publishing not finished
    }

    enum class NOVELSORTBY {
        // sort arguments:
        // allvisit 总排行榜; allvote 总推荐榜; monthvisit 月排行榜; monthvote 月推荐榜;
        // weekvisit 周排行榜; weekvote 周推荐榜; dayvisit 日排行榜; dayvote 日推荐榜;
        // postdate 最新入库; lastupdate 最近更新; goodnum 总收藏榜; size 字数排行;
        // fullflag 完结列表
        allVisit, allVote, monthVisit, monthVote, weekVisit, weekVote, dayVisit, dayVote, postDate, lastUpdate, goodNum, size, fullFlag
    }
}