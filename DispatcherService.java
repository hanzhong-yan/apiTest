package com.mobimtech.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mobimtech.util.Config;
import com.mobimtech.util.ConfigInfo;
import com.mobimtech.util.Env;
import com.mobimtech.util.FunctionUtil;
import com.mobimtech.util.RequestParamUtil;
import com.mobimtech.util.ResponseParamUtil;
import com.mobimtech.vo.GameInfoVo;
import com.mobimtech.vo.GameVersionVo;
import com.mobimtech.vo.RequestParamVo;

@Service
public class DispatcherService {
	@Autowired
	AccountService accountService;
	@Autowired
	HomePageService homePageService;
	@Autowired
	RoomService roomService;
	@Autowired
	UserService userService;
	@Autowired
	GiftService giftService;
	@Autowired
	VersionService versionService;
	@Autowired
	ActivityService activityService;
	@Autowired
	MassService massService;
	@Autowired
	EmceeInfoService emceeInfoService;
	@Autowired
	MyImifunService myImifunService;
	@Autowired
	PayService payService;
	@Autowired
	AwardService awardService;
	@Autowired
	MissionService missionService;
	@Autowired
	RankService rankService;
	@Autowired
	SystemPropsService systemPropsService;
	@Autowired
	SignService signService;
	@Autowired
	SongService songService;
	@Autowired
	RemindService remindService;
	@Autowired
	PhotoCommentService photoCommentService;
	@Autowired
	PhotoService photoService;
	@Autowired
	CustomerService customerService;
	@Autowired
	DynamicService dynamicService;
	@Autowired
	EmceeSpaceService emceeSpaceService;
	@Autowired
	QihooService qihooService;
	@Autowired
	QihooPayService qihooPayService;
	@Autowired
	KuaiboService kuaiboService;
	@Autowired
	KuaiboXmlService kuaiboXmlService;
	@Autowired
	SwitchService switchService;
	@Autowired
	EggService eggService;
	@Autowired
	UserCarService userCarService;
	@Autowired
	FruitGameService fruitGameService;
	@Autowired
	GameCenterService gameCenterService;
	@Autowired
	MessageService messageService;
	@Autowired
	DaemonService daemonService ; 
	@Autowired
	CharmService charmService;
	@Autowired
	WxPayService wxPayService;
	@Autowired
	UpmpPayService upmpPayService;
	@Autowired
	WeeklyVoiceService weeklyVoiceService;
	@Autowired
	YeePayService yeePayService;
	
	private static Logger log = Logger.getLogger(DispatcherService.class);
	
	private static final int[] acidCfg = {2109,2110,2111,2112,2113,2114,2115,2116,2117,2118,2119,2121};
	
	public void dispatch(HttpServletRequest request, HttpServletResponse response){
		String jsonStr = "";
		//Compatible handler
		RequestParamVo rpv = RequestParamUtil.getRequestParamVo(request);
		//for old version like v2.3,2.4, there is no VERS parameter. so it can not access server again
		//(will be force upgraded to v2.4.1 and later)
		int acid = Integer.parseInt(rpv.getACID());
		log.info("request acid = " + acid);
		if(Config.ENV != Env.PRODUCT){
			ResponseParamUtil.threadLocal.set(acid);//设置acid到线程变量中，供上下文使用，目前是打印返回结果中使用
		}
		
		if(rpv.getVERS().length() == 0){//no version
			log.info("acid = " + acid);
			//1009 is accessible always to make force upgrade available.
			//for 1010, client can not get version(it's service). so filter it out.
			if(acid != 1009 && acid != 1010){
				ResponseParamUtil.writeJsonMessage(response,RequestParamUtil.operationFailed(501,
						ConfigInfo.getString("system_unresponse"),""));
				return;
			}
		}
		try {
			//log client request
			jsonStr = RequestParamUtil.getRequestContents(request);
			log.info("client request[acid="+acid+"]:{" + jsonStr + "}");
			
			//根据接口要求，判断用户是否已经登陆
			if(isNeedLogin(acid)){
				//0=可以正常登陆       1=session 超时   2=sessionId为null请重新登陆 (未登陆 重新登陆)
				String sessionId = request.getHeader("sessionId");
				Map<String,String> params = BaseService.getRequestParams(jsonStr);
				int senderId = 0;
				if(params.containsKey("uid")){
					senderId = Integer.parseInt(params.get("uid"));
				}
				if(params.containsKey("uId")){
					senderId = Integer.parseInt(params.get("uId"));
				}
				if(params.containsKey("userId")){
					senderId = Integer.parseInt(params.get("userId"));
				}
				if(senderId > 0 ){
					int temp = userService.isUserLogin(sessionId,senderId);
					if(temp==1){
						//session 超时
						ResponseParamUtil.writeJsonMessage(response,RequestParamUtil.operationFailed(401,
								ConfigInfo.getString("session_timeout"),params.get("requeststamp")));
						return;
					}else if(temp != 0) {
						//sessionId为null请重新登陆 (未登陆 重新登陆)
						ResponseParamUtil.writeJsonMessage(response,RequestParamUtil.operationFailed(10032,
								ConfigInfo.getString("passeword_error"),params.get("requeststamp")));
						return ;
					}
				}
			}
		} catch (UnsupportedEncodingException e) {
			ResponseParamUtil.writeJsonMessage(response,RequestParamUtil.operationFailed(204,"请求消息体为空，请检查！",""));
			return;
		}
		
		
		//begin dispatching for different action Id
		switch (acid) {
		case 1001: //register account
			accountService.register(request, response,jsonStr);
			break;
		case 1002: //login
			accountService.loginUser(request, response, jsonStr);
			break;
		case 1003: //iso 2.2.1 首页
			homePageService.homePage(request, response, jsonStr);
			break;
		case 1004: //enter room
			roomService.enterRoom(request,response, jsonStr);
			break;
		case 1005: //get user info: not self
			userService.getUserInfo(request, response, jsonStr);
			break;
		case 1006:
			giftService.sendGift(request, response, jsonStr);
			break;
		case 1007://member list
			roomService.getMemberList(request, response, jsonStr);
			break;
		case 1008: //修改昵称/头像/性别/所在地
			userService.modifyUserInfo(request, response, jsonStr);
			break;
		case 1009: //检测更新
			versionService.detectUpdate(request, response, jsonStr);
			break;
		case 1010://活动开播提醒，复用以前的代码
			roomService.roomStartRemind(request, response, jsonStr);
			break;
		case 1013: //活动预告:do not use any more
			activityService.activityNotice(request, response, jsonStr);
			break;
		case 1014: //活动详细页:do not use any more
			activityService.getActivityDetail(request, response,jsonStr);
			break;
		case 1015: //预约报名:do not use any more
			activityService.order(request, response, jsonStr);
			break;
		case 1017: //粉丝列表 关注列表
			userService.getFansList(request, response, jsonStr);
			break;
		case 1018: //社团首页
			massService.massHomePage(request, response, jsonStr);
			break;
		case 1019: //获取分类主播：明星主播直播/人气新秀直播/精彩推荐直播: only used by iso
			emceeInfoService.getEmceeByCategory(request, response, jsonStr);
			break;
		case 1020: //查看我的资料 self
			userService.getSelfInfo(request, response, jsonStr);
			break;
		case 1022: //我的艾米范:历史切页
			myImifunService.getHistory(request, response, jsonStr);
			break;
		case 1023: //我的艾米范:直播切页,关注的正在直播的主播
			myImifunService.getFollowLive(request, response, jsonStr);
			break;
		case 1024: //我的艾米范:活动切页: maybe only be used by ios. Android doesn't use it any more.
			myImifunService.getActivity(request, response, jsonStr);
			break;
		case 1025: //关注
			userService.follow(request, response, jsonStr);
			break;
		case 1026: //cancel follow
			userService.cancelFollow(request, response, jsonStr);
			break;
		case 1029: //客户端神州付充值请求
			payService.shenzhoufuRequest(request, response, jsonStr);
			break;
		case 1030: //客户端支付宝充值请求
			payService.alipayRequest(request, response, jsonStr);
			break;
		case 1031: //神州付 回调的url
			payService.shenzhoufuCallback(request, response,jsonStr);
			break;
		case 1033: //获取剩余金豆和充值比例
			userService.getRemainMoney(request, response, jsonStr);
			break;
		case 1034: //获取用户最新信息
			userService.getUserNewestInfo(request, response, jsonStr);
			break;
		case 1036://每日领金豆：only used by iso
			awardService.getDailyAward(request, response, jsonStr);
			break;
		case 1037: //获取用户任务列表（包括状态）
			missionService.getUserMission(request, response, jsonStr);
			break;
		case 1038: //新手任务领奖
			missionService.getMissionAward(request, response, jsonStr);
			break;
		case 1039: //小时粉丝榜接口
			rankService.getHourRank(request, response, jsonStr);
			break;
		case 1040: //QQ/新浪微博登陆接口
			accountService.thirdPartyLogin(request, response, jsonStr);
			break;
		case 1042: //查询购买vip的价格接口
			systemPropsService.getVipInfo(request, response, jsonStr);
			break;
		case 1043: //购买vip接口
			systemPropsService.buyVip(request, response, jsonStr);
			break;
		case 1044: //ios充值
			payService.isoPayRequest(request, response, jsonStr);
			break;
		case 1045: //取得签到列表
			signService.getUserSignInfo(request, response, jsonStr);
			break;
		case 1046: //签到/补签
			signService.doSign(request, response, jsonStr); 
			break;
		case 1047: //得到累积签到礼物
			signService.getAdditionalAward(request, response, jsonStr);
			break;
		case 1048: // 排行榜:明星榜/富豪榜[日榜，周榜和月榜] 
			rankService.getRankList(request, response, jsonStr);
			break;
		case 1049:
			songService.getSongList(request, response, jsonStr);
			break;
		case 1050: //点歌
			songService.orderSong(request, response, jsonStr);
			break;
		case 1051:
			giftService.getStorageGift(request, response, jsonStr);
			break;
		case 1052:
			rankService.getWeekFanList(request, response, jsonStr);
			break;		
		case 1054: //android 2.5 首页
			homePageService.vhomePage(request, response, jsonStr);
			break;
		case 1055: //新手任务是否全部完成
			missionService.areAllMissionsFinished(request, response, jsonStr);
			break;
		case 1056: //有无消息提醒 used only by 2.5/2.2.1
			remindService.hasNewRemind(request, response, jsonStr);
			break;
		case 1057: //used only by 2.5/2.2.1
			remindService.getRemind(request, response, jsonStr);
			break;		
		case 1059: //照片评论
			photoCommentService.addPhotoComment(request, response, jsonStr);
			break;
		case 1060: //获取照片评论列表
			photoCommentService.getPhotoComments(request, response, jsonStr);
			break;
		case 1061: //获取照片列表 only used by ios. it should get by page
			photoService.getPhotoList(request, response, jsonStr);
			break;
		case 1062: // 获取礼物列表信息
			giftService.getGiftList(request, response, jsonStr);
			break;
		case 1063: // 密码修改
			userService.modifyPwd(request, response, jsonStr);
			break;
		case 1064: //qq登录
			accountService.qzoneLogin(request, response, jsonStr);
			break;
		case 1065: //ios举报
			customerService.iosReport(request, response, jsonStr);
			break;
		case 1066: //native礼物列表（会有gif图片）only used by native version
			giftService.getNativeGiftList(request, response, jsonStr);
			break;
		case 1067: //照片喜欢
			photoService.photoLove(request, response, jsonStr);
			break;
		case 1068: //照片送玫瑰
			photoService.sendRose(request, response, jsonStr);
			break;
		case 1069: //动态信息获取接口 ((人气魅图 ivp4.0))
			dynamicService.getDynamic(request, response, jsonStr);
			break;
		case 1070: //获取单张照片鲜花排行
			photoService.getPhotoRoseRankList(request, response, jsonStr);
			break;
		case 1071: //获取单张照片详细记录
			photoService.getPhotoDetail(request, response, jsonStr);
			break;
		case 1072: //意见反馈
			customerService.userFeedback(request, response, jsonStr);
			break;
		case 1073: //主播空间
			emceeSpaceService.enter(request, response, jsonStr);
			break;
		case 1074: //消息提醒列表（1.新喜欢2.送玫瑰3.新评论）only used by 3.0
			remindService.getNewRemind(request, response, jsonStr);
			break;
		case 1075: //组合型首页数据获取接口
			homePageService.getCombinePage(request, response, jsonStr);
			break;
		case 1076: //qq充值 Q点支付
			payService.qqPayRequest(request, response, jsonStr);
			break;
		case 1077: //native版本开播活动提醒。兼容性考虑，1010接口不做修改。
			roomService.nativeRoomStartRemind(request, response, jsonStr);
	 		break;
		case 1078: //奇虎(360)登录
	 		accountService.qihooLogin(request, response, jsonStr);
	 		break;
	 	case 1079: //360充值
	 		qihooPayService.qihooRecharge(request, response, jsonStr);
	 		break;
	 	case 1081: //获取360 token
	 		qihooService.getQihooToken(request, response, jsonStr);
	 		break;
	 	case 1082: //获取360用户信息
	 		qihooService.getQihooUserInfo(request, response, jsonStr);
	 		break;
	 	case 1083: //获取快播 access token
	 		kuaiboService.getKuaiboAccessToken(request, response, jsonStr);
	 		break;
	 	case 1084: //获取快播用户信息
	 		kuaiboService.getKuaiboUserInfo(request, response, jsonStr);
	 		break;
	 	case 1085: // 快播登录
	 		accountService.kuaiboLogin(request, response, jsonStr);
	 		break;
	 	case 1086:  //快播充值，生成订单号
	 		kuaiboService.kuaiboRecharge(request, response, jsonStr);
	 		break;
	 	case 1087:  //获取房间榜单
			rankService.getAllFanListByRoomId(request, response, jsonStr);
	 		break; 
	 	case 1088: //快播修改用户资料
	 		userService.kuaiboModifyUserInfo(request, response, jsonStr);
	 		break;
	 	case 1089:
	 		switchService.getSwitchValue(request, response, jsonStr);
	 		break;
	 	case 1090: //获取带分类的礼物列表。注意：同一个礼物可以属于不同的分类
	 		giftService.getGiftListWithCategory(request, response, jsonStr);
	 		break;
	 	case 1091://获取活动信息
	 		activityService.getActivityConfig(request, response, jsonStr);
	 		break;
	 	case 1092:
	 		userService.saveUserData(request, response, jsonStr);
	 		break;
	 	case 1093:
	 		eggService.addTimes(request, response, jsonStr);
	 		break;
	 	case 1094:
	 		eggService.hitEgg(request, response, jsonStr);
	 		break;
	 	case 1096:
	 		userCarService.buyCar(request, response, jsonStr);
	 		break;
	 	case 1097:
	 		userCarService.changeCar(request, response, jsonStr);
	 		break;
	 	case 1098: //飞屏接口
	 		giftService.sendConfession(request, response,jsonStr);
	 		break;
	 	case 1099: //盖章接口
	 		giftService.seal(request, response,jsonStr);
	 		break;
	 	case 1100: //水果机仓库接口
	 		fruitGameService.getStorage(request, response,jsonStr);
	 		break;
	 	case 1101: //仓库接口
	 		giftService.showUserStorage(request, response,jsonStr);
	 		break;
	 	case 1102: //仓库中用户领取礼包
	 		giftService.withDrawUserPackage(request, response,jsonStr);
	 		break;
	 	case 2000: //IVP 4.0 主播搜索
	 		homePageService.searchEmcee(request, response,jsonStr);
	 		break;
	 	case 2001: //IVP 4.0 活动中心
	 		activityService.getAllActivity(request, response,jsonStr);
	 		break;
	 	case 2047: //IVP 4.0 排行榜索引
	 		rankService.getRankIndex(request, response,jsonStr);
	 		break;
	 	case 2048: //IVP 4.0 礼物之星排行榜
	 		rankService.getGiftStar(request, response,jsonStr);
	 		break;
	 	case 2049: //IVP 4.0 常规排行榜（包括 综合排行榜 ， 叫早女神榜 ， 有声有色榜 等）
	 		rankService.getNormalRankList(request, response,jsonStr);
	 		break;
	 	case 2068: //照片送红心
			photoService.sendLoyalHeart(request, response, jsonStr);
			break;
	 	case 2075: //IVP 4.0 直播大厅
	 		homePageService.getLiveLobby(request, response,jsonStr);
	 		break;
	 	case 2100: //IVP 4.0 发送消息 
	 		messageService.sendMessage(request, response,jsonStr);
	 		break;
	 	case 2101: //设置消息
	 		messageService.setup(request, response,jsonStr);
	 		break;
	 	case 2102: //获取消息设置
	 		messageService.querySetup(request, response,jsonStr);
	 		break;
	 	case 2103: //获取黑名单
	 		messageService.getBlacklist(request, response,jsonStr);
	 		break;
	 	case 2104://管理黑名单
	 		messageService.manageBlacklist(request, response, jsonStr);
	 		break;
	 	case 2105://举报骚扰
	 		messageService.reportHarass(request, response, jsonStr);
	 		break;
	 	case 2106: //IVP 4.0  消息系统-deviceToken上报
	 		messageService.reportDeviceToken(request, response,jsonStr);
	 		break;
	 	case 2107: //IVP 4.0  消息系统 手机上报接收到的消息
	 		messageService.reportReceivedMessage(request, response,jsonStr);
	 		break;
	 	case 2109: //购买守护
	 		daemonService.buyDaemon(request,response,jsonStr);
	 		break;
	 	case 2110: //获取守护列表
	 		daemonService.getDaemonList(request,response,jsonStr);
	 		break;
	 	case 2111: //删除评论
	 		photoCommentService.deleteComment(request,response,jsonStr);
	 		break;
	 	case 2112: //删除图片
	 		photoService.deletePhoto(request,response,jsonStr);
	 		break;
	 	case 2113: //在直播间获取图片
	 		photoService.getMyPhotosInRoom(request,response,jsonStr);
	 		break;
	 	case 2114: //在直播间发送图片
	 		photoService.publishMyPhotosInRoom(request,response,jsonStr);
	 		break;
	 	case 2115: //魅力提升指引
	 		charmService.charmUpIntro(request,response,jsonStr);
	 		break;
	 	case 2116: //魅力提升任务状态
	 		charmService.charmUpTaskStatus(request,response,jsonStr);
	 		break;
	 	case 2117: //获取用户vip保级、升级提示
	 		userService.getUserVipTipsInfo(request,response,jsonStr);
	 		break;
	 	case 2118: //微信支付
	 		wxPayService.wxRecharge(request,response,jsonStr);
	 		break;
	 	case 2119: //银联支付
	 		upmpPayService.upmpRecharge(request,response,jsonStr);
	 		break;
	 	case 2120: //新手引导 3选1 换主播
	 		homePageService.get3Choose1(request,response,jsonStr);
	 		break;
	 	case 2121: //获取新手引导 奖励
	 		userService.getReward(request,response,jsonStr);
	 		break;
	 	case 2122: //直播间签到查询
	 		roomService.roomSignQuery(request,response,jsonStr);
	 		break;
	 	case 2123: //直播间签到奖励领取
	 		roomService.roomSignReward(request,response,jsonStr);
	 		break;
	 	case 2124: //查询小财神剩余时间
	 		userService.queryGodWealthVal(request,response,jsonStr);
	 		break;
	 	case 2125: //显示靓号礼包中的靓号
	 		userService.showUserGoodNumPackage(request,response,jsonStr);
	 		break;
	 	case 2126: //使用靓号礼包
	 		userService.useUserGoodNumPackage(request,response,jsonStr);
	 		break;
		case 2127: //客户端支付宝充值请求 -new
			payService.alipayRequestNew(request, response, jsonStr);
			break;
		case 2128: //获取主播的每周之声任务 
			weeklyVoiceService.getWeeklyVoiceTask(request, response, jsonStr);
			break;
		case 2129: //获取每周之声的任务要求、范例等
			weeklyVoiceService.getWeeklyVoiceDetail(request, response, jsonStr);
			break;
		case 2130: //获取主播的录音清单
			weeklyVoiceService.getUserWeeklyVoiceList(request, response, jsonStr);
			break;
		case 2131: //绑定为微信号 
			userService.bindWeiXin(request, response, jsonStr);
			break;
		case 2132: //易宝支付请求
			yeePayService.payRequest(request, response, jsonStr);
			break;
		}
	}
	
	private boolean isNeedLogin(int acid) {
		if(acidCfg != null && acidCfg.length > 0){
			for (int i = 0; i < acidCfg.length; i++) {
				if(acidCfg[i] == acid) return true;
			}
		}
		return false;
	}

	//alipay callback handler
	public void onAlipayCallback(HttpServletRequest request, HttpServletResponse response){
		payService.alipayCallback(request, response);
	}
	
	//alipay callback handler
	public void onAlipayCallbackNew(HttpServletRequest request, HttpServletResponse response){
		payService.alipayCallbackNew(request, response);
	}
	
	//qihoo callback handler
	public void onQihooCallback(HttpServletRequest request, HttpServletResponse response) throws IOException{
		qihooPayService.handleCallback(request,response);
	}
	
	//kuaibo callback handler
	public void onKuaiboCallback(HttpServletRequest request, HttpServletResponse response) throws IOException{
		kuaiboService.handleCallback(request,response);
	}
	
	public void onKuaiboGetEmcee(HttpServletRequest request, HttpServletResponse response){
		String reqType = StringUtils.defaultIfEmpty(request.getParameter("type"),"0");
		int type = Integer.parseInt(reqType);
		
		kuaiboXmlService.getKuaiboEmceeXml(type,response);
	}
	
	
	public void onKuaiboGetOrderId(HttpServletRequest request, HttpServletResponse response){
		String jsonStr = "";
		try {
			//log client request
			jsonStr = RequestParamUtil.getRequestContents(request);
			log.info("kuaibo server request orderId:{" + jsonStr + "}");
		} catch (UnsupportedEncodingException e) {
			ResponseParamUtil.writeJsonMessage(response,RequestParamUtil.operationFailed(204,"请求消息体为空，请检查！",""));
			return;
		}
		
		kuaiboService.onKuaiboGetOrderId(request,response, jsonStr);
	}
	

	public void onKuaiboGetRate(HttpServletRequest request, HttpServletResponse response){
		kuaiboService.getKuaiboRate(response);
	}
	
	public void onKuaiboGetAmount(HttpServletRequest request, HttpServletResponse response){		
		kuaiboService.getKuaiboAmount(request,response);
	}
	
	
	/**
	 * 提供给运维人员设置提供给快播的主播人数及推荐主播池
	 * <pre>
	 * Author: hanzhong.yan
	 * @param request
	 * @param response
	 * Modifications:
	 * Modifier hanzhong.yan; 2014-4-30; Create new Method 
	 * </pre>
	 */
	public void onSetupKuaiboEmcee(HttpServletRequest request, HttpServletResponse response) {
		String suppliedEmceeNumber = request.getParameter("suppliedEmceeNumber");
		String recommendedEmceePool = request.getParameter("recommendedEmceePool");
		log.info(String.format("[onSetupKuaiboEmcee]:Received setup kuaibo emcee request , the request parameters are :[suppliedEmceeNumber=%s,recommendedEmceePool=%s]"
				, suppliedEmceeNumber,recommendedEmceePool));
		log.info(String.format("[onSetupKuaiboEmcee]:The full request parameter is :%s", request.getParameterMap().toString()));
		
		Map<String,String> currentStatus = new HashMap<String, String>();
		if(suppliedEmceeNumber !=null){
			try {
				int cfgedEmceeNumber = kuaiboService.setupEmceeNumber(suppliedEmceeNumber);
				if(cfgedEmceeNumber >= 0){
					currentStatus.put("INFO:", "设置提供给快播的在线主播人数成功！");
					currentStatus.put("INFO:提供给快播的在线主播人数(当前值):", cfgedEmceeNumber+"");
				}else{
					currentStatus.put("ERROR:", "设置提供给快播的在线主播人数失败！请联系开发人员。");
				}
			} catch (Exception e) {
				currentStatus.put("ERROR:", "设置提供给快播的在线主播人数失败！请联系开发人员。具体错误如下：" + e.getMessage());
			}
		}else{
			int emceeNumber = kuaiboService.getEmceeNumber();
			currentStatus.put("INFO:提供给快播的在线主播人数(当前值):", emceeNumber+"");
		}
		
		if(recommendedEmceePool != null){
			try {
				boolean succ = kuaiboService.setupEmceeRecommendPool(recommendedEmceePool.trim());
				if(succ){
					currentStatus.put("INFO:", "设置提供给快播的主播推荐池成功！");
					currentStatus.put("INFO:提供给快播的主播推荐池(当前值):", recommendedEmceePool);
				}else{
					currentStatus.put("ERROR:", "设置提供给快播的主播推荐池失败！请联系开发人员。");
				}
			} catch (Exception e) {
				currentStatus.put("ERROR:", "设置提供给快播的主播推荐池失败！请联系开发人员。具体错误如下：" + e.getMessage());
			}
		}else{
			recommendedEmceePool = kuaiboService.getEmceePool();
			currentStatus.put("INFO:提供给快播的主播池(当前值):", recommendedEmceePool);
		}
		
		StringBuilder outMsg = new StringBuilder("<html><body>");
		outMsg.append("E.G.::::/app/open/setupKuaiboEmcee?suppliedEmceeNumber=10&recommendedEmceePool=10061,10087").append("<br/>");
		if(currentStatus.size() > 0){
			for (Map.Entry<String, String> msg : currentStatus.entrySet()) {
				outMsg.append(msg.getKey()).append(msg.getValue()).append("<br/>");
			}
			outMsg.append("</body></html>");
		}
		ResponseParamUtil.writeHTMLMessage(response,outMsg.toString());
	}
	
	/**
	 * 返回所有的游戏数据
	 * <pre>
	 * Author: hanzhong.yan
	 * @param request
	 * @param response
	 * Modifications:
	 * Modifier hanzhong.yan; 2014-6-10; Create new Method 
	 * </pre>
	 */
	public void getAllGameInfo(HttpServletRequest request, HttpServletResponse response) {
		String jsonStr = "";
		Map<String,String> params = null;
		try {
			jsonStr = RequestParamUtil.getRequestContents(request);
			log.info("the params is " + jsonStr);
			params = BaseService.getRequestParams(jsonStr);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		int fromType = Integer.parseInt(org.apache.commons.lang3.StringUtils.defaultString(params.get("fromType"), "-1"));
		double version = Double.parseDouble(org.apache.commons.lang3.StringUtils.defaultString(params.get("version"), "4.0.0").replaceAll("\\.", ""));
		double latestVersion = Double.parseDouble("4.0.5".replaceAll("\\.", ""));
		List<GameInfoVo> allGameInfo = new ArrayList<GameInfoVo>();
		allGameInfo = gameCenterService.getAllGameInfo();
		List<GameInfoVo> filteredGames = new ArrayList<GameInfoVo>();//按照客户端的版本，对某些游戏过滤掉
		if(allGameInfo != null){
			for (GameInfoVo gameInfoVo : allGameInfo) {
				if(version < latestVersion  ){//满足过滤条件
					if(gameInfoVo.getApp_id() != 1 && gameInfoVo.getApp_id() < 1000)//小于4.0.5的版本，只显示水果机
						filteredGames.add(gameInfoVo);
				}else{//大于或等于4.0.5显示所有
					;
				}
				
				if(gameInfoVo.getApp_id() == 1000){//对于女神游戏，只有大于等于4.1.0的版本才可以显示
					if(version < 410 && fromType == 1110){ //官网的版本要求>=4.1
						filteredGames.add(gameInfoVo);
					}
					if(version < 420 && fromType == 1111){ //qq空间的版本要求>=4.2
						filteredGames.add(gameInfoVo);
					}
				}

				gameInfoVo.setApp_icons_img(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_icons_img(),true));
				gameInfoVo.setApp_img_1(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_img_1(),true));
				gameInfoVo.setApp_img_2(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_img_2(),true));
				gameInfoVo.setApp_img_3(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_img_3(),true));
				gameInfoVo.setApp_img_4(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_img_4(),true));
				gameInfoVo.setApp_img_5(FunctionUtil.assamblePhotoPath(gameInfoVo.getApp_img_5(),true));
			}
		}
		if(filteredGames.size() > 0){
			allGameInfo.removeAll(filteredGames);//过滤掉
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("allGameInfo", allGameInfo);
		JSONObject json = JSONObject.fromObject(data);
		ResponseParamUtil.writeJsonMessage(response, json.toString());
	}

	/**
	 * 获取游戏的版本信息
	 * <pre>
	 * Author: hanzhong.yan
	 * @param request
	 * @param response
	 * Modifications:
	 * Modifier hanzhong.yan; 2014-7-17; Create new Method 
	 * </pre>
	 */
	public void getGameUpgradeInfo(HttpServletRequest request, HttpServletResponse response) {
		Map<String,Object> result = new HashMap<String, Object>();
		int code = 200 ; 
		String errorMsg = "";
		String appId = request.getParameter("appId");
		String nativeVersion = request.getParameter("nativeVersion");
		String md5 = request.getParameter("nativeVersion");
		GameVersionVo gameVersion = null;
		if(StringUtils.isEmpty(appId)){
			code = 500;
			errorMsg = "invalid param of appId :can't be empty!";
		}else{
			try {
				gameVersion = gameCenterService.getGameUpgradeInfo(appId,nativeVersion,md5);
				if(gameVersion == null){
					code = 500;
					errorMsg = "There is not app info for " + appId;
				}
			} catch (Exception e) {
				code = 500 ; 
				errorMsg = e.getMessage();
			}
			
		}
		
		result.put("code", code);
		result.put("errorMsg", errorMsg);
		result.put("data", gameVersion);
		JSONObject json = JSONObject.fromObject(result);
		ResponseParamUtil.writeJsonMessage(response, json.toString());
	}
}
