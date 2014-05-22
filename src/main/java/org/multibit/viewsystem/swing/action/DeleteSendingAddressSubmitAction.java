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

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JTable;

import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.model.fastcoin.WalletAddressBookData;
import org.multibit.model.fastcoin.WalletData;
import org.multibit.model.fastcoin.WalletInfoData;
import org.wallet.store.FastcoinWalletVersion;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.swing.view.dialogs.DeleteSendingAddressConfirmDialog;
import org.multibit.viewsystem.swing.view.models.AddressBookTableModel;
import org.multibit.viewsystem.swing.view.panels.SendFastcoinPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link Action} represents an action to delete a sending address.
 */
public class DeleteSendingAddressSubmitAction extends FastcoinWalletSubmitAction {

    private static final long serialVersionUID = 200111999465875405L;

    private static final Logger log = LoggerFactory.getLogger(DeleteSendingAddressSubmitAction.class);

    private SendFastcoinPanel sendFastcoinPanel;
    private DeleteSendingAddressConfirmDialog deleteSendingAddressConfirmDialog;

    /**
     * Creates a new {@link DeleteSendingAddressSubmitAction}.
     */
    public DeleteSendingAddressSubmitAction(FastcoinController fastcoinController, SendFastcoinPanel sendFastcoinPanel, DeleteSendingAddressConfirmDialog deleteSendingAddressConfirmDialog) {
        super(fastcoinController, "deleteSendingAddressSubmitAction.text", "deleteSendingAddressSubmitAction.tooltip",
                "deleteSendingAddressSubmitAction.mnemonicKey", ImageLoader.createImageIcon(ImageLoader.DELETE_ADDRESS_ICON_FILE));
        this.sendFastcoinPanel = sendFastcoinPanel;
        this.deleteSendingAddressConfirmDialog = deleteSendingAddressConfirmDialog;
    }

    /**
     * Delete the currently selected sending address.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        WalletData perWalletModelData = super.fastcoinController.getModel().getActivePerWalletModelData();

        WalletInfoData walletInfo = perWalletModelData.getWalletInfo();
        if (walletInfo == null) {
            walletInfo = new WalletInfoData(perWalletModelData.getWalletFilename(), perWalletModelData.getWallet(), FastcoinWalletVersion.PROTOBUF_ENCRYPTED);
            perWalletModelData.setWalletInfo(walletInfo);
        }

        if (walletInfo.getSendingAddresses().size() > 0) {
            JTable addressesTable = sendFastcoinPanel.getAddressesTable();
            AddressBookTableModel addressesTableModel = sendFastcoinPanel.getAddressesTableModel();
            int viewRow = addressesTable.getSelectedRow();
            if (viewRow >= 0) {
                int selectedAddressRowModel = addressesTable.convertRowIndexToModel(viewRow);
                WalletAddressBookData rowData = addressesTableModel.getAddressBookDataByRow(selectedAddressRowModel, false);
                if (rowData != null) {
                    if (selectedAddressRowModel < addressesTableModel.getRowCount()) {
                        walletInfo.getSendingAddresses().remove(rowData);
                        super.fastcoinController.getModel().getActivePerWalletModelData().setDirty(true);
                        addressesTableModel.fireTableDataChanged();
                    } else {
                        log.error("Could not remove row " + selectedAddressRowModel + " as table model only contained " + addressesTableModel.getRowCount() + " rows");
                    }
                    
                    int newViewRowToSelect = viewRow == 0 ? 0 : viewRow - 1;
                    if (addressesTableModel.getRowCount() > 0) {
                        int newModelRowtoSelect = addressesTable.convertRowIndexToModel(newViewRowToSelect);
                        WalletAddressBookData newRowData = addressesTableModel.getAddressBookDataByRow(newModelRowtoSelect, false);
                    
                        super.fastcoinController.getModel().setActiveWalletPreference(sendFastcoinPanel.getAddressConstant(),
                                newRowData.getAddress());
                        super.fastcoinController.getModel().setActiveWalletPreference(sendFastcoinPanel.getLabelConstant(),
                                newRowData.getLabel());

                        if (sendFastcoinPanel.getAddressTextField() != null) {
                            sendFastcoinPanel.getAddressTextField().setText(newRowData.getAddress());
                        }
                        sendFastcoinPanel.getLabelTextArea().setText(newRowData.getLabel());

                        sendFastcoinPanel.displayQRCode(newRowData.getAddress(), sendFastcoinPanel.getAmount(), newRowData.getLabel());
                    }
                }
            }     
        }
        
        sendFastcoinPanel.checkDeleteSendingEnabled();
        
        if (deleteSendingAddressConfirmDialog != null) {
            deleteSendingAddressConfirmDialog.setVisible(false);
        }
    }
}
