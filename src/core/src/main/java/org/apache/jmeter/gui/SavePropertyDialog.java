/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jmeter.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;

import org.apache.jmeter.gui.action.KeyStrokes;
import org.apache.jmeter.samplers.SampleSaveConfiguration;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.reflect.Functor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Configure pop-up dialogue for Listeners from all methods in SampleSaveConfiguration
 * with the signature "boolean saveXXX()".
 * There must be a corresponding "void setXXX(boolean)" method, and a property save_XXX which is
 * used to name the field on the dialogue.
 *
 */
public class SavePropertyDialog extends JDialog implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(SavePropertyDialog.class);

    private static final long serialVersionUID = 233L;

    private static final Map<String, Functor> functors = new HashMap<>();

    private static final String RESOURCE_PREFIX = "save_"; // $NON-NLS-1$ e.g. save_XXX property

    private SampleSaveConfiguration saveConfig;

    /**
     * @deprecated Constructor only intended for use in testing
     */
    @Deprecated // Constructor only intended for use in testing
    public SavePropertyDialog() {
        log.warn("Constructor only intended for use in testing"); // $NON-NLS-1$
    }
    /**
     * @param owner The {@link Frame} from which the dialog is displayed
     * @param title The string to be used as a title of this dialog
     * @param modal specifies whether the dialog should be modal
     * @param s The details, which sample attributes are to be saved
     * @throws java.awt.HeadlessException - when run headless
     */
    public SavePropertyDialog(Frame owner, String title, boolean modal, SampleSaveConfiguration s)
    {
        super(owner, title, modal);
        saveConfig = s;
        log.debug("SampleSaveConfiguration = {}", saveConfig);// $NON-NLS-1$
        initDialog();
    }

    private void initDialog() {
        this.getContentPane().setLayout(new BorderLayout());

        JCheckBox asXml = null;
        try {
            asXml = new JCheckBox(JMeterUtils.getResString(RESOURCE_PREFIX + "AsXml"), getSaveState(SampleSaveConfiguration.getterName("AsXml")));
            asXml.addActionListener(this);
            final String actionCommand = SampleSaveConfiguration.setterName("AsXml");
            asXml.setActionCommand(actionCommand);
            if (!functors.containsKey(actionCommand)) {
                functors.put(actionCommand, new Functor(actionCommand));
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.warn("Problem creating 'Save as XML' checkbox", e);
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        if (asXml != null) {
            mainPanel.add(asXml, BorderLayout.NORTH);
        }

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel csvPanel = new JPanel(new GridBagLayout());
        JPanel xmlPanel = new JPanel(new GridBagLayout());

        JPanel defaultPanel = new JPanel(new BorderLayout());

        JPanel samplerPanel = new JPanel(new GridBagLayout());
        samplerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sampler Settings"));
        JPanel dataPanel = new JPanel(new GridBagLayout());
        dataPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Data Settings"));
        JPanel networkPanel = new JPanel(new GridBagLayout());
        networkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Network Settings"));
        JPanel timePanel = new JPanel(new GridBagLayout());
        timePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Time Settings"));
        JPanel assertionsPanel = new JPanel(new GridBagLayout());
        assertionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Assertions Settings"));

        Box defaultBox = Box.createVerticalBox();
        defaultBox.add(samplerPanel);
        defaultBox.add(dataPanel);
        defaultBox.add(networkPanel);
        defaultBox.add(timePanel);
        defaultBox.add(assertionsPanel);

        defaultPanel.add(defaultBox, BorderLayout.NORTH);

        tabbedPane.add("Default", defaultPanel);
        tabbedPane.add("CSV", csvPanel);
        tabbedPane.add("XML", xmlPanel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;

        for (final String name : SampleSaveConfiguration.SAVE_CONFIG_NAMES) {
            if ("AsXml".equals(name)) {
                continue;
            }
            try {
                JCheckBox check = new JCheckBox(
                        JMeterUtils.getResString(RESOURCE_PREFIX + name),
                        getSaveState(SampleSaveConfiguration.getterName(name)));
                check.addActionListener(this);
                final String actionCommand = SampleSaveConfiguration.setterName(name); // $NON-NLS-1$
                check.setActionCommand(actionCommand);
                if (!functors.containsKey(actionCommand)) {
                    functors.put(actionCommand, new Functor(actionCommand));
                }

                JPanel currentPanel;
                String label = JMeterUtils.getResString(RESOURCE_PREFIX + name);
                if (label.endsWith("(CSV)")) {
                    currentPanel = csvPanel;
                } else if (label.endsWith("(XML)")) {
                    currentPanel = xmlPanel;
                } else {
                    switch (name) {
                        case "Label":
                        case "ThreadName":
                            currentPanel = samplerPanel;
                            break;
                        case "Bytes":
                        case "SentBytes":
                        case "DataType":
                        case "Encoding":
                        case "Code":
                        case "Message":
                        case "FileName":
                            currentPanel = dataPanel;
                            break;
                        case "Hostname":
                        case "Url":
                            currentPanel = networkPanel;
                            break;
                        case "Timestamp":
                        case "IdleTime":
                        case "Time":
                        case "ConnectTime":
                        case "Latency":
                            currentPanel = timePanel;
                            break;
                        case "Success":
                        case "SampleCount":
                        case "Subresults":
                            currentPanel = assertionsPanel;
                            break;
                        default:
                            currentPanel = defaultPanel;
                            break;
                    }
                }

                constraints.gridy++;
                if ("SamplerData".equals(name)) {
                    JPanel samplerDataPanel = new JPanel();
                    samplerDataPanel.setLayout(new BorderLayout());
                    samplerDataPanel.add(check, BorderLayout.NORTH);
                    JLabel explanation = new JLabel(" (" + JMeterUtils.getResString("samplerdata_contains_cookies") + ")");
                    explanation.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
                    samplerDataPanel.add(explanation, BorderLayout.SOUTH);
                    currentPanel.add(samplerDataPanel, constraints);
                } else {
                    currentPanel.add(check, constraints);
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.warn("Problem creating save config dialog", e);
            }
        }

        if (asXml != null) {
            final JCheckBox finalAsXml = asXml;
            asXml.addActionListener(e -> {
                enableTabs(tabbedPane, finalAsXml.isSelected());
            });
            enableTabs(tabbedPane, asXml.isSelected());
        }


        // Add some space at the bottom
        constraints.gridy++;
        constraints.weighty = 1.0;
        defaultPanel.add(new JLabel(), constraints);
        csvPanel.add(new JLabel(), constraints);
        xmlPanel.add(new JLabel(), constraints);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        JButton exit = new JButton(JMeterUtils.getResString("done")); // $NON-NLS-1$
        this.getContentPane().add(exit, BorderLayout.SOUTH);
        exit.addActionListener(e -> dispose());
    }

    private static void enableTabs(JTabbedPane tabbedPane, boolean xmlSelected) {
        // CSV Tab is at index 1
        setEnabled(tabbedPane.getComponentAt(1), !xmlSelected);
        // XML Tab is at index 2
        setEnabled(tabbedPane.getComponentAt(2), xmlSelected);
    }

    private static void setEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setEnabled(child, enabled);
            }
        }
    }

    @Override
    protected JRootPane createRootPane() {
        JRootPane rootPane = new JRootPane();
        Action escapeAction = new AbstractAction("ESCAPE") {
            private static final long serialVersionUID = 2208129319916921772L;

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStrokes.ESC, escapeAction.getValue(Action.NAME));
        rootPane.getActionMap().put(escapeAction.getValue(Action.NAME), escapeAction);
        return rootPane;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String action = e.getActionCommand();
        Functor f = functors.get(action);
        f.invoke(saveConfig, new Object[] {((JCheckBox) e.getSource()).isSelected()});
    }

    private boolean getSaveState(String methodName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method = SampleSaveConfiguration.class.getMethod(methodName);
        return (Boolean) method.invoke(saveConfig);
    }

    /**
     * @return Returns the saveConfig.
     */
    public SampleSaveConfiguration getSaveConfig() {
        return saveConfig;
    }

    /**
     * @param saveConfig
     *            The saveConfig to set.
     */
    public void setSaveConfig(SampleSaveConfiguration saveConfig) {
        this.saveConfig = saveConfig;
    }
}
