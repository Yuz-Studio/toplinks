package com.yuz.toplinks.dto;

import java.util.List;

/**
 * USPS 运费计算结果 DTO。
 */
public class UspsRateResponse {

    private boolean success;
    private String message;
    private List<RateDetail> rates;

    public UspsRateResponse() {}

    public UspsRateResponse(boolean success, String message, List<RateDetail> rates) {
        this.success = success;
        this.message = message;
        this.rates = rates;
    }

    public static UspsRateResponse error(String message) {
        return new UspsRateResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RateDetail> getRates() {
        return rates;
    }

    public void setRates(List<RateDetail> rates) {
        this.rates = rates;
    }

    /**
     * 单条费率明细。
     */
    public static class RateDetail {
        private String mailClass;
        private String productName;
        private double totalPrice;
        private String deliveryDays;
        private String zone;

        public String getMailClass() {
            return mailClass;
        }

        public void setMailClass(String mailClass) {
            this.mailClass = mailClass;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(double totalPrice) {
            this.totalPrice = totalPrice;
        }

        public String getDeliveryDays() {
            return deliveryDays;
        }

        public void setDeliveryDays(String deliveryDays) {
            this.deliveryDays = deliveryDays;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }
    }
}
