package org.wallet.exchange;

import org.joda.money.Money;

/**
 * A pojo to store the result of a currency conversion.
 * 
 * The fiat and fst are kept separate just to avoid mixing them up accidentally.
 */
public class CurrencyConverterResult {
    
    private boolean fiatMoneyValid;
    
    private Money fiatMoney;
    
    private String fiatMessage;
    
    private boolean fstMoneyValid;
    
    private Money fstMoney;
    
    private String fstMessage;

    public CurrencyConverterResult() {
        fiatMoneyValid = false;
        fiatMoney = null;
        fiatMessage = null;
        
        fstMoneyValid = false;
        fstMoney = null;
        fstMessage = null;
    }

    public boolean isFiatMoneyValid() {
        return fiatMoneyValid;
    }

    public void setFiatMoneyValid(boolean fiatMoneyValid) {
        this.fiatMoneyValid = fiatMoneyValid;
    }

    public Money getFiatMoney() {
        return fiatMoney;
    }

    public void setFiatMoney(Money fiatMoney) {
        this.fiatMoney = fiatMoney;
    }

    public String getFiatMessage() {
        return fiatMessage;
    }

    public void setFiatMessage(String fiatMessage) {
        this.fiatMessage = fiatMessage;
    }

    public boolean isFstMoneyValid() {
        return fstMoneyValid;
    }

    public void setFstMoneyValid(boolean fstMoneyValid) {
        this.fstMoneyValid = fstMoneyValid;
    }

    public Money getFstMoney() {
        return fstMoney;
    }

    public void setFstMoney(Money fstMoney) {
        this.fstMoney = fstMoney;
    }

    public String getFstMessage() {
        return fstMessage;
    }

    public void setFstMessage(String fstMessage) {
        this.fstMessage = fstMessage;
    }
}
