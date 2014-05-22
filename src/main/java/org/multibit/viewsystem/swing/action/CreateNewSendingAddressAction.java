/**
 * Copyright 2011 wallet.org
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
package org.multibit.viewsystem.swing.action;

import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.model.fastcoin.FastcoinModel;
import org.multibit.model.fastcoin.WalletAddressBookData;
import org.multibit.model.fastcoin.WalletData;
import org.multibit.model.fastcoin.WalletInfoData;
import org.wallet.store.FastcoinWalletVersion;
import org.multibit.viewsystem.swing.view.panels.SendFastcoinPanel;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * This {@link Action} represents an action to create a sending address.
 */
public class CreateNewSendingAddressAction extends FastcoinWalletSubmitAction {

    private static final long serialVersionUID = 200111935465875405L;

    private SendFastcoinPanel sendFastcoinPanel;

    /**
     * Creates a new {@link CreateNewSendingAddressAction}.
     */
    public CreateNewSendingAddressAction(FastcoinController fastcoinController, SendFastcoinPanel sendFastcoinPanel) {
        super(fastcoinController, "createOrEditAddressAction.createReceiving.text", "createOrEditAddressAction.createSending.tooltip",
                "createOrEditAddressAction.createSending.mnemonicKey", null);
        this.sendFastcoinPanel = sendFastcoinPanel;
    }

    /**
     * Create new send address.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        // Check to see if the wallet files have changed.
        WalletData perWalletModelData = super.fastcoinController.getModel().getActivePerWalletModelData();

        WalletInfoData walletInfo = perWalletModelData.getWalletInfo();
        if (walletInfo == null) {
            walletInfo = new WalletInfoData(perWalletModelData.getWalletFilename(), perWalletModelData.getWallet(), FastcoinWalletVersion.PROTOBUF_ENCRYPTED);
            perWalletModelData.setWalletInfo(walletInfo);
        }

        if (walletInfo.getSendingAddresses().isEmpty()) {
            String address = super.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_ADDRESS);
            String label = super.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_LABEL);

            perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData(label, address));
            sendFastcoinPanel.getAddressesTableModel().fireTableDataChanged();
            super.fastcoinController.getModel().getActivePerWalletModelData().setDirty(true);
        } else {
            perWalletModelData.getWalletInfo().addSendingAddress(new WalletAddressBookData("", ""));
            sendFastcoinPanel.getAddressesTableModel().fireTableDataChanged();
            sendFastcoinPanel.selectRows();

            super.fastcoinController.getModel().setActiveWalletPreference(FastcoinModel.SEND_ADDRESS, "");
            super.fastcoinController.getModel().setActiveWalletPreference(FastcoinModel.SEND_LABEL, "");
        }
        
        sendFastcoinPanel.checkDeleteSendingEnabled();
        
        controller.displayView(controller.getCurrentView());

        if (sendFastcoinPanel.getLabelTextArea() != null) {
            sendFastcoinPanel.getLabelTextArea().requestFocusInWindow();
        }
    }
}