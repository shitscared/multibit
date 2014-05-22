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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import org.multibit.controller.Controller;
import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.model.fastcoin.WalletAddressBookData;
import org.multibit.model.fastcoin.FastcoinModel;
import org.multibit.model.fastcoin.WalletData;
import org.multibit.utils.WhitespaceTrimmer;
import org.multibit.viewsystem.swing.view.panels.SendFastcoinPanel;

/**
 * This {@link Action} represents the swing paste address action
 */
public class PasteAddressAction extends AbstractAction {

    private static final long serialVersionUID = 114352235465057705L;

    private final Controller controller;
    private final FastcoinController fastcoinController;
    
    private SendFastcoinPanel sendFastcoinPanel;

    /**
     * Creates a new {@link PasteAddressAction}.
     */
    public PasteAddressAction(FastcoinController fastcoinController, SendFastcoinPanel sendFastcoinPanel, ImageIcon icon) {
        super("", icon);
        // super(controller.getLocaliser().getString("pasteAddressAction.text"));
        
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        
        this.sendFastcoinPanel = sendFastcoinPanel;

        MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("pasteAddressAction.tooltip"));
        putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("pasteAddressAction.mnemonicKey"));
    }

    /**
     * delegate to generic paste address action
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // check to see if the wallet files have changed
        WalletData perWalletModelData = this.fastcoinController.getModel().getActivePerWalletModelData();
        boolean haveFilesChanged = this.fastcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

        if (haveFilesChanged) {
            // set on the perWalletModelData that files have changed and fire
            // data changed
            perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
            this.fastcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
        } else {
            TextTransfer textTransfer = new TextTransfer();
            String stringToPaste = textTransfer.getClipboardContents();
            stringToPaste = WhitespaceTrimmer.trim(stringToPaste);

            // TODO parse string - if fastcoin URI then fill out other fields

            String label = sendFastcoinPanel.getLabelTextArea().getText();
            WalletAddressBookData addressBookData = new WalletAddressBookData(label, stringToPaste);
            sendFastcoinPanel.setAddressBookDataByRow(addressBookData);

            // put it in the user preferences - will then get loaded when view
            // form loads
            this.fastcoinController.getModel().setActiveWalletPreference(FastcoinModel.SEND_ADDRESS, stringToPaste);

            // forward back to the view currently being displayed
            controller.displayView(controller.getCurrentView());
         }
    }
}