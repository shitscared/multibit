/**
 * Copyright 2013 wallet.org
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
package org.multibit.viewsystem.swing.view.panels;

import com.google.fastcoin.core.Address;
import com.google.fastcoin.core.AddressFormatException;
import com.google.fastcoin.core.Utils;
import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.exchange.CurrencyConverter;
import org.multibit.exchange.CurrencyConverterResult;
import org.multibit.model.fastcoin.FastcoinModel;
import org.multibit.model.fastcoin.WalletAddressBookData;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.action.*;
import org.multibit.viewsystem.swing.view.components.*;
import org.multibit.viewsystem.swing.view.models.AddressBookTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class SendFastcoinPanel extends AbstractTradePanel implements Viewable {

  private static final long serialVersionUID = -2065108865497111662L;
  private static SendFastcoinConfirmAction sendFastcoinConfirmAction;
  private static boolean enableSendButton = false;
  private static FastcoinWalletButton sendButton;
  private FastcoinWalletButton pasteAddressButton;

  private static SendFastcoinPanel thisPanel;

  private static String regularTooltipText = "";
  private static String pleaseWaitTooltipText = "";

  public SendFastcoinPanel(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame) {
    super(mainFrame, fastcoinController);
    thisPanel = this;
    checkDeleteSendingEnabled();

    regularTooltipText = controller.getLocaliser().getString("sendFastcoinAction.tooltip");
    pleaseWaitTooltipText = controller.getLocaliser().getString("sendFastcoinAction.pleaseWait.tooltip");
  }

  public static void setEnableSendButton(boolean enableSendButton) {
    SendFastcoinPanel.enableSendButton = enableSendButton;

    if (EventQueue.isDispatchThread()) {
      enableSendButtonOnSwingThread();
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          enableSendButtonOnSwingThread();
        }
      });
    }
  }

  private static void enableSendButtonOnSwingThread() {
    if (sendFastcoinConfirmAction != null) {
      sendFastcoinConfirmAction.setEnabled(SendFastcoinPanel.enableSendButton);
    }

    final String finalRegularTooltipText = regularTooltipText;
    final String finalPleaseWaitTooltipText = pleaseWaitTooltipText;

    if (sendButton != null) {
      if (SendFastcoinPanel.enableSendButton) {
        sendButton.setEnabled(true);
        sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalRegularTooltipText));
      } else {
        sendButton.setEnabled(false);
        sendButton.setToolTipText(HelpContentsPanel.createTooltipText(finalPleaseWaitTooltipText));
      }
    }

    if (thisPanel != null) {
      thisPanel.invalidate();
      thisPanel.validate();
      thisPanel.repaint();
    }
  }

  @Override
  protected boolean isReceiveFastcoin() {
    return false;
  }

  @Override
  public Action getCreateNewAddressAction() {
    return new CreateNewSendingAddressAction(super.fastcoinController, this);
  }

  @Override
  protected Action getDeleteAddressAction() {
    if (deleteAddressAction == null) {
      return new DeleteSendingAddressAction(this.fastcoinController, mainFrame, this);
    } else {
      return deleteAddressAction;
    }
  }

  @Override
  public void checkDeleteSendingEnabled() {
    AddressBookTableModel addressesTableModel = getAddressesTableModel();
    if (deleteAddressAction != null) {
      deleteAddressAction.setEnabled(addressesTableModel != null && addressesTableModel.getRowCount() > 0);
    }
  }

  @Override
  public String getAddressConstant() {
    return FastcoinModel.SEND_ADDRESS;
  }

  @Override
  public String getLabelConstant() {
    return FastcoinModel.SEND_LABEL;
  }

  @Override
  public String getAmountConstant() {
    return FastcoinModel.SEND_AMOUNT;
  }

  /**
   * method for concrete impls to populate the localisation map
   */
  @Override
  protected void populateLocalisationMap() {
    localisationKeyConstantToKeyMap.put(ADDRESSES_TITLE, "sendFastcoinPanel.sendingAddressesTitle");
    localisationKeyConstantToKeyMap.put(CREATE_NEW_TOOLTIP, "createOrEditAddressAction.createSending.tooltip");
    localisationKeyConstantToKeyMap.put(DELETE_TOOLTIP, "deleteSendingAddressSubmitAction.tooltip");
  }

  @Override
  protected JPanel createFormPanel(JPanel formPanel, GridBagConstraints constraints) {
    formPanel.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);

    JPanel buttonPanel = new JPanel();
    FlowLayout flowLayout = new FlowLayout();
    flowLayout.setAlignment(FlowLayout.LEADING);
    buttonPanel.setLayout(flowLayout);

    formPanel.setLayout(new GridBagLayout());

    // create stents and forcers
    createFormPanelStentsAndForcers(formPanel, constraints);

    FastcoinWalletLabel addressLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("sendFastcoinPanel.addressLabel"));
    addressLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinPanel.addressLabel.tooltip")));
    addressLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 4.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_END;
    formPanel.add(addressLabel, constraints);
    String receiveAddressText = controller.getLocaliser().getString("receiveFastcoinPanel.addressLabel");
    FastcoinWalletLabel notUsedReceiveAddressLabel = new FastcoinWalletLabel(receiveAddressText);
    formPanel.add(FastcoinWalletTitledPanel.createStent((int) notUsedReceiveAddressLabel.getPreferredSize().getWidth()), constraints);

    int longFieldWidth = fontMetrics.stringWidth(FastcoinWalletFrame.EXAMPLE_LONG_FIELD_TEXT);
    addressTextField = new FastcoinWalletTextField("", 24, controller);
    addressTextField.setHorizontalAlignment(JTextField.LEADING);
    addressTextField.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));
    addressTextField.setPreferredSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));
    addressTextField.setMaximumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() + TEXTFIELD_VERTICAL_DELTA));

    addressTextField.addKeyListener(new QRCodeKeyListener());
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 2;
    constraints.gridy = 1;
    constraints.weightx = 1.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 3;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(addressTextField, constraints);

    ImageIcon copyIcon = ImageLoader.createImageIcon(ImageLoader.COPY_ICON_FILE);
    CopySendAddressAction copyAddressAction = new CopySendAddressAction(controller, this, copyIcon);
    FastcoinWalletButton copyAddressButton = new FastcoinWalletButton(copyAddressAction, controller);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 6;
    constraints.gridy = 1;
    constraints.weightx = 1;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(copyAddressButton, constraints);

    ImageIcon pasteIcon = ImageLoader.createImageIcon(ImageLoader.PASTE_ICON_FILE);
    PasteAddressAction pasteAddressAction = new PasteAddressAction(super.fastcoinController, this, pasteIcon);
    pasteAddressButton = new FastcoinWalletButton(pasteAddressAction, controller);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 8;
    constraints.gridy = 1;
    constraints.weightx = 10.0;
    constraints.weighty = 0.2;
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(pasteAddressButton, constraints);

    FastcoinWalletLabel labelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("sendFastcoinPanel.labelLabel"));
    labelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinPanel.labelLabel.tooltip")));
    labelLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 0;
    constraints.gridy = 3;
    constraints.weightx = 0.1;
    constraints.weighty = 1.0;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_END;
    formPanel.add(labelLabel, constraints);

    JTextField aTextField = new JTextField();
    labelTextArea = new FastcoinWalletTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS, 20, controller);
    labelTextArea.setBorder(aTextField.getBorder());
    labelTextArea.addKeyListener(new QRCodeKeyListener());

    final JScrollPane labelScrollPane = new JScrollPane(labelTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    labelScrollPane.setOpaque(true);
    labelScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
    labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    labelScrollPane.getViewport().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        if (labelScrollPane.getVerticalScrollBar().isVisible()) {
          labelScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));
        } else {
          labelScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }
      }
    });
    labelScrollPane.setMinimumSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
    labelScrollPane.setPreferredSize(new Dimension(longFieldWidth, getFontMetrics(FontSizer.INSTANCE.getAdjustedDefaultFont())
            .getHeight() * AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + TEXTFIELD_VERTICAL_DELTA + 6));
    labelScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
    labelScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 2;
    constraints.gridy = 3;
    constraints.weightx = 0.6;
    constraints.weighty = 1.0;
    constraints.gridwidth = 3;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(labelScrollPane, constraints);

    FastcoinWalletLabel amountLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("sendFastcoinPanel.amountLabel"));
    amountLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinPanel.amountLabel.tooltip")));
    amountLabel.setHorizontalAlignment(JLabel.TRAILING);
    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 0;
    constraints.gridy = 5;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0.1;
    constraints.weighty = 0.20;
    constraints.anchor = GridBagConstraints.LINE_END;
    formPanel.add(amountLabel, constraints);

    JPanel amountPanel = createAmountPanel();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 2;
    constraints.gridy = 5;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.weightx = 0.1;
    constraints.weighty = 0.20;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(amountPanel, constraints);

    notificationLabel = new FastcoinWalletLabel("");
    notificationLabel.setForeground(Color.RED);
    //notificationLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN));
    constraints.fill = GridBagConstraints.BOTH;
    constraints.gridx = 2;
    constraints.gridy = 6;
    constraints.gridwidth = 8;
    constraints.gridheight = 3;
    constraints.weightx = 0.1;
    constraints.weighty = 0.1;
    constraints.anchor = GridBagConstraints.ABOVE_BASELINE_LEADING;
    formPanel.add(notificationLabel, constraints);

    Action helpAction;
    if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
      helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
              "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
              HelpContentsPanel.HELP_SENDING_URL);
    } else {
      helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
              "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
              HelpContentsPanel.HELP_SENDING_URL);
    }
    HelpButton helpButton = new HelpButton(helpAction, controller);
    helpButton.setText("");

    String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[]{
            controller.getLocaliser().getString("sendFastcoinPanel.helpLabel1.message"),
            controller.getLocaliser().getString("sendFastcoinPanel.helpLabel2.message"),
            controller.getLocaliser().getString("sendFastcoinPanel.helpLabel3.message"), "\n",
            controller.getLocaliser().getString("fastcoinWalletFrame.helpMenuTooltip")});
    helpButton.setToolTipText(tooltipText);
    helpButton.setHorizontalAlignment(SwingConstants.LEADING);
    helpButton.setBorder(BorderFactory.createEmptyBorder(0, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT, HELP_BUTTON_INDENT));
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 8;
    constraints.weightx = 1;
    constraints.weighty = 0.3;
    constraints.gridwidth = 1;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.BELOW_BASELINE_LEADING;
    formPanel.add(helpButton, constraints);

    sendFastcoinConfirmAction = new SendFastcoinConfirmAction(super.fastcoinController, mainFrame, this);
    sendFastcoinConfirmAction.setEnabled(enableSendButton);
    sendButton = new FastcoinWalletButton(sendFastcoinConfirmAction, controller);
    if (enableSendButton) {
      sendButton.setEnabled(true);
      sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinAction.tooltip")));
    } else {
      sendButton.setEnabled(false);
      sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinAction.pleaseWait.tooltip")));
    }

    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 6;
    constraints.gridy = 5;
    constraints.weightx = 0.1;
    constraints.weighty = 0.1;
    constraints.gridwidth = 3;
    constraints.gridheight = 1;
    constraints.anchor = GridBagConstraints.LINE_START;
    formPanel.add(sendButton, constraints);

    Action sidePanelAction = new MoreOrLessAction(controller, this);
    sidePanelButton = new FastcoinWalletButton(sidePanelAction, controller);
    sidePanelButton.setBorder(BorderFactory.createEmptyBorder());
    sidePanelButton.setBorderPainted(false);
    sidePanelButton.setFocusPainted(false);
    sidePanelButton.setContentAreaFilled(false);

    displaySidePanel();

    constraints.fill = GridBagConstraints.NONE;
    constraints.gridx = 4;
    constraints.gridy = 3;
    constraints.weightx = 0.1;
    constraints.weighty = 0.3;
    constraints.gridwidth = 7;
    constraints.gridheight = 3;
    constraints.anchor = GridBagConstraints.BASELINE_TRAILING;
    formPanel.add(sidePanelButton, constraints);

    return formPanel;
  }

  @Override
  public String getAddress() {
    if (addressTextField != null) {
      return addressTextField.getText();
    } else {
      return "";
    }
  }

  @Override
  public void loadForm() {
    // get the current address, label and amount from the model
    String address = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_ADDRESS);
    String label = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_LABEL);
    String amountNotLocalised = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_AMOUNT);

    if (amountFSTTextField != null) {
      CurrencyConverterResult converterResult = CurrencyConverter.INSTANCE.parseToFSTNotLocalised(amountNotLocalised);

      if (converterResult.isFstMoneyValid()) {
        parsedAmountFST = converterResult.getFstMoney();
        String amountLocalised = CurrencyConverter.INSTANCE.getFSTAsLocalisedString(converterResult.getFstMoney());
        amountFSTTextField.setText(amountLocalised);
        if (notificationLabel != null) {
          notificationLabel.setText("");
        }
      } else {
        parsedAmountFST = null;
        amountFSTTextField.setText("");
        if (notificationLabel != null) {
          notificationLabel.setText(converterResult.getFstMessage());
        }
      }
    }

    if (address != null) {
      addressTextField.setText(address);
    } else {
      addressTextField.setText("");
    }
    if (label != null) {
      labelTextArea.setText(label);
    } else {
      labelTextArea.setText("");
    }

    // if there is a pending 'handleopenURI' that needs pasting into the
    // send form, do it
    String performPasteNow = this.fastcoinController.getModel().getActiveWalletPreference(FastcoinModel.SEND_PERFORM_PASTE_NOW);
    if (Boolean.TRUE.toString().equalsIgnoreCase(performPasteNow)) {
      try {
        Address decodeAddress = new Address(this.fastcoinController.getModel().getNetworkParameters(), address);
        processDecodedString(com.google.fastcoin.uri.FastcoinURI.convertToFastcoinURI(decodeAddress, Utils.toNanoCoins(amountNotLocalised), label, null), null);
        this.fastcoinController.getModel().setActiveWalletPreference(FastcoinModel.SEND_PERFORM_PASTE_NOW, "false");
        sendButton.requestFocusInWindow();

        mainFrame.bringToFront();
      } catch (AddressFormatException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void setAddressBookDataByRow(WalletAddressBookData addressBookData) {
    addressTextField.setText(addressBookData.getAddress());
    addressesTableModel.setAddressBookDataByRow(addressBookData, selectedAddressRowModel, false);
  }

  @Override
  public void displayView(DisplayHint displayHint) {
    super.displayView(displayHint);

    if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
      return;
    }

    JTextField aTextField = new JTextField();

    labelTextArea.setBorder(aTextField.getBorder());

    String bringToFront = controller.getModel().getUserPreference(FastcoinModel.BRING_TO_FRONT);
    if (Boolean.TRUE.toString().equals(bringToFront)) {
      controller.getModel().setUserPreference(FastcoinModel.BRING_TO_FRONT, "false");
      mainFrame.bringToFront();
    }

    // disable any new changes if another process has changed the wallet
    if (this.fastcoinController.getModel().getActivePerWalletModelData() != null
            && this.fastcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
      // files have been changed by another process - disallow edits
      mainFrame.setUpdatesStoppedTooltip(addressTextField);
      addressTextField.setEditable(false);
      addressTextField.setEnabled(false);

      if (sendButton != null) {
        sendButton.setEnabled(false);
        mainFrame.setUpdatesStoppedTooltip(sendButton);
      }
      if (pasteAddressButton != null) {
        pasteAddressButton.setEnabled(false);
        mainFrame.setUpdatesStoppedTooltip(pasteAddressButton);
      }
      titleLabel.setText(controller.getLocaliser().getString("sendFastcoinPanel.sendingAddressesTitle.mayBeOutOfDate"));
      mainFrame.setUpdatesStoppedTooltip(titleLabel);
    } else {
      addressTextField.setToolTipText(null);
      addressTextField.setEditable(true);
      addressTextField.setEnabled(true);

      if (sendButton != null) {
        if (SendFastcoinPanel.enableSendButton) {
          sendButton.setEnabled(true);
          sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinAction.tooltip")));
        } else {
          sendButton.setEnabled(false);
          sendButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("sendFastcoinAction.pleaseWait.tooltip")));
        }
      }
      if (pasteAddressButton != null) {
        pasteAddressButton.setEnabled(true);
        pasteAddressButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("pasteAddressAction.tooltip")));
      }
      titleLabel.setText(controller.getLocaliser().getString("sendFastcoinPanel.sendingAddressesTitle"));
      titleLabel.setToolTipText(null);
    }
    checkDeleteSendingEnabled();
  }

  @Override
  public Icon getViewIcon() {
    return ImageLoader.createImageIcon(ImageLoader.SEND_FASTCOIN_ICON_FILE);
  }

  @Override
  public String getViewTitle() {
    return controller.getLocaliser().getString("sendFastcoinConfirmAction.text");
  }

  @Override
  public String getViewTooltip() {
    return controller.getLocaliser().getString("sendFastcoinConfirmAction.tooltip");
  }

  @Override
  public View getViewId() {
    return View.SEND_FASTCOIN_VIEW;
  }

  public SendFastcoinConfirmAction getSendFastcoinConfirmAction() {
    return sendFastcoinConfirmAction;
  }
}
