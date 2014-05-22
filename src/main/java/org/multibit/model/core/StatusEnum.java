package org.multibit.model.core;

public enum StatusEnum {
    ONLINE("fastcoinWalletFrame.onlineText"),
    CONNECTING("fastcoinWalletFrame.offlineText"),
    ERROR("fastcoinWalletFrame.errorText");
    
    private String localisationKey;
    
    private StatusEnum(String localisationKey) {
        this.localisationKey = localisationKey;
      }

    public String getLocalisationKey() {
        return localisationKey;
    }         
}