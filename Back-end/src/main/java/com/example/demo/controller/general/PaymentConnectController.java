package com.example.demo.controller.general;

import com.example.demo.base.code.exception.CustomException;
import com.example.demo.base.status.ErrorStatus;
import com.example.demo.controller.BaseController;
import com.example.demo.domain.converter.KakaoPayConverter;
import com.example.demo.domain.dto.Payment.ApproveResponse;
import com.example.demo.domain.dto.Payment.PaymentRequest;
import com.example.demo.domain.dto.Payment.ReadyResponse;
import com.example.demo.entity.Payment.Payment;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.service.general.KakaoPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentConnectController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentConnectController.class);

    private final KakaoPayService kakaoPayService;
    private final BaseController baseController;
    private final PaymentRepository paymentRepository;

    @Value("${kakao.redirect-url}")
    private String redirectUrl;

    @Value("${kakao.delivery-url}")
    private String redirectUrlDelivery;

    @Value("${kakao.domain}")
    private String domain;

    @GetMapping("/approve")
    public String approvePayment(@RequestParam String pg_token, HttpServletRequest request) {

        String tid = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("tid".equals(cookie.getName())) {
                    tid = cookie.getValue();
                    break;
                }
            }
        }
        logger.info("pg_token: {}", pg_token);
        logger.info("tid: {}", tid);

        ApproveResponse approveResponse = kakaoPayService.approvePayment(pg_token, tid).getResult();

        String redirectTarget = redirectUrl + "/?approvedAt=" + approveResponse.getApprovedAt().toString();

        // 배송비는 다른 경로로 리다이렉트
        Payment payment = findPaymentByTid(tid);
        if (payment.getItemId().equals("배송비")) {
            redirectTarget = redirectUrlDelivery + "/?approvedAt=" + approveResponse.getApprovedAt().toString();
        }

        return "redirect:" + redirectTarget;
    }

    private Payment findPaymentByTid(String tid) {
        return paymentRepository.findByTid(tid)
                .orElseThrow(() -> new CustomException(ErrorStatus.PAYMENT_NOT_FOUND));
    }

    @GetMapping("/redirect")
    public String redirectPayment(@RequestParam String tid, @RequestParam String url, HttpServletResponse response) {
        Cookie cookie = new Cookie("tid", tid);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60);
        cookie.setDomain(domain);
        response.addCookie(cookie);
        logger.info("Saved TID in cookie: {}", tid);
        return "redirect:" + url;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestParam String itemId, @RequestParam String itemName, @RequestParam int totalAmount) {

        // 사용자Id 가져오기
        Long userId = baseController.getCurrentUserId();

        PaymentRequest paymentRequest = KakaoPayConverter.toPaymentRequest(userId, itemId, itemName, totalAmount);
        ReadyResponse readyResponse = kakaoPayService.preparePayment(paymentRequest).getResult();
        String tid = readyResponse.getTid();
        String url = readyResponse.getNextRedirectPcUrl();
        logger.info("KakaoPay ReadyResponse - TID: {}, Redirect URL: {}", tid, url);
        Map<String, String> response = new HashMap<>();
        response.put("redirectUrl", "/api/payment/redirect?tid=" + tid + "&url=" + url);

        return ResponseEntity.ok(response);
    }
}