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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.multibit.controller.fastcoin.FastcoinController;
import org.fastcoinj.wallet.Protos.Wallet.EncryptionType;
import org.multibit.controller.Controller;
import org.multibit.model.fastcoin.WalletBusyListener;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.action.HelpContextAction;
import org.multibit.viewsystem.swing.action.SignMessageSubmitAction;
import org.multibit.viewsystem.swing.view.components.HelpButton;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletButton;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletLabel;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletTextArea;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletTitledPanel;

/**
 * View for signing messages.
 */
public class SignMessagePanel extends JPanel implements Viewable, WalletBusyListener {

    private static final long serialVersionUID = 444992294329957705L;

    private final Controller controller;
    private final FastcoinController fastcoinController;

    private FastcoinWalletFrame mainFrame;

    private FastcoinWalletLabel messageLabel1;
    private FastcoinWalletLabel messageLabel2;

    private FastcoinWalletLabel walletTextLabel;
    private JPasswordField walletPasswordField;
    private FastcoinWalletLabel walletPasswordPromptLabel;

    private FastcoinWalletTextArea addressTextArea;
    private FastcoinWalletLabel addressLabel;
    
    private FastcoinWalletTextArea messageTextArea;
    private FastcoinWalletLabel messageLabel;
    
    private FastcoinWalletTextArea signatureTextArea;
    private FastcoinWalletLabel signatureLabel;
    
    private SignMessageSubmitAction signMessageSubmitAction;
    private FastcoinWalletButton clearAllButton;
    
    private static final int FIELD_WIDTH = 360;
    private static final int FIELD_HEIGHT = 30;

    /**
     * Creates a new {@link SignMessagePanel}.
     */
    public SignMessagePanel(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame) {
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        this.mainFrame = mainFrame;

        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        initUI();
        
        walletBusyChange(this.fastcoinController.getModel().getActivePerWalletModelData().isBusy());
        this.fastcoinController.registerWalletBusyListener(this);
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setMinimumSize(new Dimension(800, 480));
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        String[] keys = new String[] { "showExportPrivateKeysPanel.walletPasswordPrompt", "sendFastcoinPanel.addressLabel",
                "verifyMessagePanel.message.text", "verifyMessagePanel.signature.text" };

        int stentWidth = FastcoinWalletTitledPanel.calculateStentWidthForKeys(controller.getLocaliser(), keys, this)
                + ExportPrivateKeysPanel.STENT_DELTA;

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel instructionsPanel = createInstructionsPanel(stentWidth);
        mainPanel.add(instructionsPanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(FastcoinWalletTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel walletPanel = createWalletPanel(stentWidth);
        mainPanel.add(walletPanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(FastcoinWalletTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel messagePanel = createMessagePanel(stentWidth);
        mainPanel.add(messagePanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(FastcoinWalletTitledPanel.createStent(12, 12), constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 1;
        constraints.weightx = 0.4;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, constraints);

        messageLabel1 = new FastcoinWalletLabel(" ");
        messageLabel1.setOpaque(false);
        messageLabel1.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel1.setHorizontalAlignment(JLabel.LEADING);
        messageLabel1.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 7;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel1, constraints);

        messageLabel2 = new FastcoinWalletLabel(" ");
        messageLabel2.setOpaque(false);
        messageLabel2.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel2.setHorizontalAlignment(JLabel.LEADING);
        messageLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 8;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel2, constraints);

        Action helpAction;
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
                    "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_SIGN_AND_VERIFY_MESSAGE_URL);
        } else {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
                    "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_SIGN_AND_VERIFY_MESSAGE_URL);
        }   
               HelpButton helpButton = new HelpButton(helpAction, controller);
        helpButton.setText("");

        String tooltipText = HelpContentsPanel.createMultilineTooltipText(new String[] { controller.getLocaliser().getString(
                "fastcoinWalletFrame.helpMenuTooltip") });
        helpButton.setToolTipText(tooltipText);
        helpButton.setHorizontalAlignment(SwingConstants.LEADING);
        helpButton.setBorder(BorderFactory.createEmptyBorder(0, AbstractTradePanel.HELP_BUTTON_INDENT,
                AbstractTradePanel.HELP_BUTTON_INDENT,  AbstractTradePanel.HELP_BUTTON_INDENT));
        helpButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 0;
        constraints.gridy = 9;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.BASELINE_LEADING;
        mainPanel.add(helpButton, constraints);

        JLabel filler2 = new JLabel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 1;
        constraints.weightx = 1;
        constraints.weighty = 100;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(filler2, constraints);

        JScrollPane mainScrollPane = new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getViewport().setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        mainScrollPane.getViewport().setOpaque(true);
        mainScrollPane.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        mainScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);

        add(mainScrollPane, BorderLayout.CENTER);
    }

    private JPanel createInstructionsPanel(int stentWidth) {
        FastcoinWalletTitledPanel instructionsPanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "resetTransactionsPanel.explainTitle"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("signMessagePanel.instructions.text1"), 3, instructionsPanel);

        FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("signMessagePanel.instructions.text2"), 4, instructionsPanel);

        return instructionsPanel;
    }
    
    private JPanel createWalletPanel(int stentWidth) {
        FastcoinWalletTitledPanel inputWalletPanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "showExportPrivateKeysPanel.walletPasswordPrompt"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();

        walletTextLabel = FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("signMessagePanel.wallet.text"), 3, inputWalletPanel);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(FastcoinWalletTitledPanel.createStent(stentWidth, (int) (ExportPrivateKeysPanel.STENT_HEIGHT * 0.5)), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        inputWalletPanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);
        
        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(filler3, constraints);

        walletPasswordPromptLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("showExportPrivateKeysPanel.walletPasswordPrompt"));
        walletPasswordPromptLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        inputWalletPanel.add(walletPasswordPromptLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 8;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        inputWalletPanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        walletPasswordField = new JPasswordField(24);
        walletPasswordField.setMinimumSize(new Dimension(200, 20));
        walletPasswordField.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(walletPasswordField, constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(filler4, constraints);

        return inputWalletPanel;
    }

    private JPanel createMessagePanel(int stentWidth) {
        FastcoinWalletTitledPanel messagePanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "signMessagePanel.message.title"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();
        FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("signMessagePanel.instructions.text3"), 3, messagePanel);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(FastcoinWalletTitledPanel.createStent(stentWidth, ExportPrivateKeysPanel.STENT_HEIGHT), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler3, constraints);

        addressLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("sendFastcoinPanel.addressLabel"));
        addressLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(addressLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 9;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        //addressTextArea = new FastcoinWalletTextField("", 30, controller);
        JTextField aTextField = new JTextField();
        addressTextArea = new FastcoinWalletTextArea("", 1, 30, controller);
        addressTextArea.setBorder(aTextField.getBorder());

        addressTextArea.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        addressTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(addressTextArea, constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler4, constraints);

        JTextField secondTextField = new JTextField();
        messageTextArea = new FastcoinWalletTextArea("", AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1, 20, controller);
        messageTextArea.setBorder(secondTextField.getBorder());
        
        messageTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        
        final JScrollPane messageScrollPane = new JScrollPane(messageTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setOpaque(true);
        messageScrollPane.setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); 
        messageScrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (messageScrollPane.getVerticalScrollBar().isVisible()) {
                    messageScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));          
                } else {
                    messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));              
                }
            }
        });
        messageScrollPane.setMinimumSize(new Dimension(FIELD_WIDTH, (AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1) * FIELD_HEIGHT + 6));
        messageScrollPane.setPreferredSize(new Dimension(FIELD_WIDTH, (AbstractTradePanel.PREFERRED_NUMBER_OF_LABEL_ROWS + 1) * FIELD_HEIGHT + 6));
        messageScrollPane.getHorizontalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        messageScrollPane.getVerticalScrollBar().setUnitIncrement(CoreModel.SCROLL_INCREMENT);
        if (messageScrollPane.getVerticalScrollBar().isVisible()) {
            messageScrollPane.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY));          
        } else {
            messageScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));              
        }

        messageLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("verifyMessagePanel.message.text"));
        messageLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(messageLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 11;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 11;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(messageScrollPane, constraints);

        JPanel filler5 = new JPanel();
        filler5.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 12;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(filler5, constraints);

        signatureLabel = new FastcoinWalletLabel(controller.getLocaliser().getString("verifyMessagePanel.signature.text"));
        signatureLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 13;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        messagePanel.add(signatureLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 13;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        messagePanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        JTextField anotherTextField = new JTextField();
        signatureTextArea = new FastcoinWalletTextArea("", 2, 30, controller);
        signatureTextArea.setBorder(anotherTextField.getBorder());
        signatureTextArea.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setMaximumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT * 2));
        signatureTextArea.setLineWrap(true);
        signatureTextArea.setEditable(false);
        signatureTextArea.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 3;
        constraints.gridy = 13;
        constraints.weightx = 0.3;
        constraints.weighty = 0.4;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        messagePanel.add(signatureTextArea, constraints);

        return messagePanel;
    }
 

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEADING);
        buttonPanel.setLayout(flowLayout);
        buttonPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        signMessageSubmitAction = new SignMessageSubmitAction(this.fastcoinController, mainFrame, this,
                ImageLoader.createImageIcon(ImageLoader.MESSAGE_SIGN_ICON_FILE));
        FastcoinWalletButton submitButton = new FastcoinWalletButton(signMessageSubmitAction, controller);
        submitButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        buttonPanel.add(submitButton);
        
        clearAllButton = new FastcoinWalletButton(controller.getLocaliser().getString("signMessagePanel.clearAll.text"));
        clearAllButton.setToolTipText(controller.getLocaliser().getString("signMessagePanel.clearAll.tooltip"));
        clearAllButton.setIcon(ImageLoader.createImageIcon(ImageLoader.DELETE_ICON_FILE));
        clearAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                walletPasswordField.setText("");
                addressTextArea.setText("");
                messageTextArea.setText("");
                signatureTextArea.setText("");
                messageLabel1.setText(" ");
                messageLabel2.setText(" ");
            }
        }); 
        clearAllButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        buttonPanel.add(clearAllButton);

        return buttonPanel;
    }

    @Override
    public void displayView(DisplayHint displayHint) {
        // If it is a wallet transaction change no need to update.
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
       
        boolean walletPasswordRequired = false;
        if (this.fastcoinController.getModel().getActiveWallet() != null && this.fastcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            walletPasswordRequired = true;
        }
        enableWalletPassword(walletPasswordRequired);

        walletBusyChange(this.fastcoinController.getModel().getActivePerWalletModelData().isBusy());
        
        messageLabel1.setText(" ");
        messageLabel2.setText(" ");
    }

    @Override
    public void navigateAwayFromView() {
    }

    public void setMessageText1(String message1) {
        if (messageLabel1 != null) {
            messageLabel1.setText(message1);
        }
    }

    public String getMessageText1() {
        if (messageLabel1 != null) {
            return messageLabel1.getText();
        } else {
            return "";
        }
    }

    public void setMessageText2(String message2) {
        if (messageLabel2 != null) {
            messageLabel2.setText(message2);
        }
    }

    public String getMessageText2() {
        if (messageLabel2 != null) {
            return messageLabel2.getText();
        } else {
            return "";
        }
    }

    @Override
    public Icon getViewIcon() {
        return ImageLoader.createImageIcon(ImageLoader.MESSAGE_SIGN_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("signMessageAction.text");
    }

    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("signMessageAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.SIGN_MESSAGE_VIEW;
    }
    
    public SignMessageSubmitAction getSignMessageSubmitAction() {
        return signMessageSubmitAction;
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {       
        // Update the enable status of the action to match the wallet busy status.
        if (this.fastcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // MultiBitWallet is busy with another operation that may change the private keys - Action is disabled.
            signMessageSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("fastcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.fastcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())})));
            signMessageSubmitAction.setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!this.fastcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                signMessageSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("signMessageAction.tooltip")));
                signMessageSubmitAction.setEnabled(true);
            }
        }
    }
    
    private void enableWalletPassword(boolean enableWalletPassword) {
        // Enable/ disable the wallet password fields.
        walletPasswordField.setEnabled(enableWalletPassword);
        walletPasswordPromptLabel.setEnabled(enableWalletPassword);
        walletTextLabel.setEnabled(enableWalletPassword);
    }

    public FastcoinWalletTextArea getMessageTextArea() {
        return messageTextArea;
    }

    public FastcoinWalletTextArea getAddressTextArea() {
        return addressTextArea;
    }

    public FastcoinWalletTextArea getSignatureTextArea() {
        return signatureTextArea;
    }

    public JPasswordField getWalletPasswordField() {
        return walletPasswordField;
    }
}