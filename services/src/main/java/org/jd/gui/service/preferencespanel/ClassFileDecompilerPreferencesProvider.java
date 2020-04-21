/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.preferencespanel;

import org.jd.gui.spi.PreferencesPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class ClassFileDecompilerPreferencesProvider extends JPanel implements PreferencesPanel {
    protected static final String ESCAPE_UNICODE_CHARACTERS = "ClassFileDecompilerPreferences.escapeUnicodeCharacters";
    protected static final String REALIGN_LINE_NUMBERS = "ClassFileDecompilerPreferences.realignLineNumbers";

    protected PreferencesPanel.PreferencesPanelChangeListener listener = null;
    protected JCheckBox escapeUnicodeCharactersCheckBox;
    protected JCheckBox realignLineNumbersCheckBox;

    public ClassFileDecompilerPreferencesProvider() {
        super(new GridLayout(0,1));

        escapeUnicodeCharactersCheckBox = new JCheckBox("유니코드 문자 escape");
        realignLineNumbersCheckBox = new JCheckBox("열 번호 재정렬");

        add(escapeUnicodeCharactersCheckBox);
        add(realignLineNumbersCheckBox);
    }

    // --- PreferencesPanel --- //
    @Override public String getPreferencesGroupTitle() { return "디컴파일러"; }
    @Override public String getPreferencesPanelTitle() { return "클래스 파일"; }
    @Override public JComponent getPanel() { return this; }

    @Override public void init(Color errorBackgroundColor) {}

    @Override public boolean isActivated() { return true; }

    @Override
    public void loadPreferences(Map<String, String> preferences) {
        escapeUnicodeCharactersCheckBox.setSelected("true".equals(preferences.get(ESCAPE_UNICODE_CHARACTERS)));
        realignLineNumbersCheckBox.setSelected("true".equals(preferences.get(REALIGN_LINE_NUMBERS)));
    }

    @Override
    public void savePreferences(Map<String, String> preferences) {
        preferences.put(ESCAPE_UNICODE_CHARACTERS, Boolean.toString(escapeUnicodeCharactersCheckBox.isSelected()));
        preferences.put(REALIGN_LINE_NUMBERS, Boolean.toString(realignLineNumbersCheckBox.isSelected()));
    }

    @Override public boolean arePreferencesValid() { return true; }

    @Override public void addPreferencesChangeListener(PreferencesPanel.PreferencesPanelChangeListener listener) {}
}
