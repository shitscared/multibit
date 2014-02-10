package org.wallet.viewsystem.dataproviders;


/**
 * DataProvider for send fastcoin and send fastcoin confirm action
 * @author jim
 *
 */
public interface FastcoinFormDataProvider extends DataProvider {
    /**
     * Get the address
     */
    public String getAddress();
    
    /**
     * Get the label
     */
    public String getLabel();
    
    /**
     * Get the amount (denominated in FST).
     */
    public String getAmount();
    
    /**
     * Get the amount (denominated in fiat)
     */
    public String getAmountFiat();
}
