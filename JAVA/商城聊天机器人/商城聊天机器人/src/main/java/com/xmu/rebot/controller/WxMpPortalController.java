package com.xmu.rebot.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.xmu.rebot.wechat.service.WeixinService;

import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
@RestController
@RequestMapping("/wechat/portal")
public class WxMpPortalController {
  @Autowired
  private WeixinService wxService;
  
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @ResponseBody
  @GetMapping(produces = "text/plain;charset=utf-8")
  //@GetMapping
  public String authGet(@RequestParam(name = "signature", required = false) String signature,
      @RequestParam(name = "timestamp", required = false) String timestamp,
      @RequestParam(name = "nonce", required = false) String nonce,
      @RequestParam(name = "echostr", required = false) String echostr) {

	
	  
	 System.out.println( signature+" "+ timestamp+" "+ nonce+" "+ echostr+"||||");
	 System.out.println("收到微信请求");
    if (StringUtils.isAnyBlank(signature, timestamp, nonce, echostr)) {
    	//System.out.println("请求参数非法，请核实!");
    	return "请求参数非法，请核实";
     // throw new IllegalArgumentException("请求参数非法，请核实!");
    }

    if (this.getWxService().checkSignature(timestamp, nonce, signature)) {
    
    	
      return echostr;
    }

  
    System.out.println(echostr);
    return echostr;
   // return "非法请求";
  }

  @ResponseBody
  @PostMapping(produces = "application/xml; charset=UTF-8")
  public String post(@RequestBody String requestBody, @RequestParam("signature") String signature,
      @RequestParam(name = "encrypt_type", required = false) String encType,
      @RequestParam(name = "msg_signature", required = false) String msgSignature,
      @RequestParam("timestamp") String timestamp, @RequestParam("nonce") String nonce) {
	/*
	  try {
		requestBody=new String(requestBody.getBytes("ISO-8859-1"),"UTF-8");
	} catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    */
	  this.logger.info(requestBody);
    if (!this.wxService.checkSignature(timestamp, nonce, signature)) {
      throw new IllegalArgumentException("非法请求，可能属于伪造的请求！");
    }

    String out = null;
    if (encType == null) {
      // 明文传输的消息
    	//修改微信的消息格式 ，方便解析
      WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(requestBody);
      
      WxMpXmlOutMessage outMessage = this.getWxService().route(inMessage);
      if (outMessage == null) {
        return "";
      }

      out = outMessage.toXml();
    } else if ("aes".equals(encType)) {
      // aes加密的消息
      WxMpXmlMessage inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody,
          this.getWxService().getWxMpConfigStorage(), timestamp, nonce, msgSignature);
		this.logger.debug("\n消息解密后内容为：\n{} ", inMessage.toString());
      WxMpXmlOutMessage outMessage = this.getWxService().route(inMessage);
      if (outMessage == null) {
        return "";
      }

      out = outMessage.toEncryptedXml(this.getWxService().getWxMpConfigStorage());
    }

    this.logger.debug("\n组装回复信息：{}", out);

    return out;
  }
  


  protected WeixinService getWxService() {
    return this.wxService;
  }

}
