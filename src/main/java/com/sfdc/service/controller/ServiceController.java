package com.sfdc.service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sfdc.service.helper.ServiceUtil;

@RestController
public class ServiceController {
	@GetMapping("/popupInfo")
	public String getPopupInfo(@RequestParam String siteCode, @RequestParam String sitePassword, @RequestParam String reqNumber
								, @RequestParam String authType, @RequestParam String closeYN, @RequestParam String viewType
								, @RequestParam String gender, @RequestParam String domain, @RequestParam String successURL
								, @RequestParam String errorURL) {
		
		System.out.println("popupInfo Start");
	    NiceID.Check.CPClient niceCheck = new  NiceID.Check.CPClient();
	    
	    String sSiteCode = siteCode;			// NICE로부터 부여받은 사이트 코드
	    String sSitePassword = sitePassword;		// NICE로부터 부여받은 사이트 패스워드
	    
	    String sRequestNumber = "";        	// 요청 번호, 이는 성공/실패후에 같은 값으로 되돌려주게 되므로 업체에서 적절하게 변경하여 쓰거나, 아래와 같이 생성한다.
	    
	    if (reqNumber == null || reqNumber.trim().isEmpty()) {
	    	sRequestNumber = niceCheck.getRequestNO(siteCode);
	    } else {
	    	sRequestNumber = reqNumber;
	    }
	    System.out.println("Request No: " + sRequestNumber);
	  	//session.setAttribute("REQ_SEQ" , sRequestNumber);	// 해킹등의 방지를 위하여 세션을 쓴다면, 세션에 요청번호를 넣는다.
	  	
	   	String sAuthType = authType;      	// 없으면 기본 선택화면, M: 핸드폰, C: 신용카드, X: 공인인증서
	   	
	   	String popgubun 	= closeYN;		//Y : 취소버튼 있음 / N : 취소버튼 없음
		String customize 	= viewType;		//없으면 기본 웹페이지 / Mobile : 모바일페이지
		
		String sGender = gender; 			//없으면 기본 선택 값, 0 : 여자, 1 : 남자 
		
	    // CheckPlus(본인인증) 처리 후, 결과 데이타를 리턴 받기위해 다음예제와 같이 http부터 입력합니다.
		//리턴url은 인증 전 인증페이지를 호출하기 전 url과 동일해야 합니다. ex) 인증 전 url : http://www.~ 리턴 url : http://www.~
	    String sReturnUrl = domain+successURL;      // 성공시 이동될 URL
	    String sErrorUrl = domain+errorURL;          // 실패시 이동될 URL

	    // 입력될 plain 데이타를 만든다.
	    String sPlainData = "7:REQ_SEQ" + sRequestNumber.getBytes().length + ":" + sRequestNumber +
	                        "8:SITECODE" + sSiteCode.getBytes().length + ":" + sSiteCode +
	                        "9:AUTH_TYPE" + sAuthType.getBytes().length + ":" + sAuthType +
	                        "7:RTN_URL" + sReturnUrl.getBytes().length + ":" + sReturnUrl +
	                        "7:ERR_URL" + sErrorUrl.getBytes().length + ":" + sErrorUrl +
	                        "11:POPUP_GUBUN" + popgubun.getBytes().length + ":" + popgubun +
	                        "9:CUSTOMIZE" + customize.getBytes().length + ":" + customize + 
							"6:GENDER" + sGender.getBytes().length + ":" + sGender;
	    
	    String sResult = "";
	    
	    System.out.println("Request Data: " + sPlainData);
	    int iReturn = niceCheck.fnEncode(sSiteCode, sSitePassword, sPlainData);
	    System.out.println("Result Code: " + iReturn);
	    if( iReturn == 0 )
	    {
	    	sResult = niceCheck.getCipherData();
	    }
	    else if( iReturn == -1)
	    {
	    	sResult = "암호화 시스템 에러입니다.";
	    }    
	    else if( iReturn == -2)
	    {
	    	sResult = "암호화 처리오류입니다.";
	    }    
	    else if( iReturn == -3)
	    {
	    	sResult = "암호화 데이터 오류입니다.";
	    }    
	    else if( iReturn == -9)
	    {
	    	sResult = "입력 데이터 오류입니다.";
	    }    
	    else
	    {
	    	sResult = "알수 없는 에러 입니다. iReturn : " + iReturn;
	    }
	    
	    System.out.println("Result Message: " + sResult);

	    return iReturn + ":" + sResult;
	}

	@GetMapping("/decodeData")
	public String getDecodeData(@RequestParam String siteCode, @RequestParam String sitePassword, @RequestParam String encodeData) {
		System.out.println("decode Start");
	    NiceID.Check.CPClient niceCheck = new  NiceID.Check.CPClient();

	    String sSiteCode = siteCode;				// NICE로부터 부여받은 사이트 코드
	    String sSitePassword = sitePassword;			// NICE로부터 부여받은 사이트 패스워드
	    System.out.println("Origin Encode Data: " + encodeData);
	    String sEncodeData = ServiceUtil.requestReplace(encodeData, "encodeData");
	    System.out.println("Replace Encode Data: " + sEncodeData);

	    String sCipherTime = "";			// 복호화한 시간
	    String sPlainData = "";
	    
	    String sResult = "";
	    int iReturn = niceCheck.fnDecode(sSiteCode, sSitePassword, sEncodeData);
	    System.out.println("Result Code: " + iReturn);

	    if( iReturn == 0 )
	    {
	        sPlainData = niceCheck.getPlainData();
	        sCipherTime = niceCheck.getCipherDateTime();
	        
	        // 데이타를 추출합니다.
	        java.util.HashMap mapResult = niceCheck.fnParse(sPlainData);
	        mapResult.put("CIPHER_TIME", sCipherTime);
	        
	        ObjectMapper mapper = new ObjectMapper();
	        try {
				sResult = mapper.writeValueAsString(mapResult);
			} catch (JsonProcessingException e) {
				iReturn = -99;
				sResult = e.getMessage();
			}
	    }
	    else if( iReturn == -1)
	    {
	    	sResult = "복호화 시스템 오류입니다.";
	    }    
	    else if( iReturn == -4)
	    {
	    	sResult = "복호화 처리 오류입니다.";
	    }    
	    else if( iReturn == -5)
	    {
	    	sResult = "복호화 해쉬 오류입니다.";
	    }    
	    else if( iReturn == -6)
	    {
	    	sResult = "복호화 데이터 오류입니다.";
	    }    
	    else if( iReturn == -9)
	    {
	    	sResult = "입력 데이터 오류입니다.";
	    }    
	    else if( iReturn == -12)
	    {
	    	sResult = "사이트 패스워드 오류입니다.";
	    }    
	    else
	    {
	    	sResult = "알수 없는 에러 입니다. iReturn : " + iReturn;
	    }
	    
	    System.out.println("Result Message: " + sResult);
	    
	    return iReturn + ":" + sResult;
	}
}
