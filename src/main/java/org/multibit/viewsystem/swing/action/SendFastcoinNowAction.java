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
package org.multibit.viewsystem.swing.action;

import com.google.fastcoin.core.AddressFormatException;
import com.google.fastcoin.core.Transaction;
import com.google.fastcoin.core.Wallet.SendRequest;
import com.google.fastcoin.crypto.KeyCrypterException;
import org.multibit.controller.fastcoin.FastcoinController;
import org.fastcoinj.wallet.Protos.Wallet.EncryptionType;
import org.multibit.controller.Controller;
import org.multibit.file.WalletSaveException;
import org.multibit.message.Message;
import org.multibit.message.MessageManager;
import org.multibit.model.fastcoin.*;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.view.panels.SendFastcoinConfirmPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.CharBuffer;

/**
 * This {@link Action} actually spends fastcoin.
 */
public class SendFastcoinNowAction extends AbstractAction implements WalletBusyListener {

  public Logger log = LoggerFactory.getLogger(SendFastcoinNowAction.class.getName());

  private static final long serialVersionUID = 1913592460523457765L;

  private final Controller controller;
  private final FastcoinController fastcoinController;

  private SendFastcoinConfirmPanel sendFastcoinConfirmPanel;
  private JPasswordField walletPasswordField;

  private final static int MAX_LENGTH_OF_ERROR_MESSAGE = 120;

  /**
   * Boolean to indicate that the test parameters should be used for "sending".
   */
  private boolean useTestParameters = false;

  /**
   * Boolean to indicate that the "send was successful" or not (when useTestParameters = true).
   */
  private boolean sayTestSendWasSuccessful = false;

  private Transaction transaction;

  private SendRequest sendRequest;


  /**
   * Creates a new {@link SendFastcoinNowAction}.
   */
  public SendFastcoinNowAction(FastcoinWalletFrame mainFrame, FastcoinController fastcoinController,
                                SendFastcoinConfirmPanel sendFastcoinConfirmPanel, JPasswordField walletPasswordField, ImageIcon icon, SendRequest sendRequest) {
    super(fastcoinController.getLocaliser().getString("sendFastcoinConfirmAction.text"), icon);

    this.fastcoinController = fastcoinController;
    this.controller = this.fastcoinController;

    this.sendFastcoinConfirmPanel = sendFastcoinConfirmPanel;
    this.walletPasswordField = walletPasswordField;
    this.sendRequest = sendRequest;

    MnemonicUtil mnemonicUtil = new MnemonicUtil(controller.getLocaliser());

    putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("sendFastcoinConfirmAction.tooltip"));
    putValue(MNEMONIC_KEY, mnemonicUtil.getMnemonic("sendFastcoinConfirmAction.mnemonicKey"));

    // This action is a WalletBusyListener.
    this.fastcoinController.registerWalletBusyListener(this);
    walletBusyChange(this.fastcoinController.getModel().getActivePerWalletModelData().isBusy());
  }

  /**
   * Actually send the fastcoin.
   */
  @Override
  public void actionPerformed(ActionEvent event) {
    sendFastcoinConfirmPanel.setMessageText(" ", " ");

    // Check to see if the wallet files have changed.
    WalletData perWalletModelData = this.fastcoinController.getModel().getActivePerWalletModelData();
    boolean haveFilesChanged = this.fastcoinController.getFileHandler().haveFilesChanged(perWalletModelData);

    if (haveFilesChanged) {
      // Set on the perWalletModelData that files have changed and fire data changed.
      perWalletModelData.setFilesHaveBeenChangedByAnotherProcess(true);
      this.fastcoinController.fireFilesHaveBeenChangedByAnotherProcess(perWalletModelData);
    } else {
      // Put sending message and remove the send button.
      sendFastcoinConfirmPanel.setMessageText(controller.getLocaliser().getString("sendFastcoinNowAction.sendingFastcoin"), "");

      // Get the label and address out of the wallet preferences.
      String sendAddress = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_ADDRESS);
      String sendLabel = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_LABEL);

      if (sendLabel != null && !sendLabel.equals("")) {
        WalletInfoData addressBook = perWalletModelData.getWalletInfo();
        addressBook.addSendingAddress(new WalletAddressBookData(sendLabel, sendAddress));
      }

      char[] walletPassword = walletPasswordField.getPassword();

      if (this.fastcoinController.getModel().getActiveWallet() != null
              && this.fastcoinController.getModel().getActiveWallet().getEncryptionType() != EncryptionType.UNENCRYPTED) {
        // Encrypted wallet.
        if (walletPassword == null || walletPassword.length == 0) {
          // User needs to enter password.
          sendFastcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("showExportPrivateKeysAction.youMustEnterTheWalletPassword"), "");
          return;
        }

        try {
          if (!this.fastcoinController.getModel().getActiveWallet().checkPassword(CharBuffer.wrap(walletPassword))) {
            // The password supplied is incorrect.
            sendFastcoinConfirmPanel.setMessageText(
                    controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.passwordIsIncorrect"),
                    "");
            return;
          }
        } catch (KeyCrypterException kce) {
          log.debug(kce.getClass().getCanonicalName() + " " + kce.getMessage());
          // The password supplied is probably incorrect.
          sendFastcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("createNewReceivingAddressSubmitAction.passwordIsIncorrect"), "");
          return;
        }
      }

      // Double check wallet is not busy then declare that the active wallet is busy with the task
      if (!perWalletModelData.isBusy()) {
        perWalletModelData.setBusy(true);
        perWalletModelData.setBusyTaskVerbKey("sendFastcoinNowAction.sendingFastcoin");

        this.fastcoinController.fireWalletBusyChange(true);
        sendFastcoinConfirmPanel.setMessageText(controller.getLocaliser().getString("sendFastcoinNowAction.sendingFastcoin"), "");
        sendFastcoinConfirmPanel.invalidate();
        sendFastcoinConfirmPanel.validate();
        sendFastcoinConfirmPanel.repaint();

        performSend(perWalletModelData, sendRequest, CharBuffer.wrap(walletPassword));
      }
    }
  }

  /**
   * Send the transaction directly.
   */
  private void performSend(WalletData perWalletModelData, SendRequest sendRequest, CharSequence walletPassword) {
    String message = null;

    boolean sendWasSuccessful = Boolean.FALSE;
    try {
      if (sendRequest != null && sendRequest.tx != null) {
        log.debug("Sending from wallet " + perWalletModelData.getWalletFilename() + ", tx = " + sendRequest.tx.toString());
      }

      if (useTestParameters) {
        log.debug("Using test parameters - not really sending");
        if (sayTestSendWasSuccessful) {
          sendWasSuccessful = Boolean.TRUE;
          log.debug("Using test parameters - saying send was successful");
        } else {
          message = "test - send failed";
          log.debug("Using test parameters - saying send failed");
        }
      } else {
        transaction = this.fastcoinController.getFastcoinWalletService().sendCoins(perWalletModelData, sendRequest, walletPassword);
        if (transaction == null) {
          // a null transaction returned indicates there was not
          // enough money (in spite of our validation)
          message = controller.getLocaliser().getString("sendFastcoinNowAction.thereWereInsufficientFundsForTheSend");
          log.error(message);
        } else {
          sendWasSuccessful = Boolean.TRUE;
          log.debug("Sent transaction was:\n" + transaction.toString());
        }
      }
    } catch (KeyCrypterException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (WalletSaveException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (AddressFormatException e) {
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } catch (IllegalStateException e) {
      log.error(e.getMessage(), e);
      message = controller.getLocaliser().getString("sendFastcoinNowAction.pingFailure");
    } catch (Exception e) {
      // Really trying to catch anything that goes wrong with the send fastcoin.
      log.error(e.getMessage(), e);
      message = e.getMessage();
    } finally {
      // Save the wallet.
      try {
        this.fastcoinController.getFileHandler().savePerWalletModelData(perWalletModelData, false);
      } catch (WalletSaveException e) {
        log.error(e.getMessage(), e);
        message = e.getMessage();
      }

      if (sendWasSuccessful) {
        String successMessage = controller.getLocaliser().getString("sendFastcoinNowAction.fastcoinSentOk");
        if (sendFastcoinConfirmPanel != null && (sendFastcoinConfirmPanel.isVisible() || useTestParameters)) {
          sendFastcoinConfirmPanel.setMessageText(
                  controller.getLocaliser().getString("sendFastcoinNowAction.fastcoinSentOk"));
          sendFastcoinConfirmPanel.showOkButton();
          sendFastcoinConfirmPanel.clearAfterSend();
        } else {
          MessageManager.INSTANCE.addMessage(new Message(successMessage));
        }
      } else {
        log.error(message);

        if (message != null && message.length() > MAX_LENGTH_OF_ERROR_MESSAGE) {
          message = message.substring(0, MAX_LENGTH_OF_ERROR_MESSAGE) + "...";
        }

        String errorMessage = controller.getLocaliser().getString("sendFastcoinNowAction.fastcoinSendFailed");
        if (sendFastcoinConfirmPanel != null && (sendFastcoinConfirmPanel.isVisible() || useTestParameters)) {
          sendFastcoinConfirmPanel.setMessageText(errorMessage, message);
        } else {
          MessageManager.INSTANCE.addMessage(new Message(errorMessage + " " + message));
        }
      }

      // Declare that wallet is no longer busy with the task.
      perWalletModelData.setBusyTaskKey(null);
      perWalletModelData.setBusy(false);
      this.fastcoinController.fireWalletBusyChange(false);

      log.debug("firing fireRecreateAllViews...");
      controller.fireRecreateAllViews(false);
      log.debug("firing fireRecreateAllViews...done");
    }
  }

  public Transaction getTransaction() {
    return transaction;
  }

  void setTestParameters(boolean useTestParameters, boolean sayTestSendWasSuccessful) {
    this.useTestParameters = useTestParameters;
    this.sayTestSendWasSuccessful = sayTestSendWasSuccessful;
  }

  @Override
  public void walletBusyChange(boolean newWalletIsBusy) {
    // Update the enable status of the action to match the wallet busy status.
    if (this.fastcoinController.getModel().getActivePerWalletModelData().isBusy()) {
      // MultiBitWallet is busy with another operation that may change the private keys - Action is disabled.
      putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("fastcoinWalletSubmitAction.walletIsBusy",
              new Object[]{controller.getLocaliser().getString(this.fastcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())}));
      setEnabled(false);
    } else {
      // Enable unless wallet has been modified by another process.
      if (!this.fastcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
        putValue(SHORT_DESCRIPTION, controller.getLocaliser().getString("sendFastcoinConfirmAction.tooltip"));
        setEnabled(true);
      }
    }
  }
}