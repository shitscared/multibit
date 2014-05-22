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
package org.multibit.viewsystem.swing.view.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import org.multibit.controller.Controller;
import org.multibit.controller.fastcoin.FastcoinController;
import org.multibit.crypto.KeyCrypterOpenSSL;
import org.multibit.file.PrivateKeyAndDate;
import org.multibit.file.PrivateKeysHandler;
import org.multibit.file.PrivateKeysHandlerException;
import org.multibit.model.fastcoin.FastcoinModel;
import org.multibit.model.fastcoin.WalletBusyListener;
import org.multibit.model.core.CoreModel;
import org.multibit.utils.ImageLoader;
import org.multibit.viewsystem.DisplayHint;
import org.multibit.viewsystem.View;
import org.multibit.viewsystem.Viewable;
import org.multibit.viewsystem.swing.ColorAndFontConstants;
import org.multibit.viewsystem.swing.FastcoinWalletFrame;
import org.multibit.viewsystem.swing.action.HelpContextAction;
import org.multibit.viewsystem.swing.action.ImportPrivateKeysSubmitAction;
import org.multibit.viewsystem.swing.view.PrivateKeyFileFilter;
import org.multibit.viewsystem.swing.view.components.FontSizer;
import org.multibit.viewsystem.swing.view.components.HelpButton;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletButton;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletLabel;
import org.multibit.viewsystem.swing.view.components.FastcoinWalletTitledPanel;

import com.google.fastcoin.crypto.KeyCrypterException;

import org.fastcoinj.wallet.Protos.Wallet.EncryptionType;

import com.piuk.blockchain.MyWallet;
import com.piuk.blockchain.MyWalletEncryptedKeyFileFilter;
import com.piuk.blockchain.MyWalletPlainKeyFileFilter;

/**
 * The import private keys view.
 */
public class ImportPrivateKeysPanel extends JPanel implements Viewable, WalletBusyListener {

    private static final long serialVersionUID = 444992294329957705L;

    private final Controller controller;
    private final FastcoinController fastcoinController;

    private FastcoinWalletFrame mainFrame;

    private FastcoinWalletLabel walletFilenameLabel;

    private FastcoinWalletLabel walletDescriptionLabel;

    private FastcoinWalletButton chooseFilenameButton;

    private String chooseFilenameButtonText;
    private JFileChooser fileChooser;

    private FastcoinWalletLabel outputFilenameLabel;

    private FastcoinWalletLabel messageLabel1;
    private FastcoinWalletLabel messageLabel2;

    private String outputFilename;

    private FastcoinWalletLabel passwordInfoLabel;
    private JPasswordField passwordField1;
    private FastcoinWalletLabel passwordPromptLabel1;
    private FastcoinWalletButton unlockButton;

    private JPasswordField passwordField2;
    private FastcoinWalletLabel passwordPromptLabel2;

    private JPasswordField walletPasswordField;
    private FastcoinWalletLabel walletPasswordPromptLabel;

    private JLabel numberOfKeysLabel;
    private JLabel replayDateLabel;
    
    private ImportPrivateKeysSubmitAction importPrivateKeysSubmitAction;

    private KeyCrypterOpenSSL encrypterDecrypter;

    public FileFilter fastcoinWalletFileChooser;
    public FileFilter myWalletPlainFileChooser;
    public FileFilter myWalletEncryptedFileChooser;

    private Font adjustedFont;

    /**
     * Creates a new {@link ImportPrivateKeysPanel}.
     */
    public ImportPrivateKeysPanel(FastcoinController fastcoinController, FastcoinWalletFrame mainFrame) {
        this.fastcoinController = fastcoinController;
        this.controller = this.fastcoinController;
        this.mainFrame = mainFrame;

        setBackground(ColorAndFontConstants.VERY_LIGHT_BACKGROUND_COLOR);
        applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        outputFilename = "";

        initUI();
        
        walletBusyChange(this.fastcoinController.getModel().getActivePerWalletModelData().isBusy());
        this.fastcoinController.registerWalletBusyListener(this);
        
        enableImportFilePasswordPanel(false);
        passwordField1.setText("");
        passwordField2.setText("");

        boolean walletPasswordRequired = false;
        if (this.fastcoinController.getModel().getActiveWallet() != null && this.fastcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            walletPasswordRequired = true;
        }
        enableWalletPassword(walletPasswordRequired);
        
        encrypterDecrypter = new KeyCrypterOpenSSL();
        fastcoinWalletFileChooser = new PrivateKeyFileFilter(controller);
        myWalletPlainFileChooser = new MyWalletPlainKeyFileFilter();
        myWalletEncryptedFileChooser = new MyWalletEncryptedKeyFileFilter();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setMinimumSize(new Dimension(550, 160));
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        String[] keys = new String[] { "resetTransactionsPanel.walletDescriptionLabel",
                "resetTransactionsPanel.walletFilenameLabel", "showExportPrivateKeysPanel.passwordPrompt",
                "showExportPrivateKeysPanel.repeatPasswordPrompt", "showImportPrivateKeysPanel.numberOfKeys.text",
                "showImportPrivateKeysPanel.replayDate.text" };

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
        JPanel walletPanel = createWalletPanel(stentWidth);
        mainPanel.add(walletPanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel filenamePanel = createFilenamePanel(stentWidth);
        mainPanel.add(filenamePanel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 0.2;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel passwordPanel = createPasswordPanel(stentWidth);
        mainPanel.add(passwordPanel, constraints);

        JLabel filler1 = new JLabel();
        filler1.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1;
        constraints.weighty = 0.1;
        constraints.anchor = GridBagConstraints.CENTER;
        mainPanel.add(filler1, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 1;
        constraints.weightx = 0.4;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, constraints);

        messageLabel1 = new FastcoinWalletLabel("");
        messageLabel1.setOpaque(false);
        messageLabel1.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel1.setHorizontalAlignment(JLabel.LEADING);
        messageLabel1.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel1, constraints);

        messageLabel2 = new FastcoinWalletLabel("");
        messageLabel2.setOpaque(false);
        messageLabel2.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        messageLabel2.setHorizontalAlignment(JLabel.LEADING);
        messageLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.06;
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(messageLabel2, constraints);

        Action helpAction;
        if (ComponentOrientation.LEFT_TO_RIGHT == ComponentOrientation.getOrientation(controller.getLocaliser().getLocale())) {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_ICON_FILE,
                    "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_IMPORTING_PRIVATE_KEYS_URL);
        } else {
            helpAction = new HelpContextAction(controller, ImageLoader.HELP_CONTENTS_BIG_RTL_ICON_FILE,
                    "fastcoinWalletFrame.helpMenuText", "fastcoinWalletFrame.helpMenuTooltip", "fastcoinWalletFrame.helpMenuText",
                    HelpContentsPanel.HELP_IMPORTING_PRIVATE_KEYS_URL);
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
        constraints.gridy = 7;
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
        constraints.gridy = 8;
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

    private JPanel createWalletPanel(int stentWidth) {
        FastcoinWalletTitledPanel inputWalletPanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "showImportPrivateKeysPanel.wallet.title"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        GridBagConstraints constraints = new GridBagConstraints();

        FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("showImportPrivateKeysPanel.wallet.text"), 3, inputWalletPanel);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(FastcoinWalletTitledPanel.createStent(stentWidth, ExportPrivateKeysPanel.STENT_HEIGHT), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        inputWalletPanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS), constraints);

        FastcoinWalletLabel walletDescriptionLabelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString(
                "resetTransactionsPanel.walletDescriptionLabel"));
        walletDescriptionLabelLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        inputWalletPanel.add(walletDescriptionLabelLabel, constraints);

        walletDescriptionLabel = new FastcoinWalletLabel(this.fastcoinController.getModel().getActivePerWalletModelData().getWalletDescription());
        walletDescriptionLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 5;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(walletDescriptionLabel, constraints);

        FastcoinWalletLabel walletFilenameLabelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString(
                "resetTransactionsPanel.walletFilenameLabel"));
        walletFilenameLabelLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        inputWalletPanel.add(walletFilenameLabelLabel, constraints);

        walletFilenameLabel = new FastcoinWalletLabel(this.fastcoinController.getModel().getActiveWalletFilename());
        walletFilenameLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        inputWalletPanel.add(walletFilenameLabel, constraints);

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

    private JPanel createFilenamePanel(int stentWidth) {
        FastcoinWalletTitledPanel outputFilenamePanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "showImportPrivateKeysPanel.filename.title"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(" ", 1, outputFilenamePanel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(FastcoinWalletTitledPanel.getIndentPanel(1), constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 3;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(FastcoinWalletTitledPanel.createStent(stentWidth, ExportPrivateKeysPanel.STENT_HEIGHT), constraints);

        chooseFilenameButtonText = "";
        String chooseFilenameButtonText1 = controller.getLocaliser().getString("showImportPrivateKeysPanel.filename.text");
        String chooseFilenameButtonText2 = controller.getLocaliser().getString("showImportPrivateKeysPanel.filename.text.2");
        // If the second term is localised, use that, otherwise the first.
        if (controller.getLocaliser().getLocale().equals(Locale.ENGLISH)) {
            chooseFilenameButtonText = chooseFilenameButtonText2;
        } else {
            if (!"Import from ...".equals(chooseFilenameButtonText2)) {
                chooseFilenameButtonText = chooseFilenameButtonText2;
            } else {
                chooseFilenameButtonText = chooseFilenameButtonText1;
            }
        }
        chooseFilenameButton = new FastcoinWalletButton(chooseFilenameButtonText);
        chooseFilenameButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("showImportPrivateKeysPanel.filename.tooltip")));
        chooseFilenameButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        final FastcoinWalletButton finalChooseFilenameButton = chooseFilenameButton;
        chooseFilenameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                chooseFile(finalChooseFilenameButton);
            }
        });

        FastcoinWalletLabel walletFilenameLabelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString(
                "resetTransactionsPanel.walletFilenameLabel"));
        walletFilenameLabelLabel.setHorizontalAlignment(JLabel.TRAILING);
        walletFilenameLabelLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        outputFilenamePanel.add(walletFilenameLabelLabel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        outputFilenamePanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        outputFilenameLabel = new FastcoinWalletLabel(outputFilename);
        outputFilenameLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 4;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(outputFilenameLabel, constraints);

        JPanel filler0 = new JPanel();
        filler0.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 5;
        constraints.gridy = 4;
        constraints.weightx = 100;
        constraints.weighty = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        outputFilenamePanel.add(filler0, constraints);

        JPanel filler2 = new JPanel();
        filler2.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(filler2, constraints);

        FastcoinWalletLabel numberOfKeysLabelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString(
                "showImportPrivateKeysPanel.numberOfKeys.text"));
        numberOfKeysLabelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser()
                .getString("showImportPrivateKeysPanel.numberOfKeys.tooltip")));
        numberOfKeysLabelLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        outputFilenamePanel.add(numberOfKeysLabelLabel, constraints);

        JPanel filler3 = new JPanel();
        filler3.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(filler3, constraints);

        numberOfKeysLabel = new FastcoinWalletLabel(" ");
        numberOfKeysLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(numberOfKeysLabel, constraints);

        FastcoinWalletLabel replayDateLabelLabel = new FastcoinWalletLabel(controller.getLocaliser().getString(
                "showImportPrivateKeysPanel.replayDate.text"));
        replayDateLabelLabel.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("showImportPrivateKeysPanel.replayDate.tooltip")));
        replayDateLabelLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        outputFilenamePanel.add(replayDateLabelLabel, constraints);

        replayDateLabel = new FastcoinWalletLabel(" ");
        replayDateLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 3;
        constraints.gridy = 7;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(replayDateLabel, constraints);

        JPanel filler4 = new JPanel();
        filler4.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(filler4, constraints);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 9;
        constraints.weightx = 0.5;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(chooseFilenameButton, constraints);

        JPanel filler5 = new JPanel();
        filler5.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 10;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        outputFilenamePanel.add(filler5, constraints);

        return outputFilenamePanel;
    }

    private JPanel createPasswordPanel(int stentWidth) {
        // Do/do not password protect radios.
        FastcoinWalletTitledPanel passwordProtectPanel = new FastcoinWalletTitledPanel(controller.getLocaliser().getString(
                "showImportPrivateKeysPanel.password.title"), ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        GridBagConstraints constraints = new GridBagConstraints();

        passwordInfoLabel = FastcoinWalletTitledPanel.addLeftJustifiedTextAtIndent(
                controller.getLocaliser().getString("showImportPrivateKeysPanel.enterPassword"), 3, passwordProtectPanel);
        passwordInfoLabel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 4;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        passwordProtectPanel.add(FastcoinWalletTitledPanel.createStent(stentWidth, ExportPrivateKeysPanel.STENT_HEIGHT), constraints);

        passwordPromptLabel1 = new FastcoinWalletLabel(controller.getLocaliser().getString("showExportPrivateKeysPanel.passwordPrompt"));
        passwordPromptLabel1.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        passwordProtectPanel.add(passwordPromptLabel1, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 5;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        passwordProtectPanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        passwordField1 = new JPasswordField(24);
        passwordField1.setMinimumSize(new Dimension(200, 20));
        passwordField1.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 5;
        constraints.weightx = 0.3;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        passwordProtectPanel.add(passwordField1, constraints);

        passwordPromptLabel2 = new FastcoinWalletLabel(controller.getLocaliser().getString("showImportPrivateKeysPanel.secondPassword"));
        passwordPromptLabel2.setVisible(false);
        passwordPromptLabel2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 6;
        constraints.weightx = 0.3;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_END;
        passwordProtectPanel.add(passwordPromptLabel2, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 6;
        constraints.weightx = 0.05;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        passwordProtectPanel.add(FastcoinWalletTitledPanel.createStent(FastcoinWalletTitledPanel.SEPARATION_BETWEEN_NAME_VALUE_PAIRS),
                constraints);

        passwordField2 = new JPasswordField(24);
        passwordField2.setMinimumSize(new Dimension(200, 20));
        passwordField2.setVisible(false);
        passwordField2.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 6;
        constraints.weightx = 0.3;
        constraints.weighty = 0.6;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        passwordProtectPanel.add(passwordField2, constraints);

        JLabel filler3 = new JLabel();
        filler3.setMinimumSize(new Dimension(3, 3));
        filler3.setMaximumSize(new Dimension(3, 3));
        filler3.setPreferredSize(new Dimension(3, 3));
        filler3.setOpaque(false);

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 7;
        constraints.weightx = 0.1;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        passwordProtectPanel.add(filler3, constraints);

        unlockButton = new FastcoinWalletButton(controller.getLocaliser().getString("showImportPrivateKeysPanel.unlock.text"));
        unlockButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setMessageText1(" ");
                try {
                    readInImportFileAndUpdateDetails();
                } catch (KeyCrypterException ede) {
                    setMessageText1(controller.getLocaliser().getString("importPrivateKeysSubmitAction.privateKeysUnlockFailure",
                            new Object[] { ede.getMessage() }));
                }
            }
        });
        unlockButton.setToolTipText(HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("showImportPrivateKeysPanel.unlock.tooltip")));
        unlockButton.setEnabled(false);
        unlockButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 8;
        constraints.weightx = 0.3;
        constraints.weighty = 0.6;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.LINE_START;
        passwordProtectPanel.add(unlockButton, constraints);

        JPanel filler5 = new JPanel();
        filler5.setOpaque(false);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 1;
        constraints.gridy = 9;
        constraints.weightx = 0.3;
        constraints.weighty = 0.3;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        passwordProtectPanel.add(filler3, constraints);

        return passwordProtectPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEADING);
        buttonPanel.setLayout(flowLayout);
        buttonPanel.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));

        importPrivateKeysSubmitAction = new ImportPrivateKeysSubmitAction(this.fastcoinController, mainFrame, this,
                ImageLoader.createImageIcon(ImageLoader.IMPORT_PRIVATE_KEYS_ICON_FILE), walletPasswordField, passwordField1, passwordField2);
        FastcoinWalletButton submitButton = new FastcoinWalletButton(importPrivateKeysSubmitAction, controller);
        submitButton.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        buttonPanel.add(submitButton);

        return buttonPanel;
    }

    @Override
    public void displayView(DisplayHint displayHint) {
        // If it is a wallet transaction change no need to update.
        if (DisplayHint.WALLET_TRANSACTIONS_HAVE_CHANGED == displayHint) {
            return;
        }
        walletFilenameLabel.setText(this.fastcoinController.getModel().getActiveWalletFilename());
        walletDescriptionLabel.setText(this.fastcoinController.getModel().getActivePerWalletModelData().getWalletDescription());

        boolean walletPasswordRequired = false;
        if (this.fastcoinController.getModel().getActiveWallet() != null && this.fastcoinController.getModel().getActiveWallet().getEncryptionType() == EncryptionType.ENCRYPTED_SCRYPT_AES) {
            walletPasswordRequired = true;
        }
        enableWalletPassword(walletPasswordRequired);
        
        walletBusyChange(this.fastcoinController.getModel().getActivePerWalletModelData().isBusy());

        if (outputFilename == null || "".equals(outputFilename)) {
            outputFilenameLabel.setText(controller.getLocaliser().getString("showImportPrivateKeysPanel.noFileSelected"));
        }
        
        messageLabel1.setText(" ");
        messageLabel2.setText(" ");
    }

    @Override
    public void navigateAwayFromView() {
    }

    private void chooseFile(FastcoinWalletButton callingButton) {
        JFileChooser.setDefaultLocale(controller.getLocaliser().getLocale());
        fileChooser = new JFileChooser();
        fileChooser.setLocale(controller.getLocaliser().getLocale());
        fileChooser.setDialogTitle(chooseFilenameButtonText);
        adjustedFont = FontSizer.INSTANCE.getAdjustedDefaultFont();
        if (adjustedFont != null) {
            setFileChooserFont(new Container[] {fileChooser});
        }

        fileChooser.applyComponentOrientation(ComponentOrientation.getOrientation(controller.getLocaliser().getLocale()));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        fileChooser.addChoosableFileFilter(fastcoinWalletFileChooser);
        fileChooser.addChoosableFileFilter(myWalletPlainFileChooser);
        fileChooser.addChoosableFileFilter(myWalletEncryptedFileChooser);

        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setFileFilter(fastcoinWalletFileChooser);

        if (outputFilename != null && !"".equals(outputFilename)) {
            fileChooser.setCurrentDirectory(new File(outputFilename));
            fileChooser.setSelectedFile(new File(outputFilename));
        } else {
            if (this.fastcoinController.getModel().getActiveWalletFilename() != null) {
                fileChooser.setCurrentDirectory(new File(this.fastcoinController.getModel().getActiveWalletFilename()));
            }
            String defaultFileName = fileChooser.getCurrentDirectory().getAbsoluteFile() + File.separator
                    + controller.getLocaliser().getString("saveWalletAsView.untitled") + "."
                    + FastcoinModel.PRIVATE_KEY_FILE_EXTENSION;
            fileChooser.setSelectedFile(new File(defaultFileName));
        }

        try {
            callingButton.setEnabled(false);
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            fileChooser.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            int returnVal = fileChooser.showOpenDialog(mainFrame);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                numberOfKeysLabel.setText(" ");
                replayDateLabel.setText(" ");
                passwordField1.setText("");

                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    outputFilename = file.getAbsolutePath();
                    outputFilenameLabel.setText(outputFilename);

                    if (fastcoinWalletFileChooser.accept(file)) {
                        try {
                            String firstLine = readFirstLineInFile(file);

                            if (firstLine != null && firstLine.startsWith(encrypterDecrypter.getOpenSSLMagicText())) {
                                // File is encrypted.
                                enableImportFilePasswordPanel(true);
                                passwordField1.requestFocusInWindow();
                            } else {
                                // File is not encrypted.
                                enableImportFilePasswordPanel(false);
                                readInImportFileAndUpdateDetails();
                            }
                        } catch (IOException e) {
                            setMessageText1(controller.getLocaliser().getString(
                                    "importPrivateKeysSubmitAction.privateKeysImportFailure",
                                    new Object[] { e.getClass().getName() + " " + e.getMessage() }));
                        } catch (KeyCrypterException e) {
                            // TODO User may not have entered a password yet so
                            // password incorrect is ok at this stage.
                            // Other errors indicate a more general problem with
                            // the
                            // import.
                            setMessageText1(controller.getLocaliser().getString(
                                    "importPrivateKeysSubmitAction.privateKeysImportFailure",
                                    new Object[] { e.getClass().getName() + " " + e.getMessage() }));
                        }
                    } else if (myWalletEncryptedFileChooser.accept(file)) {
                        enableImportFilePasswordPanel(true);
                        passwordField1.requestFocusInWindow();
                    } else if (myWalletPlainFileChooser.accept(file)) {
                        // File is not encrypted.
                        enableImportFilePasswordPanel(false);
                        try {
                            readInImportFileAndUpdateDetails();
                        } catch (KeyCrypterException e) {
                            setMessageText1(controller.getLocaliser().getString(
                                    "importPrivateKeysSubmitAction.privateKeysImportFailure",
                                    new Object[] { e.getClass().getName() + " " + e.getMessage() }));
                        }
                    }
                }
            }
        } finally {
            mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            callingButton.setEnabled(true);
        }
    }

    private void enableSecondPasswordPanel(boolean enablePanel) {
        passwordField2.setEnabled(enablePanel);
        passwordPromptLabel2.setEnabled(enablePanel);
        passwordField2.setVisible(enablePanel);
        passwordPromptLabel2.setVisible(enablePanel);
    }

    private void enableImportFilePasswordPanel(boolean enableImportFilePanel) {
        if (enableImportFilePanel) {
            // Enable the import file password panel.
            passwordPromptLabel1.setEnabled(true);
            passwordField1.setEnabled(true);
            unlockButton.setEnabled(true);
            passwordInfoLabel.setForeground(Color.BLACK);
        } else {
            // Disable the import file password panel.
            passwordPromptLabel1.setEnabled(false);
            passwordField1.setEnabled(false);
            unlockButton.setEnabled(false);
            passwordInfoLabel.setForeground(Color.GRAY);
        }
    }

    private void enableWalletPassword(boolean enableWalletPassword) {
        if (enableWalletPassword) {
            // Enable the wallet password.
            walletPasswordField.setEnabled(true);
            walletPasswordPromptLabel.setEnabled(true);
        } else {
            // Disable the wallet password.
            walletPasswordField.setEnabled(false);
            walletPasswordPromptLabel.setEnabled(false);
        }
    }

    /**
     * Read in the import file and show the file details.
     * @throws EncrypterDecrypterException 
     * @throws PrivateKeysHandlerException 
     */
    private void readInImportFileAndUpdateDetails() throws PrivateKeysHandlerException, KeyCrypterException {
        // Update number of keys and earliest date.

        try {
            File file = new File(outputFilename);

            if (fastcoinWalletFileChooser.accept(file)) {
                // Read in contents of file.
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                PrivateKeysHandler privateKeysHandler = new PrivateKeysHandler(this.fastcoinController.getModel().getNetworkParameters());
                Collection<PrivateKeyAndDate> privateKeyAndDates = privateKeysHandler.readInPrivateKeys(new File(outputFilename),
                        CharBuffer.wrap(passwordField1.getPassword()));
                numberOfKeysLabel.setText("" + privateKeyAndDates.size());

                Date replayDate = privateKeysHandler.calculateReplayDate(privateKeyAndDates, this.fastcoinController.getModel()
                        .getActiveWallet());

                if (replayDate == null) {
                    replayDateLabel.setText(controller.getLocaliser().getString(
                            "showImportPrivateKeysPanel.thereWereMissingKeyDates"));
                } else {
                    replayDateLabel.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, controller.getLocaliser().getLocale())
                            .format(replayDate));
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else if (myWalletEncryptedFileChooser.accept(file)) {
                try {
                    String importFileContents = PrivateKeysHandler.readFile(file);

                    String mainPassword = new String(passwordField1.getPassword());
                    String secondPassword = new String(passwordField2.getPassword());

                    MyWallet wallet = new MyWallet(importFileContents, mainPassword);

                    boolean needSecondPassword = false;
                    if (wallet.isDoubleEncrypted()) {
                        if ("".equals(secondPassword)) {
                            needSecondPassword = true;
                            requestSecondPassword();
                        }
                    }

                    if (!needSecondPassword) {
                        wallet.setTemporySecondPassword(secondPassword);

                        int numberOfKeys = 0;
                        if (wallet.getFastcoinJWallet() != null && wallet.getFastcoinJWallet().getKeychain() != null) {
                            numberOfKeys = wallet.getFastcoinJWallet().getKeychainSize();
                        }
                        numberOfKeysLabel.setText("" + numberOfKeys);

                        replayDateLabel.setText(controller.getLocaliser().getString(
                                "showImportPrivateKeysPanel.thereWereMissingKeyDates"));

                    }
                } catch (Exception e) {
                    throw new KeyCrypterException("Error Decrypting MultiBitWallet");
                }
            } else if (myWalletPlainFileChooser.accept(file)) {
                try {
                    String importFileContents = PrivateKeysHandler.readFile(file);

                    MyWallet wallet = new MyWallet(importFileContents);

                    int numberOfKeys = 0;
                    if (wallet.getFastcoinJWallet() != null && wallet.getFastcoinJWallet().getKeychain() != null) {
                        numberOfKeys = wallet.getFastcoinJWallet().getKeychainSize();
                    }
                    numberOfKeysLabel.setText("" + numberOfKeys);

                    replayDateLabel.setText(controller.getLocaliser().getString(
                            "showImportPrivateKeysPanel.thereWereMissingKeyDates"));
                } catch (Exception e) {
                    throw new KeyCrypterException("Error Opening MultiBitWallet");
                }
            }
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    public void requestSecondPassword() {
        enableSecondPasswordPanel(true);
        setMessageText1(controller.getLocaliser().getString("importPrivateKeysSubmitAction.enterTheSecondPassword"));
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public void clearPasswords() {
        walletPasswordField.setText("");
        passwordField1.setText("");
        if (passwordField2 != null) {
            passwordField2.setText("");
        }
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

    private String readFirstLineInFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        return reader.readLine();
    }

    @Override
    public Icon getViewIcon() {
        return ImageLoader.createImageIcon(ImageLoader.IMPORT_PRIVATE_KEYS_ICON_FILE);
    }

    @Override
    public String getViewTitle() {
        return controller.getLocaliser().getString("showImportPrivateKeysAction.text");
    }

    @Override
    public String getViewTooltip() {
        return controller.getLocaliser().getString("showImportPrivateKeysAction.tooltip");
    }

    @Override
    public View getViewId() {
        return View.SHOW_IMPORT_PRIVATE_KEYS_VIEW;
    }
    
    // Used in testing.

    public FastcoinWalletButton getUnlockButton() {
        return unlockButton;
    }

    public ImportPrivateKeysSubmitAction getImportPrivateKeysSubmitAction() {
        return importPrivateKeysSubmitAction;
    }
    
    public void setOutputFilename(String outputFilename) {
        this.outputFilename = outputFilename;
    }
    
    public void setImportFilePassword(CharSequence password) {
        passwordField1.setText(password.toString());
    }
    
    public void setWalletPassword(CharSequence password) {
        walletPasswordField.setText(password.toString());
    }
    
    public boolean isWalletPasswordFieldEnabled() {
        return walletPasswordField.isEnabled();
    }

    @Override
    public void walletBusyChange(boolean newWalletIsBusy) {       
        // Update the enable status of the action to match the wallet busy status.
        if (this.fastcoinController.getModel().getActivePerWalletModelData().isBusy()) {
            // MultiBitWallet is busy with another operation that may change the private keys - Action is disabled.
            importPrivateKeysSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("fastcoinWalletSubmitAction.walletIsBusy",
                    new Object[]{controller.getLocaliser().getString(this.fastcoinController.getModel().getActivePerWalletModelData().getBusyTaskKey())})));
            importPrivateKeysSubmitAction.setEnabled(false);           
        } else {
            // Enable unless wallet has been modified by another process.
            if (!this.fastcoinController.getModel().getActivePerWalletModelData().isFilesHaveBeenChangedByAnotherProcess()) {
                importPrivateKeysSubmitAction.putValue(Action.SHORT_DESCRIPTION, HelpContentsPanel.createTooltipText(controller.getLocaliser().getString("importPrivateKeysSubmitAction.tooltip")));
                importPrivateKeysSubmitAction.setEnabled(true);
            }
        }
    }
    
    private void setFileChooserFont(Component[] comp) {
        for (int x = 0; x < comp.length; x++) {
            if (comp[x] instanceof Container)
                setFileChooserFont(((Container) comp[x]).getComponents());
            try {
                comp[x].setFont(adjustedFont);
            } catch (Exception e) {
            }// do nothing
        }
    }
}