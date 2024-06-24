package com.imokkkk.model;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author wyliu
 * @date 2024/6/24 14:48
 * @since 1.0
 */
public class OrderInfo implements Serializable {
    private String consignee;

    private String description;

    private BigDecimal amount;

    public OrderInfo(String consignee, String description, BigDecimal amount) {
        this.consignee = consignee;
        this.description = description;
        this.amount = amount;
    }

    public String getConsignee() {
        return consignee;
    }

    public void setConsignee(String consignee) {
        this.consignee = consignee;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
