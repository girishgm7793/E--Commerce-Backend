package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentService {

    private final OrderService orderService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public PaymentService(OrderService orderService) {
        this.orderService = orderService;
    }

    public Map<String, String> createPaymentIntent(String email, Long orderId) {
        Order order = orderService.getOrder(email, orderId);

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new RuntimeException("Order is already paid");
        }

        try {
            long amountInCents = order.getTotalPrice()
                    .multiply(BigDecimal.valueOf(100)).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("usd")
                    .putMetadata("orderId", order.getId().toString())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            orderService.attachPaymentIntent(orderId, paymentIntent.getId());

            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentIntentId", paymentIntent.getId());
            return response;

        } catch (StripeException e) {
            throw new RuntimeException("Payment failed: " + e.getMessage());
        }
    }

    public void handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid webhook signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
            orderService.updatePaymentStatus(intent.getId(), "PAID", "CONFIRMED");

        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
            orderService.updatePaymentStatus(intent.getId(), "FAILED", "PENDING");
        }
    }
}
