/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.view;

import org.jd.gui.Constants;
import org.jd.gui.api.API;
import org.jd.gui.api.feature.*;
import org.jd.gui.model.configuration.Configuration;
import org.jd.gui.model.history.History;
import org.jd.gui.service.platform.PlatformService;
import org.jd.gui.util.exception.ExceptionUtil;
import org.jd.gui.view.component.IconButton;
import org.jd.gui.view.component.panel.MainTabbedPanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.jd.gui.util.swing.SwingUtil.*;

@SuppressWarnings("unchecked")
public class MainView<T extends JComponent & UriGettable> implements UriOpenable, PreferencesChangeListener {
    protected History history;
    protected Consumer<File> openFilesCallback;
    protected JFrame mainFrame;
    protected JMenu recentFiles = new JMenu("최근 파일 목록");
    protected Action closeAction;
    protected Action openTypeAction;
    protected Action backwardAction;
    protected Action forwardAction;
    protected MainTabbedPanel mainTabbedPanel;
    protected Box findPanel;
    protected JComboBox findComboBox;
    protected JCheckBox findCaseSensitive;
    protected Color findBackgroundColor;
    protected Color findErrorBackgroundColor;

    public MainView(
            Configuration configuration, API api, History history,
            ActionListener openActionListener,
            ActionListener closeActionListener,
            ActionListener saveActionListener,
            ActionListener saveAllSourcesActionListener,
            ActionListener exitActionListener,
            ActionListener copyActionListener,
            ActionListener pasteActionListener,
            ActionListener selectAllActionListener,
            ActionListener findActionListener,
            ActionListener findPreviousActionListener,
            ActionListener findNextActionListener,
            ActionListener findCaseSensitiveActionListener,
            Runnable findCriteriaChangedCallback,
            ActionListener openTypeActionListener,
            ActionListener openTypeHierarchyActionListener,
            ActionListener goToActionListener,
            ActionListener backwardActionListener,
            ActionListener forwardActionListener,
            ActionListener searchActionListener,
            ActionListener jdWebSiteActionListener,
            ActionListener jdGuiIssuesActionListener,
            ActionListener jdCoreIssuesActionListener,
            ActionListener preferencesActionListener,
            ActionListener aboutActionListener,
            Runnable panelClosedCallback,
            Consumer<T> currentPageChangedCallback,
            Consumer<File> openFilesCallback) {
        this.history = history;
        this.openFilesCallback = openFilesCallback;
        // Build GUI
        invokeLater(() -> {
            mainFrame = new JFrame("Java 디컴파일러");
            mainFrame.setIconImages(Arrays.asList(getImage("/org/jd/gui/images/jd_icon_32.png"), getImage("/org/jd/gui/images/jd_icon_64.png"), getImage("/org/jd/gui/images/jd_icon_128.png")));
            mainFrame.setMinimumSize(new Dimension(Constants.MINIMAL_WIDTH, Constants.MINIMAL_HEIGHT));
            mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Find panel //
            Action findNextAction = newAction("Next", newImageIcon("/org/jd/gui/images/next_nav.png"), true, findNextActionListener);
            findPanel = Box.createHorizontalBox();
            findPanel.setVisible(false);
            findPanel.add(new JLabel("검색: "));
            findComboBox = new JComboBox();
            findComboBox.setEditable(true);
            JComponent editorComponent = (JComponent)findComboBox.getEditor().getEditorComponent();
            editorComponent.addKeyListener(new KeyAdapter() {
                protected String lastStr = "";

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_ESCAPE:
                            findPanel.setVisible(false);
                            break;
                        case KeyEvent.VK_ENTER:
                            String str = getFindText();
                            if (str.length() > 1) {
                                int index = ((DefaultComboBoxModel)findComboBox.getModel()).getIndexOf(str);
                                if(index != -1 ) {
                                    findComboBox.removeItemAt(index);
                                }
                                findComboBox.insertItemAt(str, 0);
                                findComboBox.setSelectedIndex(0);
                                findNextAction.actionPerformed(null);
                            }
                            break;
                        default:
                            str = getFindText();
                            if (! lastStr.equals(str)) {
                                findCriteriaChangedCallback.run();
                                lastStr = str;
                            }
                    }
                }
            });
            editorComponent.setOpaque(true);
            findComboBox.setBackground(this.findBackgroundColor = editorComponent.getBackground());
            this.findErrorBackgroundColor = Color.decode(configuration.getPreferences().get("JdGuiPreferences.errorBackgroundColor"));

            findPanel.add(findComboBox);
            findPanel.add(Box.createHorizontalStrut(5));
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.setRollover(true);

            IconButton findNextButton = new IconButton("뒤로", newAction(newImageIcon("/org/jd/gui/images/next_nav.png"), true, findNextActionListener));
            toolBar.add(findNextButton);

            toolBar.add(Box.createHorizontalStrut(5));

            IconButton findPreviousButton = new IconButton("앞으로", newAction(newImageIcon("/org/jd/gui/images/prev_nav.png"), true, findPreviousActionListener));
            toolBar.add(findPreviousButton);

            findPanel.add(toolBar);
            findCaseSensitive = new JCheckBox();
            findCaseSensitive.setAction(newAction("대소문자 구분", true, findCaseSensitiveActionListener));
            findPanel.add(findCaseSensitive);
            findPanel.add(Box.createHorizontalGlue());

            IconButton findCloseButton = new IconButton(newAction(null, null, true, e -> findPanel.setVisible(false)));
            findCloseButton.setContentAreaFilled(false);
            findCloseButton.setIcon(newImageIcon("/org/jd/gui/images/close.gif"));
            findCloseButton.setRolloverIcon(newImageIcon("/org/jd/gui/images/close_active.gif"));
            findPanel.add(findCloseButton);

            if (PlatformService.getInstance().isMac()) {
                findPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                Border border = BorderFactory.createEmptyBorder();
                findNextButton.setBorder(border);
                findPreviousButton.setBorder(border);
                findCloseButton.setBorder(border);
            } else {
                findPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 2));
            }

            // Actions //
            boolean browser = Desktop.isDesktopSupported() ? Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) : false;
            Action openAction = newAction("파일 열기...", newImageIcon("/org/jd/gui/images/open.png"), true, "파일 열기", openActionListener);
            closeAction = newAction("닫기", false, closeActionListener);
            Action saveAction = newAction("저장", newImageIcon("/org/jd/gui/images/save.png"), false, saveActionListener);
            Action saveAllSourcesAction = newAction("전체 소스 저장", newImageIcon("/org/jd/gui/images/save_all.png"), false, saveAllSourcesActionListener);
            Action exitAction = newAction("나가기", true, "이 프로그램 나가기", exitActionListener);
            Action copyAction = newAction("복사", newImageIcon("/org/jd/gui/images/copy.png"), false, copyActionListener);
            Action pasteAction = newAction("붙여넣기", newImageIcon("/org/jd/gui/images/paste.png"), true, pasteActionListener);
            Action selectAllAction = newAction("전체 선택", false, selectAllActionListener);
            Action findAction = newAction("검색...", false, findActionListener);
            openTypeAction = newAction("클래스 찾기...", newImageIcon("/org/jd/gui/images/open_type.png"), false, openTypeActionListener);
            Action openTypeHierarchyAction = newAction("상속 계층 확인...", false, openTypeHierarchyActionListener);
            Action goToAction = newAction("열 이동...", false, goToActionListener);
            backwardAction = newAction("뒤로", newImageIcon("/org/jd/gui/images/backward_nav.png"), false, backwardActionListener);
            forwardAction = newAction("앞으로", newImageIcon("/org/jd/gui/images/forward_nav.png"), false, forwardActionListener);
            Action searchAction = newAction("찾기...", newImageIcon("/org/jd/gui/images/search_src.png"), false, searchActionListener);
            Action jdWebSiteAction = newAction("JD 웹사이트", browser, "JD 웹사이트 열기", jdWebSiteActionListener);
            Action jdGuiIssuesActionAction = newAction("JD-GUI 이슈", browser, "JD-GUI 이슈 페이지 열기", jdGuiIssuesActionListener);
            Action jdCoreIssuesActionAction = newAction("JD-Core 이슈", browser, "JD-Core 이슈 페이지 열기", jdCoreIssuesActionListener);
            Action preferencesAction = newAction("설정...", newImageIcon("/org/jd/gui/images/preferences.png"), true, "설정 패널 열기", preferencesActionListener);
            Action aboutAction = newAction("정보", true, "JD-GUI의 정보를 확인합니다.", aboutActionListener);

            // Menu //
            int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("파일");
            menuBar.add(menu);
            menu.add(openAction).setAccelerator(KeyStroke.getKeyStroke('O', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(closeAction).setAccelerator(KeyStroke.getKeyStroke('W', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(saveAction).setAccelerator(KeyStroke.getKeyStroke('S', menuShortcutKeyMask));
            menu.add(saveAllSourcesAction).setAccelerator(KeyStroke.getKeyStroke('S', menuShortcutKeyMask|InputEvent.ALT_MASK));
            menu.addSeparator();
            menu.add(recentFiles);
            if (!PlatformService.getInstance().isMac()) {
                menu.addSeparator();
                menu.add(exitAction).setAccelerator(KeyStroke.getKeyStroke('X', InputEvent.ALT_MASK));
            }
            menu = new JMenu("편집");
            menuBar.add(menu);
            menu.add(copyAction).setAccelerator(KeyStroke.getKeyStroke('C', menuShortcutKeyMask));
            menu.add(pasteAction).setAccelerator(KeyStroke.getKeyStroke('V', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(selectAllAction).setAccelerator(KeyStroke.getKeyStroke('A', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(findAction).setAccelerator(KeyStroke.getKeyStroke('F', menuShortcutKeyMask));
            menu = new JMenu("탐색");
            menuBar.add(menu);
            menu.add(openTypeAction).setAccelerator(KeyStroke.getKeyStroke('T', menuShortcutKeyMask));
            menu.add(openTypeHierarchyAction).setAccelerator(KeyStroke.getKeyStroke('H', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(goToAction).setAccelerator(KeyStroke.getKeyStroke('L', menuShortcutKeyMask));
            menu.addSeparator();
            menu.add(backwardAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.ALT_MASK));
            menu.add(forwardAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, InputEvent.ALT_MASK));
            menu = new JMenu("찾기");
            menuBar.add(menu);
            menu.add(searchAction).setAccelerator(KeyStroke.getKeyStroke('S', menuShortcutKeyMask|InputEvent.SHIFT_MASK));
            menu = new JMenu("도움말");
            menuBar.add(menu);
            if (browser) {
                menu.add(jdWebSiteAction);
                menu.add(jdGuiIssuesActionAction);
                menu.add(jdCoreIssuesActionAction);
                menu.addSeparator();
            }
            menu.add(preferencesAction).setAccelerator(KeyStroke.getKeyStroke('P', menuShortcutKeyMask|InputEvent.SHIFT_MASK));
            if (!PlatformService.getInstance().isMac()) {
                menu.addSeparator();
                menu.add(aboutAction).setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
            }
            mainFrame.setJMenuBar(menuBar);

            // Icon bar //
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            toolBar = new JToolBar();
            toolBar.setFloatable(false);
            toolBar.setRollover(true);
            toolBar.add(new IconButton(openAction));
            toolBar.addSeparator();
            toolBar.add(new IconButton(openTypeAction));
            toolBar.add(new IconButton(searchAction));
            toolBar.addSeparator();
            toolBar.add(new IconButton(backwardAction));
            toolBar.add(new IconButton(forwardAction));
            panel.add(toolBar, BorderLayout.PAGE_START);

            mainTabbedPanel = new MainTabbedPanel(api);
            mainTabbedPanel.getPageChangedListeners().add(new PageChangeListener() {
                protected JComponent currentPage = null;

                @Override public <U extends JComponent & UriGettable> void pageChanged(U page) {
                    if (currentPage != page) {
                        // Update current page
                        currentPage = page;
                        currentPageChangedCallback.accept((T)page);

                        invokeLater(() -> {
                            if (page == null) {
                                // Update title
                                mainFrame.setTitle("Java 디컴파일러");
                                // Update menu
                                saveAction.setEnabled(false);
                                copyAction.setEnabled(false);
                                selectAllAction.setEnabled(false);
                                openTypeHierarchyAction.setEnabled(false);
                                goToAction.setEnabled(false);
                                // Update find panel
                                findPanel.setVisible(false);
                            } else {
                                // Update title
                                String path = page.getUri().getPath();
                                int index = path.lastIndexOf('/');
                                String name = (index == -1) ? path : path.substring(index + 1);
                                mainFrame.setTitle((name != null) ? name + " - Java 디컴파일러" : "Java 디컴파일러");
                                // Update history
                                history.add(page.getUri());
                                // Update history actions
                                updateHistoryActions();
                                // Update menu
                                saveAction.setEnabled(page instanceof ContentSavable);
                                copyAction.setEnabled(page instanceof ContentCopyable);
                                selectAllAction.setEnabled(page instanceof ContentSelectable);
                                findAction.setEnabled(page instanceof ContentSearchable);
                                openTypeHierarchyAction.setEnabled(page instanceof FocusedTypeGettable);
                                goToAction.setEnabled(page instanceof LineNumberNavigable);
                                // Update find panel
                                if (findPanel.isVisible()) {
                                    findPanel.setVisible(page instanceof ContentSearchable);
                                }
                            }
                        });
                    }
                }
            });
            mainTabbedPanel.getTabbedPane().addChangeListener(new ChangeListener() {
                protected int lastTabCount = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    int tabCount = mainTabbedPanel.getTabbedPane().getTabCount();
                    boolean enabled = (tabCount > 0);

                    closeAction.setEnabled(enabled);
                    openTypeAction.setEnabled(enabled);
                    searchAction.setEnabled(enabled);
                    saveAllSourcesAction.setEnabled((mainTabbedPanel.getTabbedPane().getSelectedComponent() instanceof SourcesSavable));

                    if (tabCount < lastTabCount) {
                        panelClosedCallback.run();
                    }

                    lastTabCount = tabCount;
                }
            });
            mainTabbedPanel.preferencesChanged(configuration.getPreferences());
            panel.add(mainTabbedPanel, BorderLayout.CENTER);

            panel.add(findPanel, BorderLayout.PAGE_END);
            mainFrame.add(panel);
        });
    }

    public void show(Point location, Dimension size, boolean maximize) {
        invokeLater(() -> {
            // Set position, resize and show
            mainFrame.setLocation(location);
            mainFrame.setSize(size);
            mainFrame.setExtendedState(maximize ? JFrame.MAXIMIZED_BOTH : 0);
            mainFrame.setVisible(true);
        });
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public void showFindPanel() {
        invokeLater(() -> {
            findPanel.setVisible(true);
            findComboBox.requestFocus();
        });
    }

    public void setFindBackgroundColor(boolean wasFound) {
        invokeLater(() -> {
            findComboBox.getEditor().getEditorComponent().setBackground(wasFound ? findBackgroundColor : findErrorBackgroundColor);
        });
    }

    public <T extends JComponent & UriGettable> void addMainPanel(String title, Icon icon, String tip, T component) {
        invokeLater(() -> {
            mainTabbedPanel.addPage(title, icon, tip, component);
        });
    }

    public <T extends JComponent & UriGettable> List<T> getMainPanels() {
        return mainTabbedPanel.getPages();
    }

    public <T extends JComponent & UriGettable> T getSelectedMainPanel() {
        return (T)mainTabbedPanel.getTabbedPane().getSelectedComponent();
    }

    public void closeCurrentTab() {
        invokeLater(() -> {
            Component component = mainTabbedPanel.getTabbedPane().getSelectedComponent();
            if (component instanceof PageClosable) {
                if (!((PageClosable)component).closePage()) {
                    mainTabbedPanel.removeComponent(component);
                }
            } else {
                mainTabbedPanel.removeComponent(component);
            }
        });
    }

    public void updateRecentFilesMenu(List<File> files) {
        invokeLater(() -> {
            recentFiles.removeAll();

            for (File file : files) {
                JMenuItem menuItem = new JMenuItem(reduceRecentFilePath(file.getAbsolutePath()));
                menuItem.addActionListener(e -> openFilesCallback.accept(file));
                recentFiles.add(menuItem);
            }
        });
    }

    public String getFindText() {
        Document doc = ((JTextField)findComboBox.getEditor().getEditorComponent()).getDocument();

        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            assert ExceptionUtil.printStackTrace(e);
            return "";
        }
    }

    public boolean getFindCaseSensitive() { return findCaseSensitive.isSelected(); }

    public void updateHistoryActions() {
        invokeLater(() -> {
            backwardAction.setEnabled(history.canBackward());
            forwardAction.setEnabled(history.canForward());
        });
    }

    // --- Utils --- //
    static String reduceRecentFilePath(String path) {
        int lastSeparatorPosition = path.lastIndexOf(File.separatorChar);

        if ((lastSeparatorPosition == -1) || (lastSeparatorPosition < Constants.RECENT_FILE_MAX_LENGTH)) {
            return path;
        }

        int length = Constants.RECENT_FILE_MAX_LENGTH/2 - 2;
        String left = path.substring(0, length);
        String right = path.substring(path.length() - length);

        return left + "..." + right;
    }

    // --- URIOpener --- //
    @Override
    public boolean openUri(URI uri) {
        boolean success = mainTabbedPanel.openUri(uri);

        if (success) {
            closeAction.setEnabled(true);
            openTypeAction.setEnabled(true);
        }

        return success;
    }

    // --- PreferencesChangeListener --- //
    @Override
    public void preferencesChanged(Map<String, String> preferences) {
        mainTabbedPanel.preferencesChanged(preferences);
    }
}
