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

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel csvPanel = new JPanel(new GridBagLayout());
        JPanel xmlPanel = new JPanel(new GridBagLayout());
        JPanel defaultPanel = new JPanel(new GridBagLayout());
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
                if (name.equals("FieldNames")) {
                    currentPanel = csvPanel;
                } else if (name.equals("RequestHeaders") || name.equals("SamplerData") ||
                           name.equals("ResponseHeaders") || name.equals("ResponseData") ||
                           name.equals("Subresults") || name.equals("Assertions") ||
                           name.equals("AssertionResultsFailureMessage") || name.equals("FileName") ||
                           name.equals("Hostname") || name.equals("Url")) {
                    currentPanel = xmlPanel;
                } else {
                    currentPanel = defaultPanel;
                }

                constraints.gridy++;
                if ("SamplerData".equals(name)) {
                    Box box = Box.createHorizontalBox();
                    box.add(check);
                    box.add(new JLabel(" (" + JMeterUtils.getResString("samplerdata_contains_cookies") + ")"));
                    currentPanel.add(box, constraints);
                } else {
                    currentPanel.add(check, constraints);
                }

                if ("AsXml".equals(name)) {
                    check.addActionListener(e -> {
                        enableTabs(tabbedPane, ((JCheckBox) e.getSource()).isSelected());
                    });
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.warn("Problem creating save config dialog", e);
            }
        }

        // Initial state
        try {
            enableTabs(tabbedPane, getSaveState(SampleSaveConfiguration.getterName("AsXml")));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.warn("Could not read initial state of AsXml checkbox", e);
        }

        // Add some space at the bottom
        constraints.gridy++;
        constraints.weighty = 1.0;
        defaultPanel.add(new JLabel(), constraints);
        csvPanel.add(new JLabel(), constraints);
        xmlPanel.add(new JLabel(), constraints);


        getContentPane().add(tabbedPane, BorderLayout.CENTER);
        JButton exit = new JButton(JMeterUtils.getResString("done")); // $NON-NLS-1$
        this.getContentPane().add(exit, BorderLayout.SOUTH);
        exit.addActionListener(e -> dispose());
    }

    private void enableTabs(JTabbedPane tabbedPane, boolean xmlSelected) {
        // CSV Tab is at index 1
        setEnabled(tabbedPane.getComponentAt(1), !xmlSelected);
        // XML Tab is at index 2
        setEnabled(tabbedPane.getComponentAt(2), xmlSelected);
    }

    private void setEnabled(Component component, boolean enabled) {
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
