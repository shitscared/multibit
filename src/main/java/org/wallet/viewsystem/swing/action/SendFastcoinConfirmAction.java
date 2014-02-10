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

import com.google.fastcoin.core.*;
import com.google.fastcoin.core.Wallet.SendRequest;
import com.google.fastcoin.crypto.KeyCrypterException;
import org.wallet.controller.fastcoin.FastcoinController;
import org.wallet.message.Message;
import org.wallet.message.MessageManager;
import org.wallet.model.fastcoin.FastcoinModel;
import org.wallet.utils.ImageLoader;
import org.wallet.viewsystem.dataproviders.FastcoinFormDataProvider;
import org.wallet.viewsystem.swing.FastcoinWalletFrame;
import org.wallet.viewsystem.swing.view.dialogs.SendFastcoinConfirmDialog;
import org.wallet.viewsystem.swing.view.dialogs.ValidationErrorDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.math.BigInteger;

/**
 * This {@link Action} shows the send fastcoin confirm dialog or validation dialog on an attempted spend.
 */
public class SendFastcoinConfirmAction extends FastcoinWalletSubmitAction {

    private static final long serialVersionUID = 1913592460523457765L;

    private static final Logger log = LoggerFactory.getLogger(SendFastcoinConfirmAction.class);

    private FastcoinWalletFrame mainFrame;
    private FastcoinFormDataProvider dataProvider;
    private FastcoinController fastcoinController;

    /**
     * Creates a new {@link SendFastcoinConfirmAction}.
     */
    public SendFastcoinConfirmAction(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame, FastcoinFormDataProvider dataProvider) {
        super(fastcoinController, "sendFastcoinConfirmAction.text", "sendFastcoinConfirmAction.tooltip", "sendFastcoinConfirmAction.mnemonicKey", ImageLoader.createImageIcon(ImageLoader.SEND_FASTCOIN_ICON_FILE));
        this.mainFrame = mainFrame;
        this.dataProvider = dataProvider;
        this.fastcoinController = fastcoinController;
    }

    /**
     * Complete the transaction to work out the fee) and then show the send fastcoin confirm dialog.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (abort()) {
            return;
        }

        SendFastcoinConfirmDialog sendFastcoinConfirmDialog = null;
        ValidationErrorDialog validationErrorDialog = null;

        try {
            String sendAddress = dataProvider.getAddress();
            String sendAmount = dataProvider.getAmount();

            Validator validator = new Validator(super.fastcoinController);
            if (validator.validate(sendAddress, sendAmount)) {
                // The address and amount are valid.

                // Create a SendRequest.
                Address sendAddressObject;

                sendAddressObject = new Address(fastcoinController.getModel().getNetworkParameters(), sendAddress);
                SendRequest sendRequest = Wallet.SendRequest.to(sendAddressObject, Utils.toNanoCoins(sendAmount));

                sendRequest.ensureMinRequiredFee = true;
                sendRequest.fee = BigInteger.ZERO;
                sendRequest.feePerKb = FastcoinModel.SEND_FEE_PER_KB_DEFAULT;

                // Note - Request is populated with the AES key in the SendFastcoinNowAction after the user has entered it on the SendFastcoinConfirm form.

                // Complete it (which works out the fee) but do not sign it yet.
                log.debug("Just about to complete the tx (and calculate the fee)...");
                boolean completedOk = fastcoinController.getModel().getActiveWallet().completeTx(sendRequest, false);
                log.debug("The fee after completing the transaction was " + sendRequest.fee);
                if (completedOk) {
                    // There is enough money.

                  //  fastcoinController.getModel().getActiveWallet().g.getp.broadcastTransaction(sendRequest.tx);
                    sendFastcoinConfirmDialog = new SendFastcoinConfirmDialog(super.fastcoinController, mainFrame, sendRequest);
                    sendFastcoinConfirmDialog.setVisible(true);
                } else {
                    // There is not enough money.
                    // TODO setup validation parameters accordingly so that it displays ok.
                    validationErrorDialog = new ValidationErrorDialog(super.fastcoinController, mainFrame, sendRequest, true);
                    validationErrorDialog.setVisible(true);
                }

            } else {
                validationErrorDialog = new ValidationErrorDialog(super.fastcoinController, mainFrame, null, false);
                validationErrorDialog.setVisible(true);
            }
        } catch (WrongNetworkException e1) {
            logMessage(e1);
        } catch (AddressFormatException e1) {
            logMessage(e1);
        } catch (KeyCrypterException e1) {
            logMessage(e1);
        } catch (Exception e1) {
            logMessage(e1);
        }
    }

    private void logMessage(Exception e) {
        e.printStackTrace();
        String errorMessage = controller.getLocaliser().getString("sendFastcoinNowAction.fastcoinSendFailed");
        String detailMessage = controller.getLocaliser().getString("deleteWalletConfirmDialog.walletDeleteError2", new String[]{e.getClass().getCanonicalName() + " " + e.getMessage()});
        MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + detailMessage));
    }
}