/**
 * Copyright 2012 wallet.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wallet.viewsystem.swing.action;

import java.awt.event.ActionEvent;
import java.nio.CharBuffer;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

import org.wallet.controller.fastcoin.FastcoinController;
import org.wallet.file.BackupManager;
import org.wallet.file.FileHandler;
import org.wallet.model.fastcoin.WalletData;
import org.wallet.model.fastcoin.WalletBusyListener;
import org.wallet.model.fastcoin.WalletInfoData;
import org.wallet.store.FastcoinWalletVersion;
import org.wallet.viewsystem.swing.FastcoinWalletFrame;
import org.wallet.viewsystem.swing.view.panels.RemovePasswordPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.fastcoin.core.Wallet;
import com.google.fastcoin.crypto.KeyCrypterException;

/**
 * This {@link Action} action removes the encryption of private keys in a wallet.
 */
public class RemovePasswordSubmitAction extends FastcoinWalletSubmitAction implements WalletBusyListener {
    private static final Logger log = LoggerFactory.getLogger(RemovePasswordSubmitAction.class);

    private static final long serialVersionUID = 1923492460598757765L;

    private RemovePasswordPanel removePasswordPanel;
    private JPasswordField password1;

    /**
     * Creates a new {@link RemovePasswordSubmitAction}.
     */
    public RemovePasswordSubmitAction(FastcoinController fastcoinController, RemovePasswordPanel removePasswordPanel,
            ImageIcon icon, JPasswordField password1, FastcoinWalletFrame mainFrame) {
        super(fastcoinController, "removePasswordSubmitAction.text", "removePasswordSubmitAction.tooltip", "removePasswordSubmitAction.mnemonicKey", icon);
        this.removePasswordPanel = removePasswordPanel;
        this.password1 = password1;
        
        // This action is a WalletBusyListener.
        super.fastcoinController.registerWalletBusyListener(this);
        walletBusyChange(super.fastcoinController.getModel().getActivePerWalletModelData().isBusy());
    }

    /**
     * Remove the password protection on a wallet.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        removePasswordPanel.clearMessages();

        char[] passwordToUse = password1.getPassword();

        // Get the passwords on the password fields.
        if (password1.getPassword() == null || password1.getPassword().length == 0) {
            // Notify that the user must enter a password.
            removePasswordPanel.setMessage1(controller.getLocaliser()
                    .getString("removePasswordPanel.enterPassword"));
            return;
        }
       
        if (super.fastcoinController.getModel().getActiveWallet() != null) {
            Wallet wallet = super.fastcoinController.getModel().getActiveWallet();
            if (wallet != null) {
                    WalletData perWalletModelData = null;
                    WalletInfoData walletInfoData = null;
                    
                    try {
                        // Double check wallet is not busy then declare that the active
                        // wallet is busy with the task
                        perWalletModelData = super.fastcoinController.getModel().getActivePerWalletModelData();
                        walletInfoData = super.fastcoinController.getModel().getActiveWalletInfo();

                        if (!perWalletModelData.isBusy()) {
                            perWalletModelData.setBusy(true);
                            perWalletModelData.setBusyTaskKey("removePasswordSubmitAction.text");

                            super.fastcoinController.fireWalletBusyChange(true);

                            wallet.decrypt(wallet.getKeyCrypter().deriveKey(CharBuffer.wrap(passwordToUse)));
                            walletInfoData.setWalletVersion(FastcoinWalletVersion.PROTOBUF);
                            perWalletModelData.setDirty(true);
                            FileHandler fileHandler = new FileHandler(super.fastcoinController);
                            fileHandler.savePerWalletModelData(perWalletModelData, true);
                            
                            // Backup the wallet and wallet info.
                            BackupManager.INSTANCE.backupPerWalletModelData(fileHandler, perWalletModelData);
                        }
                    } catch (KeyCrypterException kce) {
                        removePasswordPanel.setMessage1(controller.getLocaliser()
                                .getString("removePasswordPanel.removePasswordFailed", new String[]{kce.getMessage()}));
                        return;
                    } finally {
                        // Declare that wallet is no longer busy with the task.
                        if (perWalletModelData != null) {
                            perWalletModelData.setBusyTaskKey(null);
                            perWalletModelData.setBusy(false);
                        }
                        super.fastcoinController.fireWalletBusyChange(false);
                    }
            }
        }
        controller.fireDataChangedUpdateNow();

        // Success.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                removePasswordPanel.clearMessages();
                removePasswordPanel.clearPasswords();
                removePasswordPanel.setMessage1(controller.getLocaliser()
                        .getString("removePasswordPanel.removePasswordSuccess")); 
            }});
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {
        // Update the enable status of the action to match the wallet busy status.
        if (super.fastcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // FastcoinWallet is busy with another operation that may change the private keys - Action is disabled.
            putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("fastcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.fastcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
        } else {
            // Enable unless wallet has been modified by another process.
            if (!super.fastcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("removePasswordSubmitAction.text"));
            }
        }
    }
}