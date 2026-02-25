package com.yuz.toplinks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * USPS 运费计算请求 DTO。
 * <p>
 * 使用 USPS Prices v3 API 计算国内运费。
 * </p>
 */
public class UspsRateRequest {

    /** 寄件人 ZIP 编码（5 位） */
    @NotBlank(message = "originZIPCode is required")
    private String originZIPCode;

    /** 收件人 ZIP 编码（5 位） */
    @NotBlank(message = "destinationZIPCode is required")
    private String destinationZIPCode;

    /** 包裹重量（磅） */
    @Positive(message = "weight must be positive")
    private double weight;

    /** 包裹长度（英寸），可选 */
    private double length;

    /** 包裹宽度（英寸），可选 */
    private double width;

    /** 包裹高度（英寸），可选 */
    private double height;

    /**
     * 邮寄类型，例如：
     * PRIORITY_MAIL, USPS_GROUND_ADVANTAGE, FIRST_CLASS_MAIL,
     * PRIORITY_MAIL_EXPRESS 等。
     * 如果不传则查询所有可用类型的费率。
     */
    private String mailClass;

    /**
     * 处理类别，例如：
     * LETTERS, FLATS, PARCELS, MACHINABLE 等。
     * 默认 PARCELS。
     */
    private String processingCategory;

    /**
     * 价格类型，例如：RETAIL, COMMERCIAL, CONTRACT。
     * 默认 RETAIL。
     */
    private String priceType;

    /**
     * 费率指示器，例如：DR（Dimensional Rectangular）、SP（Single Piece）。
     * 默认 SP。
     */
    private String rateIndicator;

    public String getOriginZIPCode() {
        return originZIPCode;
    }

    public void setOriginZIPCode(String originZIPCode) {
        this.originZIPCode = originZIPCode;
    }

    public String getDestinationZIPCode() {
        return destinationZIPCode;
    }

    public void setDestinationZIPCode(String destinationZIPCode) {
        this.destinationZIPCode = destinationZIPCode;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public String getMailClass() {
        return mailClass;
    }

    public void setMailClass(String mailClass) {
        this.mailClass = mailClass;
    }

    public String getProcessingCategory() {
        return processingCategory;
    }

    public void setProcessingCategory(String processingCategory) {
        this.processingCategory = processingCategory;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public String getRateIndicator() {
        return rateIndicator;
    }

    public void setRateIndicator(String rateIndicator) {
        this.rateIndicator = rateIndicator;
    }
}
