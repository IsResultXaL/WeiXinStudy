package com.caogen.WeiXinStudy.service.impl;

import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.caogen.WeiXinStudy.entity.ImageMessage;
import com.caogen.WeiXinStudy.entity.ReportLocation;
import com.caogen.WeiXinStudy.entity.TextMessage;
import com.caogen.WeiXinStudy.service.CoreService;
import com.caogen.WeiXinStudy.util.BaiDuUtil;
import com.caogen.WeiXinStudy.util.MessageUtil;
import com.caogen.WeiXinStudy.util.RedisPool;
import com.caogen.WeiXinStudy.util.TemplateMessageUtil;
import com.caogen.WeiXinStudy.util.TuringRobots;

import redis.clients.jedis.Jedis;

@Service
public class CoreServiceImpl implements CoreService {

	/**
	 * 消息业务处理分发器
	 * 
	 * @param MsgType
	 * @throws Exception 
	 */
	@Override
	public String processMessage(Map<String, String> map) throws Exception {
		String reMessage = "";
		String MsgType = map.get("MsgType");

		String openid = map.get("FromUserName"); // 用户 openid
		String mpid = map.get("ToUserName"); // 公众号原始 ID

		switch (MsgType) {
		case MessageUtil.REQ_MESSAGE_TYPE_TEXT:
			TextMessage textMessage = new TextMessage();
			textMessage.setToUserName(openid);
			textMessage.setFromUserName(mpid);
			textMessage.setCreateTime(new Date().getTime());
			textMessage.setMsgType(MessageUtil.REQ_MESSAGE_TYPE_TEXT);
			textMessage.setContent(TuringRobots.dialogue(map.get("Content").toString()));
			reMessage = MessageUtil.textMessageToXml(textMessage);
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_IMAGE:
			reMessage = "<xml>"+
						"<ToUserName><![CDATA["+openid+"]]></ToUserName>" +
						"<FromUserName><![CDATA["+mpid+"]]></FromUserName>" +
						"<CreateTime>"+new Date().getTime()+"</CreateTime>" +
						"<MsgType><![CDATA[image]]></MsgType>" +
						"<Image><MediaId><![CDATA[GjqnHKOGrkbKj9FE2RSlkyM5XdMjP0qWv0ZtU3bBhiY]]></MediaId></Image>" + 
						"</xml>";
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_VOICE:
			reMessage = "语音消息";
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_VIDEO:
			reMessage = "视频消息";
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_SHORTVIDEO:
			reMessage = "小视频消息";
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_LOCATION:
			reMessage = "地理位置消息";
			break;
		case MessageUtil.REQ_MESSAGE_TYPE_LINK:
			reMessage = "链接消息";
			break;
		default:
			break;
		}

		return reMessage;
	}

	/**
	 * 事件消息业务分发器
	 * 
	 * @param Event
	 * @throws Exception 
	 */
	@Override
	public String EventDispatcher(Map<String, String> map) throws Exception {
		String reMessage = "";
		String Event = map.get("Event");

		String openid = map.get("FromUserName"); // 用户 openid
		String mpid = map.get("ToUserName"); // 公众号原始 ID
		
		switch (Event) {
		case MessageUtil.EVENT_TYPE_SUBSCRIBE:
			// 发送消息模板
			TemplateMessageUtil.ConcernedSuccess(openid);
			break;
		case MessageUtil.EVENT_TYPE_UNSUBSCRIBE:
			// 取消订阅
			break;
		case MessageUtil.EVENT_TYPE_LOCATION:
			//上报地理位置
			ReportLocation location = new ReportLocation();
			location.setFromUserName(openid);
			location.setLatitude(map.get("Latitude"));
			location.setLongitude(map.get("Longitude"));
			location.setPrecision(map.get("Precision"));
			
			location = BaiDuUtil.getBaiDuCoordinate(location);
			location = BaiDuUtil.getBaiDuAddress(location);
			//发送地理位置消息模板
			TemplateMessageUtil.ConcernedLocation(location);
			break;
		case MessageUtil.EVENT_TYPE_CLICK:
			/**
			 * 点击菜单拉取消息时的事件推送
			 * 根据key的通过发送不同的消息
			 */
			String EventKey = map.get("EventKey");
			TextMessage textMessage = new TextMessage();
			if(EventKey.equals("V1001_TODAY_JOKE")){
				Jedis jedis = RedisPool.getJedis();
				textMessage.setToUserName(openid);
				textMessage.setFromUserName(mpid);
				textMessage.setCreateTime(new Date().getTime());
				textMessage.setMsgType(MessageUtil.REQ_MESSAGE_TYPE_TEXT);
				textMessage.setContent(jedis.srandmember("joke")==null?"":jedis.srandmember("joke"));
				reMessage = MessageUtil.textMessageToXml(textMessage);
				RedisPool.close(jedis);
			}else if(EventKey.equals("V1001_GOOD")){
				textMessage.setToUserName(openid);
				textMessage.setFromUserName(mpid);
				textMessage.setCreateTime(new Date().getTime());
				textMessage.setMsgType(MessageUtil.REQ_MESSAGE_TYPE_TEXT);
				textMessage.setContent("谢谢您的赞！");
				reMessage = MessageUtil.textMessageToXml(textMessage);
			}
			break;
		case MessageUtil.EVENT_TYPE_SCAN:
			/**
			 * 扫描二维码的事件推送
			 * 根据key的通过发送不同的消息
			 */
			String EventKeyBySCAN = map.get("EventKey");
			TextMessage textMessageBySCAN = new TextMessage();
			textMessageBySCAN.setToUserName(openid);
			textMessageBySCAN.setFromUserName(mpid);
			textMessageBySCAN.setCreateTime(new Date().getTime());
			textMessageBySCAN.setMsgType(MessageUtil.REQ_MESSAGE_TYPE_TEXT);
			textMessageBySCAN.setContent("二维码推送！"+EventKeyBySCAN);
			reMessage = MessageUtil.textMessageToXml(textMessageBySCAN);
			break;
		default:
			break;
		}

		return reMessage;
	}

}
