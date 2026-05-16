package com.connecthub.auth.controller;

import com.connecthub.auth.model.User;
import com.connecthub.auth.repository.UserRepository;
import com.connecthub.auth.security.JwtTokenProvider;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/payments")
public class PaymentController {

    private final String KEY_ID = "rzp_test_Sm22LBiuYSCtKP";
    private final String KEY_SECRET = "fgQYu2sudCQCKsctg2EohbCS";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UUID getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                return UUID.fromString(jwtTokenProvider.getUserIdFromToken(token));
            }
        }
        throw new RuntimeException("Unauthorized");
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            getUserIdFromRequest(request); // Validate user

            String plan = payload.get("plan"); // MONTHLY or YEARLY
            int amount = 0;

            if ("MONTHLY".equalsIgnoreCase(plan)) {
                amount = 499 * 100; // 499 INR in paise
            } else if ("YEARLY".equalsIgnoreCase(plan)) {
                amount = 4999 * 100; // 4999 INR in paise
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid plan selected"));
            }

            RazorpayClient razorpay = new RazorpayClient(KEY_ID, KEY_SECRET);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", amount);
            response.put("currency", "INR");
            response.put("keyId", KEY_ID);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            UUID userId = getUserIdFromRequest(request);

            String razorpayOrderId = payload.get("razorpayOrderId");
            String razorpayPaymentId = payload.get("razorpayPaymentId");
            String razorpaySignature = payload.get("razorpaySignature");
            String plan = payload.get("plan");

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = Utils.verifyPaymentSignature(options, KEY_SECRET);

            if (isValid) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    LocalDateTime currentExpiration = user.getPrimeExpirationDate();
                    if (currentExpiration == null || currentExpiration.isBefore(LocalDateTime.now())) {
                        currentExpiration = LocalDateTime.now();
                    }

                    if ("MONTHLY".equalsIgnoreCase(plan)) {
                        user.setPrimeExpirationDate(currentExpiration.plusMonths(1));
                    } else if ("YEARLY".equalsIgnoreCase(plan)) {
                        user.setPrimeExpirationDate(currentExpiration.plusYears(1));
                    }
                    
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Payment successful! You are now a Prime member."));
                } else {
                    return ResponseEntity.status(404).body(Map.of("error", "User not found"));
                }
            } else {
                return ResponseEntity.status(400).body(Map.of("error", "Payment verification failed"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
