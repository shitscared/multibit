package org.multibit.model.fastcoin;


/**
 * Interface to implement if you are interested in hearing about when the state of a wallets busy status changes.
 * 
 * @author jim
 *
 */
public interface WalletBusyListener {
    /**
     * The MultiBitWallet wallets busy status has changed to 'newWalletIsBusy'.
     * 
     * @param newWalletIsBusy
     */
    public void walletBusyChange(boolean newWalletIsBusy);
}
