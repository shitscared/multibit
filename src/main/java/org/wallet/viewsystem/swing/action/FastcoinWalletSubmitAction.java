package org.wallet.viewsystem.swing.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.wallet.controller.Controller;
import org.wallet.controller.fastcoin.FastcoinController;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.fastcoin.WalletData;
import org.wallet.viewsystem.swing.view.panels.HelpContentsPanel;

/**
 * Abstract super class to check for whether wallet files have changed and
 * whether there is an active wallet available
 * @author jim
 *
 */
public abstract class FastcoinWalletSubmitAction extends AbstractAction {
    private static final long serialVersionUID = 3750799470657961967L;

    protected final Controller controller;
    protected final FastcoinController fastcoinController;
    
    /**
     * Creates a new {@link ResetTransactionsSubmitAction}.
     */
    public FastcoinWalletSubmitAction(FastcoinController fastcoinController, String textKey, String tooltipKey, String mnemonicKey,  Icon icon) {
        super(fastcoinController.getLocaliser().getString(textKey), icon);
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        
        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString(tooltipKey)));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic(mnemonicKey));
    }
   
    /**
     * Abort due to there not being an active wallet or the wallet has been changed by another process.
     * @return abort True if called method should abort
     */
    public boolean abort() {
        // Check if there is an active wallet.
        if (this.fastcoinController.getModel().thereIsNoActiveWallet()) {
            MessageManager.INSTANCE.addMessage(new Message(controller.getLocaliser().getString("fastcoinWalletSubmitAction.thereIsNoActiveWallet")));
            return true;
        }

        // check to see if another process has changed the active wallet
        WalletData perWalletModelData = this.fastcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.fastcoinController.getFileHandler().haveFilesChanged(perWalletModelData);
        
        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.fastcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
 
            return true;
        }
        
        return false;
    }
}
