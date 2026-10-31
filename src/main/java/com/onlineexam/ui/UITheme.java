package com.onlineexam.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Centralized Design System and UI Component Theme.
 * Provides curated color tokens, typography scales, table styling, and reusable Swing components.
 */
public final class UITheme {

    // Primary & Accent Colors
    public static final Color PRIMARY = new Color(24, 43, 73);
    public static final Color PRIMARY_HOVER = new Color(30, 58, 102);
    public static final Color ACCENT = new Color(79, 70, 229); // Indigo
    public static final Color ACCENT_HOVER = new Color(67, 56, 202);
    public static final Color ACCENT_LIGHT = new Color(238, 242, 255);

    // Functional State Colors
    public static final Color SUCCESS = new Color(16, 185, 129); // Emerald
    public static final Color SUCCESS_DARK = new Color(5, 150, 105);
    public static final Color SUCCESS_LIGHT = new Color(236, 253, 245);

    public static final Color DANGER = new Color(239, 68, 68); // Rose / Red
    public static final Color DANGER_DARK = new Color(220, 38, 38);
    public static final Color DANGER_LIGHT = new Color(254, 242, 242);

    public static final Color WARNING = new Color(245, 158, 11); // Amber
    public static final Color WARNING_DARK = new Color(217, 119, 6);
    public static final Color WARNING_LIGHT = new Color(254, 252, 232);

    public static final Color SECONDARY = new Color(100, 116, 139); // Slate
    public static final Color SECONDARY_LIGHT = new Color(241, 245, 249);

    // Background & Surface
    public static final Color BG_LIGHT = new Color(248, 249, 250);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(226, 232, 240);
    public static final Color BORDER_FOCUS = new Color(99, 102, 241);

    // Typography Colors
    public static final Color TEXT_MAIN = new Color(15, 23, 42);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);

    // Fonts
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_REGULAR = FONT_BODY;
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_DIGITAL = new Font("Consolas", Font.BOLD, 26);
    public static final Font FONT_QUESTION = new Font("Segoe UI", Font.PLAIN, 15);

    private UITheme() {}

    /**
     * Creates a styled modern button with mouse hover animation and rounded borders.
     */
    public static JButton createButton(String text, Color bg, Color fg, int fontSize, boolean bold) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, fontSize));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color hoverBg = bg.brighter();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(hoverBg);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(bg);
                }
            }
        });
        return button;
    }

    /**
     * Creates a card panel with white background and light slate border.
     */
    public static JPanel createCard() {
        return createCardPanel(new FlowLayout());
    }

    public static JPanel createCardPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));
        return panel;
    }

    /**
     * Creates a modern badge/pill.
     */
    public static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(bg);
        badge.setForeground(fg);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1, true),
                new EmptyBorder(3, 10, 3, 10)
        ));
        return badge;
    }

    /**
     * Creates a standardized header panel with title and subtitle.
     */
    public static JPanel createHeader(String title, String subtitle) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_TITLE_LARGE);
        titleLbl.setForeground(PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLbl = new JLabel(subtitle);
        subtitleLbl.setFont(FONT_BODY);
        subtitleLbl.setForeground(TEXT_MUTED);
        subtitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleLbl);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLbl);
        return header;
    }

    /**
     * Styles a JTable with modern font, row height, and sleek table header.
     */
    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setGridColor(BORDER_COLOR);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(ACCENT_LIGHT);
        table.setSelectionForeground(TEXT_MAIN);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(TEXT_MAIN);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer paddedRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, val, isSel, hasFocus, r, c);
                if (comp instanceof JLabel lbl) {
                    lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
                }
                return comp;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(paddedRenderer);
        }
    }

    /**
     * Formats seconds into MM:SS string.
     */
    public static String formatSeconds(long seconds) {
        long s = Math.max(0, seconds);
        long mins = s / 60;
        long secs = s % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
